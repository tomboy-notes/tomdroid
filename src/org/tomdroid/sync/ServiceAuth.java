package org.tomdroid.sync;

import java.net.UnknownHostException;

import android.net.Uri;

public interface ServiceAuth {
	
	public boolean isConfigured();
	public Uri getAuthUri(String server) throws UnknownHostException;
	public void remoteAuthComplete(Uri uri);
}
