package org.tomdroid.ui;

import org.tomdroid.R;
import org.tomdroid.sync.SyncManager;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

public class Actionbar extends RelativeLayout {

	public Actionbar(Context context
			, AttributeSet attrs) {
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
		setupSyncButton();
	}
	
	private void setupSyncButton(){
		final View syncButton = findViewById(R.id.sync);
		syncButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				SyncManager.getInstance().sync();
			}
		});
	}
	
}
