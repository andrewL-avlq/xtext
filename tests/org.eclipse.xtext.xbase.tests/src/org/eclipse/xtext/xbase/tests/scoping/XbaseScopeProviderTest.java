/*******************************************************************************
 * Copyright (c) 2010 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xbase.tests.scoping;

import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmIdentifyableElement;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.xbase.XAssignment;
import org.eclipse.xtext.xbase.XBinaryOperation;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XCatchClause;
import org.eclipse.xtext.xbase.XClosure;
import org.eclipse.xtext.xbase.XConstructorCall;
import org.eclipse.xtext.xbase.XFeatureCall;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.eclipse.xtext.xbase.XTryCatchFinallyExpression;
import org.eclipse.xtext.xbase.XVariableDeclaration;
import org.eclipse.xtext.xbase.tests.AbstractXbaseTestCase;

/**
 * @author Sven Efftinge - Initial contribution and API
 */
public class XbaseScopeProviderTest extends AbstractXbaseTestCase {
	
	public void testMemberCall() throws Exception {
		XMemberFeatureCall expression = (XMemberFeatureCall) expression("'x'.length()");
		assertEquals("java.lang.String.length()",expression.getFeature().getCanonicalName());
	}
	
	public void testAssignment_1() throws Exception {
		XBinaryOperation assignment = (XBinaryOperation) expression("new java.util.ArrayList<java.lang.String>() += 'foo'", true);
		assertEquals("java.util.ArrayList.add(E)",assignment.getFeature().getCanonicalName());
	}
	
	public void testLocalVarAssignment_1() throws Exception {
		XBlockExpression block = (XBlockExpression) expression("{ var x = ''; x = '' }");
		XAssignment assignment = (XAssignment) block.getExpressions().get(1);
		assertNull(assignment.getAssignable());
		assertTrue(String.valueOf(assignment.getFeature()), assignment.getFeature() instanceof XVariableDeclaration);
		assertSame(block.getExpressions().get(0), assignment.getFeature());
	}
	
	public void testImplicitThis_1() throws Exception {
		XBlockExpression bop = (XBlockExpression) expression(
				"{ " +
				"	val this = new java.util.ArrayList<java.lang.String>(); " +
				"	size;" +
				"}");
		assertEquals("java.util.ArrayList.size()",((XFeatureCall)bop.getExpressions().get(1)).getFeature().getCanonicalName());
	}
	
	public void testImplicitThis_2() throws Exception {
		XBlockExpression bop = (XBlockExpression) expression(
				"{ " +
				"	val this = new java.util.ArrayList<java.lang.String>(); " +
				"	size();" +
				"}");
		assertEquals("java.util.ArrayList.size()",((XFeatureCall)bop.getExpressions().get(1)).getFeature().getCanonicalName());
	}
	
	public void testImplicitThis_3() throws Exception {
		XBlockExpression bop = (XBlockExpression) expression(
				"{ " +
				"	val java.util.List this = new java.util.ArrayList<java.lang.String>(); " +
				"	size;" +
				"}");
		assertEquals("java.util.List.size()",((XFeatureCall)bop.getExpressions().get(1)).getFeature().getCanonicalName());
	}
	
	public void testImplicitThis_4() throws Exception {
		XBlockExpression bop = (XBlockExpression) expression(
				"{ " +
				"	val java.util.List this = new java.util.ArrayList<java.lang.String>(); " +
				"	size();" +
				"}");
		assertEquals("java.util.List.size()",((XFeatureCall)bop.getExpressions().get(1)).getFeature().getCanonicalName());
	}
	
	public void testShadowing_1() throws Exception {
		XBlockExpression bop = (XBlockExpression) expression(
				"{ " +
				"	val this = new java.util.List<java.lang.String>(); " +
				"	val size = 23;" +
				"	size;" +
				"}", false);
		final JvmIdentifyableElement feature = ((XFeatureCall)bop.getExpressions().get(2)).getFeature();
		assertTrue(feature.getClass().getName(), feature instanceof XVariableDeclaration);
		
		XConstructorCall xConstructorCall = (XConstructorCall)((XVariableDeclaration)bop.getExpressions().get(0)).getRight();
		assertTrue(xConstructorCall.getConstructor().eIsProxy());
	}
	
	public void testShadowing_2() throws Exception {
		XBlockExpression bop = (XBlockExpression) expression(
				"{ " +
				"	val size = 23;" +
				"	val this = new java.util.ArrayList<java.lang.String>(); " +
				"	size;" +
				"}");
		assertTrue(((XFeatureCall)bop.getExpressions().get(2)).getFeature() instanceof XVariableDeclaration);
	}
	
