package org.eclipse.refactoring;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;

public class IntroduceIndirectionRefactoring extends Refactoring {

	private static final String METHOD = "method";

	private static final String NAME = "name";

	private static final String REFERENCES = "references";

	private static final String TYPE = "type";

	private Map<ICompilationUnit, TextFileChange> fChanges = null;

	IMethod fMethod = null;

	String fName = null;

	IType fType = null;

	boolean fUpdateReferences = true;

	@SuppressWarnings("unchecked")
	private void addTypeParameters(ImportRewrite rewrite, List<TypeParameter> list, ITypeBinding type, AST ast) {

		ITypeBinding enclosing = type.getDeclaringClass();
		if (enclosing != null)
			addTypeParameters(rewrite, list, enclosing, ast);

		ITypeBinding[] typeParameters = type.getTypeParameters();
		for (ITypeBinding element : typeParameters) {
			TypeParameter parameter = ast.newTypeParameter();
			parameter.setName(ast.newSimpleName(element.getName()));
			ITypeBinding[] bounds = element.getTypeBounds();
			for (ITypeBinding element0 : bounds)
				if (!"java.lang.Object".equals(element0.getQualifiedName())) //$NON-NLS-1$
					parameter.typeBounds().add(rewrite.addImport(element0, ast));
			list.add(parameter);
		}
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor monitor)
			throws CoreException, OperationCanceledException {
		final RefactoringStatus status = new RefactoringStatus();
		try {
			monitor.beginTask("Checking preconditions...", 2);
			fChanges = new LinkedHashMap<ICompilationUnit, TextFileChange>();
			final Set<SearchMatch> invocations = new HashSet<SearchMatch>();

			if (fUpdateReferences) {
				IJavaSearchScope scope = SearchEngine.createWorkspaceScope();
				SearchPattern pattern = SearchPattern.createPattern(fMethod, IJavaSearchConstants.REFERENCES,
						SearchPattern.R_EXACT_MATCH);
				SearchEngine engine = new SearchEngine();
				engine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, scope,
						new SearchRequestor() {

							@Override
							public void acceptSearchMatch(SearchMatch match) throws CoreException {
								if (match.getAccuracy() == SearchMatch.A_ACCURATE && !match.isInsideDocComment())
									invocations.add(match);
							}
						}, new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			}

			final Map<ICompilationUnit, Collection<SearchMatch>> units = new HashMap<ICompilationUnit, Collection<SearchMatch>>();
			units.put(fMethod.getCompilationUnit(), new ArrayList<SearchMatch>());
			units.put(fType.getCompilationUnit(), new ArrayList<SearchMatch>());

			if (fUpdateReferences) {
				for (SearchMatch match : invocations) {
					Object element = match.getElement();
					if (element instanceof IMember) {
						ICompilationUnit unit = ((IMember) element).getCompilationUnit();
						if (unit != null) {
							Collection<SearchMatch> collection = units.get(unit);
							if (collection == null) {
								collection = new ArrayList<SearchMatch>();
								units.put(unit, collection);
							}
							collection.add(match);
						}
					}
				}
			}

			final Map<IJavaProject, Collection<ICompilationUnit>> projects = new HashMap<IJavaProject, Collection<ICompilationUnit>>();
			for (ICompilationUnit unit : units.keySet()) {
				IJavaProject project = unit.getJavaProject();
				if (project != null) {
					Collection<ICompilationUnit> collection = projects.get(project);
					if (collection == null) {
						collection = new ArrayList<ICompilationUnit>();
						projects.put(project, collection);
					}
					collection.add(unit);
				}
			}

			ASTRequestor requestors = new ASTRequestor() {

				@Override
				public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
					try {
						rewriteCompilationUnit(this, source, units.get(source), ast, status);
					} catch (CoreException exception) {
						RefactoringPlugin.log(exception);
					}
				}
			};

			IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);
			try {
				final Set<IJavaProject> set = projects.keySet();
				subMonitor.beginTask("Compiling source...", set.size());

				for (IJavaProject project : set) {
					ASTParser parser = ASTParser.newParser(AST.JLS3);
					parser.setProject(project);
					parser.setResolveBindings(true);
					Collection<ICompilationUnit> collection = projects.get(project);
					parser.createASTs(collection.toArray(new ICompilationUnit[collection.size()]), new String[0],
							requestors, new SubProgressMonitor(subMonitor, 1));
				}

			} finally {
				subMonitor.done();
			}

		} finally {
			monitor.done();
		}
		return status;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor monitor)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		try {
			monitor.beginTask("Checking preconditions...", 1);
			if (fMethod == null)
				status.merge(RefactoringStatus.createFatalErrorStatus("Method has not been specified."));
			else if (!fMethod.exists())
				status.merge(RefactoringStatus.createFatalErrorStatus(MessageFormat
						.format("Method ''{0}'' does not exist.", new Object[] { fMethod.getElementName() })));
			else {
				if (!fMethod.isBinary() && !fMethod.getCompilationUnit().isStructureKnown())
					status.merge(RefactoringStatus.createFatalErrorStatus(
							MessageFormat.format("Compilation unit ''{0}'' contains compile errors.",
									new Object[] { fMethod.getCompilationUnit().getElementName() })));
			}
		} finally {
			monitor.done();
		}
		return status;
	}

	private RefactoringStatus checkMethodName(String name, IStatus status) {
		RefactoringStatus result = new RefactoringStatus();
		if ("".equals(name)) //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus("Choose a name.");

		if (status.isOK())
			return result;

		switch (status.getSeverity()) {
		case IStatus.ERROR:
			return RefactoringStatus.createFatalErrorStatus(status.getMessage());
		case IStatus.WARNING:
			return RefactoringStatus.createWarningStatus(status.getMessage());
		case IStatus.INFO:
			return RefactoringStatus.createInfoStatus(status.getMessage());
		default: // no nothing
			return new RefactoringStatus();
		}
	}

	private RefactoringStatus checkOverloading() {
		try {
			if (fType != null) {
				IMethod[] methods = fType.getMethods();
				for (IMethod method : methods) {
					if (method.getElementName().equals(fName))
						return RefactoringStatus.createWarningStatus(
								MessageFormat.format("A method with the same name already exists.", fName));
				}
			}
		} catch (JavaModelException exception) {
			return RefactoringStatus.createFatalErrorStatus("Could not parse declaring type.");
		}
		return new RefactoringStatus();
	}

	@SuppressWarnings("unchecked")
	private void copyArguments(ImportRewrite rewrite, IMethodBinding binding, MethodDeclaration declaration, AST ast)
			throws JavaModelException {
		String[] names = fMethod.getParameterNames();
		ITypeBinding[] types = binding.getParameterTypes();
		for (int index = 0; index < names.length; index++) {
			ITypeBinding type = types[index];
			SingleVariableDeclaration variable = ast.newSingleVariableDeclaration();
			variable.setName(ast.newSimpleName(names[index]));

			if (index == (names.length - 1) && binding.isVarargs()) {
				variable.setVarargs(true);
				if (type.isArray())
					type = type.getComponentType();
			}

			variable.setType(rewrite.addImport(type, ast));
			declaration.parameters().add(variable);
		}
	}

	@SuppressWarnings("unchecked")
	private void copyExceptions(ImportRewrite rewrite, IMethodBinding binding, MethodDeclaration declaration, AST ast) {
		ITypeBinding[] types = binding.getExceptionTypes();
		for (ITypeBinding element : types) {
			declaration.thrownExceptions().add(ast.newName(rewrite.addImport(element)));
		}
	}

	@SuppressWarnings("unchecked")
	private void copyInvocationParameters(MethodInvocation invocation, AST ast) throws JavaModelException {
		String[] names = fMethod.getParameterNames();
		for (String element : names)
			invocation.arguments().add(ast.newSimpleName(element));
	}

	@SuppressWarnings("unchecked")
	private void copyTypeParameters(ImportRewrite rewrite, IMethodBinding binding, MethodDeclaration declaration,
			AST ast) {
		ITypeBinding[] types = binding.getTypeParameters();
		for (ITypeBinding element : types) {
			TypeParameter parameter = ast.newTypeParameter();
			parameter.setName(ast.newSimpleName(element.getName()));
			ITypeBinding[] bounds = element.getTypeBounds();
			for (ITypeBinding element0 : bounds)
				if (!"java.lang.Object".equals(element0.getQualifiedName())) //$NON-NLS-1$
					parameter.typeBounds().add(rewrite.addImport(element0, ast));

			declaration.typeParameters().add(parameter);
		}
	}

	@Override
	public Change createChange(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		try {
			monitor.beginTask("Creating change...", 1);
			final Collection<TextFileChange> changes = fChanges.values();
//			CompositeChange change = new CompositeChange(getName(), changes.toArray(new Change[changes.size()])) {
//
//				@Override
//				public ChangeDescriptor getDescriptor() {
//					String project = fMethod.getJavaProject().getElementName();
//					String description = MessageFormat.format("Introduce indirection for ''{0}''",
//							new Object[] { fMethod.getElementName() });
//					String methodLabel = JavaElementLabels.getTextLabel(fMethod, JavaElementLabels.ALL_FULLY_QUALIFIED);
//					String typeLabel = JavaElementLabels.getTextLabel(fType, JavaElementLabels.ALL_FULLY_QUALIFIED);
//					String comment = MessageFormat.format("Introduce indirection for ''{0}'' in ''{1}''",
//							new Object[] { methodLabel, typeLabel });
//					Map<String, String> arguments = new HashMap<String, String>();
//					arguments.put(METHOD, fMethod.getHandleIdentifier());
//					arguments.put(TYPE, fType.getHandleIdentifier());
//					arguments.put(NAME, fName);
//					arguments.put(REFERENCES, Boolean.valueOf(fUpdateReferences).toString());
//					return new RefactoringChangeDescriptor(
//							new IntroduceIndirectionDescriptor(project, description, comment, arguments));
//				}
//			};
			return Utilities.createContextClass(fMethod);
		} finally {
			monitor.done();
		}
	}

	private Statement createInvocationStatement(MethodDeclaration declaration, MethodInvocation invocation) {
		final Type type = declaration.getReturnType2();

		if (type == null || (type instanceof PrimitiveType
				&& PrimitiveType.VOID.equals(((PrimitiveType) type).getPrimitiveTypeCode())))
			return invocation.getAST().newExpressionStatement(invocation);

		ReturnStatement statement = invocation.getAST().newReturnStatement();
		statement.setExpression(invocation);
		return statement;
	}

	private IMethodBinding findMethodInHierarchy(ITypeBinding type, IMethodBinding binding) {
		IMethodBinding method = findMethodInType(type, binding);
		if (method != null)
			return method;
		ITypeBinding superClass = type.getSuperclass();
		if (superClass != null) {
			method = findMethodInHierarchy(superClass, binding);
			if (method != null)
				return method;
		}
		ITypeBinding[] interfaces = type.getInterfaces();
		for (ITypeBinding element : interfaces) {
			method = findMethodInHierarchy(element, binding);
			if (method != null)
				return method;
		}
		return null;
	}

	private IMethodBinding findMethodInType(ITypeBinding type, IMethodBinding binding) {
		if (type.isPrimitive())
			return null;

		IMethodBinding[] methods = type.getDeclaredMethods();
		for (IMethodBinding element : methods) {
			if (element.isSubsignature(binding))
				return element;
		}
		return null;
	}

	public IType getDeclaringType() {
		return fType;
	}

	private ITypeBinding getEnclosingType(ASTNode node) {
		while (node != null) {
			if (node instanceof AbstractTypeDeclaration) {
				return ((AbstractTypeDeclaration) node).resolveBinding();
			} else if (node instanceof AnonymousClassDeclaration) {
				return ((AnonymousClassDeclaration) node).resolveBinding();
			}
			node = node.getParent();
		}
		return null;
	}

	private ASTNode getEnclosingTypeDeclaration(ASTNode node) {
		while (node != null) {
			if (node instanceof AbstractTypeDeclaration) {
				return node;
			} else if (node instanceof AnonymousClassDeclaration) {
				return node;
			}
			node = node.getParent();
		}
		return null;
	}

	public IMethod getMethod() {
		return fMethod;
	}

	public String getMethodName() {
		return fName;
	}

	@Override
	public String getName() {
		return "Introduce Indirection";
	}

	private ASTNode getParent(ASTNode node, Class parentClass) {
		do {
			node = node.getParent();
		} while (node != null && !parentClass.isInstance(node));
		return node;
	}

	private ITypeBinding getTypeBinding(Name node) {
		IBinding binding = node.resolveBinding();
		if (binding instanceof ITypeBinding)
			return (ITypeBinding) binding;
		return null;
	}

	public RefactoringStatus initialize(Map arguments) {
		RefactoringStatus status = new RefactoringStatus();
		String value = (String) arguments.get(METHOD);
		if (value != null)
			setMethod((IMethod) JavaCore.create(value));
		value = (String) arguments.get(TYPE);
		if (value != null)
			setDeclaringType((IType) JavaCore.create(value));
		value = (String) arguments.get(NAME);
		if (value != null)
			setMethodName(value);
		value = (String) arguments.get(REFERENCES);
		if (value != null)
			setUpdateReferences(Boolean.valueOf(value).booleanValue());
		return status;
	}

	private boolean isEmptyEdit(TextEdit edit) {
		return edit.getClass() == MultiTextEdit.class && !edit.hasChildren();
	}

	private boolean isParentNode(ASTNode node, ASTNode parent) {
		Assert.isNotNull(parent);
		do {
			node = node.getParent();
			if (node == parent)
				return true;
		} while (node != null);
		return false;
	}

	private void rewriteAST(ICompilationUnit unit, ASTRewrite astRewrite, ImportRewrite importRewrite) {
		try {
			MultiTextEdit edit = new MultiTextEdit();
			TextEdit astEdit = astRewrite.rewriteAST();

			if (!isEmptyEdit(astEdit))
				edit.addChild(astEdit);
			TextEdit importEdit = importRewrite.rewriteImports(new NullProgressMonitor());
			if (!isEmptyEdit(importEdit))
				edit.addChild(importEdit);
			if (isEmptyEdit(edit))
				return;

			TextFileChange change = fChanges.get(unit);
			if (change == null) {
				change = new TextFileChange(unit.getElementName(), (IFile) unit.getResource());
				change.setTextType("java");
				change.setEdit(edit);
			} else
				change.getEdit().addChild(edit);

			fChanges.put(unit, change);
		} catch (MalformedTreeException exception) {
			RefactoringPlugin.log(exception);
		} catch (IllegalArgumentException exception) {
			RefactoringPlugin.log(exception);
		} catch (CoreException exception) {
			RefactoringPlugin.log(exception);
		}
	}

	protected void rewriteCompilationUnit(ASTRequestor requestor, ICompilationUnit unit, Collection matches,
			CompilationUnit node, RefactoringStatus status) throws CoreException {
		ASTRewrite astRewrite = ASTRewrite.create(node.getAST());
		ImportRewrite importRewrite = ImportRewrite.create(node, true);
		if (unit.equals(fType.getCompilationUnit()))
			rewriteDeclaringType(requestor, astRewrite, importRewrite, unit, node);
		if (!fUpdateReferences) {
			rewriteAST(unit, astRewrite, importRewrite);
			return;
		}
		for (final Iterator iterator = matches.iterator(); iterator.hasNext();) {
			SearchMatch match = (SearchMatch) iterator.next();
			if (match.getAccuracy() == SearchMatch.A_ACCURATE) {
				ASTNode result = NodeFinder.perform(node, match.getOffset(), match.getLength());
				if (result instanceof MethodInvocation)
					status.merge(
							rewriteMethodInvocation(requestor, astRewrite, importRewrite, (MethodInvocation) result));
			}
		}
		rewriteAST(unit, astRewrite, importRewrite);
	}

	@SuppressWarnings("unchecked")
	private void rewriteDeclaringType(ASTRequestor requestor, ASTRewrite astRewrite, ImportRewrite importRewrite,
			ICompilationUnit unit, CompilationUnit node) throws CoreException {

		IMethodBinding methodBinding = null;
		ITypeBinding firstParameterType = null;
		IBinding[] bindings = requestor.createBindings(new String[] { fMethod.getKey() });
		if (bindings[0] instanceof IMethodBinding) {
			methodBinding = (IMethodBinding) bindings[0];
			if (methodBinding != null)
				firstParameterType = methodBinding.getDeclaringClass();
		}

		if (methodBinding == null || firstParameterType == null)
			return;

		AST ast = node.getAST();

		// Method declaration
		MethodDeclaration methodDeclaration = ast.newMethodDeclaration();

		// Name
		methodDeclaration.setName(ast.newSimpleName(fName));

		// Flags
		List<Modifier> modifiers = methodDeclaration.modifiers();
		modifiers.add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		modifiers.add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));

		// Parameters
		if (!Flags.isStatic(fMethod.getFlags())) {

			// Add first parameter
			SingleVariableDeclaration variable = ast.newSingleVariableDeclaration();
			Type type = importRewrite.addImport(firstParameterType, ast);
			if (firstParameterType.isGenericType()) {
				ParameterizedType parameterized = ast.newParameterizedType(type);
				ITypeBinding[] typeParameters = firstParameterType.getTypeParameters();
				for (ITypeBinding element : typeParameters)
					parameterized.typeArguments().add(importRewrite.addImport(element, ast));
				type = parameterized;
			}
			variable.setType(type);
			variable.setName(ast.newSimpleName("target"));
			methodDeclaration.parameters().add(variable);
		}
		// Add other parameters
		copyArguments(importRewrite, methodBinding, methodDeclaration, ast);

		// Add type parameters of declaring class (and enclosing classes)
		if (!Flags.isStatic(fMethod.getFlags()) && firstParameterType.isGenericType())
			addTypeParameters(importRewrite, methodDeclaration.typeParameters(), firstParameterType, ast);

		// Add type parameters of method
		copyTypeParameters(importRewrite, methodBinding, methodDeclaration, ast);

		// Return type
		methodDeclaration.setReturnType2(importRewrite.addImport(methodBinding.getReturnType(), ast));

		// Exceptions
		copyExceptions(importRewrite, methodBinding, methodDeclaration, ast);

		// Body
		MethodInvocation invocation = ast.newMethodInvocation();
		invocation.setName(ast.newSimpleName(fMethod.getElementName()));
		if (Flags.isStatic(fMethod.getFlags())) {
			Type type = importRewrite.addImport(methodBinding.getDeclaringClass(), ast);
			invocation.setExpression(ast.newName(type.toString()));
		} else {
			invocation.setExpression(ast.newSimpleName("target"));
		}
		copyInvocationParameters(invocation, ast);

		final Block body = ast.newBlock();
		body.statements().add(createInvocationStatement(methodDeclaration, invocation));
		methodDeclaration.setBody(body);

		// Comment
		if (Boolean.valueOf(
				PreferenceConstants.getPreference(PreferenceConstants.CODEGEN_ADD_COMMENTS, unit.getJavaProject()))
				.booleanValue()) {
			String comment = CodeGeneration.getMethodComment(unit, fType.getFullyQualifiedName('.'), methodDeclaration,
					null, "\n");
			if (comment != null) {
				Javadoc javadoc = (Javadoc) astRewrite.createStringPlaceholder(comment, ASTNode.JAVADOC);
				methodDeclaration.setJavadoc(javadoc);
			}
		}

		AbstractTypeDeclaration declaration = (AbstractTypeDeclaration) typeToDeclaration(fType, node);
		ChildListPropertyDescriptor descriptor = typeToBodyDeclarationProperty(fType, node);

		astRewrite.getListRewrite(declaration, descriptor).insertLast(methodDeclaration, null);
	}

	@SuppressWarnings("unchecked")
	private RefactoringStatus rewriteMethodInvocation(ASTRequestor requestor, ASTRewrite astRewrite,
			ImportRewrite importRewrite, MethodInvocation oldInvocation) throws JavaModelException {
		RefactoringStatus status = new RefactoringStatus();

		ITypeBinding declaringBinding = null;
		IBinding[] bindings = requestor.createBindings(new String[] { fType.getKey() });
		if (bindings[0] instanceof ITypeBinding) {
			declaringBinding = (ITypeBinding) bindings[0];
		}

		if (declaringBinding == null)
			return status;

		AST ast = oldInvocation.getAST();

		// If the method invocation uses type arguments, skip this call as the
		// new target method may have additional parameters
		if (oldInvocation.typeArguments().size() > 0)
			return RefactoringStatus.createWarningStatus(MessageFormat.format(
					"The method invocation ''{0}'' uses type arguments. This occurrence will not be updated.",
					oldInvocation.toString()));

		MethodInvocation newInvocation = ast.newMethodInvocation();
		List<ASTNode> newArguments = newInvocation.arguments();
		List<ASTNode> oldArguments = oldInvocation.arguments();

		newInvocation.setExpression(ast.newName(importRewrite.addImport(declaringBinding)));
		newInvocation.setName(ast.newSimpleName(getMethodName()));

		Expression expression = oldInvocation.getExpression();

		if (!Flags.isStatic(fMethod.getFlags())) {
			// Add the expression as the first parameter
			if (expression == null) {
				// There is no expression for this call.
				// Use a (possibly qualified) "this" expression.
				ThisExpression thisExpression = ast.newThisExpression();
				RefactoringStatus result = rewriteThisExpression(thisExpression, oldInvocation);
				status.merge(result);
				if (result.hasEntries())
					// warning means don't include this invocation
					return status;
				newArguments.add(thisExpression);
			} else {
				newArguments.add(astRewrite.createMoveTarget(expression));
			}
		} else {
			if (expression != null) {
				// Check if expression is the class name.
				// If not, there may be side effects (e.g. inside methods)
				if (!(expression instanceof Name) || getTypeBinding((Name) expression) == null)
					return RefactoringStatus.createWarningStatus(MessageFormat.format(
							"The target method is static, but the method invocation ''{0}'' is based on an expression rather than the class itself. This occurrence will not be updated.",
							oldInvocation.toString()));
			}
		}

		for (int index = 0; index < oldArguments.size(); index++)
			newArguments.add(astRewrite.createMoveTarget(oldArguments.get(index)));

		astRewrite.replace(oldInvocation, newInvocation, null);

		return status;
	}

	private RefactoringStatus rewriteThisExpression(ThisExpression expression, MethodInvocation invocation) {
		RefactoringStatus status = new RefactoringStatus();

		AST ast = invocation.getAST();

		IMethodBinding binding = invocation.resolveMethodBinding();
		MethodDeclaration declaration = (MethodDeclaration) ((CompilationUnit) invocation.getRoot())
				.findDeclaringNode(binding);

		ITypeBinding declaringBinding = null;
		if (declaration != null) {
			// Declaring class is inside this compilation unit => use its name
			// if it's declared in an enclosing type
			if (isParentNode(invocation, declaration.getParent()))
				declaringBinding = binding.getDeclaringClass();
			else
				declaringBinding = getEnclosingType(invocation);
		} else {
			// Declaring class is outside of this compilation unit
			// Find subclass in this compilation unit.
			ASTNode enclosing = getEnclosingTypeDeclaration(invocation);
			declaringBinding = getEnclosingType(enclosing);
			while (enclosing != null && findMethodInHierarchy(declaringBinding, binding) == null) {
				enclosing = getEnclosingTypeDeclaration(enclosing.getParent());
				declaringBinding = getEnclosingType(enclosing);
			}
		}

		if (declaringBinding == null) {
			status.merge(RefactoringStatus.createWarningStatus(MessageFormat.format(
					"The declaring type of the method invocation ''{0}'' could not be found. This occurrence will not be updated.",
					invocation.toString())));
			return status;
		}

		ITypeBinding type = getEnclosingType(invocation);
		if (!type.equals(declaringBinding)) {
			if (declaringBinding.isAnonymous()) {
				status.merge(RefactoringStatus.createWarningStatus(MessageFormat.format(
						"The declaring type of the method invocation ''{0}'' is anonymous and therefore cannot be qualified. This occurrence will not be updated.",
						invocation.toString())));
			} else {
				expression.setQualifier(ast.newSimpleName(declaringBinding.getName()));
			}
		} else {
			// Do not qualify
		}

		return status;
	}

	public void setDeclaringType(IType type) {
		fType = type;
	}

	public RefactoringStatus setDeclaringTypeName(String name) {
		IType type = null;

		try {
			if (name.length() == 0)
				return RefactoringStatus.createFatalErrorStatus("Select a type.");

			type = fMethod.getJavaProject().findType(name, new NullProgressMonitor());
			if (type == null || !type.exists())
				return RefactoringStatus.createErrorStatus(MessageFormat.format("Type ''{0}'' does not exist.", name));
			if (type.isAnnotation())
				return RefactoringStatus.createErrorStatus("Type must not be an annotation.");
			if (type.isInterface())
				return RefactoringStatus.createErrorStatus("Type must not be an interface.");
		} catch (JavaModelException exception) {
			return RefactoringStatus.createFatalErrorStatus("Could not determine type.");
		}

		if (type.isReadOnly())
			return RefactoringStatus.createErrorStatus("Type is read-only.");

		if (type.isBinary())
			return RefactoringStatus.createErrorStatus("Type is binary.");

		fType = type;

		return new RefactoringStatus();
	}

	public void setMethod(IMethod method) {
		fMethod = method;
		fName = fMethod.getElementName();
		fType = fMethod.getDeclaringType();
	}

	public RefactoringStatus setMethodName(String name) {
		fName = name;
		RefactoringStatus status = checkMethodName(name, JavaConventions.validateMethodName(name));
		status.merge(checkOverloading());
		return status;
	}

	public void setUpdateReferences(boolean update) {
		fUpdateReferences = update;
	}

	private ChildListPropertyDescriptor typeToBodyDeclarationProperty(IType type, CompilationUnit node)
			throws JavaModelException {
		ASTNode result = typeToDeclaration(type, node);
		if (result instanceof AbstractTypeDeclaration)
			return ((AbstractTypeDeclaration) result).getBodyDeclarationsProperty();
		else if (result instanceof AnonymousClassDeclaration)
			return AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY;

		Assert.isTrue(false);
		return null;
	}

	private ASTNode typeToDeclaration(IType type, CompilationUnit node) throws JavaModelException {
		Name result = (Name) NodeFinder.perform(node, type.getNameRange());
		if (type.isAnonymous())
			return getParent(result, AnonymousClassDeclaration.class);
		return getParent(result, AbstractTypeDeclaration.class);
	}
}