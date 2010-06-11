package org.tomdroid.ui;

import org.tomdroid.R;
import org.tomdroid.sync.SyncManager;
import org.tomdroid.sync.SyncService;

import android.app.Activity;
import android.app.AlertDialog;
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

	private static String	TAG	= "SycMessageHandler";
	private Activity activity;

	// State variables
	private boolean				parsingErrorShown	= false;
	
	public SyncMessageHandler(Activity activity) {
		this.activity = activity;
	}

	@Override
	public void handleMessage(Message msg) {
		if (Tomdroid.LOGGING_ENABLED) Log.d(TAG, "handling message for " + activity.getClass().getCanonicalName());

		switch (msg.what) {
			case SyncService.PARSING_COMPLETE:
				// TODO put string in a translatable bundle
				Toast.makeText(
						activity,
						"Synchronization with "
								+ SyncManager.getInstance().getCurrentService().getDescription()
								+ " is complete.", Toast.LENGTH_SHORT).show();
				break;

			case SyncService.PARSING_NO_NOTES:
				// TODO put string in a translatable bundle
				Toast.makeText(
						activity,
						"No notes found on "
								+ SyncManager.getInstance().getCurrentService().getDescription()
								+ ".", Toast.LENGTH_SHORT).show();
				break;

			case SyncService.PARSING_FAILED:
				if (Tomdroid.LOGGING_ENABLED)
					Log.w(TAG, "handler called with a parsing failed message");

				// if we already shown a parsing error in this pass, we
				// won't show it again
				if (!parsingErrorShown) {
					parsingErrorShown = true;

					// TODO put error string in a translatable resource
					new AlertDialog.Builder(activity).setMessage(
							"There was an error trying to parse your note collection. If "
									+ "you are able to replicate the problem, please contact us!")
							.setTitle("Error").setNeutralButton("Ok", new OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							}).show();
				}
				break;

			case SyncService.NO_INTERNET:
				// TODO put string in a translatable bundle
				Toast.makeText(activity, "You are not connected to the internet.",
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
		ImageView syncButton = (ImageView) activity.findViewById(R.id.sync);

		RotateAnimation rotation = new RotateAnimation(180 * msg.arg2 / 100f,
				180 * msg.arg1 / 100f, Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
		rotation.setDuration(700);
		rotation.setFillAfter(true);
		syncButton.startAnimation(rotation);

		if (msg.arg1 == 0) {
			onSynchronizationStarted();
		} else if (msg.arg1 == 100) {
			onSynchronizationDone();
		}
	}

	private void onSynchronizationDone() {
		ImageView syncButton = (ImageView) activity.findViewById(R.id.sync);

		syncButton.setClickable(true);
		syncButton.getDrawable().setAlpha(Actionbar.DEFAULT_ICON_ALPHA);

		View dot = activity.findViewById(R.id.sync_dot);
		dot.setVisibility(View.INVISIBLE);
		dot.getAnimation().setRepeatCount(0);
	}

	private void onSynchronizationStarted() {
		ImageView syncButton = (ImageView) activity.findViewById(R.id.sync);

		syncButton.setClickable(false);
		syncButton.getDrawable().setAlpha(40);
		
		Animation pulse = AnimationUtils.loadAnimation(activity, R.anim.pulse);
		View dot = activity.findViewById(R.id.sync_dot);
		dot.setVisibility(View.VISIBLE);
		dot.startAnimation(pulse);
	}

}
