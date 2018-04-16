package org.xvm.runtime.template;


import org.xvm.asm.ClassStructure;
import org.xvm.asm.Constants.Access;
import org.xvm.asm.MethodStructure;
import org.xvm.asm.Op;

import org.xvm.asm.op.L_Set;
import org.xvm.asm.op.Return_0;

import org.xvm.runtime.Frame;
import org.xvm.runtime.ObjectHandle;
import org.xvm.runtime.ObjectHandle.ExceptionHandle;
import org.xvm.runtime.ServiceContext;
import org.xvm.runtime.TypeComposition;
import org.xvm.runtime.TemplateRegistry;


/**
 * TODO:
 */
public class xException
        extends xConst
    {
    public static xException INSTANCE;

    public xException(TemplateRegistry templates, ClassStructure structure, boolean fInstance)
        {
        super(templates, structure, false);

        if (fInstance)
            {
            INSTANCE = this;
            }
        }

    @Override
    public void initDeclared()
        {
        // TODO: remove everything when compiler generates the constructors
        f_templates.f_adapter.addMethod(f_struct, "construct", new String[]{"String", "Exception"}, VOID);

        MethodStructure ct = getMethodStructure("construct", new String[]{"String", "Exception"}, VOID);
        ct.setOps(new Op[] // #0 - text, #1 - cause
            {
            new L_Set(Op.CONSTANT_OFFSET - getProperty("text").getIdentityConstant().getPosition(), 0),
            new L_Set(Op.CONSTANT_OFFSET - getProperty("cause").getIdentityConstant().getPosition(), 1),
            new Return_0(),
            });
        }

    @Override
    public ObjectHandle createStruct(Frame frame, TypeComposition clazz)
        {
        return makeMutableStruct(clazz, null, null);
        }

    // ---- ObjectHandle helpers -----

    public static ExceptionHandle immutable()
        {
        return xException.makeHandle("Immutable object");
        }

    public static ExceptionHandle makeHandle(String sMessage)
        {
        ExceptionHandle hException = makeMutableStruct(INSTANCE.getCanonicalClass(), null, null);

        hException.setField("text", xString.makeHandle(sMessage));

        hException.ensureAccess(Access.PUBLIC);
        hException.makeImmutable();
        return hException;
        }

    private static ExceptionHandle makeMutableStruct(TypeComposition clazz,
                                                     ExceptionHandle hCause, Throwable eCause)
        {
        clazz = clazz.ensureAccess(Access.STRUCT);

        ExceptionHandle hException = new ExceptionHandle(clazz, true, eCause);

        Frame frame = ServiceContext.getCurrentContext().getCurrentFrame();

        hException.setField("stackTrace", xString.makeHandle(frame.getStackTrace()));
        hException.setField("cause", hCause == null ? xNullable.NULL : hCause);

        return hException;
        }
    }
