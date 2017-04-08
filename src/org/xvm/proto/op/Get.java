package org.xvm.proto.op;

import org.xvm.proto.Frame;
import org.xvm.proto.ObjectHandle;
import org.xvm.proto.ObjectHandle.ExceptionHandle;
import org.xvm.proto.OpInvocable;
import org.xvm.proto.TypeCompositionTemplate;
import org.xvm.proto.TypeCompositionTemplate.PropertyTemplate;
import org.xvm.proto.TypeCompositionTemplate.MethodTemplate;

/**
 * GET rvalue-target, CONST_PROPERTY, lvalue
 *
 * @author gg 2017.03.08
 */
public class Get extends OpInvocable
    {
    private final int f_nTargetValue;
    private final int f_nPropConstId;
    private final int f_nRetValue;

    public Get(int nTarget, int nPropId, int nRet)
        {
        f_nTargetValue = nTarget;
        f_nPropConstId = nPropId;
        f_nRetValue = nRet;
        }

    @Override
    public int process(Frame frame, int iPC)
        {
        ObjectHandle hTarget = frame.f_ahVar[f_nTargetValue];

        TypeCompositionTemplate template = hTarget.f_clazz.f_template;

        PropertyTemplate property = getPropertyTemplate(frame, template, -f_nPropConstId);

        MethodTemplate method = property.m_templateGet;

        ObjectHandle[] ahRet;
        ExceptionHandle hException;

        if (method == null)
            {
            hException = template.getProperty(hTarget, property.f_sName, ahRet = new ObjectHandle[1]);
            }
        else if (method.isNative())
            {
            hException = template.invokeNative01(frame, hTarget, method, ahRet = new ObjectHandle[1]);
            }
        else
            {
            ObjectHandle[] ahVar = new ObjectHandle[method.m_cVars];

            Frame frameNew = frame.f_context.createFrame(frame, method, hTarget, ahVar);

            hException = frameNew.execute();

            ahRet = frameNew.f_ahReturn;
            }

        if (hException == null)
            {
            frame.f_ahVar[f_nRetValue] = ahRet[0];
            return iPC + 1;
            }
        else
            {
            frame.m_hException = hException;
            return RETURN_EXCEPTION;
            }
        }
    }
