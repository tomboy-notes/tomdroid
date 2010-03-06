package org.tomdroid.ui;

import java.util.ArrayList;

import org.tomdroid.R;
import org.tomdroid.sync.ServiceAuth;
import org.tomdroid.sync.SyncManager;
import org.tomdroid.sync.SyncService;
import org.tomdroid.util.Preferences;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;

public class PreferencesActivity extends PreferenceActivity {
	
	private static final String TAG = "PreferencesActivity";
	
	// TODO: put the various preferences in fields and figure out what to do on activity suspend/resume
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		Preference syncServer = findPreference(Preferences.Key.SYNC_SERVER.getName());
		syncServer.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				
				// update the value before doing anything
				String server = (String)newValue;
				Preferences.putString(Preferences.Key.SYNC_SERVER, server);
				
				// get the current service
				SyncService currentService = SyncManager.getInstance().getCurrentService();
				
				// check if the service needs authentication
				if (currentService.needsAuth()) {
					
					Uri authorizationUri = ((ServiceAuth)currentService).getAuthUri(server);
					
					if (authorizationUri != null) {
						
						Intent i = new Intent(Intent.ACTION_VIEW, authorizationUri);
						startActivity(i);
						return true;
						
					} else {
						connectionFailed();
						// Auth failed, don't update the value
						return false;
					}
				}
				
				return true;
			}
			
		});
		
		ListPreference syncService = (ListPreference)findPreference(Preferences.Key.SYNC_SERVICE.getName());
		
		ArrayList<SyncService> availableServices = SyncManager.getInstance().getServices();
		CharSequence[] entries = new CharSequence[availableServices.size()];
		CharSequence[] entryValues = new CharSequence[availableServices.size()];
		
		for (int i = 0; i < availableServices.size(); i++) {
			entries[i] = availableServices.get(i).getDescription();
			entryValues[i] = availableServices.get(i).getName();
		}
		
		syncService.setEntries(entries);
		syncService.setEntryValues(entryValues);
		
		if (syncService.getValue() == null)
			syncService.setValue((String)Preferences.Key.SYNC_SERVICE.getDefault());
		
		setServer(syncService.getValue());
		
		syncService.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				
				setServer((String)newValue);
				return true;
			}
		});
	}
	
	private void setServer(String syncServiceKey) {
		
		Preference syncServer = findPreference(Preferences.Key.SYNC_SERVER.getName());
		Preference syncService = findPreference(Preferences.Key.SYNC_SERVICE.getName());
		
		SyncService service = SyncManager.getInstance().getService(syncServiceKey);
		
		if (service != null) {
			syncServer.setEnabled(service.needsServer());
			syncService.setSummary(service.getDescription());
		}
	}
	
	private void connectionFailed() {
		new AlertDialog.Builder(this)
			.setMessage(getString(R.string.prefSyncConnectionFailed))
			.setNeutralButton(getString(R.string.btnOk), new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}})
			.show();
	}
}
