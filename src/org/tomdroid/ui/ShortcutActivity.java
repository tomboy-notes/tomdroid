/**
 Tomdroid
 Tomboy on Android
 http://www.launchpad.net/tomdroid

 Copyright 2011 Piotr Adamski <mcveat@gmail.com>

 This file is part of Tomdroid.

 Tomdroid is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Tomdroid is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Tomdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomdroid.ui;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import org.tomdroid.Note;
import org.tomdroid.NoteManager;
import org.tomdroid.R;

import static android.content.Intent.ShortcutIconResource;

/**
 * @author Piotr Adamski <mcveat@gmail.com>
 */
public class ShortcutActivity extends ListActivity {
    private final String TAG = ShortcutActivity.class.getName();
    private ListAdapter adapter;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "creating shortcut...");
        setContentView(R.layout.shortcuts_list);

        adapter = NoteManager.getListAdapter(this);
        setListAdapter(adapter);
        getListView().setEmptyView(findViewById(R.id.list_empty));
    }

    @Override
    protected void onListItemClick(final ListView l, final View v, final int position, final long id) {
        final Cursor item = (Cursor) adapter.getItem(position);
        Intent launchIntent = getNoteViewIntent(item);
        final String name = getNoteTitle(item);
        final ShortcutIconResource icon = ShortcutIconResource.fromContext(this, R.drawable.shortcut_icon);
        setResult(RESULT_OK, createShortcutIntent(icon, launchIntent, name));
        finish();
    }

    private Intent getNoteViewIntent(final Cursor item) {
        final int noteId = item.getInt(item.getColumnIndexOrThrow(Note.ID));
        final Uri intentUri = Tomdroid.getNoteIntentUri(noteId);
        return new Intent(Intent.ACTION_VIEW, intentUri, this, ViewNote.class);
    }

    private String getNoteTitle(final Cursor item) {
        return item.getString(item.getColumnIndexOrThrow(Note.TITLE));
    }

    private Intent createShortcutIntent(final ShortcutIconResource icon, final Intent launchIntent, final String name) {
        Intent i = new Intent();
        i.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);
        i.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        i.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
        return i;
    }
}
