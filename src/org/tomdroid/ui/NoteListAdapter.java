package org.tomdroid.ui;

import java.io.File;

import org.tomdroid.Note;
import org.tomdroid.R;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;

public class NoteListAdapter extends SimpleCursorAdapter{
	
	public NoteListAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to) {
		super(context, layout, c, from, to);		
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return super.newView(context, cursor, parent);
	}
	
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		File voiceNote = new File(context.getFilesDir(),cursor.getString(cursor.getColumnIndex(Note.GUID))+".note.amr");
		ImageView iv = (ImageView)view.findViewById(R.id.voice_indicator);
		iv.setImageBitmap(null);
	if (voiceNote.exists()) {
		iv.setImageDrawable(context.getResources().getDrawable(R.drawable.voicenote));
	} 
		super.bindView(view, context, cursor);
	}

}

