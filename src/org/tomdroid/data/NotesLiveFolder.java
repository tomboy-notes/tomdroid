/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2009 Benoit Garret <benoit.garret_launchpad@gadz.org>
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

/*
 * This file was inspired by the Live Folders tutorial available at
 * http://android-developers.blogspot.com/2009/04/live-folders.html
 */

package org.tomdroid.data;

import org.tomdroid.R;
import org.tomdroid.ui.Tomdroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.LiveFolders;

public class NotesLiveFolder extends Activity {
	
	public static final Uri CONTENT_URI = Uri.parse("content://" + Tomdroid.AUTHORITY + "/live_folder/notes");
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final Intent intent = getIntent();
		final String action = intent.getAction();
		
		if (LiveFolders.ACTION_CREATE_LIVE_FOLDER.equals(action)) {
			setResult(RESULT_OK, createLiveFolder(this, CONTENT_URI,
					"Notes", R.drawable.icon));
		} else {
			setResult(RESULT_CANCELED);
		}
		
		finish();
	}
	
	private static Intent createLiveFolder(Context context, Uri uri, String name, int icon) {
		
		final Intent intent = new Intent();
		
		intent.setData(uri);
		intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_NAME, name);
		intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_ICON,
				Intent.ShortcutIconResource.fromContext(context, icon));
		intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_DISPLAY_MODE, LiveFolders.DISPLAY_MODE_LIST);
		
		return intent;
    }

}
