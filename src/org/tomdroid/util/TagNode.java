package org.tomdroid.util;

import java.util.LinkedList;
import java.util.List;

public class TagNode {
	
	private List<TagNode> children = new LinkedList<TagNode>();
	public TagType tagType;
	public String text = "";
	public int start;
	public int end;
	public int listLevel;
	private TagNode parent;

	public void add (TagNode node) {
		this.children.add(node);
		node.parent = this;
	}
	
	public void remove (TagNode node) {
		this.children.remove(node);
		node.parent = null;
	}
	
	public TagNode[] getChildren() {
		return this.children.toArray(new TagNode[this.children.size()]);
	}
	
	public TagNode getParent() {
		return this.parent;
	}

	public boolean equals(TagNode t) {
		return t.start == this.start && t.end == this.end && t.tagType == this.tagType;
	}
	
	public static void moveTo(TagNode node, TagNode from, TagNode to) {
		from.remove(node);
		to.add(node);
	}
	
	public static void moveChildren(TagNode from, TagNode to) {
		for (TagNode child : from.getChildren()) {
			TagNode.moveTo(child, from, to);
		}
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
		// TODO remove if code is working correctly
		else if (this.tagType.equals(TagType.MARGIN)) return "margin";
		else throw new Exception();
	}
}
