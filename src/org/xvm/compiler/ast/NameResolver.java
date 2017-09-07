package org.xvm.compiler.ast;


import java.util.Iterator;
import java.util.List;

import org.xvm.asm.Component;
import org.xvm.asm.Component.Format;
import org.xvm.asm.Component.ResolutionCollector;
import org.xvm.asm.Component.ResolutionResult;
import org.xvm.asm.CompositeComponent;
import org.xvm.asm.Constant;
import org.xvm.asm.ConstantPool;

import org.xvm.asm.PropertyStructure;
import org.xvm.asm.constants.IdentityConstant;
import org.xvm.asm.constants.PropertyConstant;
import org.xvm.asm.constants.RegisterConstant;
import org.xvm.asm.constants.TypeConstant;

import org.xvm.compiler.Compiler;
import org.xvm.compiler.ErrorListener;

import org.xvm.util.Severity;


/**
 * This represents the progress toward resolution of a name for a particular AstNode.
 *
 * @author cp 2017.07.20
 */
public class NameResolver
        implements ResolutionCollector
    {
    public NameResolver(AstNode node, Iterator<String> iterNames)
        {
        assert node instanceof NameResolving;
        assert iterNames != null && iterNames.hasNext();

        m_node = node;
        m_iter = iterNames;
        }

    /**
     * @return true iff the NameResolver has not begun the process of resolving the name
     */
    public boolean isFirstTime()
        {
        return m_status == Status.INITIAL;
        }

    /**
     * Resolve the name, or at least try to make progress doing so.
     *
     * @param listRevisit  a list to add any nodes to that need to be revisted during this compiler
     *                     pass
     * @param errs         the error list to log any errors etc. to
     *
     * @return one of {@link Result#DEFERRED} to indicate that the NameResolver added the node to
     *         {@code listRevisit}, {@link Result#ERROR} to indicate that the NameResolver has
     *         logged an error to {@code errs} and given up all hope of resolving the name, or
     *         {@link Result#RESOLVED} to indicate that the name has been successfully resolved
     */
    public Result resolve(List<AstNode> listRevisit, ErrorListener errs)
        {
        // store off the error list for use by call backs
        // (note: there's no attempt to clean this up later)
        m_errs = errs;

        switch (m_status)
            {
            case INITIAL:
                // just starting out. load the first name to resolve
                m_sName = m_iter.next();

                // the first name could be an import, in which case that needs to be evaluated right
                // away (because the imports will continue to be registered as the AST is resolved,
                // so the answers to the questions about the imports will change if we don't ask now
                // and store off the result)
                m_stmtImport = m_node.resolveImportBySingleName(m_sName);
                if (m_stmtImport != null)
                    {
                    AstNode parent = m_stmtImport.getParent();
                    while (!(parent instanceof StatementBlock))
                        {
                        parent = parent.getParent();
                        }
                    m_blockImport = (StatementBlock) parent;
                    }
                m_status = Status.CHECKED_IMPORTS;
                // fall through

            case CHECKED_IMPORTS:
                // resolve the first name if possible; otherwise defer (to come back to this point).
                // remember to check the import statement if it exists (but only use it when we make
                // our way up to the node that the import was registered with). if no one knows what
                // the first name is, then check if it is an implicitly imported identity.

                // check if the name is an unhideable name
                Component componentParent = m_node.resolveParentBySimpleName(m_sName);
                if (componentParent == null)
                    {
                    // start with the current node, and one by one walk up to the root, asking at
                    // each level for the node to resolve the name
                    AstNode node = m_node;
                    WalkUpToTheRoot: while (node != null)
                        {
                        // if the first name refers to an import, then ask that import to figure out
                        // what the corresponding qualified name refers to (i.e. delegate!)
                        if (node == m_blockImport)
                            {
                            NameResolver resolver = m_stmtImport.getNameResolver();
                            switch (resolver.resolve(listRevisit, errs))
                                {
                                case RESOLVED:
                                    m_constant  = resolver.getConstant();
                                    m_component = resolver.getComponent();
                                    break WalkUpToTheRoot;

                                case DEFERRED:
                                    // dependent on a node that is deferred, so this is deferred
                                    listRevisit.add(m_node);
                                    return Result.DEFERRED;

                                default:
                                case ERROR:
                                    // no need to log an error; the import already should have
                                    m_status = Status.ERROR;
                                    return Result.ERROR;
                                }
                            }

                        // otherwise, if the node has a component associated with it that is
                        // prepared to resolve names, then ask it to resolve the name, and if it
                        // isn't ready, we'll come back later
                        if (node instanceof ComponentStatement)
                            {
                            Component componentResolver = getResolvingComponent((ComponentStatement) node);
                            if (componentResolver == null)
                                {
                                // the component that can do the resolve isn't yet available; come
                                // back later
                                listRevisit.add(m_node);
                                return Result.DEFERRED;
                                }

                            // ask the component to resolve the name
                            switch (componentResolver.resolveName(m_sName, this))
                                {
                                case UNKNOWN:
                                    // the component didn't know the name; keep walking up
                                    break;

                                case RESOLVED:
                                    // the component resolved the first name
                                    break WalkUpToTheRoot;

                                case ERROR:
                                    m_status = Status.ERROR;
                                    return Result.ERROR;

                                case DEFERRED:
                                    listRevisit.add(m_node);
                                    return Result.DEFERRED;

                                default:
                                    throw new IllegalStateException();
                                }
                            }

                        // walk up towards the root
                        node = node.getParent();
                        }

                    // last chance: check the implicitly imported names
                    if (m_constant == null)
                        {
                        Component component = getPool().getImplicitlyImportedComponent(m_sName);
                        if (component == null)
                            {
                            m_node.log(errs, Severity.ERROR, Compiler.NAME_UNRESOLVABLE, m_sName);
                            m_status = Status.ERROR;
                            return Result.ERROR;
                            }
                        else
                            {
                            resolvedComponent(component);
                            }
                        }
                    }
                else
                    {
                    resolvedComponent(componentParent);
                    }

                // first name has been resolved
                m_status = Status.RESOLVED_PARTIAL;
                m_sName  = m_iter.hasNext() ? m_iter.next() : null;
                // fall through

            case RESOLVED_PARTIAL:
                // at this point, we have a component (or other identity) to work from, so the next
                // name has to be relative to that component
                while (m_sName != null)
                    {
                    switch (ensurePartiallyResolvedComponent().resolveName(m_sName, this))
                        {
                        case UNKNOWN:
                            // the component didn't know the name
                            m_node.log(errs, Severity.ERROR, Compiler.NAME_MISSING, m_sName, m_component.getIdentityConstant());
                            m_status = Status.ERROR;
                            return Result.ERROR;

                        case RESOLVED:
                            // the component resolved the name; advance to the next one
                            m_sName = m_iter.hasNext() ? m_iter.next() : null;
                            break;

                        case ERROR:
                            m_status = Status.ERROR;
                            return Result.ERROR;

                        case DEFERRED:
                            listRevisit.add(m_node);
                            return Result.DEFERRED;

                        default:
                            throw new IllegalStateException();
                        }
                    }

                // no names left to resolve
                m_status = Status.RESOLVED;
                // fall through

            case RESOLVED:
                // already resolved
                return Result.RESOLVED;

            default:
            case ERROR:
                // already determined to be unresolvable
                return Result.ERROR;
            }
        }

    /**
     * Obtain the component for the specified node, but only if the node is ready to resolve names.
     *
     * @param node  a ComponentStatement
     *
     * @return the corresponding Component, iff the ComponentStatement is ready to resolve names
     */
    private static Component getResolvingComponent(ComponentStatement node)
        {
        return node.canResolveNames()
                ? node.getComponent()
                : null;
        }

    /**
     * @return the component that is responsible for resolving the next name
     */
    private Component ensurePartiallyResolvedComponent()
        {
        assert m_component != null || (m_fTypeParam && m_constant != null);

        if (m_fTypeParam)
            {
            // TODO find the component that is the "extends ?" of the type param
            // the last resolve step resulted in a type parameter, so the next step must
            // also result in a type parameter
            if (m_constant instanceof RegisterConstant)
                {
                RegisterConstant constReg  = (RegisterConstant) m_constant;
                TypeConstant     constType = constReg.getMethod().getSignature().getRawParams()[constReg.getRegister()];
                }
            m_constant instanceof PropertyConstant;
            assert m_constant instanceof != null;
            // TODO "<T extends Map>" would mean use String as the componentResolver
            throw new UnsupportedOperationException();

            throw new UnsupportedOperationException("TODO");
            }

        return m_component;
        }

    /**
     * @return the result of the NameResolver thus far; the ERROR and RESOLVED states are the
     *         terminal states
     */
    public Result getResult()
        {
        switch (m_status)
            {
            case INITIAL:
            case CHECKED_IMPORTS:
            case RESOLVED_PARTIAL:
                // not done yet
                return Result.DEFERRED;

            case RESOLVED:
                // completed successfully
                return Result.RESOLVED;

            default:
            case ERROR:
                // cannot complete successfully
                return Result.ERROR;
            }
        }

    /**
     * @return the Constant that the NameResolver has resolved to thus far; this value can only be
     *         depended on after the NameResolver result is RESOLVED
     */
    public Constant getConstant()
        {
        return m_constant;
        }

    /**
     * @return the Component that the NameResolver has resolved to thus far, which may be null if
     *         the name resolves to something that is not representable as a Component; this value
     *         can only be depended on after the NameResolver result is RESOLVED
     */
    public Component getComponent()
        {
        return m_component;
        }

    /**
     * @return the ConstantPool
     */
    private ConstantPool getPool()
        {
        return m_node.getConstantPool();
        }


    // ----- ResolutionCollector -------------------------------------------------------------------

    @Override
    public ResolutionResult resolvedComponent(Component component)
        {
        assert !m_fTypeParam;

        // it is possible that the name "resolved to" an ambiguous component, which is an error
        if (component instanceof CompositeComponent && ((CompositeComponent) m_component).isAmbiguous())
            {
            m_node.log(m_errs, Severity.ERROR, Compiler.NAME_AMBIGUOUS, m_sName);
            m_status = Status.ERROR;
            return ResolutionResult.ERROR;
            }

        IdentityConstant constId = component.getIdentityConstant();
        if (component.getFormat() == Format.PROPERTY)
            {
            // while it resolved to a component, the component is a property, which indicates that
            // the name resolved to a type parameter
            return resolvedTypeParam(constId);
            }

        m_component = component;
        m_constant  = constId;
        return ResolutionResult.RESOLVED;
        }

    @Override
    public ResolutionResult resolvedTypeParam(Constant constParam)
        {
        assert constParam instanceof RegisterConstant || constParam instanceof PropertyConstant;

        m_constant   = constParam;
        m_component  = null;
        m_fTypeParam = true;

        return ResolutionResult.RESOLVED;
        }


    // ----- inner classes -------------------------------------------------------------------------

    /**
     * Classes that can provide a NameResolver should implement this interface.
     */
    public interface NameResolving
        {
        NameResolver getNameResolver();
        }

    /**
     * The result of an attempt to resolve the name.
     */
    public enum Result {DEFERRED, RESOLVED, ERROR}

    /**
     * The possible internal states for the resolver.
     */
    private enum Status {INITIAL, CHECKED_IMPORTS, RESOLVED_PARTIAL, RESOLVED, ERROR}


    // ----- fields --------------------------------------------------------------------------------

    /**
     * The node that this NameResolver is working for.
     */
    private AstNode          m_node;

    /**
     * The sequence of names to resolve.
     */
    private Iterator<String> m_iter;

    /**
     * The current simple name to resolve.
     */
    private String           m_sName;

    /**
     * The current internal status of the name resolution.
     */
    private Status           m_status = Status.INITIAL;

    /**
     * The import statement selected by the import-checking phase, if any possible match was found.
     */
    private ImportStatement  m_stmtImport;

    /**
     * The node that the import was registered with, if any possible import match was found.
     */
    private StatementBlock   m_blockImport;

    /**
     * The constant representing what the node has thus far resolved to.
     */
    private Constant         m_constant;

    /**
     * The component representing what the node has thus far resolved to.
     */
    private Component        m_component;

    /**
     * Set to true when the resolution has switched into "type param mode".
     */
    private boolean          m_fTypeParam;

    /**
     * The ErrorListener to log errors to.
     */
    private ErrorListener    m_errs;
    }
