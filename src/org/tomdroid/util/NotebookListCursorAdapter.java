package org.tomdroid.util;


import org.tomdroid.Notebook;
import org.tomdroid.R;
import org.tomdroid.ui.Tomdroid;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class NotebookListCursorAdapter extends SimpleCursorAdapter {
	private String TAG = "NotebookListCursorAdapter";
	

	public NotebookListCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
		super(context, layout, c, from, to);
	}


	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView name = (TextView) view.findViewById(R.id.notebook_name);
		if (name!=null){
			if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "notebook name:"+cursor.getString(cursor.getColumnIndex(Notebook.NAME)));
			name.setText(cursor.getString(cursor.getColumnIndex(Notebook.NAME)));
		}
		
		CheckBox chekbox = (CheckBox) view.findViewById(R.id.notebook_checkbox);
		if (chekbox!=null){
			int dipslay = cursor.getInt(cursor.getColumnIndex(Notebook.DISPLAY)); 
			if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "dipslay:"+dipslay);
			chekbox.setChecked(dipslay==1);
		}
	}
	
	
}
