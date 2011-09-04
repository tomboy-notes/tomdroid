/*
 * Tomdroid
 * Tomboy on Android
 * http://www.launchpad.net/tomdroid
 * 
 * Copyright 2011 Stefan Hammer <j.4@gmx.at>
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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class Actionbar extends RelativeLayout {

	public static final int DEFAULT_ICON_ALPHA = 200;

	public Actionbar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public Actionbar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public Actionbar(Context context) {
		super(context);
	}

	@Override
	public void onFinishInflate(){
		super.onFinishInflate();
		setupButtons();
	}
	
	private void setupButtons(){
		
		final ImageView syncButton = (ImageView) findViewById(R.id.sync);
		final ImageView syncIcon = (ImageView) findViewById(R.id.syncIcon);
		syncIcon.getDrawable().setAlpha(Actionbar.DEFAULT_ICON_ALPHA);
		syncButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				SyncManager.getInstance().startSynchronization();
			}
		});
		
		final ImageView TomdroidIcon = (ImageView) findViewById(R.id.action_icon);
		TomdroidIcon.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				Tomdroid.ViewList(getContext());
			}
		});
	}
	
}
