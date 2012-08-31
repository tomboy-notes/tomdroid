package org.tomdroid.util;

import android.annotation.TargetApi;
import android.app.Activity;

public class Honeycomb {
	@TargetApi(11)
	public static void invalidateOptionsMenuWrapper(Activity activity) {
		activity.invalidateOptionsMenu();
	}
}
