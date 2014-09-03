/*******************************************************************************
 * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.common.types.xtext.ui;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.internal.compiler.env.AccessRestriction;
import org.eclipse.jdt.internal.core.search.BasicSearchEngine;
import org.eclipse.jdt.internal.core.search.IRestrictedAccessTypeRequestor;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.TypesPackage;
import org.eclipse.xtext.common.types.access.IJvmTypeProvider;
import org.eclipse.xtext.common.types.access.jdt.IJavaProjectProvider;
import org.eclipse.xtext.common.types.access.jdt.IJdtTypeProvider;
import org.eclipse.xtext.common.types.access.jdt.JdtTypeProviderFactory;
import org.eclipse.xtext.common.types.util.RawSuperTypes;
import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.ui.editor.IDirtyStateManager;
import org.eclipse.xtext.ui.editor.contentassist.ConfigurableCompletionProposal;
import org.eclipse.xtext.ui.editor.contentassist.ConfigurableCompletionProposal.IReplacementTextApplier;
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext.Builder;
import org.eclipse.xtext.ui.editor.contentassist.ICompletionProposalAcceptor;
import org.eclipse.xtext.ui.editor.contentassist.ICompletionProposalFactory;
import org.eclipse.xtext.ui.editor.contentassist.IContentProposalPriorities;
import org.eclipse.xtext.ui.editor.contentassist.PrefixMatcher;
import org.eclipse.xtext.ui.editor.contentassist.ReplacementTextApplier;
import org.eclipse.xtext.ui.editor.hover.IEObjectHover;
import org.eclipse.xtext.util.Strings;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 * @author Jan Koehnlein - introduced QualifiedName
 * @author Christoph Kulla - added support for hovers
 */
public class JdtTypesProposalProvider extends AbstractTypesProposalProvider {

	@Inject
	private IJavaProjectProvider projectProvider;
	
	@Inject
	private IScopeProvider scopeProvider;
	
	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;
	
	@Inject
	private IEObjectHover hover;
	
	@Inject
	private JdtTypeProviderFactory jdtTypeProviderFatory;
	
	@Inject
	private JdtTypeRelevance jdtTypeRelevance;
	
	@Inject
	private IContentProposalPriorities priorities;

	@Inject
	private IDirtyStateManager dirtyStateManager;
	
	@Inject
	private RawSuperTypes superTypeCollector;
	
	public static class FQNShortener extends ReplacementTextApplier {
		protected final IScope scope;
		protected final Resource context;
		protected final IQualifiedNameConverter qualifiedNameConverter;
		protected final IValueConverter<String> valueConverter;
		
		public FQNShortener(Resource context, IScope scope, IQualifiedNameConverter qualifiedNameConverter, IValueConverter<String> valueConverter) {
			this.context = context;
			this.scope = scope;
			this.qualifiedNameConverter = qualifiedNameConverter;
			this.valueConverter = valueConverter;
		}
		
		protected String applyValueConverter(QualifiedName qualifiedName) {
			String result = qualifiedNameConverter.toString(qualifiedName);
			if (valueConverter != null)
				result = valueConverter.toString(result);
			return result;
		}
		
		@Override
		public String getActualReplacementString(ConfigurableCompletionProposal proposal) {
			String replacementString = proposal.getReplacementString();
			if (scope != null) {
				String qualifiedNameAsString = replacementString;
				if (valueConverter != null) {
					qualifiedNameAsString = valueConverter.toValue(qualifiedNameAsString, null);
				}
				IEObjectDescription element = scope.getSingleElement(qualifiedNameConverter.toQualifiedName(qualifiedNameAsString));
				if (element != null) {
					EObject resolved = EcoreUtil.resolve(element.getEObjectOrProxy(), context);
					if (!resolved.eIsProxy()) {
						IEObjectDescription shortendElement = scope.getSingleElement(resolved);
						if (shortendElement != null)
							replacementString = applyValueConverter(shortendElement.getName());
					}
				}
			}
			return replacementString;
		}
	}
	
