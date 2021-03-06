/*******************************************************************************
 * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.ide.tests.editor.contentassist.antlr;

import java.util.Collection;

import org.antlr.runtime.RecognitionException;
import org.eclipse.xtext.ide.editor.contentassist.antlr.AbstractContentAssistParser;
import org.eclipse.xtext.ide.editor.contentassist.antlr.FollowElement;
import org.eclipse.xtext.testlanguages.xtextgrammar.ide.contentassist.antlr.XtextGrammarTestLanguageParser;

import com.google.inject.Inject;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public class ParserTest extends AbstractParserTest {

	@Inject
	private XtextGrammarTestLanguageParser parser;

	@Override
	protected Collection<FollowElement> getFollowSet(String input) throws RecognitionException {
		AbstractContentAssistParser parser = this.parser;
		return parser.getFollowElements(input, false);
	}
}
