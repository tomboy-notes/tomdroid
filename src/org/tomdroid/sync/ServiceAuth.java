package org.tomdroid.sync;

import android.net.Uri;
import android.os.Handler;

public interface ServiceAuth {
	
	public boolean isConfigured();
	public void getAuthUri(final String server, Handler handler);
	public void remoteAuthComplete(Uri uri, Handler handler);
}
