package org.tomdroid.ui;

import org.tomdroid.R;
import org.tomdroid.sync.SyncManager;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class Actionbar extends RelativeLayout {

	public static final int	DEFAULT_ICON_ALPHA	=200;

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
		final ImageView syncButton = (ImageView) findViewById(R.id.sync);
		syncButton.getDrawable().setAlpha(Actionbar.DEFAULT_ICON_ALPHA);
		syncButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				SyncManager.getInstance().startSynchronization();
			}
		});
		syncButton.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus)
					findViewById(R.id.sync_dot).setVisibility(VISIBLE);
				else 
					findViewById(R.id.sync_dot).setVisibility(INVISIBLE);
				 
			}
		});
	}
	
}
