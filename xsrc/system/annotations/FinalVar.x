/**
 * An FinalVar annotation is used to indicate that the declared variable is not permitted to be
 * modified after it has been assigned.
 *
 * This is both a compile-time and a run-time annotation. Its purpose is to produce a compiler error
 * when a variable is potentially assigned more than once, and to produce a runtime exception when
 * an attempt to assign to a variable occurs after the variable has already been assigned.
 */
mixin FinalVar<RefType>
        into Var<RefType>
    {
    @Override
    void set(RefType value)
        {
        assert:always !assigned;
        super(value);
        }
    }
