package org.xvm.runtime.template.collections;


import org.xvm.asm.ClassStructure;
import org.xvm.asm.ConstantPool;

import org.xvm.asm.constants.TypeConstant;

import org.xvm.runtime.ObjectHandle;
import org.xvm.runtime.ObjectHandle.JavaLong;
import org.xvm.runtime.TemplateRegistry;

import org.xvm.runtime.template.xBit;


/**
 * Native Array<Bit> implementation.
 */
public class xBitArray
        extends BitBasedArray
    {
    public static xBitArray INSTANCE;

    public xBitArray(TemplateRegistry templates, ClassStructure structure, boolean fInstance)
        {
        super(templates, structure);

        if (fInstance)
            {
            INSTANCE = this;
            }
        }

    @Override
    public void initDeclared()
        {
        }

    @Override
    public TypeConstant getCanonicalType()
        {
        ConstantPool pool = pool();
        return pool.ensureParameterizedTypeConstant(pool.typeArray(), pool.ensureEcstasyTypeConstant("Bit"));
        }

    @Override
    protected boolean isSet(ObjectHandle hValue)
        {
        return ((JavaLong) hValue).getValue() != 0;
        }

    @Override
    protected ObjectHandle makeBitHandle(boolean f)
        {
        return xBit.makeHandle(f);
        }

    /**
     * Create a bit array handle.
     *
     * @param cBits       the array arity
     * @param abValue     the underlying bytes
     * @param mutability  the mutability
     *
     * @return the array handle
     */
    public static BitArrayHandle makeHandle(int cBits, byte[] abValue, Mutability mutability)
        {
        return new BitArrayHandle(INSTANCE.getCanonicalClass(), cBits, abValue, mutability);
        }
    }