	public void testShadowing_3() throws Exception {
		XBlockExpression bop = (XBlockExpression) expression(
				"{" +
				"	val size = 23;" +
				"	{" +
				"		val this = new java.util.ArrayList<java.lang.String>(); " +
				"		size;" +
				"	};" +
				"}");
		XBlockExpression innerBlock = (XBlockExpression)bop.getExpressions().get(1);
		assertSame(bop.getExpressions().get(0),((XFeatureCall)innerBlock.getExpressions().get(1)).getFeature());
	}
	
	public void testShadowing_4() throws Exception {
		XBlockExpression bop = (XBlockExpression) expression(
				"{" +
				"	val size = 23;" +
				"	{" +
				"		val this = new java.util.ArrayList<java.lang.String>(); " +
				"		size();" +
				"	};" +
				"}");
		XBlockExpression innerBlock = (XBlockExpression)bop.getExpressions().get(1);
//		TODO: fix expectation
//		assertEquals("java.util.ArrayList.size()",((XFeatureCall)innerBlock.getExpressions().get(1)).getFeature().getCanonicalName());
		assertSame(bop.getExpressions().get(0),((XFeatureCall)innerBlock.getExpressions().get(1)).getFeature());
	}
	
	public void testShadowing_5() throws Exception {
		XBlockExpression bop = (XBlockExpression) expression(
				"{" +
				"	val size = 23;" +
				"	java.lang.String size|size;" +
				"}");
		XClosure closure = (XClosure)bop.getExpressions().get(1);
		assertSame(closure.getFormalParameters().get(0), ((XFeatureCall)closure.getExpression()).getFeature());
	}
	
	public void testShadowing_6() throws Exception {
		XBlockExpression bop = (XBlockExpression) expression(
				"{" +
				"	val size = 23;" +
				"	java.lang.String x|size;" +
				"}");
		XClosure closure = (XClosure)bop.getExpressions().get(1);
		assertSame(bop.getExpressions().get(0), ((XFeatureCall)closure.getExpression()).getFeature());
	}
	
	public void testShadowing_7() throws Exception {
		XBlockExpression bop = (XBlockExpression) expression(
				"{" +
				"	val size = 23;" +
				"	java.lang.String size| java.lang.String x|size;" +
		"}");
		XClosure closure = (XClosure)bop.getExpressions().get(1);
		assertSame(closure.getFormalParameters().get(0), ((XFeatureCall)((XClosure)closure.getExpression()).getExpression()).getFeature());
	}
	
	public void testTryCatch_0() throws Exception {
		XTryCatchFinallyExpression exp = (XTryCatchFinallyExpression) expression("try 'foo' catch (java.lang.Exception e) e",true);
		XCatchClause xCatchClause = exp.getCatchClauses().get(0);
		assertSame(xCatchClause.getDeclaredParam(), ((XFeatureCall)xCatchClause.getExpression()).getFeature());
	}
	
	public void testTryCatch_1() throws Exception {
		expression("try { (java.lang.Boolean) 'literal' } catch(java.lang.ClassCastException e) {e.getClass().getSimpleName()}", true);
	}
	
	public void testFeatureCall() throws Exception {
		XBlockExpression block = (XBlockExpression) expression("{" +
				"  val this = new testdata.FieldAccessSub();" +
				"  stringField;" +
				"  finalField;" +
				"}");
		XFeatureCall call1 = (XFeatureCall) block.getExpressions().get(1);
		XFeatureCall call2 = (XFeatureCall) block.getExpressions().get(2);
		assertEquals("testdata.FieldAccessSub.stringField",call1.getFeature().getCanonicalName());
		assertEquals("testdata.FieldAccess.finalField",call2.getFeature().getCanonicalName());
	}
	
	public void testFeatureCall_1() throws Exception {
		XBlockExpression block = (XBlockExpression) expression("{" +
				"  val this = new testdata.FieldAccessSub();" +
				"  stringField;" +
				"  stringField();" +
				"}");
		XFeatureCall call1 = (XFeatureCall) block.getExpressions().get(1);
		XFeatureCall call2 = (XFeatureCall) block.getExpressions().get(2);
		assertEquals("testdata.FieldAccessSub.stringField",call1.getFeature().getCanonicalName());
		assertEquals("testdata.FieldAccessSub.stringField()",((JvmOperation)call2.getFeature()).getCanonicalName());
	}
	
