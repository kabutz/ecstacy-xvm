package org.xvm.runtime;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.xvm.asm.ClassStructure;
import org.xvm.asm.Component;
import org.xvm.asm.Constant;
import org.xvm.asm.ConstantPool;
import org.xvm.asm.Constants;
import org.xvm.asm.MethodStructure;
import org.xvm.asm.MultiMethodStructure;
import org.xvm.asm.Op;
import org.xvm.asm.Parameter;
import org.xvm.asm.PropertyStructure;
import org.xvm.asm.TypedefStructure;

import org.xvm.asm.constants.ClassConstant;
import org.xvm.asm.constants.IdentityConstant;
import org.xvm.asm.constants.MethodConstant;
import org.xvm.asm.constants.TypeConstant;

import org.xvm.util.Handy;


/**
 * A temporary intermediary between the RT, the ConstantPool and ClassStructure;
 * FOR SIMULATION ONLY
 */
public class Adapter
    {
    public final Container f_container;

    // the template composition: name -> the corresponding ClassConstant id
    private Map<String, Integer> m_mapClasses = new HashMap<>();

    public Adapter(Container container)
        {
        f_container = container;
        }

    // get a "relative" type id for the specified name in the context of the
    // specified template
    // if sName is a semicolon delimited array of names, the resulting type
    // is a tuple
    public int getClassTypeConstId(String sName)
        {
        if (m_mapClasses.containsKey(sName))
            {
            return m_mapClasses.get(sName);
            }

        TypeConstant constType;

        if (sName.indexOf(';') < 0)
            {
            constType = getClassType(sName, null);
            }
        else
            {
            String[] asName = Handy.parseDelimitedString(sName, ';');
            int cNames = asName.length;

            TypeConstant[] aconstTypes = new TypeConstant[cNames];
            for (int i = 0; i < cNames; ++i)
                {
                aconstTypes[i] = getClassType(asName[i], null);
                }

            ConstantPool pool = f_container.f_pool;
            constType = pool.ensureParameterizedTypeConstant(pool.typeTuple(), aconstTypes);
            }

        int nTypeId = Op.CONSTANT_OFFSET - constType.getPosition();
        m_mapClasses.put(sName, nTypeId);

        return nTypeId;
        }

    // get a class type for the specified name in the context of the
    // specified template
    public TypeConstant getClassType(String sName, ClassTemplate template)
        {
        ConstantPool pool = f_container.f_pool;

        boolean fNullable = sName.endsWith("?");
        if (fNullable)
            {
            sName = sName.substring(0, sName.length() - 1);
            }

        TypeConstant constType = null;

        int ofTypeParam = sName.indexOf('<');
        if (ofTypeParam >= 0)
            {
            String sParam = sName.substring(ofTypeParam + 1, sName.length() - 1);
            String sSimpleName = sName.substring(0, ofTypeParam);

            // TODO: auto-narrowing (ThisTypeConstant)
            if (sSimpleName.endsWith("!"))
                {
                sSimpleName = sSimpleName.substring(0, sSimpleName.length() - 1);
                }

            ClassConstant constClass = f_container.f_templates.getClassConstant(sSimpleName);
            if (constClass != null)
                {
                String[] asType = Handy.parseDelimitedString(sParam, ',');
                TypeConstant[] acType = getTypeConstants(template, asType);

                constType = pool.ensureClassTypeConstant(constClass, null, acType);
                }
            }
        else
            {
            Component component = f_container.f_module.getChildByPath(sName);
            if (component != null)
                {
                IdentityConstant constId = component.getIdentityConstant();
                switch (constId.getFormat())
                    {
                    case Module:
                    case Package:
                    case Class:
                        constType = constId.asTypeConstant();
                        break;

                    case Typedef:
                        constType = ((TypedefStructure) component).getType();
                        break;
                    }
                }
            }

        if (constType == null)
            {
            throw new IllegalArgumentException("ClassTypeConstant is not defined: " + sName);
            }

        return fNullable ? pool.ensureNullableTypeConstant(constType) : constType;
        }

    public int ensureEnumConstId(String sEnum)
        {
        ClassConstant constClass = f_container.f_templates.getClassConstant(sEnum);
        return Op.CONSTANT_OFFSET -
            f_container.f_pool.ensureSingletonConstConstant(constClass).getPosition();
        }

    // get a "relative" method constant id
    public int getMethodConstId(String sClassName, String sMethName)
        {
        return getMethodConstId(sClassName, sMethName, null, null);
        }

    // get a "relative" method constant id
    public int getMethodConstId(String sClassName, String sMethName, String[] asArgType, String[] asRetType)
        {
        ClassTemplate template = f_container.f_templates.getTemplate(sClassName);

        return Op.CONSTANT_OFFSET -
            getMethod(template, sMethName, asArgType, asRetType).getIdentityConstant().getPosition();
        }

    public MethodStructure getMethod(ClassTemplate template, String sMethName, String[] asArgType, String[] asRetType)
        {
        TypeConstant[] atArg = getTypeConstants(template, asArgType);
        TypeConstant[] atRet = getTypeConstants(template, asRetType);

        ClassTemplate templateTop = template;
        MethodStructure method;
        do
            {
            method = template.getDeclaredMethod(sMethName, atArg, atRet);
            if (method != null)
                {
                return method;
                }

            ClassTemplate templateCategory = template.f_templateCategory;
            if (templateCategory != null)
                {
                method = templateCategory.getDeclaredMethod(sMethName, atArg, atRet);
                if (method != null)
                    {
                    return method;
                    }
                }

            template = template.getSuper();
            }
        while (template != null);

        if (method == null && (asArgType != null || asRetType != null))
            {
            method = getMethod(templateTop, sMethName, null, null);
            if (method != null)
                {
                MethodConstant constMethod = method.getIdentityConstant();
                System.out.println("\n******** parameter mismatch at " + templateTop.f_sName + "#" + sMethName);
                System.out.println("     provided:");
                System.out.println("         arguments " + Arrays.toString(atArg));
                System.out.println("         return " + Arrays.toString(atRet));
                System.out.println("     found:");
                System.out.println("         arguments " + Arrays.toString(constMethod.getRawParams()));
                System.out.println("         return " + Arrays.toString(constMethod.getRawReturns()));
                }
            }
        return method;
        }

    // get a "relative" property constant id
    public int getPropertyConstId(String sClassName, String sPropName)
        {
        try
            {
            ClassConstant constClass = f_container.f_templates.getClassConstant(sClassName);
            ClassStructure struct = (ClassStructure) constClass.getComponent();
            PropertyStructure prop = (PropertyStructure) struct.getChild(sPropName);
            return Op.CONSTANT_OFFSET - prop.getIdentityConstant().getPosition();
            }
        catch (NullPointerException e)
            {
            throw new IllegalArgumentException("Property is not defined: " + sClassName + '#' + sPropName);
            }
        }

    // get a "relative" value constant id
    public int ensureValueConstantId(Object oValue)
        {
        return Op.CONSTANT_OFFSET - ensureValueConstant(oValue).getPosition();
        }

    public TypeConstant[] getTypeConstants(ClassTemplate template, String[] asType)
        {
        if (asType == null)
            {
            return null;
            }

        ConstantPool pool = f_container.f_pool;
        int cTypes = asType.length;
        TypeConstant[] aType = new TypeConstant[cTypes];
        for (int i = 0; i < cTypes; i++)
            {
            String sType = asType[i].trim();
            if (template != null && template.isGenericType(sType))
                {
                PropertyStructure prop = template.getProperty(sType);
                aType[i] = pool.ensureTerminalTypeConstant(prop.getIdentityConstant());
                }
            else
                {
                aType[i] = getClassType(sType, template);
                }
            }
        return aType;
        }

    private Parameter[] getTypeParameters(String[] asType, boolean fReturn)
        {
        ConstantPool pool = f_container.f_pool;
        int cTypes = asType.length;
        Parameter[] aType = new Parameter[cTypes];
        for (int i = 0; i < cTypes; i++)
            {
            String sType = asType[i].trim();
            aType[i] = new Parameter(pool, (TypeConstant) pool.getConstant(
                Op.CONSTANT_OFFSET - getClassTypeConstId(sType)),
                    (fReturn ?"r":"p")+i, null, fReturn, i, false);
            }
        return aType;
        }

    protected Constant ensureValueConstant(Object oValue)
        {
        ConstantPool pool = f_container.f_pool;
        if (oValue instanceof Integer || oValue instanceof Long)
            {
            return pool.ensureIntConstant(((Number) oValue).longValue());
            }

        if (oValue instanceof String)
            {
            return pool.ensureStringConstant((String) oValue);
            }

        if (oValue instanceof Character)
            {
            return pool.ensureCharConstant(((Character) oValue).charValue());
            }

        if (oValue instanceof Boolean)
            {
            return ((Boolean) oValue).booleanValue() ? pool.valTrue() : pool.valFalse();
            }

        if (oValue instanceof Object[])
            {
            Object[] ao = (Object[]) oValue;
            int c = ao.length;
            TypeConstant[] aType = new TypeConstant[c];
            Constant[] aconst = new Constant[c];
            TypeConstant typeUniform = null;

            for (int i = 0; i < c; i++)
                {
                Constant constVal = ensureValueConstant(ao[i]);
                TypeConstant type = constVal.getType();

                if (i == 0)
                    {
                    typeUniform = type;
                    }
                else if (type != typeUniform)
                    {
                    typeUniform = null;
                    }

                aType[i]  = type;
                aconst[i] = constVal;
                }

            if (typeUniform == null)
                {
                TypeConstant typeTuple = pool.ensureParameterizedTypeConstant(
                    pool.typeTuple(), aType);
                return pool.ensureTupleConstant(typeTuple, aconst);
                }
            else
                {
                TypeConstant typeArray = pool.ensureParameterizedTypeConstant(
                    pool.typeArray(), typeUniform);
                return pool.ensureArrayConstant(typeArray, aconst);
                }
            }

        if (oValue == null)
            {
            return pool.ensureSingletonConstConstant(pool.clzNull());
            }

        throw new IllegalArgumentException();
        }

    public MethodStructure addMethod(Component structure, String sName,
                                            String[] asArgType, String[] asRetType)
        {
        MultiMethodStructure mms = structure.ensureMultiMethodStructure(sName);
        return mms.createMethod(false, Constants.Access.PUBLIC,
                null, getTypeParameters(asRetType, true), getTypeParameters(asArgType, false),
                true);
        }

    public static boolean isNative(MethodStructure method)
        {
        return method.isNative();
        }

    public static MethodStructure getGetter(PropertyStructure property)
        {
        MultiMethodStructure mms = (MultiMethodStructure) property.getChild("get");

        // TODO: use the type
        return mms == null ? null : mms.methods().get(0);
        }

    public static MethodStructure getSetter(PropertyStructure property)
        {
        MultiMethodStructure mms = (MultiMethodStructure) property.getChild("set");

        // TODO: use the type
        return mms == null ? null : mms.methods().get(0);
        }
    }
