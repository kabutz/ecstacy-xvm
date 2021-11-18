import ecstasy.iterators.CompoundIterator;

import ecstasy.collections.maps.EntryKeys;
import ecstasy.collections.maps.EntryValues;

import ecstasy.collections.Hasher;
import ecstasy.collections.HashMap;
import ecstasy.collections.ImmutableAble;
import ecstasy.collections.NaturalHasher;

import ecstasy.collections.maps.KeyEntry;

/**
 * A hash based map which allows for parallel and concurrent access with scalable performance.
 *
 * Parallelism is provided by partitioning the keys into a number of inner [HashMap] based partition.
 * Each partition can be independently accessed without contention.
 *
 * Concurrency is provided within a partition down to the key level, that is if an operation on one
 * key within a partition blocks it will not prevent reads or writes to other keys in the same
 * partition. Furthermore blocking writes such as by [#process] on a key will not block concurrent
 * reads of that same key. Writes to any given key are ordered.
 */
//@Concurrent // TODO: GG marking the const as @Concurrent this causes an IllegalStateException
// TODO: GG, if this is a service rather then a const Maps.equals throws an IllegalArgument complaining
//       about a mutable being used for a service call, I don't see what that mutable is
const ConcurrentHashMap<Key extends immutable Object, Value extends ImmutableAble>
        implements Map<Key, Value>
    {
    // ----- constructors --------------------------------------------------------------------------

    /**
     * Construct a new [ConcurrentHashMap].
     *
     * @param parallelism  the target parallelism to optimize for
     * @param initCapacity  the number of expected entries
     */
    construct(Int parallelism = 16, Int initCapacity = 0)
        {
        assert(Key.is(Type<Hashable>));
        construct ConcurrentHashMap(new NaturalHasher<Key>(), parallelism, initCapacity);
        }

    /**
     * Construct a new [ConcurrentHashMap].
     *
     * @param parallelism  the target parallelism to optimize for
     * @param initCapacity  the number of expected entries
     */
    construct(Hasher<Key> hasher, Int parallelism = 16, Int initCapacity = 0)
        {
        assert parallelism > 0;
        assert initCapacity >= 0;
        this.hasher = hasher;

        // select a prime partition count greater then the requested concurrency
        Int partCount = parallelism;
        Int capacity = initCapacity / parallelism;
        Int buckets = Partition.calcBucketCount(capacity);
        if (parallelism == 1)
            {
            // user asked for it, allow it; there is still value here as compared to a simple
            // service wrapper around HashMap because we still offer key-level concurrency even it
            // not parallelism
            }
        else if (parallelism <= Partition.PRIMES[0])
            {
            // Explicitly avoid the first prime as our underlying HashMaps will have this as their
            // starting bucket count. See Partition.selectBucketCount for how this is resolved as
            // the HashMaps grow
            partCount = Partition.PRIMES[1];
            if (partCount == buckets)
                {
                partCount = Partition.PRIMES[2];
                }
            }
        else
            {
            for (Int p : Partition.PRIMES)
                {
                partCount = p;
                if (partCount >= parallelism && p != buckets)
                    {
                    break;
                    }
                }
            }

        partitions = new Array(partCount, i -> new Partition<Key, Value>(hasher, partCount, capacity));
        }


    // ----- properties ----------------------------------------------------------------------------

    /**
     * The partitions backing this map.
     */
    private Partition<Key, Value>[] partitions;

    /**
     * The key hasher.
     */
    private Hasher<Key> hasher;

    /**
     * The random number generator.
     * TODO: replace with something equivalent of Java's ThreadLocalRandom
     */
     @Inject Random rnd;


    // ----- Map interface -------------------------------------------------------------------------

    @Override
    @Lazy public/private Set<Key> keys.calc()
        {
        return new EntryKeys(this);
        }

    @Override
    @Lazy public/private Collection<Value> values.calc()
        {
        return new EntryValues(this);
        }

    @Override
    @Lazy public/private Collection<Map.Entry> entries.calc()
        {
        return new Entries();
        }

    @Override
    @RO Int size.get()
        {
        Int sum = 0;
        Int step = computeRandomStep();
        Int first = step % partitions.size;
        Int i = first;
        do
            {
            sum += partitions[i].size;
            i = (i + step) % partitions.size;
            }
        while (i != first);

        return sum;
        }

    @Override
    @RO Boolean empty.get()
        {
        Int step = computeRandomStep();
        Int first = step % partitions.size;
        Int i = first;
        do
            {
            if (!partitions[i].empty)
                {
                return False;
                }

            i = (i + step) % partitions.size;
            }
        while (i != first);

        return True;
        }

    @Override
    conditional Value get(Key key)
        {
        return partitionOf(key).get(key);
        }

    @Override
    Boolean contains(Key key)
        {
        return partitionOf(key).contains(key);
        }

    @Override
    ConcurrentHashMap put(Key key, Value value)
        {
        partitionOf(key).putOrdered(key, value);
        return this;
        }

    @Override
    conditional ConcurrentHashMap putIfAbsent(Key key, Value value)
        {
        return partitionOf(key).putIfAbsentOrdered(key, value)
            ? (True, this) : False;
        }

    @Override
    conditional ConcurrentHashMap replace(Key key, Value valueOld, Value valueNew)
        {
        return partitionOf(key).replaceOrdered(key, valueOld, valueNew)
            ? (True, this) : False;
        }

    @Override
    ConcurrentHashMap remove(Key key)
        {
        partitionOf(key).removeOrdered(key);
        return this;
        }

    @Override
    conditional ConcurrentHashMap remove(Key key, Value value)
        {
        return partitionOf(key).removeOrdered(key, value)
            ? (True, this) : False;
        }

    @Override
    ConcurrentHashMap clear()
        {
        Int step = computeRandomStep();
        Int first = step % partitions.size;
        Int i = first;
        do
            {
            partitions[i].clearOrdered();
            i = (i + step) % partitions.size;
            }
        while (i != first);

        return this;
        }

    @Override
    <Result> Result process(Key key, function Result(Map<Key, Value>.Entry) compute)
        {
        Result result = partitionOf(key).process^(key, compute);
        return &result;
        }

    @Override
    <Result> conditional Result processIfPresent(Key key,
            function Result(Map<Key, Value>.Entry) compute)
        {
        return partitionOf(key).processIfPresent(key, compute);
        }

    @Override
    (Value, Boolean) computeIfAbsent(Key key, function Value() compute)
        {
        return partitionOf(key).computeIfAbsent(key, compute);
        }


    // ----- helpers -------------------------------------------------------------------------------

    /**
     * Compute the partition which owns the specified key.
     */
    protected Partition<Key, Value> partitionOf(Key key)
        {
        return partitions[hasher.hashOf(key) % partitions.size];
        }

    /**
     * Compute a random prime value to use perform a "random walk" over the partitions.
     * This approach helps to reduce contention when multiple services iterate ove the
     * map a the same time.
     *
     * @return the step
     */
    protected Int computeRandomStep()
        {
        Int step = Partition.PRIMES[rnd.int(Partition.PRIMES.size)];
        return step == partitions.size ? 1 : step;
        }


    // ----- Entries implementation ----------------------------------------------------------------

    /**
     * A collection of the map's entries, backed by the map.
     */
    protected const Entries
            implements Collection<Map.Entry>
        {
        @Override
        @RO Int size.get()
            {
            return this.ConcurrentHashMap.size;
            }

        @Override
        @RO Boolean empty.get()
            {
            return this.ConcurrentHashMap.empty;
            }

        @Override
        Boolean contains(Map.Entry entry)
            {
            if (Value value := this.ConcurrentHashMap.get(entry.key))
                {
                return value == entry.value;
                }

            return False;
            }

        @Override
        Boolean containsAll(Collection!<Map.Entry> that)
            {
            for (Map.Entry entry : that)
                {
                if (!contains(entry))
                    {
                    return False;
                    }
                }

            return True;
            }

        @Override
        @Op("+")
        Entries add(Map.Entry entry)
            {
            this.ConcurrentHashMap.put(entry.key, entry.value);
            return this;
            }

        @Override
        @Op("+")
        Entries addAll(Iterable<Map.Entry> that)
            {
            for (Map.Entry entry : that)
                {
                this.ConcurrentHashMap.put(entry.key, entry.value);
                }

            return this;
            }

        @Override
        Entries addAll(Iterator<Map.Entry> iter)
            {
            while (Map.Entry entry := iter.next())
                {
                this.ConcurrentHashMap.put(entry.key, entry.value);
                }

            return this;
            }

        @Override
        conditional Entries addIfAbsent(Map.Entry entry)
            {
            return this.ConcurrentHashMap.putIfAbsent(entry.key, entry.value) ? (True, this) : False;
            }

        @Override
        @Op("-")
        Entries remove(Map.Entry entry)
            {
            this.ConcurrentHashMap.remove(entry.key, entry.value);
            return this;
            }

        @Override
        conditional Entries removeIfPresent(Map.Entry entryThat)
            {
            return this.ConcurrentHashMap.processIfPresent(entryThat.key, entry ->
                {
                if (entry.value == entryThat.value)
                    {
                    entry.delete();
                    }
                })
                ? (True, this) : False;
            }

        @Override
        Entries clear()
            {
            this.ConcurrentHashMap.clear();
            return this;
            }

        @Override
        Iterator<Map.Entry> iterator()
            {
            Partition[] partitions = this.ConcurrentHashMap.partitions;
            if (partitions.size == 1)
                {
                return partitions[0].entries.iterator();
                }

            Int step = computeRandomStep();
            Int first = step % partitions.size;
            Int second = (first + step) % partitions.size;

            GrowableCompoundIterator<Map.Entry> iter = new GrowableCompoundIterator(
                partitions[first].entries.iterator(), partitions[second].entries.iterator());

            for (Int i = (second + step) % partitions.size; i != first; i = (i + step) % partitions.size)
                {
                iter.add(partitions[i].entries.iterator());
                }

            return iter;
            }
        }


    // ----- GrowableCompoundIterator --------------------------------------------------------------

    /**
     * A [CompoundIterator] which supports adding iterators.
     */
    protected static class GrowableCompoundIterator<Element>
            extends CompoundIterator<Element>
        {
        construct (Iterator<Element> iter1, Iterator<Element> iter2)
            {
            construct CompoundIterator(iter1, iter2);
            }
        }

    // ----- Partition implementation --------------------------------------------------------------

    /**
     * A portion of the concurrent map's data.
     */
    protected static service Partition<Key extends immutable Object, Value extends ImmutableAble>
            extends HashMap<Key, Value>
        {
        // ----- constructors ----------------------------------------------------------------------

        construct(Hasher<Key> hasher, Int partitionCount, Int initCapacity)
            {
            this.partitionCount = partitionCount;
            construct HashMap(hasher, initCapacity);
            }


        // ----- properties ------------------------------------------------------------------------

        /**
         * The number of partitions in the [ConcurrentHashMap].
         */
        protected Int partitionCount;

        /**
         * A secondary map of pending operations, null up until the first call to process.
         * TODO: does/should @Lazy have a way to do a not-initialized check?
         */
        protected HashMap<Key, FutureVar>? pendingByKey;


        // ----- Partition methods -----------------------------------------------------------------

        /**
         * Perform an ordered [put] operations.
         *
         * @param key the key
         * @param value the value
         *
         * @return this
         */
        @Concurrent
        protected Map putOrdered(Key key, Value value)
            {
            if (pendingByKey?.contains(key))
                {
                process(key, e -> {e.value = value;});
                }
            else
                {
                put(key, value);
                }

            return this;
            }

        /**
         * Perform an ordered [putIfAbsent] operations.
         *
         * @param key the key
         * @param value the value
         *
         * @return this
         */
        @Concurrent
        protected conditional Map putIfAbsentOrdered(Key key, Value value)
            {
            if (pendingByKey?.contains(key))
                {
                return process(key, e ->
                    {
                    if (e.exists)
                        {
                        return False;
                        }

                    e.value = value;
                    return True;
                    }), this;
                }
            else
                {
                return putIfAbsent(key, value);
                }
            }

        /**
         * Perform an ordered [replace] operations.
         *
         * @param key the key
         * @param valueOld the required old value
         * @param valueNew the new value
         *
         * @return this if the the replace occured
         */
        @Concurrent
        protected conditional Map replaceOrdered(Key key, Value valueOld, Value valueNew)
            {
            if (pendingByKey?.contains(key))
                {
                return process(key, e ->
                    {
                    if (e.exists && e.value == valueOld)
                        {
                        e.value = valueNew;
                        return True;
                        }

                    return False;
                    }), this;
                }
            else
                {
                return replace(key, valueOld, valueNew);
                }
            }

        /**
         * Perform an ordered [remove] operations.
         *
         * @param key the key
         *i
         * @return this
         */
        @Concurrent
        protected Map removeOrdered(Key key)
            {
            if (pendingByKey?.contains(key))
                {
                process(key, e -> {e.delete();});
                return this;
                }
            else
                {
                return remove(key);
                }
            }

        /**
         * Perform an ordered conditional [remove] operations.
         *
         * @param key the key
         * @param value the required old value
         *
         * @return this if the remove occured
         */
        @Concurrent
        protected conditional Map removeOrdered(Key key, Value value)
            {
            if (pendingByKey?.contains(key))
                {
                return process(key, e ->
                    {
                    if (e.exists && e.value == value)
                        {
                        e.delete();
                        return True;
                        }

                    return False;
                    }), this;
                }
            else
                {
                return remove(key, value);
                }
            }

        /**
         * Perform an ordered [clear] operations.
         *
         * @return this
         */
        @Concurrent
        protected Map clearOrdered()
            {
            if (pendingByKey == null || pendingByKey?.empty)
                {
                clear();
                }
            else
                {
                for (Key key : keys)
                    {
                    removeOrdered(key);
                    }
                }

            return this;
            }


        // ----- HashMap methods -------------------------------------------------------------------

        @Override
        @Concurrent
        <Result> Result process(Key key, function Result (Map<Key, Value>.Entry) compute)
            {
            Entry entry = new @KeyEntry(key) Entry() {};
            @Future Result result;
            FutureVar rVar = &result;

            Map<Key, FutureVar>? pbk = pendingByKey;
            Map<Key, FutureVar> pendingByKey;
            if (pbk == null)
                {
                pendingByKey = new HashMap();
                this.pendingByKey = pendingByKey;
                }
            else
                {
                pendingByKey = pbk;
                }

            // ensure that when we complete if there are no more pending actions that
            // we clean our entry from the pending map
            rVar.thenDo(() -> pendingByKey.remove(key, rVar));

            Var<FutureVar> ref = &rVar;
            rVar.thenDo(() -> pendingByKey.process(key, e -> {
                FutureVar value = e.value;
                if (&value == ref)
                    {
                    e.delete();
                    }
            }));

            if (FutureVar pending := pendingByKey.get(key))
                {
                // there are pending operations, add our action to the end of the list
                // TODO: it would be nice to have a callback when an @Concurrent frame yields
                // this would allow me to only do the extra bookkeeping when we actually yield
                pendingByKey.put(key, rVar);
                pending.thenDo(() -> {result = compute(entry);});
                }
            else
                {
                // no contention; register our action and run async
                pendingByKey.put(key, rVar);
                result = compute(entry);
                }

            return &result;
            }

        @Override
        protected (Int bucketCount, Int growAt, Int shrinkAt) selectBucketCount(Int capacity)
            {
            // ensure we never have the same partition and bucket count
            Int bucketCount;
            Int growAt;
            Int shrinkAt;
            Int max = PRIMES[PRIMES.size - 1];
            do
                {
                (bucketCount, growAt, shrinkAt) = super(capacity++);
                }
            while (bucketCount == partitionCount && bucketCount != max);

            if (bucketCount == max && bucketCount == partitionCount)
                {
                return super(--capacity);
                }

            return bucketCount, growAt, shrinkAt;
            }
        }
    }