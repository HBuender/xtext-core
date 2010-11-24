/*******************************************************************************
 * Copyright (c) 2010 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.parsetree.reconstr.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.util.Pair;

import com.google.common.collect.Lists;

/**
 * Delivers token nodes, comments and whitespaces in the order needed by the
 * {@link AbstractParseTreeConstructor.WsMergerStream}, i.e. whitespaces within token nodes 
 * before or after the token node itself.
 * 
 * @author koehnlein - Initial contribution and API
 */
public class HiddenAndTokenNodeIterator implements Iterator<INode> {

	private TokenUtil tokenUtil;

	private NodeIterator nodeIterator;

	private Queue<INode> nextNodes = Lists.newLinkedList();

	private INode next;

	public HiddenAndTokenNodeIterator(INode startNode, TokenUtil tokenUtil) {
		this.nodeIterator = new NodeIterator(startNode);
		this.tokenUtil = tokenUtil;
		this.next = findNext();
	}

	private INode findNext() {
		if (nextNodes.isEmpty()) {
			while (nodeIterator.hasNext()) {
				INode candidate = nodeIterator.next();
				if (tokenUtil.isToken(candidate)) {
					nodeIterator.prune();
					Pair<List<ILeafNode>, List<ILeafNode>> leadingAndTrailingHiddenTokens = tokenUtil
							.getLeadingAndTrailingHiddenTokens(candidate);
					nextNodes.addAll(leadingAndTrailingHiddenTokens.getFirst());
					nextNodes.add(candidate);
					nextNodes.addAll(leadingAndTrailingHiddenTokens.getSecond());
					return nextNodes.poll();
				} else if (tokenUtil.isWhitespaceOrCommentNode(candidate)) {
					return candidate;
				}
			}
			return null;
		}
		return nextNodes.poll();
	}

	public boolean hasNext() {
		return next != null;
	}

	public INode next() {
		INode result = next;
		next = findNext();
		return result;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
