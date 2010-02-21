package org.tomdroid.sync;

import android.net.Uri;

public interface ServiceAuth {
	
	public boolean isConfigured();
	public Uri getAuthUri(String server);
	public void remoteAuthComplete(Uri uri);
}
