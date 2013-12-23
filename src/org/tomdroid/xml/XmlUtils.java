/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2009, Benoit Garret <benoit.garret_launchpad@gadz.org>
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
package org.tomdroid.xml;

public class XmlUtils {
	
	public static String removeIllegal(String input) {
		
		String invalidXMLChars = "[^[\\u0009\\u000A\\u000D][\\u0020-\\uD7FF][\\uE000-\\uFFFD][\\u10000-\\u10FFFF]]";
		
		return input
				.replaceAll(invalidXMLChars, "�");
	}
	
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
