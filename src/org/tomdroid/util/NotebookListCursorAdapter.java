package org.tomdroid.util;

import java.util.ArrayList;
import java.util.HashMap;

import org.tomdroid.Notebook;
import org.tomdroid.R;
import org.tomdroid.ui.Tomdroid;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class NotebookListCursorAdapter extends SimpleCursorAdapter {
	public HashMap<String,String> checkboxInit = new HashMap<String, String>();;
	public HashMap<String,String> checkbox = new HashMap<String, String>();;
	public ArrayList<String > notebookName = new ArrayList<String>();

	private String TAG = "NotebookListCursorAdapter";
	private int nbNotebook = 0;
	private int nbCheck = 0;


	public NotebookListCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
		super(context, layout, c, from, to);
		nbNotebook = c.getCount();

		if (c.moveToFirst()){
			int colName = c.getColumnIndex(Notebook.NAME);
			int colDipslay = c.getColumnIndex(Notebook.DISPLAY);
			
			checkboxInit.put(c.getString(colName), c.getString(colDipslay));
			notebookName.add(c.getString(colName));
			
			while (c.moveToNext()){
				if (c.getInt(colDipslay)==1){
					nbCheck++;
				}
				checkboxInit.put(c.getString(colName), c.getString(colDipslay));
				notebookName.add(c.getString(colName));
			}
			checkbox = (HashMap<String, String>) checkboxInit.clone();
		}
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

	public int changeValue(String notebook,int value){
		Log.i(TAG, "changeValue");
		checkbox.remove(notebook);
		checkbox.put(notebook, ""+value);
		Log.i(TAG, "changeValue notebook:"+checkbox.get(notebook));
		
		if (value==1){
			nbCheck++;
		} else {
			nbCheck--;
		}
		return nbCheck; 
	}
	
	public void updateChange(Activity activity){
		Log.i(TAG, "updateChange");
		Uri uriNotebooks = Tomdroid.CONTENT_URI_NOTEBOOK;
		String[] whereArgs = new String[1];
		ContentResolver cr = activity.getContentResolver();	
		
		for (int i = 0; i < nbNotebook; i++) {
			String notebook = notebookName.get(i);
			if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "traitement :"+notebook);
			
			if (checkbox.get(notebook).compareTo(checkboxInit.get(notebook))!=0){
				if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, ""+notebook+" a changé");
				whereArgs[0] = notebook;
				
				ContentValues values = new ContentValues();
				values.put(Notebook.DISPLAY, Integer.parseInt(checkbox.get(notebook)));	
				cr.update(uriNotebooks, values, Notebook.NAME +" = ?", whereArgs);
				
				if (Tomdroid.LOGGING_ENABLED) Log.v(TAG,"Notebook updated in content provider. Name:"+notebook);
			}
			
		}
	}


	public int getNbCheck() {
		return nbCheck;
	}



}
