/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2011, Olivier Bilodeau <olivier@bottomlesspit.org>
 * Copyright 2009, Benoit Garret <benoit.garret_launchpad@gadz.org>
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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {
	
	public enum Key {
		SYNC_SERVICE ("sync_service", "sdcard"),
		SYNC_SERVER_ROOT_API ("sync_server_root_api", ""),
		SYNC_SERVER_USER_API ("sync_server_user_api", ""),
		SYNC_SERVER ("sync_server", "https://"),
		SD_LOCATION ("sd_location", "tomdroid"),
		LAST_FILE_PATH ("last_file_path", "/"),
		SYNC_ON_START("sync_on_start",false),
		INCLUDE_NOTE_TEMPLATES("include_note_templates", false),
		INCLUDE_DELETED_NOTES("include_deleted_notes", false),
		LINK_TITLES("link_titles", true),
		LINK_URLS("link_urls", true),
		LINK_EMAILS("link_emails", true),
		LINK_PHONES("link_phones", true),
		LINK_ADDRESSES("link_addresses", true),
		DEL_ALL_NOTES("del_all_notes", ""),
		DEL_REMOTE_NOTES("del_remote_notes", ""),
		BACKUP_NOTES("backup_notes", ""),
		AUTO_BACKUP_NOTES("auto_backup_notes", false),
		RESTORE_NOTES("restore_notes", ""),
		CLEAR_SEARCH_HISTORY ("clearSearchHistory", ""),
		ACCESS_TOKEN ("access_token", ""),
		ACCESS_TOKEN_SECRET ("access_token_secret", ""),
		REQUEST_TOKEN ("request_token", ""),
		REQUEST_TOKEN_SECRET ("request_token_secret", ""),
		OAUTH_10A ("oauth_10a", false),
		AUTHORIZE_URL ("authorize_url", ""),
		ACCESS_TOKEN_URL ("access_token_url", ""),
		REQUEST_TOKEN_URL ("request_token_url", ""),
		LATEST_SYNC_REVISION ("latest_sync_revision", -1L),
		LATEST_SYNC_DATE ("latest_sync_date", (new Time()).formatTomboy()), // will be used to tell whether we have newer notes
		SORT_ORDER ("sort_order", "sort_date"),
		FIRST_RUN ("first_run", true),
		BASE_TEXT_SIZE("base_text_size","18");

		private String name = "";
		private Object defaultValue = "";
		
		Key(String name, Object defaultValue) {
			this.name = name;
			this.defaultValue = defaultValue;
		}
		
		public String getName() {
			return name;
		}
		
		public Object getDefault() {
			return defaultValue;
		}
	}
	
	private static SharedPreferences client = null;
	private static SharedPreferences.Editor editor = null;
	
	public static void init(Context context, boolean clean) {
		
		client = PreferenceManager.getDefaultSharedPreferences(context);
		editor = client.edit();
		
		if (clean)
			editor.clear().commit();
	}
	
	public static String getString(Key key) {
		return client.getString(key.getName(), (String) key.getDefault());
	}
	
	public static void putString(Key key, String value) {
		
		if (value == null)
			editor.putString(key.getName(), (String)key.getDefault());
		else
			editor.putString(key.getName(), value);
		editor.commit();
	}
	
	public static long getLong(Key key) {
		
		return client.getLong(key.getName(), (Long)key.getDefault());
	}
	
	public static void putLong(Key key, long value) {
		
		editor.putLong(key.getName(), value);
		editor.commit();
	}
	
	public static boolean getBoolean(Key key) {
		
		return client.getBoolean(key.getName(), (Boolean)key.getDefault());
	}
	
	public static void putBoolean(Key key, boolean value) {
		
		editor.putBoolean(key.getName(), value);
		editor.commit();
	}
}
