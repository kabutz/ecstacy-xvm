package org.xvm.runtime.template.collections;


import java.util.Arrays;

import org.xvm.asm.ClassStructure;
import org.xvm.asm.Constant;
import org.xvm.asm.ConstantPool;
import org.xvm.asm.MethodStructure;
import org.xvm.asm.Op;

import org.xvm.asm.constants.ArrayConstant;
import org.xvm.asm.constants.TypeConstant;

import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle;
import org.xvm.runtime.ObjectHandle.ArrayHandle;
import org.xvm.runtime.ObjectHandle.DeferredCallHandle;
import org.xvm.runtime.ObjectHandle.JavaLong;
import org.xvm.runtime.ObjectHeap;
import org.xvm.runtime.TypeComposition;
import org.xvm.runtime.ClassTemplate;
import org.xvm.runtime.TemplateRegistry;

import org.xvm.runtime.template.IndexSupport;
import org.xvm.runtime.template.xBoolean;
import org.xvm.runtime.template.xException;
import org.xvm.runtime.template.xFunction.FunctionHandle;
import org.xvm.runtime.template.xInt64;


/**
 * TODO:
 */
public class xArray
        extends ClassTemplate
        implements IndexSupport
    {
    public static xArray INSTANCE;

    public xArray(TemplateRegistry templates, ClassStructure structure, boolean fInstance)
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
        xIntArray template = new xIntArray(f_templates, f_struct, true);
        template.initDeclared();

        ConstantPool pool = f_struct.getConstantPool();

        TypeConstant type = pool.ensureParameterizedTypeConstant(pool.typeArray(), pool.typeInt());
        f_templates.registerNativeTemplate(type, template); // Array<Int>

        markNativeMethod("construct", INT);
        markNativeMethod("construct", new String[]{"Int64", "Function"});
        markNativeMethod("elementAt", INT, new String[] {"Var<ElementType>"});
        markNativeMethod("reify", VOID, new String[]{"collections.Array<ElementType>"});
        }

    @Override
    public boolean isGenericHandle()
        {
        return false;
        }

    @Override
    public TypeComposition ensureParameterizedClass(ConstantPool pool, TypeConstant... typeParams)
        {
        assert typeParams.length == 1;

        TypeConstant typeEl = typeParams[0];

        // TODO: we should obtain the array template from the element's template
        if (typeEl.equals(pool.typeInt()))
            {
            TypeConstant typeInception = pool.ensureParameterizedTypeConstant(
                pool.typeArray(), typeEl);
            return xIntArray.INSTANCE.ensureClass(typeInception, typeInception);
            }

        return super.ensureParameterizedClass(pool, typeParams);
        }

    @Override
    public int createConstHandle(Frame frame, Constant constant)
        {
        ArrayConstant constArray = (ArrayConstant) constant;

        assert constArray.getFormat() == Constant.Format.Array;

        TypeConstant typeArray = constArray.getType();
        TypeConstant typeEl = typeArray.getGenericParamType("ElementType");
        TypeComposition clzArray = ensureParameterizedClass(frame.poolContext(), typeEl);

        Constant[] aconst = constArray.getValue();
        int cSize = aconst.length;

        ObjectHeap heap = frame.f_context.f_heapGlobal;

        ObjectHandle[] ahValue = new ObjectHandle[cSize];
        for (int i = 0; i < cSize; i++)
            {
            ObjectHandle hValue = heap.ensureConstHandle(frame, aconst[i]);

            if (hValue instanceof DeferredCallHandle)
                {
                throw new UnsupportedOperationException("not implemented"); // TODO
                }
            ahValue[i] = hValue;
            }

        xArray template = (xArray) clzArray.getTemplate();
        ArrayHandle hArray = template.createArrayHandle(frame, clzArray, ahValue);

        hArray.makeImmutable();
        frame.pushStack(hArray);
        return Op.R_NEXT;
        }

    /**
     * Create a one dimensional array for a specified class and content.
     *
     * @param frame      the current frame
     * @param clzArray   the class of the array
     * @param ahArg      the array elements
     *
     * @return the array handle
     */
    public ArrayHandle createArrayHandle(Frame frame, TypeComposition clzArray, ObjectHandle[] ahArg)
        {
        return new GenericArrayHandle(clzArray, ahArg);
        }

    /**
     * Create a one dimensional array for a specified type and arity.
     *
     * @param frame      the current frame
     * @param clzArray   the class of the array
     * @param cCapacity  the array size
     *
     * @return the array handle
     */
    public ArrayHandle createArrayHandle(Frame frame, TypeComposition clzArray, long cCapacity)
        {
        return new GenericArrayHandle(clzArray, cCapacity);
        }

    @Override
    public int construct(Frame frame, MethodStructure constructor,
                         TypeComposition clzArray, ObjectHandle[] ahVar, int iReturn)
        {
        // this is a native constructor
        long cCapacity = ((JavaLong) ahVar[0]).getValue();

        if (cCapacity < 0 || cCapacity > Integer.MAX_VALUE)
            {
            return frame.raiseException(
                xException.makeHandle("Invalid array size: " + cCapacity));
            }

        xArray      template = (xArray) clzArray.getTemplate();
        ArrayHandle hArray   = template.createArrayHandle(frame, clzArray, cCapacity);

        if (cCapacity > 0)
            {
            hArray.m_fFixed = true;

            if (ahVar.length == 2)
                {
                FunctionHandle hSupplier = (FunctionHandle) ahVar[1];

                return new Fill(this, hArray, cCapacity, hSupplier, iReturn).doNext(frame);
                }
            }

        return frame.assignValue(iReturn, hArray);
        }

    @Override
    public int callEquals(Frame frame, TypeComposition clazz,
                          ObjectHandle hValue1, ObjectHandle hValue2, int iReturn)
        {
        GenericArrayHandle hArray1 = (GenericArrayHandle) hValue1;
        GenericArrayHandle hArray2 = (GenericArrayHandle) hValue2;

        ObjectHandle[] ah1 = hArray1.m_ahValue;
        ObjectHandle[] ah2 = hArray2.m_ahValue;

        // compare the array dimensions
        int cElements = ah1.length;
        if (cElements != ah2.length)
            {
            return frame.assignValue(iReturn, xBoolean.FALSE);
            }

        // use the compile-time element type
        // and compare arrays elements one-by-one
        TypeConstant typeEl = clazz.getType().getParamTypesArray()[0];

        int[] holder = new int[] {0}; // the index holder
        return new Equals(ah1, ah2, typeEl, cElements, holder, iReturn).doNext(frame);
        }

    @Override
    public boolean compareIdentity(ObjectHandle hValue1, ObjectHandle hValue2)
        {
        GenericArrayHandle hArray1 = (GenericArrayHandle) hValue1;
        GenericArrayHandle hArray2 = (GenericArrayHandle) hValue2;

        if (hArray1.isMutable() || hArray2.isMutable() || hArray1.m_cSize != hArray2.m_cSize)
            {
            return false;
            }

        ObjectHandle[] ah1 = hArray1.m_ahValue;
        ObjectHandle[] ah2 = hArray2.m_ahValue;

        if (ah1 == ah2)
            {
            return true;
            }

        for (int i = 0, c = hArray1.m_cSize; i < c; i++)
            {
            ObjectHandle hV1 = ah1[i];
            ObjectHandle hV2 = ah2[i];

            ClassTemplate template = hV1.getTemplate();
            if (template != hV2.getTemplate() || !template.compareIdentity(hV1, hV2))
                {
                return false;
                }
            }
        return true;
        }


    // ----- IndexSupport methods -----

    @Override
    public int extractArrayValue(Frame frame, ObjectHandle hTarget, long lIndex, int iReturn)
        {
        GenericArrayHandle hArray = (GenericArrayHandle) hTarget;

        if (lIndex < 0 || lIndex >= hArray.m_cSize)
            {
            return frame.raiseException(IndexSupport.outOfRange(lIndex, hArray.m_cSize));
            }

        return frame.assignValue(iReturn, hArray.m_ahValue[(int) lIndex]);
        }

    @Override
    public int assignArrayValue(Frame frame, ObjectHandle hTarget, long lIndex, ObjectHandle hValue)
        {
        GenericArrayHandle hArray = (GenericArrayHandle) hTarget;

        int cSize = hArray.m_cSize;

        if (lIndex < 0 || lIndex > cSize)
            {
            return frame.raiseException(IndexSupport.outOfRange(lIndex, cSize));
            }

        if (lIndex == cSize)
            {
            // an array can only grow without any "holes"
            int cCapacity = hArray.m_ahValue.length;
            if (cSize == cCapacity)
                {
                if (hArray.m_fFixed)
                    {
                    return frame.raiseException(IndexSupport.outOfRange(lIndex, cSize));
                    }

                // resize (TODO: we should be much smarter here)
                cCapacity = cCapacity + Math.max(cCapacity >> 2, 16);

                ObjectHandle[] ahNew = new ObjectHandle[cCapacity];
                System.arraycopy(hArray.m_ahValue, 0, ahNew, 0, cSize);
                hArray.m_ahValue = ahNew;
                }

            hArray.m_cSize++;
            }

        hArray.m_ahValue[(int) lIndex] = hValue;
        return Op.R_NEXT;
        }

    @Override
    public TypeConstant getElementType(ObjectHandle hTarget, long lIndex)
        {
        return hTarget.getType().getGenericParamType("ElementType");
        }

    @Override
    public long size(ObjectHandle hTarget)
        {
        ArrayHandle hArray = (ArrayHandle) hTarget;

        return hArray.m_cSize;
        }

    // ----- helper classes -----

    /**
     * Helper class for array initialization.
     */
    protected static class Fill
            implements Frame.Continuation
        {
        private final xArray template;
        private final ArrayHandle hArray;
        private final long cCapacity;
        private final FunctionHandle hSupplier;
        private final int iReturn;

        private final ObjectHandle[] ahVar;
        private int index = -1;

        public Fill(xArray template, ArrayHandle hArray, long cCapacity,
                    FunctionHandle hSupplier, int iReturn)
            {
            this.template = template;
            this.hArray = hArray;
            this.cCapacity = cCapacity;
            this.hSupplier = hSupplier;
            this.iReturn = iReturn;

            this.ahVar = new ObjectHandle[hSupplier.getVarCount()];
            }

        @Override
        public int proceed(Frame frameCaller)
            {
            return template.assignArrayValue(frameCaller, hArray, index, frameCaller.popStack())
                    == Op.R_EXCEPTION ?
                Op.R_EXCEPTION : doNext(frameCaller);
            }

        public int doNext(Frame frameCaller)
            {
            while (++index < cCapacity)
                {
                ahVar[0] = xInt64.makeHandle(index);

                switch (hSupplier.call1(frameCaller, null, ahVar, Op.A_STACK))
                    {
                    case Op.R_NEXT:
                        break;

                    case Op.R_CALL:
                        frameCaller.m_frameNext.setContinuation(this);
                        return Op.R_CALL;

                    case Op.R_EXCEPTION:
                        return Op.R_EXCEPTION;

                    default:
                        throw new IllegalStateException();
                    }
                }
            return frameCaller.assignValue(iReturn, hArray);
            }

        }
    /**
     * Helper class for equals() implementation.
     */
    protected static class Equals
            implements Frame.Continuation
        {
        final private ObjectHandle[] ah1;
        final private ObjectHandle[] ah2;
        final private TypeConstant typeEl;
        final private int cElements;
        final private int[] holder;
        final private int iReturn;

        public Equals(ObjectHandle[] ah1, ObjectHandle[] ah2, TypeConstant typeEl,
                      int cElements, int[] holder, int iReturn)
            {
            this.ah1 = ah1;
            this.ah2 = ah2;
            this.typeEl = typeEl;
            this.cElements = cElements;
            this.holder = holder;
            this.iReturn = iReturn;
            }

        @Override
        public int proceed(Frame frameCaller)
            {
            ObjectHandle hResult = frameCaller.popStack();
            if (hResult == xBoolean.FALSE)
                {
                return frameCaller.assignValue(iReturn, hResult);
                }
            return doNext(frameCaller);
            }

        public int doNext(Frame frameCaller)
            {
            int iEl;
            while ((iEl = holder[0]++) < cElements)
                {
                switch (typeEl.callEquals(frameCaller, ah1[iEl], ah2[iEl], Op.A_STACK))
                    {
                    case Op.R_NEXT:
                        ObjectHandle hResult = frameCaller.popStack();
                        if (hResult == xBoolean.FALSE)
                            {
                            return frameCaller.assignValue(iReturn, hResult);
                            }
                        break;

                    case Op.R_CALL:
                        frameCaller.m_frameNext.setContinuation(this);
                        return Op.R_CALL;

                    case Op.R_EXCEPTION:
                        return Op.R_EXCEPTION;

                    default:
                        throw new IllegalStateException();
                    }
                }
            return frameCaller.assignValue(iReturn, xBoolean.TRUE);
            }
        }

    // ----- ObjectHandle helpers -----

    // generic array handle
    public static class GenericArrayHandle
            extends ArrayHandle
        {
        public ObjectHandle[] m_ahValue;

        protected GenericArrayHandle(TypeComposition clzArray, ObjectHandle[] ahValue)
            {
            super(clzArray);

            m_ahValue = ahValue;
            m_cSize = ahValue.length;
            }

        protected GenericArrayHandle(TypeComposition clzArray, long cCapacity)
            {
            super(clzArray);

            m_ahValue = new ObjectHandle[(int) cCapacity];
            }

        @Override
        public int hashCode()
            {
            return Arrays.hashCode(m_ahValue);
            }

        @Override
        public boolean equals(Object obj)
            {
            return Arrays.equals(m_ahValue, ((GenericArrayHandle) obj).m_ahValue);
            }

        @Override
        public String toString()
            {
            return super.toString() + (m_fFixed ? "fixed" : "capacity=" + m_ahValue.length)
                    + ", size=" + m_cSize;
            }
        }
    }
