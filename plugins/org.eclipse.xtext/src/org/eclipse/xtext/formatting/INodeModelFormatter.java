/*******************************************************************************
 * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.formatting;

import org.eclipse.xtext.nodemodel.ICompositeNode;

/**
 * @author Moritz Eysholdt - Initial contribution and API
 */
public interface INodeModelFormatter {

	interface IFormattedRegion {
		String getFormattedText();

		int getLenght();

		int getOffset();
	}

	IFormattedRegion format(ICompositeNode root, int offset, int length);

}
