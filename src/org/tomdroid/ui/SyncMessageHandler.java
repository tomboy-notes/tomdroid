/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2010, Rodja Trappe <mail@rodja.net>
 * Copyright 2010, Benoit Garret <benoit.garret_launchpad@gadz.org>
 * Copyright 2010, Olivier Bilodeau <olivier@bottomlesspit.org>
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.Toast;
import org.tomdroid.R;
import org.tomdroid.sync.SyncManager;
import org.tomdroid.sync.SyncService;
import org.tomdroid.util.ErrorList;
import org.tomdroid.util.TLog;

public class SyncMessageHandler extends Handler {

	private static String TAG = "SycnMessageHandler";
	private Activity activity;
	
	public SyncMessageHandler(Activity activity) {
		this.activity = activity;
	}

	@Override
	public void handleMessage(Message msg) {

		String serviceDescription = SyncManager.getInstance().getCurrentService().getDescription();
		String message = "";
			
		switch (msg.what) {

			case SyncService.PARSING_COMPLETE:
				final ErrorList errors = (ErrorList)msg.obj;
				if(errors.isEmpty()) {
					message = this.activity.getString(R.string.messageSyncComplete);
					message = String.format(message,serviceDescription);
					Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
					refreshActivity();
				} else {

					message = this.activity.getString(R.string.messageSyncError);
					new AlertDialog.Builder(activity).setMessage(message)
						.setTitle(this.activity.getString(R.string.error))
						.setPositiveButton(this.activity.getString(R.string.btnSavetoSD), new OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								if(!errors.save()) {
									Toast.makeText(activity, activity.getString(R.string.messageCouldNotSave),
											Toast.LENGTH_SHORT).show();
								}
								refreshActivity();
							}
						})
						.setNegativeButton("Close", new OnClickListener() {
							public void onClick(DialogInterface dialog, int which) { refreshActivity(); }
						}).show();
				}
				break;

			case SyncService.PARSING_NO_NOTES:
				message = this.activity.getString(R.string.messageSyncNoNote);
				message = String.format(message,serviceDescription);
				Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
				break;
				
			case SyncService.NO_INTERNET:
				Toast.makeText(activity, this.activity.getString(R.string.messageSyncNoConnection),
						Toast.LENGTH_SHORT).show();
				break;
				
			case SyncService.NO_SD_CARD:
				Toast.makeText(activity, activity.getString(R.string.messageNoSDCard),
						Toast.LENGTH_SHORT).show();
				break;

			case SyncService.SYNC_PROGRESS:
				handleSyncProgress(msg);
				break;

			default:
				TLog.i(TAG, "handler called with an unknown message");
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
	
	private void refreshActivity() {
		if (! (activity.isTaskRoot())) {
			activity.finish(); activity.startActivity(activity.getIntent());
			TLog.d(TAG,"Restarted the current Activity: {0}", activity.toString());
		}
	}

}
