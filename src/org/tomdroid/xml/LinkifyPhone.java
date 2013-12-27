/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2010 Olivier Bilodeau <olivier@bottomlesspit.org>
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
// Portions of this file are covered under APL 2 and taken from
// http://android.git.kernel.org/?p=platform/frameworks/base.git;a=blob;f=core/java/android/text/util/Linkify.java
// http://android.git.kernel.org/?p=platform/frameworks/base.git;a=blob;f=core/java/android/text/util/Regex.java
/* 
 * Copyright (C) 2007 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tomdroid.xml;

import java.util.regex.Pattern;

import android.text.util.Linkify.MatchFilter;

/**
 * Statics useful for Linkify to create a better phone handler than android's default one
 * Fixes bugs like lp:512204
 */
public class LinkifyPhone {
	/**
	  * Don't treat anything with fewer than this many digits as a
	  * phone number.
	  */
	private static final int PHONE_NUMBER_MINIMUM_DIGITS = 5;
	  
	public static final Pattern PHONE_PATTERN = Pattern.compile( // sdd = space, dot, or dash
			"(\\+[0-9]+[\\- \\.]*)?"                    // +<digits><sdd>*
			+ "(\\([0-9]+\\)[\\- \\.]*)?"               // (<digits>)<sdd>*
			+ "([0-9]+[\\- \\.][0-9\\- \\.]+[0-9])"); // <digits><sdd><digits|sdds><digit> (at least one sdd!) 

	/**
	 *  Filters out URL matches that:
	 *  - the character before the match is not a whitespace
	 *  - don't have enough digits to be a phone number
	 */
	public static final MatchFilter sPhoneNumberMatchFilter = new MatchFilter() {

		public final boolean acceptMatch(CharSequence s, int start, int end) {

			// make sure there was a whitespace before pattern
			try {
				if (!Character.isWhitespace(s.charAt(start - 1))) {
					return false;
				}
			} catch (IndexOutOfBoundsException e) {
				//Do nothing
			}

			// minimum length
			int digitCount = 0;
			for (int i = start; i < end; i++) {
				if (Character.isDigit(s.charAt(i))) {
					digitCount++;
					if (digitCount >= PHONE_NUMBER_MINIMUM_DIGITS) {
						return true;
					}
				}
			}
			return false;
		}
	};

}
