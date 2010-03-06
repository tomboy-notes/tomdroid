package org.tomdroid.util;

public class XmlUtils {
	
	/**
	 * Useful to replace the characters forbidden in xml by their escaped counterparts
	 * Ex: &amp; -> &amp;amp;
	 * 
	 * @param input the string to escape
	 * @return the escaped string
	 */
	public static String escape(String input) {
		return input
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("\'", "&apos;");
	}
	
	/**
	 * Useful to replace the escaped characters their unescaped counterparts
	 * Ex: &amp;amp; -> &amp;
	 * 
	 * @param input the string to unescape
	 * @return the unescaped string
	 */
	public static String unescape(String input) {
		return input
			.replace("&amp;", "&")
			.replace("&lt;", "<")
			.replace("&gt;", ">")
			.replace("&quot;", "\"")
			.replace("&apos;", "\'");
	}
}
