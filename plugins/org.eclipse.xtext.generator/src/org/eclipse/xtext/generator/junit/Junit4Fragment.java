/*******************************************************************************
 * Copyright (c) 2011 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.generator.junit;

import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.mwe.core.issues.Issues;
import org.eclipse.xpand2.XpandExecutionContext;
import org.eclipse.xtext.Grammar;
import org.eclipse.xtext.GrammarUtil;
import org.eclipse.xtext.generator.AbstractGeneratorFragment;
import org.eclipse.xtext.generator.Naming;

import com.google.common.collect.Lists;

/**
 * @author Michael Clay - Initial contribution and API
 */
public class Junit4Fragment extends AbstractGeneratorFragment {
	private static final Logger log = Logger.getLogger(Junit4Fragment.class);
	private boolean genContentAssistTest;

	public boolean isGenContentAssistTest() {
		return genContentAssistTest;
	}

	public void setGenContentAssistTest(boolean genUiTest) {
		this.genContentAssistTest = genUiTest;
	}

	public static String getQualifiedInjectorProviderName(Grammar grammar, Naming naming) {
		return naming.basePackageRuntime(grammar) + "." + GrammarUtil.getName(grammar) + "InjectorProvider";
	}

	public static String getQualifiedUiInjectorProviderName(Grammar grammar, Naming naming) {
		return naming.basePackageRuntime(grammar) + "." + GrammarUtil.getName(grammar) + "UiInjectorProvider";
	}

	@Override
	public void generate(Grammar grammar, XpandExecutionContext ctx) {
		if (getNaming().getPathTestProject()!=null) {
			if (log.isInfoEnabled()) {
				log.info("generating Junit4 Test support classes");
			}
			super.generate(grammar, ctx);
		} else {
			log.warn("skip execution of '"+getClass().getName()+"' due to missing configuration");
		}
	}

	@Override
	protected List<Object> getParameters(Grammar grammar) {
		return Lists.<Object>newArrayList(this);
	}

	@Override
	public void checkConfiguration(Issues issues) {
		super.checkConfiguration(issues);
		if (getNaming().getPathTestProject()==null) {
			issues.addWarning("Missing test project path configuration 'Generator#pathTestProject' required for fragment '"+getClass().getName()+"'.");
		}
	}

	@Override
	public String[] getExportedPackagesUi(Grammar grammar) {
		return new String[] { getNaming().activatorPackageName()};
	}


}
