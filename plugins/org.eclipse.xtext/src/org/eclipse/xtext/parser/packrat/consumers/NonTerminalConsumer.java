/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.parser.packrat.consumers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.AbstractElement;
import org.eclipse.xtext.AbstractRule;
import org.eclipse.xtext.Action;
import org.eclipse.xtext.Alternatives;
import org.eclipse.xtext.Assignment;
import org.eclipse.xtext.Group;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.parser.packrat.IBacktracker;
import org.eclipse.xtext.parser.packrat.IHiddenTokenHandler;
import org.eclipse.xtext.parser.packrat.IMarkerFactory;
import org.eclipse.xtext.parser.packrat.IHiddenTokenHandler.IHiddenTokenState;
import org.eclipse.xtext.parser.packrat.IMarkerFactory.IMarker;
import org.eclipse.xtext.parser.packrat.matching.ICharacterClass;
import org.eclipse.xtext.parser.packrat.matching.ISequenceMatcher;
import org.eclipse.xtext.parser.packrat.tokens.AlternativesToken;
import org.eclipse.xtext.parser.packrat.tokens.AssignmentToken;
import org.eclipse.xtext.parser.packrat.tokens.ErrorToken;
import org.eclipse.xtext.parser.packrat.tokens.GroupToken;
import org.eclipse.xtext.parser.packrat.tokens.ParsedNonTerminal;
import org.eclipse.xtext.parser.packrat.tokens.ParsedNonTerminalEnd;
import org.eclipse.xtext.parser.packrat.tokens.PlaceholderToken;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public abstract class NonTerminalConsumer extends AbstractConsumer implements INonTerminalConsumer, INonTerminalConsumerConfiguration {

	protected static final int SUCCESS = ConsumeResult.SUCCESS;

	private final IHiddenTokenHandler hiddenTokenHandler;

	private final IMarkerFactory markerFactory;

	private final IBacktracker backtracker;

	private final ITerminalConsumer[] hiddenTokens;

	private final IConsumerUtility consumerUtil;

	private int doConsumeGroup(ElementConsumer<Group> groupConsumer, IElementConsumer[] groupElements) throws Exception {
		final GroupResult result = createGroupResult(groupConsumer);
		result.reset();
		for(IElementConsumer consumer: groupElements) {
			if (result.didGroupFail(consumer.consume())) {
				result.error(groupConsumer.getErrorMessage());
				return result.getResult();
			}
		}
		return result.getResult();
	}

	private int doConsumeAlternatives(ElementConsumer<Alternatives> alternativesConsumer, IElementConsumer[] alternativesElements) throws Exception {
		final AlternativesResult result = createAlternativesResult(alternativesConsumer);
		result.reset();
		for(IElementConsumer consumer: alternativesElements) {
			result.nextAlternative();
			if (result.isAlternativeDone(consumer.consume()))
				return result.getResult();
		}
		result.error(alternativesConsumer.getErrorMessage());
		return result.getResult();
	}

	private int doConsumeAssignment(ElementConsumer<Assignment> assignmentConsumer, IElementConsumer element) throws Exception {
		final AssignmentResult result = createAssignmentResult(assignmentConsumer);
		return result.getResult(element.consume());
	}

	protected static class ConsumerAcceptor {
		private final List<IElementConsumer> result = new ArrayList<IElementConsumer>(4);

		public void accept(IElementConsumer consumer) {
			result.add(consumer);
		}

		private IElementConsumer[] getResult() {
			return result.toArray(new IElementConsumer[result.size()]);
		}
	}

	protected abstract class ElementConsumer<Element extends AbstractElement> implements IElementConsumer {

		private final Element element;

		protected ElementConsumer(Element element) {
			this.element = element;
		}

		public int consume() throws Exception {
			int result = doConsume();
			while(result != ConsumeResult.SUCCESS && skipPreviousToken()) {
				result = doConsume();
			}
			return result;
		}

		protected abstract int doConsume() throws Exception;

		protected final Element getElement() {
			return element;
		}

		protected String getErrorMessage() {
			return "Another token expected.";
		}
	}

	protected abstract class GroupConsumer extends ElementConsumer<Group> {

		private IElementConsumer[] consumers;

		public GroupConsumer(Group element) {
			super(element);
		}

		@Override
		protected final int doConsume() throws Exception {
			return doConsumeGroup(this, getConsumers());
		}

		protected final IElementConsumer[] getConsumers() {
			if (consumers == null) {
				ConsumerAcceptor acceptor = new ConsumerAcceptor();
				doGetConsumers(acceptor);
				consumers = acceptor.getResult();
			}
			return consumers;
		}

		protected abstract void doGetConsumers(ConsumerAcceptor acceptor);
	}

	protected abstract class AlternativesConsumer extends ElementConsumer<Alternatives> {

		private IElementConsumer[] consumers;

		protected AlternativesConsumer(Alternatives element) {
			super(element);
		}

		@Override
		protected final int doConsume() throws Exception {
			return doConsumeAlternatives(this, getConsumers());
		}

		protected final IElementConsumer[] getConsumers() {
			if (consumers == null) {
				ConsumerAcceptor acceptor = new ConsumerAcceptor();
				doGetConsumers(acceptor);
				consumers = acceptor.getResult();
			}
			return consumers;
		}

		protected abstract void doGetConsumers(ConsumerAcceptor acceptor);
	}

	protected abstract class AssignmentConsumer extends ElementConsumer<Assignment> {

		public AssignmentConsumer(Assignment element) {
			super(element);
		}

		@Override
		protected final int doConsume() throws Exception {
			return doConsumeAssignment(this, getConsumer());
		}

		protected abstract IElementConsumer getConsumer();
	}

	protected abstract class OptionalElementConsumer<Element extends AbstractElement> extends ElementConsumer<Element> {

		protected OptionalElementConsumer(Element element) {
			super(element);
		}

		@Override
		public final int consume() throws Exception {
			IMarker marker = mark();
			int result = doConsume();
			if (result != ConsumeResult.SUCCESS) {
				marker.rollback();
				skipped(getElement());
			} else
				marker.commit();
			return ConsumeResult.SUCCESS;
		}
	}

	protected abstract class OptionalGroupConsumer extends OptionalElementConsumer<Group> {

		private IElementConsumer[] consumers;

		protected OptionalGroupConsumer(Group element) {
			super(element);
		}

		@Override
		protected final int doConsume() throws Exception {
			return doConsumeGroup(this, getConsumers());
		}

		protected final IElementConsumer[] getConsumers() {
			if (consumers == null) {
				ConsumerAcceptor acceptor = new ConsumerAcceptor();
				doGetConsumers(acceptor);
				consumers = acceptor.getResult();
			}
			return consumers;
		}

		protected abstract void doGetConsumers(ConsumerAcceptor acceptor);
	}

	protected abstract class OptionalAlternativesConsumer extends OptionalElementConsumer<Alternatives> {

		private IElementConsumer[] consumers;

		protected OptionalAlternativesConsumer(Alternatives element) {
			super(element);
		}

		@Override
		protected final int doConsume() throws Exception {
			return doConsumeAlternatives(this, getConsumers());
		}

		protected final IElementConsumer[] getConsumers() {
			if (consumers == null) {
				ConsumerAcceptor acceptor = new ConsumerAcceptor();
				doGetConsumers(acceptor);
				consumers = acceptor.getResult();
			}
			return consumers;
		}

		protected abstract void doGetConsumers(ConsumerAcceptor acceptor);
	}

	protected abstract class OptionalAssignmentConsumer extends OptionalElementConsumer<Assignment> {

		protected OptionalAssignmentConsumer(Assignment element) {
			super(element);
		}

		@Override
		protected final int doConsume() throws Exception {
			return doConsumeAssignment(this, getConsumer());
		}

		protected abstract IElementConsumer getConsumer();
	}

	protected abstract class LoopElementConsumer<Element extends AbstractElement> extends ElementConsumer<Element> {

		protected LoopElementConsumer(Element element) {
			super(element);
		}

		@Override
		public final int consume() throws Exception {
			IMarker marker = mark();
			while(doConsume() == ConsumeResult.SUCCESS) {
				marker.flush();
			}
			marker.rollback();
			skipped(getElement());
			return ConsumeResult.SUCCESS;
		}
	}

	protected abstract class LoopGroupConsumer extends LoopElementConsumer<Group> {

		private IElementConsumer[] consumers;

		protected LoopGroupConsumer(Group element) {
			super(element);
		}

		@Override
		protected final int doConsume() throws Exception {
			return doConsumeGroup(this, getConsumers());
		}

		protected final IElementConsumer[] getConsumers() {
			if (consumers == null) {
				ConsumerAcceptor acceptor = new ConsumerAcceptor();
				doGetConsumers(acceptor);
				consumers = acceptor.getResult();
			}
			return consumers;
		}

		protected abstract void doGetConsumers(ConsumerAcceptor acceptor);
	}

	protected abstract class LoopAlternativesConsumer extends LoopElementConsumer<Alternatives> {

		private IElementConsumer[] consumers;

		protected LoopAlternativesConsumer(Alternatives element) {
			super(element);
		}

		@Override
		protected final int doConsume() throws Exception {
			return doConsumeAlternatives(this, getConsumers());
		}

		protected final IElementConsumer[] getConsumers() {
			if (consumers == null) {
				ConsumerAcceptor acceptor = new ConsumerAcceptor();
				doGetConsumers(acceptor);
				consumers = acceptor.getResult();
			}
			return consumers;
		}

		protected abstract void doGetConsumers(ConsumerAcceptor acceptor);
	}

	protected abstract class LoopAssignmentConsumer extends LoopElementConsumer<Assignment> {

		public LoopAssignmentConsumer(Assignment element) {
			super(element);
		}

		@Override
		protected final int doConsume() throws Exception {
			return doConsumeAssignment(this, getConsumer());
		}

		protected abstract IElementConsumer getConsumer();
	}

	protected abstract class MandatoryLoopElementConsumer<Element extends AbstractElement> extends ElementConsumer<Element> {

		protected MandatoryLoopElementConsumer(Element element) {
			super(element);
		}

		@Override
		public final int consume() throws Exception {
			IMarker marker = mark();
			int result = ConsumeResult.EMPTY_MATCH;
			result = doConsume();
			while(result != ConsumeResult.SUCCESS && skipPreviousToken()) {
				result = doConsume();
			}
			if (result == ConsumeResult.SUCCESS) {
				marker.flush();
				while(doConsume()==ConsumeResult.SUCCESS) {
					marker.flush();
				}
				marker.rollback();
				skipped(getElement());
				return ConsumeResult.SUCCESS;
			}
			error("Could not find token.", getElement());
			marker.commit();
			return result;
		}

	}

	protected abstract class MandatoryLoopGroupConsumer extends MandatoryLoopElementConsumer<Group> {

		private IElementConsumer[] consumers;

		protected MandatoryLoopGroupConsumer(Group element) {
			super(element);
		}

		@Override
		protected final int doConsume() throws Exception {
			return doConsumeGroup(this, getConsumers());
		}

		protected final IElementConsumer[] getConsumers() {
			if (consumers == null) {
				ConsumerAcceptor acceptor = new ConsumerAcceptor();
				doGetConsumers(acceptor);
				consumers = acceptor.getResult();
			}
			return consumers;
		}

		protected abstract void doGetConsumers(ConsumerAcceptor acceptor);
	}

	protected abstract class MandatoryLoopAlternativesConsumer extends MandatoryLoopElementConsumer<Alternatives> {

		private IElementConsumer[] consumers;

		protected MandatoryLoopAlternativesConsumer(Alternatives element) {
			super(element);
		}

		@Override
		protected final int doConsume() throws Exception {
			return doConsumeAlternatives(this, getConsumers());
		}

		protected final IElementConsumer[] getConsumers() {
			if (consumers == null) {
				ConsumerAcceptor acceptor = new ConsumerAcceptor();
				doGetConsumers(acceptor);
				consumers = acceptor.getResult();
			}
			return consumers;
		}

		protected abstract void doGetConsumers(ConsumerAcceptor acceptor);
	}

	protected abstract class MandatoryLoopAssignmentConsumer extends MandatoryLoopElementConsumer<Assignment> {

		protected MandatoryLoopAssignmentConsumer(Assignment element) {
			super(element);
		}

		@Override
		protected final int doConsume() throws Exception {
			return doConsumeAssignment(this, getConsumer());
		}

		protected abstract IElementConsumer getConsumer();

	}

	protected class AbstractElementResult<Element extends AbstractElement> {

		private final ElementConsumer<Element> elementConsumer;

		protected AbstractElementResult(ElementConsumer<Element> elementConsumer) {
			this.elementConsumer = elementConsumer;
		}

		protected void error(String errorMessage) {
			NonTerminalConsumer.this.error(errorMessage, elementConsumer.getElement());
		}

	}

	protected class AlternativesResult extends AbstractElementResult<Alternatives> {
		private int bestResult;
		private int alternative;
		private IMarker bestMarker;
		private IMarker currentMarker;

		protected AlternativesResult(ElementConsumer<Alternatives> elementConsumer) {
			super(elementConsumer);
			alternative = -1;
			bestResult = ConsumeResult.SUCCESS;
			bestMarker = mark();
		}

		public void nextAlternative() {
			alternative++;
			currentMarker = bestMarker.fork();
		}

		public void reset() {
			bestResult = ConsumeResult.EMPTY_MATCH;
		}

		public int getResult() {
			bestMarker.commit();
			getTokenAcceptor().accept(new AlternativesToken.End(getOffset(), alternative));
			return bestResult;
		}

		public boolean isAlternativeDone(int result) {
			if (result == ConsumeResult.SUCCESS || result > bestResult) {
				bestMarker = currentMarker.join(bestMarker);
				bestResult = result;
			} else {
				bestMarker = bestMarker.join(currentMarker);
			}
			currentMarker = null;
			return result == ConsumeResult.SUCCESS;
		}
	}

	protected class GroupResult extends AbstractElementResult<Group> {
		private int result;
		private final IMarker marker;

		protected GroupResult(ElementConsumer<Group> elementConsumer) {
			super(elementConsumer);
			result = ConsumeResult.SUCCESS;
			marker = mark();
		}

		public void reset() {
			result = ConsumeResult.EMPTY_MATCH;
		}

		public int getResult() {
			marker.commit();
			getTokenAcceptor().accept(new GroupToken.End(getOffset()));
			return result;
		}

		public boolean didGroupFail(int result) {
			this.result = result;
			return result != ConsumeResult.SUCCESS;
		}
	}

	protected class AssignmentResult extends AbstractElementResult<Assignment> {

		protected AssignmentResult(ElementConsumer<Assignment> elementConsumer) {
			super(elementConsumer);
		}

		public int getResult(int result) {
			getTokenAcceptor().accept(new AssignmentToken.End(getOffset()));
			return result;
		}

	}

	protected NonTerminalConsumer(INonTerminalConsumerConfiguration configuration, ITerminalConsumer[] hiddenTokens) {
		super(configuration.getInput(), configuration.getTokenAcceptor());
		this.markerFactory = configuration.getMarkerFactory();
		this.hiddenTokenHandler = configuration.getHiddenTokenHandler();
		this.consumerUtil = configuration.getConsumerUtil();
		this.hiddenTokens = hiddenTokens;
		this.backtracker = configuration.getBacktracker();
	}

	protected AlternativesResult createAlternativesResult(ElementConsumer<Alternatives> alternativesConsumer) {
		getTokenAcceptor().accept(new AlternativesToken(getOffset(), alternativesConsumer.getElement()));
		return new AlternativesResult(alternativesConsumer);
	}

	protected GroupResult createGroupResult(ElementConsumer<Group> groupConsumer) {
		getTokenAcceptor().accept(new GroupToken(getOffset(), groupConsumer.getElement()));
		return new GroupResult(groupConsumer);
	}

	protected AssignmentResult createAssignmentResult(ElementConsumer<Assignment> assignmentConsumer) {
		getTokenAcceptor().accept(new AssignmentToken(getOffset(), assignmentConsumer.getElement()));
		return new AssignmentResult(assignmentConsumer);
	}

	public boolean skipPreviousToken() {
		return backtracker.skipPreviousToken();
	}

	public void skipped(EObject grammarElement) {
		getTokenAcceptor().accept(new PlaceholderToken(getOffset(), grammarElement));
	}

	public boolean isDefiningHiddens() {
		return hiddenTokens != null;
	}

	public int consume(String feature, boolean isMany, boolean isDatatype, boolean isBoolean, AbstractElement grammarElement) throws Exception {
		IHiddenTokenState prevState = hiddenTokenHandler.replaceHiddenTokens(hiddenTokens);
		IMarker marker = mark();
		int prevOffset = getOffset();
		getTokenAcceptor().accept(new ParsedNonTerminal(getInput().getOffset(), grammarElement != null ? grammarElement : getGrammarElement(), getDefaultType()));
		int result = doConsume();
		if (result != ConsumeResult.SUCCESS) {
			getTokenAcceptor().accept(new ErrorToken(prevOffset, 0, null, "Expected " + getDefaultType() + ", but could not find."));
		}
		getTokenAcceptor().accept(new ParsedNonTerminalEnd(getInput().getOffset(), feature, isMany, isDatatype, isBoolean));
		marker.commit();
		prevState.restore();
		return result;
	}

	public void consumeAsRoot(IRootConsumerListener listener) {
		IHiddenTokenState prevState = hiddenTokenHandler.replaceHiddenTokens(hiddenTokens);
		IMarker marker = mark();
		getTokenAcceptor().accept(new ParsedNonTerminal(getInput().getOffset(), getGrammarElement(), getDefaultType()));
		listener.afterNonTerminalBegin(this, this);
		int result;
		try {
			result = doConsume();
		} catch(Exception e) {
			result = ConsumeResult.EXCEPTION;
			listener.handleException(this, e, this);
		}
		listener.beforeNonTerminalEnd(this, result, this);
		getTokenAcceptor().accept(new ParsedNonTerminalEnd(getInput().getOffset(), null, false, false, false));
		marker.commit();
		prevState.restore();
	}

	protected final IMarker mark() {
		return markerFactory.mark();
	}

	protected final void error(String message, AbstractElement grammarElement) {
		getTokenAcceptor().accept(new ErrorToken(getOffset(), 0, grammarElement, message));
	}

	protected final int consumeKeyword(Keyword keyword, String feature, boolean isMany, boolean isBoolean, ICharacterClass notFollowedBy) {
		return consumerUtil.consumeKeyword(keyword, feature, isMany, isBoolean, notFollowedBy);
	}

	protected final int consumeTerminal(ITerminalConsumer consumer, String feature, boolean isMany, boolean isBoolean, AbstractElement grammarElement, ISequenceMatcher notMatching) {
		return consumerUtil.consumeTerminal(consumer, feature, isMany, isBoolean, grammarElement, notMatching);
	}

	protected final int consumeNonTerminal(INonTerminalConsumer consumer, String feature, boolean isMany, boolean isDatatype, boolean isBoolean, AbstractElement grammarElement) throws Exception {
		return consumerUtil.consumeNonTerminal(consumer, feature, isMany, isDatatype, isBoolean, grammarElement);
	}

	protected final void consumeAction(Action action, boolean isMany) {
		consumerUtil.consumeAction(action, isMany);
	}

	protected abstract int doConsume() throws Exception;

	protected abstract EClassifier getDefaultType();

	protected abstract AbstractRule getGrammarElement();

	public IConsumerUtility getConsumerUtil() {
		return consumerUtil;
	}

	public IHiddenTokenHandler getHiddenTokenHandler() {
		return hiddenTokenHandler;
	}

	public IMarkerFactory getMarkerFactory() {
		return markerFactory;
	}

	public IBacktracker getBacktracker() {
		return backtracker;
	}

	@Override
	public String toString() {
		return "NonTerminalConsumer " + getClass().getSimpleName() + " for type '" + getDefaultType()  + "'";
	}
}