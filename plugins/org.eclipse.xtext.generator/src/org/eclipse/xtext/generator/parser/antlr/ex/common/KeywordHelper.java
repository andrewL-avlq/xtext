/*******************************************************************************
 * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.generator.parser.antlr.ex.common;

import org.eclipse.xtext.Grammar;
import org.eclipse.xtext.xtext.generator.grammarAccess.GrammarAccessExtensions;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 * @author Heiko Behrens
 */
public class KeywordHelper extends org.eclipse.xtext.xtext.generator.parser.antlr.KeywordHelper {

	public KeywordHelper(Grammar grammar, boolean ignoreCase) {
		super(grammar, ignoreCase, new GrammarAccessExtensions());
	}

}
