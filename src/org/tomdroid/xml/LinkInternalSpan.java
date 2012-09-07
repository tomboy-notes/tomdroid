/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2012 Koichi Akabe <vbkaisetsu@gmail.com>
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

import org.tomdroid.NoteManager;
import org.tomdroid.ui.Tomdroid;
import org.tomdroid.util.TLog;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.text.util.Linkify.MatchFilter;
import android.view.View;

/*
 * This class is responsible for parsing the xml note content
 * and formatting the contents in a SpannableStringBuilder
 */
public class LinkInternalSpan extends ClickableSpan {

	// Logging info
	private static final String TAG = "LinkInternalSpan";
	
	private String title;
	public LinkInternalSpan(String title) {
		super();
		this.title = title;
	}

	@Override
	public void onClick(View v) {
		Activity act = (Activity)v.getContext();
		int id = NoteManager.getNoteId(act, title);
		Uri intentUri;
		if(id != 0) {
			intentUri = Uri.parse(Tomdroid.CONTENT_URI.toString()+"/"+id);
		} else {
			/* TODO: open new note */
			TLog.d(TAG, "link: {0} was clicked", title);
			return;
		}
		Intent i = new Intent(Intent.ACTION_VIEW, intentUri);
		act.startActivity(i);
	}
	
	public static MatchFilter getNoteLinkMatchFilter(final SpannableStringBuilder noteContent, final LinkInternalSpan[] links) {
		
		return new MatchFilter() {
			
			public boolean acceptMatch(CharSequence s, int start, int end) {
				int spanstart, spanend;
				for(LinkInternalSpan link: links) {
					spanstart = noteContent.getSpanStart(link);
					spanend = noteContent.getSpanEnd(link);
					if(!(end <= spanstart || spanend <= start)) {
						return false;
					}
				}
				return true;
			}
		};
	}
}
