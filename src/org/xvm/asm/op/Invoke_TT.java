package org.xvm.asm.op;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.xvm.asm.Constant;
import org.xvm.asm.MethodStructure;
import org.xvm.asm.OpInvocable;
import org.xvm.asm.Register;

import org.xvm.asm.constants.MethodConstant;

import org.xvm.runtime.CallChain;
import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle;
import org.xvm.runtime.ObjectHandle.ExceptionHandle;
import org.xvm.runtime.TypeComposition;
import org.xvm.runtime.Utils;

import org.xvm.runtime.template.xException;

import org.xvm.runtime.template.collections.xTuple.TupleHandle;


import static org.xvm.util.Handy.readPackedInt;
import static org.xvm.util.Handy.writePackedLong;


/**
 * NVOK_TT rvalue-target, CONST-METHOD, rvalue-params-tuple, lvalue-return-tuple
 */
public class Invoke_TT
        extends OpInvocable
    {
    /**
     * Construct an NVOK_TT op.
     *
     * @param nTarget    r-value that specifies the object on which the method being invoked
     * @param nMethodId  r-value that specifies the method being invoked
     * @param nArg       the r-value location of the tuple of method arguments
     * @param nRet       the l-value location for the tuple result
     */
    public Invoke_TT(int nTarget, int nMethodId, int nArg, int nRet)
        {
        super(nTarget, nMethodId);

        m_nArgTupleValue = nArg;
        m_nTupleRetValue = nRet;
        }

    /**
     * Construct an NVOK_TT op based on the passed arguments.
     *
     * @param argTarget    the target Argument
     * @param constMethod  the method constant
     * @param argValue     the value Argument
     * @param regReturn    the Register to move the result into
     */
    public Invoke_TT(Argument argTarget, MethodConstant constMethod, Argument argValue, Register regReturn)
        {
        super(argTarget, constMethod);

        m_argValue = argValue;
        m_regReturn = regReturn;
        }

    /**
     * Deserialization constructor.
     *
     * @param in      the DataInput to read from
     * @param aconst  an array of constants used within the method
     */
    public Invoke_TT(DataInput in, Constant[] aconst)
            throws IOException
        {
        super(readPackedInt(in), readPackedInt(in));

        m_nArgTupleValue = readPackedInt(in);
        m_nTupleRetValue = readPackedInt(in);
        }

    @Override
    public void write(DataOutput out, ConstantRegistry registry)
            throws IOException
        {
        super.write(out, registry);

        if (m_argValue != null)
            {
            m_nArgTupleValue = encodeArgument(m_argValue, registry);
            m_nTupleRetValue = encodeArgument(m_regReturn, registry);
            }

        writePackedLong(out, m_nArgTupleValue);
        writePackedLong(out, m_nTupleRetValue);
        }

    @Override
    public int getOpCode()
        {
        return OP_NVOK_TT;
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        try
            {
            ObjectHandle hTarget = frame.getArgument(m_nTarget);
            TupleHandle hArgTuple = (TupleHandle) frame.getArgument(m_nArgTupleValue);

            if (hTarget == null || hArgTuple == null)
                {
                return R_REPEAT;
                }

            // Tuple values cannot be local properties
            ObjectHandle[] ahArg = hArgTuple.m_ahValue;

            if (isProperty(hTarget))
                {
                ObjectHandle[] ahTarget = new ObjectHandle[] {hTarget};
                Frame.Continuation stepLast = frameCaller -> complete(frameCaller, ahTarget[0], ahArg);

                return new Utils.GetArgument(ahTarget, stepLast).doNext(frame);
                }

            return complete(frame, hTarget, ahArg);
            }
        catch (ExceptionHandle.WrapperException e)
            {
            return frame.raiseException(e);
            }
        }

    protected int complete(Frame frame, ObjectHandle hTarget, ObjectHandle[] ahArg)
        {
        TypeComposition clz = hTarget.f_clazz;

        CallChain chain = getCallChain(frame, clz);
        MethodStructure method = chain.getTop();

        if (ahArg.length != method.getParamCount())
            {
            return frame.raiseException(xException.makeHandle("Invalid tuple argument"));
            }

        return chain.isNative()
            ? clz.f_template.invokeNativeN(frame, method, hTarget, ahArg, -m_nTupleRetValue - 1)
            : clz.f_template.invoke1(frame, chain, hTarget,
                Utils.ensureSize(ahArg, method.getMaxVars()), -m_nTupleRetValue - 1);
        }

    @Override
    public void registerConstants(ConstantRegistry registry)
        {
        super.registerConstants(registry);

        registerArgument(m_argValue, registry);
        }

    private int m_nArgTupleValue;
    private int m_nTupleRetValue;

    private Argument m_argValue;
    private Register m_regReturn;
    }
