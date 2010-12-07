/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
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
package org.tomdroid.ui;

import org.tomdroid.R;
import org.tomdroid.sync.SyncManager;
import org.tomdroid.sync.SyncService;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.Toast;

public class SyncMessageHandler extends Handler {

	private static String TAG = "SycnMessageHandler";
	private Activity activity;

	// State variables
	private boolean parsingErrorShown = false;
	
	public SyncMessageHandler(Activity activity) {
		this.activity = activity;
	}

	@Override
	public void handleMessage(Message msg) {

		String serviceDescription = SyncManager.getInstance().getCurrentService().getDescription();
		String message = "";
			
		switch (msg.what) {
			case SyncService.PARSING_COMPLETE:				
				message = this.activity.getString(R.string.messageSyncComplete);
				message = String.format(message,serviceDescription);
				Toast.makeText(	activity,message, Toast.LENGTH_SHORT).show();
				break;

			case SyncService.PARSING_NO_NOTES:
				message = this.activity.getString(R.string.messageSyncNoNote);
				message = String.format(message,serviceDescription);
				Toast.makeText(activity,message, Toast.LENGTH_SHORT).show();
				break;

			case SyncService.PARSING_FAILED:
				if (Tomdroid.LOGGING_ENABLED)
					Log.w(TAG, "handler called with a parsing failed message");

				// if we already shown a parsing error in this pass, we
				// won't show it again
				if (!parsingErrorShown) {
					parsingErrorShown = true;

					message = this.activity.getString(R.string.messageSyncError);
					new AlertDialog.Builder(activity).setMessage(message)
							.setTitle(this.activity.getString(R.string.error)).setNeutralButton("Ok", new OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							}).show();
				}
				break;

			case SyncService.NO_INTERNET:
				// TODO put string in a translatable bundle
				Toast.makeText(activity, this.activity.getString(R.string.messageSyncNoConnection),
						Toast.LENGTH_SHORT).show();
				break;

			case SyncService.SYNC_PROGRESS:
				handleSyncProgress(msg);
				break;

			default:
				if (Tomdroid.LOGGING_ENABLED)
					Log.i(TAG, "handler called with an unknown message");
				break;

		}
	}

	private void handleSyncProgress(Message msg) {
		ImageView syncIcon = (ImageView) activity.findViewById(R.id.syncIcon);

		RotateAnimation rotation = new RotateAnimation(180 * msg.arg2 / 100f,
				180 * msg.arg1 / 100f, Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
		rotation.setDuration(700);
		rotation.setFillAfter(true);
		syncIcon.startAnimation(rotation);

		if (msg.arg1 == 0) {
			onSynchronizationStarted();
		} else if (msg.arg1 == 100) {
			onSynchronizationDone();
		}
	}

	private void onSynchronizationDone() {
		ImageView syncButton = (ImageView) activity.findViewById(R.id.sync);
		ImageView syncIcon = (ImageView) activity.findViewById(R.id.syncIcon);

		syncButton.setClickable(true);
		syncIcon.getDrawable().setAlpha(Actionbar.DEFAULT_ICON_ALPHA);

		View dot = activity.findViewById(R.id.sync_dot);
		dot.setVisibility(View.INVISIBLE);
		dot.getAnimation().setRepeatCount(0);
	}

	private void onSynchronizationStarted() {
		ImageView syncButton = (ImageView) activity.findViewById(R.id.sync);
		ImageView syncIcon = (ImageView) activity.findViewById(R.id.syncIcon);

		syncButton.setClickable(false);
		syncIcon.getDrawable().setAlpha(40);
		
		Animation pulse = AnimationUtils.loadAnimation(activity, R.anim.pulse);
		View dot = activity.findViewById(R.id.sync_dot);
		dot.setVisibility(View.VISIBLE);
		dot.startAnimation(pulse);
	}

}
