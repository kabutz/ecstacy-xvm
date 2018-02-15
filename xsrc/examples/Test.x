module Test
    {
//    class Fubar
//        {
//        // problem #1 - is the solution a SubstitutableTypeConstant that takes the place of each instance of "T"?
//        <T> conditional T foo(T t)          // compiled as "(Boolean*, T) foo(Type<Object> T*, T t)"
//            {
//            return True, t;
//            }
//
//        // problem #2 ".Type" resolution ... kind of like problem #2 ... hmmm ...
//        Fubar! fn(String s)
//            {
//            return this;
//            }
//        }
//
//    // problem #3 - functions as types
//    (function (Int, Int) (String, String)) fn;
//    function (Int, Int) (String, String) fn2;
//    function (Int, Int) fn3(String, String).get()
//        {
//        return fn2;
//        }
//
//    // problem #4 - tuples weren't reporting themselves as having more than 1 value
//    static (Int, Int) f(String a, String b)
//        {
//        return (0,0);
//        }
//
//    // problem #5 - not sure what this problem was, but it compiles now (the T0D0 was an issue)
//    interface List<ElementType>
//        {
//        ElementType first;
//
//        Void add(ElementType value);
//
//        Iterator<ElementType> iterator();
//        }
//
//    class MyList<ElementType extends Int>
//            implements List<ElementType>
//        {
//        Void add(ElementType value)
//            {
//            TODO
//            }
//        }
//
//    // problem #6
//    class MyClass<MapType1 extends Map, MapType2 extends Map>
//        {
//        Void process(MapType1.KeyType k1, MapType2.KeyType k2)  // TODO resolve both "KeyType" correctly
//            {
//            // ...
//            }
//
//        <MT3 extends MapType1, KT3 extends MapType1.KeyType> Void process(MT3.KeyType k, KT3 k3)  // TODO resolve both "KeyType" correctly
//            {
//            // ...
//            }
//        }
//
//    // problem #7 - conditional mixin
//    mixin MyMixin<T>
//        {
//        // ...
//        }
//
//    class MyClass2<T>
//            incorporates conditional MyMixin<T extends Int>
//        {
//        // TODO
//        }
//
//    // problem #8 - typedefs
//    typedef function Void Alarm();
//
//    class MyTest3
//        {
//        Alarm alarm;
//
//        Alarm foo(Alarm alarm);
//        }
//
//    // problem #9 - Void compiling to Tuple instead of being eliminated
//    Void fnVoid();
//    Tuple fnTupleNone();
//    Tuple<> fnTupleEmpty();
//    Tuple<Int> fnTupleInt();
//    Tuple<Tuple> fnTupleTuple();
//
//    // problem #10 - InjectedRef.RefType resolves in compilation to Ref.RefType (wrong!)
//    @Inject String option;
//
//    // problem #11 - sig is wrong (shows Void, should be String)
//    @Inject String option2.get()
//        {
//        return super.get();
//        }
//
//    // problem #12 - constructors are named after the class instead of "construct"
//    class ConstructorTest
//        {
//        construct ConstructorTest(Int i) {}
//        construct ConstructorTest(String s) {} finally {}
//        }
//
//    // problem #13 - various return type tests, @Op tests, and conversion tests
//    Void foo1()
//        {
//        }
//
//    String foo1MissingReturn() // note: this is supposed to generate an error
//        {
//        }
//
//    String foo1String()
//        {
//        return "hello" * 5;
//        }
//
//    String foo1String2()
//        {
//        return 'x' * 5;
//        }
//
//    Void foo2()
//        {
//        return;
//        }
//
//    Int foo2b(Int i)
//        {
//        return i;
//        }
//
//    Int foo2c()
//        {
//        Int i = 0;
//        return i;
//        }
//
//    Int foo2d()
//        {
//        Int i = 0;
//        i = i + 1;
//        return i;
//        }
//
//    String foo3()
//        {
//        return "hello";
//        }
//
//    Int foo4()
//        {
//        return 0;
//        }
//
//    (String, Int) foo5()
//        {
//        return "hello", 0;
//        }
//
//    // problem #14 - TODO this still fails (AssignmentStatement#emit does not yet implement "+=")
//    Int foo2e()
//        {
//        Int i = 0;
//        i += 1;
//        return i;
//        }
//
//    // problem #15 - needs fix for isA: Tuple<String, Int>.isA(Tuple) == false
//    (String, Int) foo5b()
//        {
//        return ("hello", 0);
//        }
//
//    // problem #16 - conditional tests
//    conditional String foo6()
//        {
//        return false;
//        }
//
//    conditional String foo7()
//        {
//        return true, "hello";
//        }
//
//    // problem #17 - operator +
//    Int foo8(Int a, Int b)
//        {
//        return a + b;
//        }
//
//    // problem #18 - operator + and auto-conversion of IntLiteral to Int
//    Int foo8()
//        {
//        return 40 + 2;
//        }
//
//    // problem #19 - while loops
//    Int foo9(Iterator<Int> iter)
//        {
//        Int sum = 0;
//        while (Int i : iter.next())
//            {
//            sum += i;
//            }
//
//        // just for comparison
//        while (sum < 10)
//            {
//            ++sum;
//            }
//
//        return sum;
//        }
//
//    // problem #20 - "this", auto-narrowing types
//    class C20
//        {
//        C20 bar()
//            {
//            return this;
//            }
//        }


//    class C
//        {
//        Int x = 0;
//        Void foo(Int i) {}
//        Void testVariations()
//            {
//            Int i = x;
//            x = i;
//
//            // TODO requires multi-name resolution
//            // this.x = i;
//
//            // TODO requires post-bang
//            // Property p = x!;
//
//            // TODO requires multi-name resolution
//            // Property p = this.x!;
//            // Property p = C.x;
//
//            // TODO requires pre-ampersand
//            // Ref<Int> = &x;
//
//            // TODO
//            // Function f1 = foo;
//            // Function<<Int>, Void> f1b = foo;
//
//            // TODO requires post-bang
//            // TODO Method m1 = foo!;
//
//            // TODO requires multi-name resolution
//            // Method m2 = C.testVariations;
//            // Function f2 = this.testVariations;
//
//            // TODO
//            // Function f3 = foo(?);
//            // Function<<Int>, Void> f3b = foo(?);
//            // Function f4 = foo(4)!;
//            // Function<Void, Void> f4b = foo(4);
//            }
//        }

//    Int test(Int c)
//        {
//        Int i = 0;
//        while (i < c)
//            {
//            i = i + 1;
//            }
//        return i;
//        }

//    class Bob
//        {
//        @Auto Sam to<Sam>();
//        }
//    class Sam
//        {
//        }
//
//    Sam foo(Bob bob)
//        {
//        // assignment test
//        Sam sam = bob;
//
//        // conversion on return test
//        return bob;
//        }
//
    class MyMap<KeyType, ValueType> implements Map<KeyType, ValueType>
        {
        Void foo();
        @Auto Int size();
        }

    mixin M into MyMap {}

    class MyMap2<KeyType, ValueType> extends MyMap<KeyType, ValueType>
        {
        public/private @Unchecked Int x
            {
            @Unchecked Int get() {return 0;}
            Void set(@Unchecked Int n) {}
            }

        Void bar();
        @Auto Int size();

        static Int y = 0;
        static Int z = () -> y;
        }

    mixin M2 into MyMap2 extends M {}

    class B incorporates M {}
    class D extends B incorporates M2 {}
    function Object() foo(Object o)
        {
        return o;
        }

    Int foo()
        {
        MyMap2<Object, Object> map;
        return map;
        }
    }