	public void testFeatureCall_2() throws Exception {
		XBlockExpression block = (XBlockExpression) expression("{" +
				"  val this = new testdata.FieldAccessSub();" +
				"  privateField;" +
				"  privateField();" +
		"}");
		XFeatureCall call1 = (XFeatureCall) block.getExpressions().get(1);
		XFeatureCall call2 = (XFeatureCall) block.getExpressions().get(2);
		assertEquals("testdata.FieldAccessSub.privateField()",((JvmOperation)call1.getFeature()).getCanonicalName());
		assertSame(call1.getFeature(),call2.getFeature());
	}
	
	public void testFeatureCall_3() throws Exception {
		XBlockExpression block = (XBlockExpression) expression("{" +
				"  val this = new testdata.MethodOverrides1();" +
				"  m1('String');" +
				"  m1((java.lang.CharSequence)'CharSequence');" +
		"}");
		XFeatureCall call1 = (XFeatureCall) block.getExpressions().get(1);
		XFeatureCall call2 = (XFeatureCall) block.getExpressions().get(2);
		assertEquals("testdata.MethodOverrides1.m1(java.lang.String)",((JvmOperation)call1.getFeature()).getCanonicalName());
		assertEquals("testdata.MethodOverrides1.m1(java.lang.CharSequence)",((JvmOperation)call2.getFeature()).getCanonicalName());
	}
	
	public void testFeatureCall_4() throws Exception {
		XBlockExpression block = (XBlockExpression) expression("{" +
				"  val this = new testdata.MethodOverrides2();" +
				"  m1('String');" +
				"  m1((java.lang.CharSequence)'CharSequence');" +
				"  m1((java.lang.Object)'String');" +
		"}");
		XFeatureCall call1 = (XFeatureCall) block.getExpressions().get(1);
		XFeatureCall call2 = (XFeatureCall) block.getExpressions().get(2);
		XFeatureCall call3 = (XFeatureCall) block.getExpressions().get(3);
		assertEquals("testdata.MethodOverrides1.m1(java.lang.String)",((JvmOperation)call1.getFeature()).getCanonicalName());
		assertEquals("testdata.MethodOverrides1.m1(java.lang.CharSequence)",((JvmOperation)call2.getFeature()).getCanonicalName());
		assertEquals("testdata.MethodOverrides2.m1(java.lang.Object)",((JvmOperation)call3.getFeature()).getCanonicalName());
	}
	
	public void testGenerics() throws Exception {
		expression("new testdata.GenericType1<java.lang.String>() += 'foo'", true);
	}
	
	public void testGenerics_1() throws Exception {
		expressionWithError("((testdata.GenericType1<? extends java.lang.String>) null) += 'foo'", 0);
	}
	
	public void testGenerics_2() throws Exception {
		expression("new testdata.GenericType1() += 'foo'", true);
	}
	
	public void testPropertyAccess_1() throws Exception {
		XMemberFeatureCall exp = (XMemberFeatureCall) expression("new testdata.Properties1().prop1");
		assertTrue(exp.getFeature() instanceof JvmField);
		assertEquals("testdata.Properties1.prop1",exp.getFeature().getCanonicalName());
	}
	
	public void testPropertyAccess_2() throws Exception {
		XMemberFeatureCall exp1 = (XMemberFeatureCall) expression("new testdata.Properties1().getProp1");
		XMemberFeatureCall exp2 = (XMemberFeatureCall) expression("new testdata.Properties1().getProp1()");
		assertEquals(exp1.getFeature().getCanonicalName(),exp2.getFeature().getCanonicalName());
	}
	
	public void testPropertyAccess_4() throws Exception {
		XMemberFeatureCall exp = (XMemberFeatureCall) expression("new testdata.Properties1().prop2");
		assertTrue(exp.getFeature() instanceof JvmOperation);
		assertEquals("testdata.Properties1.getProp2()",exp.getFeature().getCanonicalName());
	}
	
	public void testPropertyAccess_5() throws Exception {
		XMemberFeatureCall exp1 = (XMemberFeatureCall) expression("new testdata.Properties1().getProp2");
		XMemberFeatureCall exp2 = (XMemberFeatureCall) expression("new testdata.Properties1().getProp2()");
		assertEquals(exp1.getFeature().getCanonicalName(),exp2.getFeature().getCanonicalName());
	}
	
	public void testPropertySetter_1() throws Exception {
		XAssignment exp = (XAssignment) expression("new testdata.Properties1().prop1 = 'Text'");
		assertEquals(((XConstructorCall)exp.getAssignable()).getConstructor().getDeclaringType(), ((JvmField)exp.getFeature()).getDeclaringType());
	}
	
	public void testPropertySetter_2() throws Exception {
		XAssignment exp = (XAssignment) expression("new testdata.Properties1().prop2 = 'Text'");
		assertEquals("testdata.Properties1.setProp2(java.lang.String)", exp.getFeature().getCanonicalName());
	}
}
