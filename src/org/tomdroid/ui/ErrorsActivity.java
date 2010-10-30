/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2009, Benoit Garret <benoit.garret_launchpad@gadz.org>
 * Copyright 2010, Olivier Bilodeau <olivier@bottomlesspit.org>
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
package org.tomdroid.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import org.tomdroid.R;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;

public class ErrorsActivity extends ListActivity {
	
	private static final String TAG = "ErrorsActivity";
	
	private ListAdapter adapter;
	private String[] titles;
	private String[] filenames;
	private String[] contents;
	private String[] errors;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.errors_app);
	}
	
	public void onResume() {
		super.onResume();
		Intent intent = this.getIntent();
		
		if (intent != null) {
			Uri uri = intent.getData();

			if (uri != null && uri.getScheme().equals("tomdroid")) {
				Log.i(TAG, "Got url : " + uri.toString());

				Bundle bundle = intent.getBundleExtra("org.tomdroid.errors.ShowAll");
				titles = bundle.getStringArray("titles");
				filenames = bundle.getStringArray("filenames");
				contents = bundle.getStringArray("contents");
				errors = bundle.getStringArray("errors");
				
				// adapter that binds the ListView UI to the notes in the note manager
				adapter = new ArrayAdapter<String>(this, R.layout.error_item, titles);
				setListAdapter(adapter);
			}
		}
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, final int position, long id) {

		AlertDialog.Builder builder;
		
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.errors_app_show, null);

		EditText text = (EditText)layout.findViewById(R.id.text);
		text.setText(errors[position]);

		builder = new AlertDialog.Builder(this);
		builder.setView(layout);
		builder.setNegativeButton("Close", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {}
		});
		builder.setPositiveButton("Save to sd card", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				saveError(position);
			}
		});
		builder.show();
	}
	
	private void saveError(int position) {
		String path = Tomdroid.NOTES_PATH+"errors/";
		String filename = new File(filenames[position]).getName();
		
		boolean exists = new File(path).exists();
		if (!exists){new File(path).mkdirs();}
		
		try {
			FileWriter file;
			
			if(contents[position] != null) {
				file = new FileWriter(path+filename);
				file.write(contents[position]);
				file.flush();
				file.close();
			}
			
			file = new FileWriter(path+filename+".exception");
			file.write(errors[position]);
			file.flush();
			file.close();
		} catch (FileNotFoundException e) {
		 // TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
		 // TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