	public void createSubTypeProposals(final JvmType superType, ICompletionProposalFactory proposalFactory, 
			ContentAssistContext context, EReference typeReference, final Filter filter, IValueConverter<String> valueConverter, ICompletionProposalAcceptor acceptor) {
		if (superType == null || superType.eIsProxy())
			return;
		if (superType.eResource() == null || superType.eResource().getResourceSet() == null)
			return;
		IJavaProject project = getProjectProvider().getJavaProject(superType.eResource().getResourceSet());
		if (project == null)
			return;
		
		final String superTypeIdentifier = superType.getIdentifier();
		// java.lang.Object - no need to create hierarchy scope
		if (Object.class.getName().equals(superTypeIdentifier)) {
			createTypeProposals(project, proposalFactory, context, typeReference, filter, valueConverter, acceptor);
			return;
		} 
		try {
			Iterable<IEObjectDescription> dirtyTypes = dirtyStateManager.getExportedObjectsByType(TypesPackage.Literals.JVM_TYPE);
			final Set<String> dirtyNames = Sets.newHashSet();
			for(IEObjectDescription description: dirtyTypes) {
				dirtyNames.add(description.getQualifiedName().toString());
			}
			final Set<String> superTypeNames = Sets.newHashSet();
			final IJdtTypeProvider provider = jdtTypeProviderFatory.createTypeProvider(superType.eResource().getResourceSet());
			IJavaSearchScope scope = createSearchScope(project, superType, superTypeNames);
			searchAndCreateProposals(scope, proposalFactory, context, typeReference, TypeMatchFilters.and(filter, new ITypesProposalProvider.Filter() {
				public boolean accept(int modifiers, char[] packageName, char[] simpleTypeName,
						char[][] enclosingTypeNames, String path) {
					if (path == null || path.endsWith(".class") || path.endsWith(".java")) { // Java index match
						String identifier = getIdentifier(packageName, simpleTypeName, enclosingTypeNames);
						if (dirtyNames.contains(identifier)) { // currently dirty, will be processed later (as path with another extension)
							return false;
						}
						if (!superTypeNames.contains(identifier)) {
							return true;
						}
						return false;
					} else { // accept match from dirty state
						String identifier = getIdentifier(packageName, simpleTypeName, enclosingTypeNames);
						if (identifier.equals(superTypeIdentifier)) {
							return true;
						}
						JvmType candidate = provider.findTypeByName(identifier, true);
						if (superTypeCollector.collect(candidate).contains(superType)) {
							return true;
						}
						return false;
					}
				}
				
				protected String getIdentifier(char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames) {
					StringBuilder result = new StringBuilder(packageName.length + simpleTypeName.length + 1);
					if (packageName.length != 0) {
						result.append(packageName);
						result.append('.');
					}
					for(char[] enclosingType: enclosingTypeNames) {
						result.append(enclosingType);
						result.append('$');
					}
					result.append(simpleTypeName);
					return result.toString();
				}
				
				public int getSearchFor() {
					return filter.getSearchFor();
				}
				
			}), valueConverter, acceptor);
		} catch(JavaModelException ex) {
			// ignore
		}
	}

	/**
	 * @param project the current Java project
	 * 
	 * @since 2.7
	 */
	protected IJavaSearchScope createSearchScope(IJavaProject project, JvmType superType, Set<String> superTypeNames) throws JavaModelException {
		IType type = project.findType(superType.getIdentifier());
		if (type == null) {
			return new IntersectingJavaSearchScope(); // empty intersection
		}
		try {
			// Method is available in JDT 3.6 or better
			// more than twice as fast as the older alternative
			Method method = SearchEngine.class.getMethod("createStrictHierarchyScope", IJavaProject.class, IType.class, Boolean.TYPE, Boolean.TYPE, WorkingCopyOwner.class);
			method.setAccessible(true);
			IJavaSearchScope result = (IJavaSearchScope) method.invoke(null, project, type, Boolean.TRUE, Boolean.TRUE, null);
			return result;
		} catch (Exception e) {
			final Collection<JvmType> superTypes = superTypeCollector.collect(superType);
			for(JvmType collectedSuperType: superTypes) {
				superTypeNames.add(collectedSuperType.getIdentifier());
			}
			superTypeNames.remove(superType.getIdentifier());
			IJavaSearchScope hierarchyScope = SearchEngine.createHierarchyScope(type);
			IJavaSearchScope projectScope = SearchEngine.createJavaSearchScope(new IJavaElement[] { project });
			IJavaSearchScope result = new IntersectingJavaSearchScope(projectScope, hierarchyScope);
			return result;
		}
	}

