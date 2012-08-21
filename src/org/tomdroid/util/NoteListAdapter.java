/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2010, Matthew Stevenson <saturnreturn@gmail.com>
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

import java.text.DateFormat;
import java.util.*;

import android.widget.BaseAdapter;
import org.tomdroid.Note;
import org.tomdroid.R;

import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/* Provides a custom ListView layout for Note List */

public class NoteListAdapter extends BaseAdapter {
    public static final int NOTEBOOK_TYPE = 0;
    public static final int NOTE_TYPE = 1;

    private Context context;
    private final List<String> notebooks;
    private final List<Note> notes;

    private DateFormat localeDateFormat;
    private DateFormat localeTimeFormat;
    private LayoutInflater inflater;

    public NoteListAdapter(Context context, List<String> notebooks, List<Note> notes) {
        super();
        this.context = context;
        this.notebooks = notebooks;
        this.notes = notes;
        localeDateFormat = android.text.format.DateFormat.getDateFormat(context);
        localeTimeFormat = android.text.format.DateFormat.getTimeFormat(context);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public NoteListAdapter(Context context, List<Note> notes) {
        this(context, Collections.<String>emptyList(), notes);
    }

    @Override
    public int getCount() {
        return notebooks.size() + notes.size();
    }

    @Override
    public Object getItem(int i) {
        int notebooksSize = notebooks.size();
        return i <= notebooksSize - 1 ? notebooks.get(i) : notes.get(i - notebooksSize);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public int getItemViewType(int position) {
        return position <= notebooks.size() - 1 ? NOTEBOOK_TYPE : NOTE_TYPE;
    }

    @Override
    public int getViewTypeCount() {
        return NOTE_TYPE + 1;
    }

    @Override
	public View getView(int position, View convertView, ViewGroup parent) {
        int type = getItemViewType(position);
        if (convertView == null) convertView = inflateRightView(type);
        switch (type) {
            case NOTE_TYPE:
                populateNoteFields(convertView, (Note) getItem(position));
                break;
            case NOTEBOOK_TYPE:
                populateNotebookFields(convertView, (String) getItem(position));
                break;
        }
        return convertView;
    }

    private void populateNotebookFields(View v, String notebookName) {
        View title = v.findViewById(R.id.notebook_title);
        if (title != null) ((TextView) title).setText(notebookName);
    }

    private View inflateRightView(int type) {
        return type == NOTE_TYPE ?
                inflater.inflate(R.layout.main_list_item, null) :
                inflater.inflate(R.layout.main_list_notebook_item, null);
    }

    private void populateNoteFields(View v, Note n){
        //Format last modified dates to be similar to desktop Tomboy
        //TODO this is messy - must be a better way than having 3 separate date types
        Time lastModified = n.getLastChangeDate();
        Long lastModifiedMillis = lastModified.toMillis(false);
        Date lastModifiedDate = new Date(lastModifiedMillis);
        
        String strModified = this.context.getString(R.string.textModified)+" ";
        //TODO this is very inefficient
        if (DateUtils.isToday(lastModifiedMillis)){
        	strModified += this.context.getString(R.string.textToday) +", " + localeTimeFormat.format(lastModifiedDate);
        } else {
        	// Add a day to the last modified date - if the date is now today, it means the note was edited yesterday
            lastModified.monthDay += 1;
        	if (DateUtils.isToday(lastModified.toMillis(false))){
        		strModified += this.context.getString(R.string.textYexterday) +", " + localeTimeFormat.format(lastModifiedDate);
        	} else {
        		strModified += localeDateFormat.format(lastModifiedDate) + ", " + localeTimeFormat.format(lastModifiedDate);
        	}
        }

        /**
         * Next set the name of the entry.
         */
        TextView note_title = (TextView) v.findViewById(R.id.note_title);
        if (note_title != null) {
        	note_title.setText(n.getTitle());
        }
        TextView note_modified = (TextView) v.findViewById(R.id.note_date);
        if (note_modified != null) {
        	note_modified.setText(strModified);
        }
    }
}
