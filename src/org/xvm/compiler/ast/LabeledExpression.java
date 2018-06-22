package org.xvm.compiler.ast;


import org.xvm.asm.Argument;
import org.xvm.asm.ErrorListener;
import org.xvm.asm.MethodStructure.Code;

import org.xvm.asm.constants.TypeConstant;

import org.xvm.asm.op.Label;

import org.xvm.compiler.Token;

import org.xvm.compiler.ast.Statement.Context;

import java.lang.reflect.Field;


/**
 * Used for named arguments.
 */
public class LabeledExpression
        extends Expression
    {
    // ----- constructors --------------------------------------------------------------------------

    public LabeledExpression(Token name, Expression expr)
        {
        this.name = name;
        this.expr = expr;
        }


    // ----- accessors -----------------------------------------------------------------------------

    /**
     * @return the token that provides the label (the name) for the expression
     */
    public Token getNameToken()
        {
        return name;
        }

    /**
     * @return the label name
     */
    public String getName()
        {
        return name.getValueText();
        }

    @Override
    public long getStartPosition()
        {
        return name.getStartPosition();
        }

    @Override
    public long getEndPosition()
        {
        return expr.getEndPosition();
        }

    @Override
    protected Field[] getChildFields()
        {
        return CHILD_FIELDS;
        }


    // ----- compilation ---------------------------------------------------------------------------

    @Override
    protected boolean hasMultiValueImpl()
        {
        return true;
        }

    @Override
    public TypeConstant getImplicitType(Context ctx)
        {
        return expr.getImplicitType(ctx);
        }

    @Override
    public TypeConstant[] getImplicitTypes(Context ctx)
        {
        return expr.getImplicitTypes(ctx);
        }

    @Override
    public TypeFit testFit(Context ctx, TypeConstant typeRequired)
        {
        return expr.testFit(ctx, typeRequired);
        }

    @Override
    public TypeFit testFitMulti(Context ctx, TypeConstant[] atypeRequired)
        {
        return expr.testFitMulti(ctx, atypeRequired);
        }

    @Override
    protected Expression validate(Context ctx, TypeConstant typeRequired, ErrorListener errs)
        {
        Expression exprNew = expr.validate(ctx, typeRequired, errs);
        if (exprNew == null)
            {
            return finishValidation(typeRequired, null, TypeFit.NoFit, null, errs);
            }

        expr = exprNew;
        return finishValidation(typeRequired, exprNew.getType(), exprNew.getTypeFit(),
                exprNew.hasConstantValue() ? exprNew.toConstant() : null, errs);
        }

    @Override
    protected Expression validateMulti(Context ctx, TypeConstant[] atypeRequired, ErrorListener errs)
        {
        Expression exprNew = expr.validateMulti(ctx, atypeRequired, errs);
        if (exprNew == null)
            {
            return finishValidations(atypeRequired, null, TypeFit.NoFit, null, errs);
            }

        expr = exprNew;
        return finishValidations(atypeRequired, exprNew.getTypes(), exprNew.getTypeFit(),
                exprNew.hasConstantValue() ? exprNew.toConstants() : null, errs);
        }

    @Override
    public boolean isAssignable()
        {
        return expr.isAssignable();
        }

    @Override
    public boolean isAborting()
        {
        return expr.isAborting();
        }

    @Override
    public boolean isShortCircuiting()
        {
        return expr.isShortCircuiting();
        }

    @Override
    public boolean isNonBinding()
        {
        return expr.isNonBinding();
        }

    @Override
    public boolean isConstant()
        {
        return expr.isConstant();
        }

    @Override
    public boolean hasSideEffects()
        {
        return expr.hasSideEffects();
        }

    @Override
    public void generateVoid(Code code, ErrorListener errs)
        {
        expr.generateVoid(code, errs);
        }

    @Override
    public Argument generateArgument(Code code, boolean fLocalPropOk, boolean fUsedOnce, ErrorListener errs)
        {
        return expr.generateArgument(code, fLocalPropOk, fUsedOnce, errs);
        }

    @Override
    public Argument[] generateArguments(Code code, boolean fLocalPropOk, boolean fUsedOnce, ErrorListener errs)
        {
        return expr.generateArguments(code, fLocalPropOk, fUsedOnce, errs);
        }

    @Override
    public void generateAssignment(Code code, Assignable LVal, ErrorListener errs)
        {
        expr.generateAssignment(code, LVal, errs);
        }

    @Override
    public void generateAssignments(Code code, Assignable[] aLVal, ErrorListener errs)
        {
        expr.generateAssignments(code, aLVal, errs);
        }

    @Override
    public void generateConditionalJump(Code code, Label label, boolean fWhenTrue, ErrorListener errs)
        {
        expr.generateConditionalJump(code, label, fWhenTrue, errs);
        }

    @Override
    public Assignable generateAssignable(Code code, ErrorListener errs)
        {
        return expr.generateAssignable(code, errs);
        }

    @Override
    public Assignable[] generateAssignables(Code code, ErrorListener errs)
        {
        return expr.generateAssignables(code, errs);
        }


    // ----- debugging assistance ------------------------------------------------------------------

    @Override
    public String toString()
        {
        return name + " = " + expr;
        }

    @Override
    public String getDumpDesc()
        {
        return toString();
        }


    // ----- fields --------------------------------------------------------------------------------

    protected Token      name;
    protected Expression expr;

    private static final Field[] CHILD_FIELDS = fieldsForNames(LabeledExpression.class, "expr");
    }
