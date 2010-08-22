package org.tomdroid.ui;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

/**
 * This code was extracted from the Transition3D sample activity found in the Android ApiDemos. The
 * animation is made of two smaller animations: the first half rotates the list by 90 degrees on the
 * Y axis and the second half rotates the picture by 90 degrees on the Y axis. When the first half
 * finishes, the list is made invisible and the picture is set visible.
 */
public class Transition3d {
	private static final String	TAG					= "Transition3d";
	private ViewGroup			mContainer;
	private View				mFrondside;
	private View				mBackside;

	private long				mDuration			= 300;
	private float				mDepthOfRotation	= 300f;

	public Transition3d(ViewGroup container) {

		mContainer = container;
		mFrondside = container.getChildAt(0);
		mBackside = container.getChildAt(1);

		// Since we are caching large views, we want to keep their cache
		// between each animation
		mContainer.setPersistentDrawingCache(ViewGroup.PERSISTENT_ANIMATION_CACHE);
	}

	public void setDuration(long duration) {
		mDuration = duration;
	}

	/**
	 * Setup a new 3D rotation on the container view.
	 * 
	 * @param position
	 *            the item that was clicked to show a picture, or -1 to show the list
	 * @param start
	 *            the start angle at which the rotation must begin
	 * @param end
	 *            the end angle of the rotation
	 */
	private void turnAround(float start, float end) {
		// Find the center of the container
		final float centerX = mContainer.getWidth() / 2.0f;
		final float centerY = mContainer.getHeight() / 2.0f;

		// Create a new 3D rotation with the supplied parameter
		// The animation listener is used to trigger the next animation
		final Rotate3dAnimation rotation = new Rotate3dAnimation(start, end, centerX, centerY,
				mDepthOfRotation, true);
		rotation.setDuration(mDuration / 2);
		rotation.setFillAfter(true);
		rotation.setInterpolator(new AccelerateInterpolator());
		rotation.setAnimationListener(new TurnAroundListener());

		mContainer.startAnimation(rotation);
	}
	
	public void switchView() {
		if (isFrontsideVisible()) {
			Log.v(TAG, "turning to backside!");
			turnAround(0, 90);
		} else {
			Log.v(TAG, "turning to frontside!");
			turnAround(180, 90);
		}
	}

	public boolean isFrontsideVisible(){
		return mFrondside.getVisibility() == View.VISIBLE;
	}

	public boolean isBacksideVisible(){
		return mBackside.getVisibility() == View.VISIBLE;
	}
	
	/**
	 * This class listens for the end of the first half of the animation. It then posts a new action
	 * that effectively swaps the views when the container is rotated 90 degrees and thus invisible.
	 */
	private final class TurnAroundListener implements Animation.AnimationListener {

		public void onAnimationStart(Animation animation) {
		}

		public void onAnimationEnd(Animation animation) {
			mContainer.post(new SwapViews());
		}

		public void onAnimationRepeat(Animation animation) {
		}
	}

	/**
	 * This class is responsible for swapping the views and start the second half of the animation.
	 */
	private final class SwapViews implements Runnable {

		public void run() {
			final float centerX = mContainer.getWidth() / 2.0f;
			final float centerY = mContainer.getHeight() / 2.0f;
			Rotate3dAnimation rotation;

			if (isFrontsideVisible()) {
				mFrondside.setVisibility(View.GONE);
				mBackside.setVisibility(View.VISIBLE);
				mBackside.requestFocus();
				
				rotation = new Rotate3dAnimation(90, 180, centerX, centerY, mDepthOfRotation, false);
			} else {
				mBackside.setVisibility(View.GONE);
				mFrondside.setVisibility(View.VISIBLE);
				mFrondside.requestFocus();
				
				rotation = new Rotate3dAnimation(90, 0, centerX, centerY, mDepthOfRotation, false);
			}

			rotation.setDuration(mDuration / 2);
			rotation.setFillAfter(true);
			rotation.setInterpolator(new DecelerateInterpolator());

			mContainer.startAnimation(rotation);
		}
	}

}
