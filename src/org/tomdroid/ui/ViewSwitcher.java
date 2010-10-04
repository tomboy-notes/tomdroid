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

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;

/**
 * This code was extracted from the Transition3D sample activity found in the Android ApiDemos. The
 * animation is made of two smaller animations: the first half rotates the list by 90 degrees on the
 * Y axis and the second half rotates the picture by 90 degrees on the Y axis. When the first half
 * finishes, the list is made invisible and the picture is set visible.
 */
public class ViewSwitcher {
	private static final String	TAG					= "ViewSwitcher";
	private ViewGroup			mContainer;
	private View				mFrondside;
	private View				mBackside;

	private long				mDuration			= 300;
	private float				mDepthOfRotation	= 300f;

	public ViewSwitcher(ViewGroup container) {

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

	public void swap() {
		float start, end;

		if (isFrontsideVisible()) {
			Log.v(TAG, "turning to the backside!");
			start = 0;
			end = 90;
		} else {
			Log.v(TAG, "turning to the frontside!");
			start = 180;
			end = 90;
		}

		Rotate3dAnimation rotation = new Rotate3dAnimation(start, end,
				mContainer.getWidth() / 2.0f, mContainer.getHeight() / 2.0f, mDepthOfRotation, true);
		rotation.setDuration(mDuration / 2);
		rotation.setFillAfter(true);
		rotation.setInterpolator(new AccelerateInterpolator());
		rotation.setAnimationListener(new TurnAroundListener());

		mContainer.startAnimation(rotation);
	}

	public boolean isFrontsideVisible() {
		return mFrondside.getVisibility() == View.VISIBLE;
	}

	public boolean isBacksideVisible() {
		return mBackside.getVisibility() == View.VISIBLE;
	}

	/**
	 * Listen for the end of the first half of the animation. Then post a new action that
	 * effectively swaps the views when the container is rotated 90 degrees and thus invisible.
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
	 * Swapping the views and start the second half of the animation.
	 */
	private final class SwapViews implements Runnable {

		public void run() {
			final float centerX = mContainer.getWidth() / 2.0f;
			final float centerY = mContainer.getHeight() / 2.0f;
			Rotate3dAnimation rotation;

			if (isFrontsideVisible()) {
				mFrondside.setVisibility(View.GONE);
				mBackside.setVisibility(View.VISIBLE);
				unmirrorTheBackside();
				mBackside.requestFocus();

				rotation = new Rotate3dAnimation(90, 180, centerX, centerY, mDepthOfRotation, false);
			} else {
				mBackside.setVisibility(View.GONE);
				mBackside.clearAnimation(); // remove the mirroring
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

	private void unmirrorTheBackside() {
		Rotate3dAnimation rotation = new Rotate3dAnimation(0, 180, mContainer.getWidth() / 2.0f,
				mContainer.getHeight() / 2.0f, mDepthOfRotation, false);
		rotation.setDuration(0);
		rotation.setFillAfter(true);
		mBackside.startAnimation(rotation);
	}
}
