module Ecstasy.xtclang.org
    {
    const Module {}
    const Package {}
    const Class {}
    interface Const {}
    interface Property {}
    interface Method {}

    typedef Tuple<> Void;

//    class Object
//        {
//        String to<String>();
//        @Auto function Object() to<function Object()>();
//        }

    interface Meta<PublicType, ProtectedType, PrivateType, StructType> {}
    class Object
        {
        protected Meta<Object:public, Object:protected, Object:private> meta;

        static Boolean equals(Object o1, Object o2);

        String to<String>();

        Object[] to<Object[]>();

        Tuple<Object> to<Tuple<Object>>();

        @Auto function Object() to<function Object()>();

        immutable Object to<immutable Object>();
        }

    interface Enum
        {
        @RO Enumeration<Enum> enumeration;
        @RO Int ordinal;
        @RO String name;
        conditional Enum next();
        conditional Enum prev();
        }

    mixin Enumeration<EnumType extends Enum>
            into Class
        {
        @Override
        String name;

        @Lazy Int count;
        @Lazy String[] names;
        @Lazy EnumType[] values;
        @Lazy Map<String, EnumType> byName;
        }

    interface Function // <ParamTypes extends Tuple<Type...>, ReturnTypes extends Tuple<Type...>>
        {
//         @Override
//         function Function() to<function Function()>();
        }

    class IntLiteral
        {
        @Auto Int to<Int>();
        }

    class Int64
        {
        @Op Int64 add(Int64 n);
        }

    class String
        {
        }

    interface Type {}

    enum Nullable{Null}
    enum Boolean{False, True}

    interface Iterator<ElementType>
        {
        conditional ElementType next();
        }

    package collections
        {
        interface Sequence<ElementType> {}
        class Array<ElementType> {}

        interface Tuple // <ElementTypes extends Tuple<ElementTypes...>>
            {
            }

        interface Map<KeyType, ValueType>
            {
            conditional ValueType get(KeyType key);

            Void put(KeyType key, ValueType value);
            }
        }

    interface Ref<RefType>
        {
        RefType get();
        }

    interface Var<RefType>
            extends Ref<RefType>
        {
        Void set(RefType value);
        }

    package annotations
        {
        mixin AutoConversion into Method {}
        mixin ReadOnly into Property {}
        mixin Operator(String? token = null) into Method {}
        mixin Override into Property | Method {}
        mixin InjectedRef<RefType> into Ref<RefType> {}
        mixin LazyVar<RefType> into Var<RefType> {}
        mixin UncheckedInt into Int64 {}
        }
    }