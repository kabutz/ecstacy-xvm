package org.xvm.runtime.template._native.collections.arrays;


import java.util.Arrays;

import org.xvm.asm.ClassStructure;
import org.xvm.asm.ConstantPool;
import org.xvm.asm.Op;

import org.xvm.asm.constants.TypeConstant;

import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle;
import org.xvm.runtime.ObjectHandle.JavaLong;
import org.xvm.runtime.TemplateRegistry;
import org.xvm.runtime.TypeComposition;

import org.xvm.runtime.template.xBoolean;
import org.xvm.runtime.template.xException;

import org.xvm.runtime.template.collections.xArray.Mutability;

import org.xvm.runtime.template.numbers.xInt64;
import org.xvm.runtime.template.numbers.xUInt8;


/**
 * Native RTDelegate<Byte> implementation.
 */
public class xRTByteDelegate
        extends xRTDelegate
        implements ByteView
    {
    public static xRTByteDelegate INSTANCE;

    public xRTByteDelegate(TemplateRegistry templates, ClassStructure structure, boolean fInstance)
        {
        super(templates, structure, false);

        if (fInstance)
            {
            INSTANCE = this;
            }
        }

    @Override
    public void initNative()
        {
        }

    @Override
    public TypeConstant getCanonicalType()
        {
        ConstantPool pool = pool();
        return pool.ensureParameterizedTypeConstant(
                getInceptionClassConstant().getType(),
                pool.typeByte());
        }

    @Override
    public DelegateHandle createDelegate(TypeConstant typeElement, int cCapacity,
                                         ObjectHandle[] ahContent, Mutability mutability)
        {
        byte[] ab    = new byte[cCapacity];
        int    cSize = ahContent.length;
        for (int i = 0; i < cSize; i++)
            {
            ab[i] = (byte) ((JavaLong) ahContent[i]).getValue();
            }
        return makeHandle(ab, cSize, mutability);
        }


    // ----- RTDelegate API ------------------------------------------------------------------------

    @Override
    public int getPropertyCapacity(Frame frame, ObjectHandle hTarget, int iReturn)
        {
        ByteArrayHandle hDelegate = (ByteArrayHandle) hTarget;

        return frame.assignValue(iReturn, xInt64.makeHandle(hDelegate.m_abValue.length));
        }

    @Override
    public int setPropertyCapacity(Frame frame, ObjectHandle hTarget, long nCapacity)
        {
        ByteArrayHandle hDelegate = (ByteArrayHandle) hTarget;

        byte[] abOld = hDelegate.m_abValue;
        int    nSize = (int) hDelegate.m_cSize;

        if (nCapacity < nSize)
            {
            return frame.raiseException(
                xException.illegalArgument(frame, "Capacity cannot be less then size"));
            }

        // for now, no trimming
        int nCapacityOld = abOld.length;
        if (nCapacity > nCapacityOld)
            {
            byte[] abNew = new byte[(int) nCapacity];
            System.arraycopy(abOld, 0, abNew, 0, abOld.length);
            hDelegate.m_abValue = abNew;
            }
        return Op.R_NEXT;
        }

    public int invokePreInc(Frame frame, ObjectHandle hTarget, long lIndex, int iReturn)
        {
        ByteArrayHandle hDelegate = (ByteArrayHandle) hTarget;

        if (lIndex < 0 || lIndex >= hDelegate.m_cSize)
            {
            return frame.raiseException(xException.outOfBounds(frame, lIndex, hDelegate.m_cSize));
            }

        return frame.assignValue(iReturn,
                xUInt8.makeHandle(++hDelegate.m_abValue[(int) lIndex]));
        }

    @Override
    public int callEquals(Frame frame, TypeComposition clazz,
                          ObjectHandle hValue1, ObjectHandle hValue2, int iReturn)
        {
        ByteArrayHandle h1 = (ByteArrayHandle) hValue1;
        ByteArrayHandle h2 = (ByteArrayHandle) hValue2;

        return frame.assignValue(iReturn,
                xBoolean.makeHandle(Arrays.equals(h1.m_abValue, h2.m_abValue)));
        }

    @Override
    public boolean compareIdentity(ObjectHandle hValue1, ObjectHandle hValue2)
        {
        ByteArrayHandle h1 = (ByteArrayHandle) hValue1;
        ByteArrayHandle h2 = (ByteArrayHandle) hValue2;

        if (h1 == h2)
            {
            return true;
            }

        return h1.getMutability() == h2.getMutability()
            && h1.m_cSize == h2.m_cSize
            && Arrays.equals(h1.m_abValue, h2.m_abValue);
        }

    @Override
    protected DelegateHandle createCopyImpl(DelegateHandle hTarget, Mutability mutability,
                                            long ofStart, long cSize, boolean fReverse)
        {
        ByteArrayHandle hDelegate = (ByteArrayHandle) hTarget;

        byte[] abValue = Arrays.copyOfRange(hDelegate.m_abValue, (int) ofStart, (int) (ofStart + cSize));
        if (fReverse)
            {
            abValue = reverse(abValue, (int) cSize);
            }

        return new ByteArrayHandle(hDelegate.getComposition(), abValue, cSize, mutability);
        }

    @Override
    protected int extractArrayValueImpl(Frame frame, DelegateHandle hTarget, long lIndex, int iReturn)
        {
        ByteArrayHandle hDelegate = (ByteArrayHandle) hTarget;

        byte b = hDelegate.m_abValue[(int) lIndex];
        return frame.assignValue(iReturn, xUInt8.makeHandle(((long) b) & 0xFF));
        }

    @Override
    protected int assignArrayValueImpl(Frame frame, DelegateHandle hTarget, long lIndex,
                                       ObjectHandle hValue)
        {
        ByteArrayHandle hDelegate = (ByteArrayHandle) hTarget;

        int    cSize   = (int) hDelegate.m_cSize;
        byte[] abValue = hDelegate.m_abValue;

        if (lIndex == cSize)
            {
            if (cSize == abValue.length)
                {
                abValue = hDelegate.m_abValue = grow(abValue, cSize + 1);
                }

            hDelegate.m_cSize++;
            }

        abValue[(int) lIndex] = (byte) ((JavaLong) hValue).getValue();
        return Op.R_NEXT;
        }

    @Override
    protected void insertElementImpl(DelegateHandle hTarget, ObjectHandle hElement, long lIndex)
        {
        ByteArrayHandle hDelegate = (ByteArrayHandle) hTarget;
        int             cSize     = (int) hDelegate.m_cSize;
        byte[]          abValue   = hDelegate.m_abValue;

        if (cSize == abValue.length)
            {
            abValue = hDelegate.m_abValue = grow(hDelegate.m_abValue, cSize + 1);
            }
        hDelegate.m_cSize++;

        if (lIndex == cSize)
            {
            // add
            abValue[cSize] = (byte) ((JavaLong) hElement).getValue();
            }
        else
            {
            // insert
            int nIndex = (int) lIndex;
            System.arraycopy(abValue, nIndex, abValue, nIndex + 1, cSize - nIndex);
            abValue[(int) lIndex] = (byte) ((JavaLong) hElement).getValue();
            }
        }

    @Override
    protected void deleteElementImpl(DelegateHandle hTarget, long lIndex)
        {
        ByteArrayHandle hDelegate = (ByteArrayHandle) hTarget;
        int             cSize     = (int) hDelegate.m_cSize;
        byte[]          abValue   = hDelegate.m_abValue;

        if (lIndex < cSize - 1)
            {
            int nIndex = (int) lIndex;
            System.arraycopy(abValue, nIndex + 1, abValue, nIndex, cSize - nIndex -1);
            }
        abValue[(int) --hDelegate.m_cSize] = 0;
        }

    @Override
    public void fill(DelegateHandle hTarget, int cSize, ObjectHandle hValue)
        {
        ByteArrayHandle hDelegate = (ByteArrayHandle) hTarget;

        Arrays.fill(hDelegate.m_abValue, 0, cSize, (byte) ((JavaLong) hValue).getValue());
        hDelegate.m_cSize = cSize;
        }


    // ----- ByteView implementation ---------------------------------------------------------------

    @Override
    public byte[] getBytes(DelegateHandle hDelegate, long ofStart, long cBytes, boolean fReverse)
        {
        ByteArrayHandle hBytes = (ByteArrayHandle) hDelegate;

        byte[] ab = hBytes.m_abValue;

        if (hBytes.getMutability() == Mutability.Constant &&
                ofStart == 0 && cBytes == hBytes.m_cSize && !fReverse)
            {
            return ab;
            }

        ab = Arrays.copyOfRange(ab, (int) ofStart, (int) (ofStart + cBytes));
        return fReverse ? reverse(ab, (int) cBytes) : ab;
        }

    @Override
    public byte extractByte(DelegateHandle hDelegate, long of)
        {
        ByteArrayHandle hBytes = (ByteArrayHandle) hDelegate;

        return hBytes.m_abValue[(int) of];
        }

    @Override
    public void assignByte(DelegateHandle hDelegate, long of, byte bValue)
        {
        ByteArrayHandle hBytes = (ByteArrayHandle) hDelegate;

        hBytes.m_abValue[(int) of] = bValue;
        }


    // ----- helper methods ------------------------------------------------------------------------

    public static byte[] reverse(byte[] abValue, int cSize)
        {
        byte[] abValueR = new byte[cSize];
        for (int i = 0; i < cSize; i++)
            {
            abValueR[i] = abValue[cSize - 1 - i];
            }
        return abValueR;
        }

    private static byte[] grow(byte[] abValue, int cSize)
        {
        int cCapacity = calculateCapacity(abValue.length, cSize);

        byte[] abNew = new byte[cCapacity];
        System.arraycopy(abValue, 0, abNew, 0, abValue.length);
        return abNew;
        }


    // ----- ObjectHandle --------------------------------------------------------------------------

    public ByteArrayHandle makeHandle(byte[] ab, long cSize, Mutability mutability)
        {
        return new ByteArrayHandle(getCanonicalClass(), ab, cSize, mutability);
        }

    public static class ByteArrayHandle
            extends DelegateHandle
        {
        public byte[] m_abValue;

        protected ByteArrayHandle(TypeComposition clazz, byte[] abValue,
                                  long cSize, Mutability mutability)
            {
            super(clazz, mutability);

            m_abValue = abValue;
            m_cSize   = cSize;
            }

        @Override
        public void makeImmutable()
            {
            if (isMutable())
                {
                // purge the unused space
                byte[] ab = m_abValue;
                int    c  = (int) m_cSize;
                if (ab.length != c)
                    {
                    byte[] abNew = new byte[c];
                    System.arraycopy(ab, 0, abNew, 0, c);
                    m_abValue = abNew;
                    }
                super.makeImmutable();
                }
            }

        @Override
        public boolean isNativeEqual()
            {
            return true;
            }

        @Override
        public int compareTo(ObjectHandle that)
            {
            byte[] abThis = m_abValue;
            int    cThis  = (int) m_cSize;
            byte[] abThat = ((ByteArrayHandle) that).m_abValue;
            int    cThat  = (int) ((ByteArrayHandle) that).m_cSize;

            if (cThis != cThat)
                {
                return cThis - cThat;
                }

            for (int i = 0; i < cThis; i++)
                {
                int iDiff = abThis[i] - abThat[i];
                if (iDiff != 0)
                    {
                    return iDiff;
                    }
                }
            return 0;
            }

        @Override
        public int hashCode()
            {
            return Arrays.hashCode(m_abValue);
            }

        @Override
        public boolean equals(Object obj)
            {
            return obj instanceof ByteArrayHandle
                && Arrays.equals(m_abValue, ((ByteArrayHandle) obj).m_abValue);
            }
        }
    }