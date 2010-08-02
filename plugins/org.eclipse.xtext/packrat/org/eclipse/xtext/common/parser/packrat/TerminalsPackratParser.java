/*
* generated by Xtext
*/
package org.eclipse.xtext.common.parser.packrat;

import com.google.inject.Inject;

import org.eclipse.xtext.parser.packrat.AbstractPackratParser;
import org.eclipse.xtext.parser.packrat.IParseResultFactory;
import org.eclipse.xtext.parser.packrat.AbstractParserConfiguration.IInternalParserConfiguration;

import org.eclipse.xtext.common.services.TerminalsGrammarAccess;

public class TerminalsPackratParser extends AbstractPackratParser {
	
	@Inject
	public TerminalsPackratParser(IParseResultFactory parseResultFactory, TerminalsGrammarAccess grammarAccess) {
		super(parseResultFactory, grammarAccess);
	}
	
	@Override
	protected org.eclipse.xtext.common.parser.packrat.TerminalsParserConfiguration createParserConfiguration(IInternalParserConfiguration configuration) {
		return new org.eclipse.xtext.common.parser.packrat.TerminalsParserConfiguration(configuration, getGrammarAccess());
	}
	
	@Override
	protected TerminalsGrammarAccess getGrammarAccess() {
		return (TerminalsGrammarAccess)super.getGrammarAccess();
	}
	
}