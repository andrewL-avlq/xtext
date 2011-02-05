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
import org.eclipse.xtext.Action;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.grammaranalysis.impl.GrammarElementFullTitleSwitch;
import org.eclipse.xtext.serializer.IRecursiveSyntacticSequenceAcceptor;
import org.eclipse.xtext.serializer.ISyntacticSequenceAcceptor;
import org.eclipse.xtext.util.EmfFormatter;
import org.eclipse.xtext.util.Pair;
import org.eclipse.xtext.util.Tuples;

import com.google.common.collect.Lists;
import com.google.inject.internal.Join;

/**
 * @author Moritz Eysholdt - Initial contribution and API
 */
public class DebugSequenceAcceptor implements ISyntacticSequenceAcceptor, IRecursiveSyntacticSequenceAcceptor {

	protected int indentation = 0;

	protected boolean printInstantly;

	protected List<Pair<String, String>> table = Lists.newArrayList();

	protected GrammarElementFullTitleSwitch titleSwitch = new GrammarElementFullTitleSwitch();

	public DebugSequenceAcceptor() {
		this(false);
	}

	public DebugSequenceAcceptor(boolean printInstantly) {
		super();
		this.printInstantly = printInstantly;
	}

	public void acceptAssignedAction(Action action, EObject semanticChild) {
		add(titleSwitch.doSwitch(action), EmfFormatter.objPath(semanticChild));
	}

	public void acceptAssignedCrossRefDatatype(RuleCall datatypeRC, EObject value) {
		add(titleSwitch.doSwitch(datatypeRC), EmfFormatter.objPath(value));
	}

	public void acceptAssignedCrossRefEnum(RuleCall enumRC, EObject value) {
		add(titleSwitch.doSwitch(enumRC), EmfFormatter.objPath(value));
	}

	public void acceptAssignedCrossRefKeyword(Keyword keyword, EObject value) {
		add(titleSwitch.doSwitch(keyword), EmfFormatter.objPath(value));
	}

	public void acceptAssignedCrossRefTerminal(RuleCall terminalRC, EObject value) {
		add(titleSwitch.doSwitch(terminalRC), EmfFormatter.objPath(value));
	}

	public void acceptAssignedDatatype(RuleCall datatypeRC, Object value) {
		add(titleSwitch.doSwitch(datatypeRC), "'" + value + "'");
	}

	public void acceptAssignedEnum(RuleCall enumRC, Object value) {
		add(titleSwitch.doSwitch(enumRC), "'" + value + "'");
	}

	public void acceptAssignedKeyword(Keyword keyword, Boolean value) {
		add(titleSwitch.doSwitch(keyword), value == null ? "null" : value.toString());
	}

	public void acceptAssignedKeyword(Keyword keyword, String value) {
		add(titleSwitch.doSwitch(keyword), "'" + value + "'");
	}

	public void acceptAssignedParserRuleCall(RuleCall ruleCall, EObject semanticChild) {
		add(titleSwitch.doSwitch(ruleCall), EmfFormatter.objPath(semanticChild));
	}

	public void acceptAssignedTerminal(RuleCall terminalRC, Object value) {
		add(titleSwitch.doSwitch(terminalRC), "'" + value + "'");
	}

	public void acceptUnassignedAction(Action action) {
		add(titleSwitch.doSwitch(action), "");
	}

	public void acceptUnassignedDatatype(RuleCall datatypeRC, Object value) {
		add(titleSwitch.doSwitch(datatypeRC), "'" + value + "'");
	}

	public void acceptUnassignedEnum(RuleCall enumRC, Object value) {
		add(titleSwitch.doSwitch(enumRC), "'" + value + "'");
	}

	public void acceptUnassignedKeyword(Keyword keyword) {
		add(titleSwitch.doSwitch(keyword), "'" + keyword.getValue() + "'");
	}

	public void acceptUnassignedTerminal(RuleCall terminalRC, Object value) {
		add(titleSwitch.doSwitch(terminalRC), "'" + value + "'");
	}

	protected void add(String col1, String col2) {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < indentation; i++)
			buf.append("  ");
		buf.append(col1);
		table.add(Tuples.create(buf.toString(), col2));
		if (printInstantly)
			System.out.println(buf.toString() + "\t" + col2);
	}

	public void enterAssignedAction(Action action, EObject semanticChild) {
		add(titleSwitch.doSwitch(action) + " {", semanticChild.eClass().getName());
		indentation++;
	}

	public void enterAssignedParserRuleCall(RuleCall rc, EObject newCurrent) {
		add(titleSwitch.doSwitch(rc) + " {", newCurrent.eClass().getName());
		indentation++;
	}

	public void enterUnassignedParserRuleCall(RuleCall rc) {
		add(titleSwitch.doSwitch(rc) + " {", "");
		indentation++;
	}

	public List<String> getList() {
		int col1Width = 0;
		for (Pair<String, String> line : table)
			col1Width = Math.max(col1Width, line.getFirst().length());
		String format = "%-" + col1Width + "s  %s";
		List<String> lines = Lists.newArrayList();
		for (Pair<String, String> line : table)
			lines.add(String.format(format, line.getFirst(), line.getSecond()));
		return lines;
	}

	public List<Pair<String, String>> getTable() {
		return table;
	}

	public void leaveAssignedAction(Action action, EObject semanticChild) {
		indentation--;
		add("}", "");
	}

	public void leaveAssignedParserRuleCall(RuleCall rc) {
		indentation--;
		add("}", "");
	}

	public void leaveUnssignedParserRuleCall(RuleCall rc) {
		indentation--;
		add("}", "");
	}

	@Override
	public String toString() {
		return Join.join("\n", getList());
	}
}
