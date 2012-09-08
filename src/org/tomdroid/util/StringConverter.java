/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2011 Koichi Akabe <vbkaisetsu@gmail.com>
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

public class StringConverter {
	public static String encode(String text) {
		return text.replace("&", "&amp;")
		           .replace("%", "&pct;")
		           .replace("_", "&und;");
	}
	
	public static String decode(String text) {
		return text.replace("&und;", "_")
		           .replace("&pct;", "%")
		           .replace("&amp;", "&");
	}
}
