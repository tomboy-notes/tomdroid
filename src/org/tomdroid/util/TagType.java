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

// enum holding all types of xml tags known by tomdroid.
// neccesary for the spannable string to xml converter.
public enum TagType { 	
	ROOT,
	LIST,
	LIST_ITEM,
	BOLD,
	ITALIC,
	HIGHLIGHT,
	LINK,
	LINK_INTERNAL,
	TEXT,
	STRIKETHROUGH,
	MONOSPACE,
	SIZE_SMALL,
	SIZE_LARGE,
	SIZE_HUGE,
	MARGIN,
	OTHER
}
