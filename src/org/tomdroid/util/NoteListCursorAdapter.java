package org.tomdroid.util;

import java.text.DateFormat;
import java.util.Date;

import android.text.format.DateUtils;
import android.text.format.Time;
import org.tomdroid.Note;
import org.tomdroid.R;
import org.tomdroid.ui.Tomdroid;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filterable;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/* Provides a custom ListView layout for Note List */

public class NoteListCursorAdapter extends SimpleCursorAdapter {

    private Context context;

    private int layout;
    private int[] colors = new int[] { 0xFFFFFFFF, 0xFFEEEEEE };

    private DateFormat localeDateFormat;
    private DateFormat localeTimeFormat;

    public NoteListCursorAdapter (Context context, int layout, Cursor c, String[] from, int[] to) {
        super(context, layout, c, from, to);
        this.context = context;
        this.layout = layout;
        localeDateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        localeTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
    }
    

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        Cursor c = getCursor();

        final LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(layout, parent, false);

        populateFields(v, c);

        return v;
    }

    @Override
    public void bindView(View v, Context context, Cursor c) {

        populateFields(v, c);
    }
    
    @Override
	public View getView(int position, View convertView, ViewGroup parent) {
    	View view = super.getView(position, convertView, parent);
    	int colorPos = position % colors.length;
    	view.setBackgroundColor(colors[colorPos]);
    	//view.setTextColor(Color.DKGRAY);
    	return view;
	}
    
    private void populateFields(View v, Cursor c){

        int nameCol = c.getColumnIndex(Note.TITLE);
        int modifiedCol = c.getColumnIndex(Note.MODIFIED_DATE);
        
        String title = c.getString(nameCol);
        
        //Format last modified dates to be similar to desktop Tomboy
        //TODO this is messy - must be a better way than having 3 separate date types
        Time lastModified = new Time();
        lastModified.parse3339(c.getString(modifiedCol));
        Long lastModifiedMillis = lastModified.toMillis(false);
        Date lastModifiedDate = new Date(lastModifiedMillis);
        
        boolean isSynced = c.getInt(c.getColumnIndex(Note.IS_SYNCED)) != 0;
        String strModified = (isSynced ? "" : "Locally ") + "Modified: ";
        //TODO this is very inefficient
        if (DateUtils.isToday(lastModifiedMillis)){
        	strModified += "Today, " + localeTimeFormat.format(lastModifiedDate);
        } else {
        	// Add a day to the last modified date - if the date is now today, it means the note was edited yesterday
        	Time yesterdayTest = lastModified;
        	yesterdayTest.monthDay += 1;
        	if (DateUtils.isToday(yesterdayTest.toMillis(false))){
        		strModified += "Yesterday, " + localeTimeFormat.format(lastModifiedDate);
        	} else {
        		strModified += localeDateFormat.format(lastModifiedDate) + ", " + localeTimeFormat.format(lastModifiedDate);
        	}
        }

        /**
         * Next set the name of the entry.
         */
        TextView note_title = (TextView) v.findViewById(R.id.note_title);
        if (note_title != null) {
        	note_title.setText(title);
        }
        TextView note_modified = (TextView) v.findViewById(R.id.note_date);
        if (note_modified != null) {
        	note_modified.setText(strModified);
        }
    }

}
