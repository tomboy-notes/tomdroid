package org.tomdroid.sync;

import java.io.FileNotFoundException;
import java.util.ArrayList;

import org.tomdroid.sync.sd.SdCardSyncService;
import org.tomdroid.sync.web.SnowySyncService;
import org.tomdroid.util.Preferences;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;

public class SyncManager {
	
	private static final String TAG = "SyncManager";
	
	private ArrayList<SyncService> services = new ArrayList<SyncService>();
	
	public SyncManager() {
		services.add(new SnowySyncService(activity, handler));
		
		try {
			services.add(new SdCardSyncService(activity, handler));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public ArrayList<SyncService> getServices() {
		return services;
	}
	
	public SyncService getService(String name) {
		
		for (int i = 0; i < services.size(); i++) {
			SyncService service = services.get(i);			
			if (name.equals(service.getName()))
				return service;
		}
		
		return null;
	}
	
	public void sync() {
		
		SyncService service = getCurrentService();
		service.sync();
	}
	
	public SyncService getCurrentService() {
		String serviceName = Preferences.getString(Preferences.Key.SYNC_SERVICE);
		return getService(serviceName);
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
	}
	
	public static void setHandler(Handler h) {
		handler = h;
	}
}
