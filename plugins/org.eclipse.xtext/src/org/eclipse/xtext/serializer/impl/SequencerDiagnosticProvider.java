/*******************************************************************************
 * Copyright (c) 2011 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.serializer.impl;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.Grammar;
import org.eclipse.xtext.serializer.IGrammarConstraintProvider.IConstraintContext;
import org.eclipse.xtext.serializer.ISemanticSequencerDiagnosticProvider;
import org.eclipse.xtext.serializer.ISerializationDiagnostic;

/**
 * @author Moritz Eysholdt - Initial contribution and API
 */
public class SequencerDiagnosticProvider implements ISemanticSequencerDiagnosticProvider {

	public ISerializationDiagnostic createInvalidContextOrTypeDiagnostic(EObject semanticObject, EObject context,
			List<IConstraintContext> constraints, Grammar grammar) {
		return new SerializationDiagnostic(semanticObject, "foo bar");
	}

}
