/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2013 Stefan Hammer <j-4@gmx.at>
 * Copyright 2013 Timo Dörr <timo@latecrew.de>
 * 
 * This file is part of Tomdroid.
 * 
 * Tomdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Tomdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Tomdroid.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tomdroid.util;

import java.util.LinkedList;
import java.util.List;

// this class was created to be able to convert a spannable string to XML
// a TagNode holds all the information necessary to create a XML tag from the Tree. 
public class TagNode {
	
	private List<TagNode> children = new LinkedList<TagNode>();
	private TagType tagType = TagType.OTHER;
	public String text = "";
	// for debugging
	//public String type = "";
	public int start;
	public int end;
	public int listLevel;
	private TagNode parent;

	public void add (TagNode node) {
		this.children.add(node);
		node.parent = this;
	}
	
	public void setType (TagType tagType) {
		this.tagType = tagType;
		//this.type = tagType.toString();
	}
	
	public TagType getType () {
		return this.tagType;
	}
	
	public TagNode[] getChildren() {
		return this.children.toArray(new TagNode[this.children.size()]);
	}
	
	public TagNode getParent() {
		return this.parent;
	}
	
	public String getTagName() throws Exception {
		if (this.tagType.equals(TagType.BOLD)) return "bold";
		else if (this.tagType.equals(TagType.ITALIC)) return "italic";
		else if (this.tagType.equals(TagType.MONOSPACE)) return "monospace";
		else if (this.tagType.equals(TagType.STRIKETHROUGH)) return "strikethrough";
		else if (this.tagType.equals(TagType.HIGHLIGHT)) return "highlight";
		else if (this.tagType.equals(TagType.LINK)) return "link";
		else if (this.tagType.equals(TagType.LINK_INTERNAL)) return "link:internal";
		else if (this.tagType.equals(TagType.SIZE_SMALL)) return "size:small";
		else if (this.tagType.equals(TagType.SIZE_LARGE)) return "size:large";
		else if (this.tagType.equals(TagType.SIZE_HUGE)) return "size:huge";
		else if (this.tagType.equals(TagType.LIST)) return "list";
		else if (this.tagType.equals(TagType.LIST_ITEM)) return "list-item";
		else throw new Exception();
	}
}
