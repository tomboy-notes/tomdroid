package org.tomdroid.ui;

import java.net.UnknownHostException;
import java.util.ArrayList;

import org.tomdroid.R;
import org.tomdroid.sync.ServiceAuth;
import org.tomdroid.sync.SyncManager;
import org.tomdroid.sync.SyncMethod;
import org.tomdroid.util.Preferences;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.widget.Toast;

public class PreferencesActivity extends PreferenceActivity {
	
	private static final String TAG = "PreferencesActivity";
	
	// TODO: put the various preferences in fields and figure out what to do on activity suspend/resume
	private EditTextPreference syncServerUriPreference = null;
	private ListPreference syncMethodPreference = null;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		// Fill the Preferences fields
		syncServerUriPreference = (EditTextPreference)findPreference(Preferences.Key.SYNC_SERVER_URI.getName());
		syncMethodPreference = (ListPreference)findPreference(Preferences.Key.SYNC_METHOD.getName());
		
		// Set the default values if nothing exists
		this.setDefaults();
		
		fillSyncServicesList();
		
		// Enable or disable the server field depending on the selected sync service
		updatePreferencesTo(syncMethodPreference.getValue());
		
		syncMethodPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				
				updatePreferencesTo((String)newValue);
				return true;
			}
		});
		
		// Re-authenticate if the sync server changes
		syncServerUriPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				
				if (newValue == null) {
					Toast.makeText(PreferencesActivity.this,
							"sync server uri changed but the new value is null",
							Toast.LENGTH_SHORT).show();
					return false;
				}
			    
				// update the value before doing anything
				String serverUri = (String) newValue;
				Preferences.putString(Preferences.Key.SYNC_SERVER_URI, serverUri);
				
				SyncMethod currentSyncMethod = SyncManager.getInstance().getCurrentSyncMethod();
				
				// check if the service needs authentication
				if (currentSyncMethod.needsAuth()) {
					
					Uri authorizationUri;
					try {
						authorizationUri = ((ServiceAuth) currentSyncMethod).getAuthUri(serverUri);
					} catch (UnknownHostException e) {
						connectionFailed();
						// Auth failed, don't update the value
						return false;
					}
					
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
	}
	
	private void fillSyncServicesList()
	{
		ArrayList<SyncMethod> availableServices = SyncManager.getInstance().getSyncMethods();
		CharSequence[] entries = new CharSequence[availableServices.size()];
		CharSequence[] entryValues = new CharSequence[availableServices.size()];
		
		for (int i = 0; i < availableServices.size(); i++) {
			entries[i] = availableServices.get(i).getDescription();
			entryValues[i] = availableServices.get(i).getName();
		}
		
		syncMethodPreference.setEntries(entries);
		syncMethodPreference.setEntryValues(entryValues);
	}
	
	private void setDefaults()
	{
		String defaultServer = (String)Preferences.Key.SYNC_SERVER_URI.getDefault();
		syncServerUriPreference.setDefaultValue(defaultServer);
		if(syncServerUriPreference.getText() == null)
			syncServerUriPreference.setText(defaultServer);
		
		String defaultService = (String)Preferences.Key.SYNC_METHOD.getDefault();
		syncMethodPreference.setDefaultValue(defaultService);
		if(syncMethodPreference.getValue() == null)
			syncMethodPreference.setValue(defaultService);
	}
	
	private void updatePreferencesTo(String syncMethodName) {
		
		SyncMethod syncMethod = SyncManager.getInstance().getSyncMethod(syncMethodName);
		
		if (syncMethod != null) {
			syncServerUriPreference.setEnabled(syncMethod.needsServer());
			syncMethodPreference.setSummary(syncMethod.getDescription());
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
