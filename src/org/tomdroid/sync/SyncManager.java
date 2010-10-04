/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2009, Benoit Garret <benoit.garret_launchpad@gadz.org>
 * Copyright 2010, Rodja Trappe <mail@rodja.net>
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
package org.tomdroid.sync;

import java.io.FileNotFoundException;
import java.util.ArrayList;

import org.tomdroid.sync.sd.SdCardSyncService;
import org.tomdroid.sync.web.SnowySyncMethod;
import org.tomdroid.util.Preferences;

import android.app.Activity;
import android.os.Handler;

public class SyncManager {
	
	@SuppressWarnings("unused")
	private static final String TAG = "SyncManager";
	
	private ArrayList<SyncMethod> syncMethods = new ArrayList<SyncMethod>();
	
	public SyncManager() {
		createSyncMethods();
	}

	public ArrayList<SyncMethod> getSyncMethods() {
		return syncMethods;
	}
	
	public SyncMethod getSyncMethod(String name) {
		
		for (int i = 0; i < syncMethods.size(); i++) {
			SyncMethod method = syncMethods.get(i);
			if (name.equals(method.getName()))
				return method;
		}
		
		return null;
	}
	
	public void startSynchronization() {
		
		SyncMethod method = getCurrentSyncMethod();
		method.startSynchronization();
	}
	
	public SyncMethod getCurrentSyncMethod() {
		
		String syncMethodName = Preferences.getString(Preferences.Key.SYNC_METHOD);
		return getSyncMethod(syncMethodName);
	}
	
	private static SyncManager instance = null;
	private static Activity activity;
	private static Handler handler;
	
	public static SyncManager getInstance() {
		
		if (instance == null)
			instance = new SyncManager();
		
		return instance;
	}
	
	public static void setActivity(Activity a) {
		activity = a;
		getInstance().createSyncMethods();
	}
	
	public static void setHandler(Handler h) {
		handler = h;
		getInstance().createSyncMethods();
	}

	private void createSyncMethods() {
		syncMethods.clear();
		
		syncMethods.add(new SnowySyncMethod(activity, handler));
		
		try {
			syncMethods.add(new SdCardSyncService(activity, handler));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
