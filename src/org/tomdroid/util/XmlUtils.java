package org.tomdroid.util;

public class XmlUtils {
	
	public static String escape(String input) {
		return input
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("\'", "&apos;");
	}
	
	public static String unescape(String input) {
		return input
			.replace("&amp;", "&")
			.replace("&lt;", "<")
			.replace("&gt;", ">")
			.replace("&quot;", "\"")
			.replace("&apos;", "\'");
	}
}
