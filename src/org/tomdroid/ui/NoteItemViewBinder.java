package org.tomdroid.ui;

import org.tomdroid.Note;
import org.tomdroid.R;

import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;
import android.widget.SimpleCursorAdapter.ViewBinder;

public class NoteItemViewBinder implements ViewBinder {

	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        TextView titleView = (TextView) view;
        titleView.setText(cursor.getString(1));
        if (cursor.getInt(2) == 1)
        	titleView.setTextAppearance(view.getContext(), R.style.NoteListTitle);
        else
        	titleView.setTextAppearance(view.getContext(), R.style.NoteListTileUnsynced);
        return true;
	}
}
