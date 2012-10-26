/*******************************************************************************
 * Copyright (c) 2011 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xbase.ui.jvmmodel.refactoring;

import static com.google.common.collect.Iterables.*;

import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.ui.refactoring.IRefactoringUpdateAcceptor;
import org.eclipse.xtext.ui.refactoring.impl.DefaultReferenceUpdater;
import org.eclipse.xtext.ui.refactoring.impl.RefactoringCrossReferenceSerializer.RefTextEvaluator;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations;

import com.google.inject.Inject;

/**
 * @author Jan Koehnlein - Initial contribution and API
 */
public class JvmModelReferenceUpdater extends DefaultReferenceUpdater {

	@Inject
	private IJvmModelAssociations jvmModelAssociations;

	@Override
	protected void createReferenceUpdate(EObject referringElement, URI referringResourceURI, EReference reference,
			int indexInList, EObject newTargetElement, IRefactoringUpdateAcceptor updateAcceptor) {
		if (!isInferredJvmModelElement(referringElement))
			super.createReferenceUpdate(referringElement, referringResourceURI, reference, indexInList,
					newTargetElement, updateAcceptor);
	}

	protected boolean isInferredJvmModelElement(EObject element) {
		EObject rootContainer = EcoreUtil.getRootContainer(element);
		return !isEmpty(jvmModelAssociations.getSourceElements(rootContainer));
	}

	/**
	 * @since 2.4
	 */
	protected static enum ReferenceSyntax {
		EXPLICIT_GET, EXPLICIT_SET, EXPLICIT_IS, OTHER;
	}
	
	/**
	 * @since 2.4
	 */
	protected ReferenceSyntax getReferenceSyntax(String referenceText) {
		 if(referenceText.startsWith("get") && referenceText.length() > 3) {
			 return ReferenceSyntax.EXPLICIT_GET;
		 } else if(referenceText.startsWith("set") && referenceText.length() > 3) {
			 return ReferenceSyntax.EXPLICIT_SET;
		 } else if(referenceText.startsWith("is") && referenceText.length() > 2) {
			 return ReferenceSyntax.EXPLICIT_IS;
		 }
		 else return ReferenceSyntax.OTHER;
	}
	
	/**
	 * Preserves the syntax of method calls if the target is refactored.
	 * @since 2.4
	 */
	@Override
	protected RefTextEvaluator getRefTextEvaluator(EObject referringElement, URI referringResourceURI,
			EReference reference, int indexInList, EObject newTargetElement) {
		List<INode> nodes = NodeModelUtils.findNodesForFeature(referringElement, reference);
		int index = Math.max(indexInList, 0);
		if(nodes.size() < index)
			return super.getRefTextEvaluator(referringElement, referringResourceURI, reference, indexInList, newTargetElement);
		INode oldNode = nodes.get(index);
		String oldRefText = oldNode.getText().trim();
		final ReferenceSyntax oldReferenceSyntax = getReferenceSyntax(oldRefText);
		return new RefTextEvaluator() {
			
			public boolean isValid(String newText) {
				switch(oldReferenceSyntax) {
					case EXPLICIT_SET:
						return getReferenceSyntax(newText) == oldReferenceSyntax;
					case OTHER:
						return getReferenceSyntax(newText) != ReferenceSyntax.EXPLICIT_SET; 
					default:
						return true;
				}
			}
			
			public boolean isBetterThan(String newText, String currentText) {
				ReferenceSyntax newSyntax = getReferenceSyntax(newText);
				ReferenceSyntax currentSyntax = getReferenceSyntax(currentText);
				// prefer the one with the same syntax as before
				if(newSyntax == oldReferenceSyntax && currentSyntax != oldReferenceSyntax)
					return true; 
				else if(newSyntax != oldReferenceSyntax && currentSyntax == oldReferenceSyntax)
					return false;
				else
					// in doubt shorter is better
					return newText.length() < currentText.length();
			}
		};
			
	}
}