	protected void searchAndCreateProposals(IJavaSearchScope scope, final ICompletionProposalFactory proposalFactory,
			ContentAssistContext context, EReference typeReference, final Filter filter, 
			final IValueConverter<String> valueConverter, final ICompletionProposalAcceptor acceptor) throws JavaModelException {
		String prefix = context.getPrefix();
		List<String> split = Strings.split(prefix, '.');
		char[] typeName = null;
		char[] packageName = null;
		if (prefix.length() > 0 && !split.isEmpty()) {
			CharMatcher dotMatcher = CharMatcher.is('.');
			if (Character.isUpperCase(split.get(split.size() - 1).charAt(0))) {
				typeName = split.get(split.size() - 1).toCharArray();
				if (split.size() > 1)
					packageName = ("*" + dotMatcher.replaceFrom(prefix.substring(0, prefix.length() - (typeName.length + 1)), "*.") + "*").toCharArray();
			} else {
				if (prefix.endsWith("."))
					prefix = prefix.substring(0, prefix.length() - 1);
				packageName = ("*" + dotMatcher.replaceFrom(prefix, "*.") + "*").toCharArray();
			}
		}
		IScope typeScope = null;
		if (context.getCurrentModel() != null) {
			typeScope = scopeProvider.getScope(context.getCurrentModel(), typeReference);
		}
		final IReplacementTextApplier textApplier = createTextApplier(context, typeScope, qualifiedNameConverter, valueConverter);
		final ICompletionProposalAcceptor scopeAware = textApplier != null ? new ICompletionProposalAcceptor.Delegate(acceptor) {
			@Override
			public void accept(ICompletionProposal proposal) {
				if (proposal instanceof ConfigurableCompletionProposal) {
					((ConfigurableCompletionProposal) proposal).setTextApplier(textApplier);
				}
				super.accept(proposal);
			}
		} : acceptor;
		Builder contextBuilder = context.copy();
		final PrefixMatcher original = context.getMatcher();
		contextBuilder.setMatcher(new PrefixMatcher() {
			@Override
			public boolean isCandidateMatchingPrefix(String name, String prefix) {
				if (original.isCandidateMatchingPrefix(name, prefix))
					return true;
				String nameWithoutDollars = name.replace('$', '.');
				String prefixWithoutDollars = prefix.replace('$', '.');
				final boolean nameOrPrefixHasDollars = (nameWithoutDollars != name) || (prefixWithoutDollars != prefix);
				if (nameOrPrefixHasDollars
						&& original.isCandidateMatchingPrefix(nameWithoutDollars, prefixWithoutDollars))
					return true;
				String sub = nameWithoutDollars;
				int delimiter = sub.indexOf('.');
				while(delimiter != -1) {
					sub = sub.substring(delimiter + 1);
					delimiter = sub.indexOf('.');
					if (delimiter == -1 || prefixWithoutDollars.length() > 0 && Character.isLowerCase(prefixWithoutDollars.charAt(0))) {
						if (original.isCandidateMatchingPrefix(sub, prefixWithoutDollars))
							return true;
					}
				}
				return false;
			}
		});
		final ContentAssistContext myContext = contextBuilder.toContext();
		final IJvmTypeProvider jvmTypeProvider = jdtTypeProviderFatory.findOrCreateTypeProvider(context.getResource().getResourceSet());
		BasicSearchEngine searchEngine = new BasicSearchEngine();
		searchEngine.searchAllTypeNames(
				packageName, SearchPattern.R_PATTERN_MATCH, 
				typeName, SearchPattern.R_PREFIX_MATCH | SearchPattern.R_CAMELCASE_MATCH, 
				filter.getSearchFor(), scope, 
				new IRestrictedAccessTypeRequestor() {
					public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName,
							char[][] enclosingTypeNames, String path, AccessRestriction access) {
						if (filter.accept(modifiers, packageName, simpleTypeName, enclosingTypeNames, path) && (!checkAccessRestriction() || (access == null || access.getProblemId() != IProblem.ForbiddenReference && !access.ignoreIfBetter()))) {
							StringBuilder fqName = new StringBuilder(packageName.length + simpleTypeName.length + 1);
							if (packageName.length != 0) {
								fqName.append(packageName);
								fqName.append('.');
							}
							for(char[] enclosingType: enclosingTypeNames) {
								/*
								 * the JDT index sometimes yields enclosingTypeNames in the form
								 * char[][] { { Outer$Middle } }
								 * rather than
								 * char[][] { { Outer }, { Middle } }
								 * thus we create the fqName as the binary name and post process the proposal later on
								 */
								fqName.append(enclosingType);
								fqName.append('$');
							}
							fqName.append(simpleTypeName);
							String fqNameAsString = fqName.toString();
							createTypeProposal(fqNameAsString, modifiers, enclosingTypeNames.length>0, proposalFactory, myContext, scopeAware, jvmTypeProvider, valueConverter);
						}
					}
				}, 
				IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, 
				new NullProgressMonitor() {
					@Override
					public boolean isCanceled() {
						return !acceptor.canAcceptMoreProposals();
					}
				});
		if (acceptor.canAcceptMoreProposals()) {
			Iterable<IEObjectDescription> allDirtyTypes = dirtyStateManager.getExportedObjectsByType(TypesPackage.Literals.JVM_TYPE);
			for(IEObjectDescription description: allDirtyTypes) {
				QualifiedName qualifiedName = description.getQualifiedName();
				if (filter.accept(Flags.AccPublic, qualifiedName.skipLast(1).toString().toCharArray(), qualifiedName.getLastSegment().toCharArray(), new char[0][0], description.getEObjectURI().toPlatformString(true))) {
					String fqName = description.getQualifiedName().toString();
					createTypeProposal(fqName, Flags.AccPublic, fqName.indexOf('$') > 0, proposalFactory, myContext, scopeAware, jvmTypeProvider, valueConverter);
				}
			}
		}
	}
	
	/**
	 * @since 2.4
	 */
	protected boolean checkAccessRestriction() {
		return true;
	}

	protected ConfigurableCompletionProposal.IReplacementTextApplier createTextApplier(
			ContentAssistContext context, IScope typeScope, 
			IQualifiedNameConverter qualifiedNameConverter, IValueConverter<String> valueConverter) {
		return new FQNShortener(context.getResource(), typeScope, qualifiedNameConverter, valueConverter);
	}
	
	public void createTypeProposals(ICompletionProposalFactory proposalFactory, ContentAssistContext context, 
			EReference typeReference, Filter filter, IValueConverter<String> valueConverter, ICompletionProposalAcceptor acceptor) {
		EObject model = context.getCurrentModel();
		if (model == null || model.eResource() == null || model.eResource().getResourceSet() == null)
			return;
		IJavaProject javaProject = projectProvider.getJavaProject(model.eResource().getResourceSet());
		createTypeProposals(javaProject, proposalFactory, context, typeReference, filter, valueConverter, acceptor);
	}
	
	public void createTypeProposals(IJavaProject project, ICompletionProposalFactory proposalFactory, ContentAssistContext context,
			EReference typeReference, Filter filter, IValueConverter<String> valueConverter, ICompletionProposalAcceptor acceptor) {
		try {
			IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] { project });
			searchAndCreateProposals(searchScope, proposalFactory, context, typeReference, filter, valueConverter, acceptor);
		}
		catch (JavaModelException e) {
			// ignore
		}
	}

	protected void createTypeProposal(final String typeName, int modifiers, boolean isInnerType, ICompletionProposalFactory proposalFactory, 
			ContentAssistContext context, ICompletionProposalAcceptor acceptor, final IJvmTypeProvider jvmTypeProvider, IValueConverter<String> valueConverter) {
		if (acceptor.canAcceptMoreProposals()) {
			int lastDot = typeName.lastIndexOf('.');
			final StyledString displayString;
			if (lastDot != -1) {
				if (isInnerType) {
					displayString = new StyledString(typeName.substring(lastDot + 1).replace('$', '.')).append(" - " + typeName.substring(0, lastDot), StyledString.QUALIFIER_STYLER); 
				} else {
					displayString = new StyledString(typeName.substring(lastDot + 1)).append(" - " + typeName.substring(0, lastDot), StyledString.QUALIFIER_STYLER);
				}
			} else {
				displayString = new StyledString(isInnerType ? typeName.replace('$', '.') : typeName);
			}
			Image img = computeImage(typeName, isInnerType, modifiers);
			String proposalAsString = typeName;
			if (valueConverter != null) {
				try {
					proposalAsString = valueConverter.toString(isInnerType ? proposalAsString.replace('$', '.') : proposalAsString);
				} catch(ValueConverterException vce) {
					return;
				}
			}
			ICompletionProposal proposal = proposalFactory.createCompletionProposal(proposalAsString, displayString, img, context);
			if (proposal instanceof ConfigurableCompletionProposal) {
				ConfigurableCompletionProposal theProposal = (ConfigurableCompletionProposal) proposal;
				// calculate the type lazy, as this require a lot of time for large completion lists
				theProposal.setAdditionalProposalInfo(new Provider<EObject>(){
					public EObject get() {
						return jvmTypeProvider.findTypeByName(typeName);
					}});
				theProposal.setHover(hover);
				priorities.adjustCrossReferencePriority(theProposal, context.getPrefix());
				theProposal.setPriority(theProposal.getPriority() + jdtTypeRelevance.getRelevance(typeName, context.getPrefix()));
			}
			acceptor.accept(proposal);
		}
	}

	protected Image computeImage(String typeName, boolean isInnerType, int modifiers) {
		return JavaPlugin.getImageDescriptorRegistry().get(
				JavaElementImageProvider.getTypeImageDescriptor(
						isInnerType, 
						Flags.isAnnotation(modifiers) || Flags.isInterface(modifiers), 
						modifiers, 
						false /* don't use light icons */));
	}

	public void setProjectProvider(IJavaProjectProvider projectProvider) {
		this.projectProvider = projectProvider;
	}

	public IJavaProjectProvider getProjectProvider() {
		return projectProvider;
	}

}
