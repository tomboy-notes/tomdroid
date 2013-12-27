package org.tomdroid.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;

public class NavButtonAdapter extends BaseAdapter {
    private Context mContext;
	private String[] directories;
    
    public NavButtonAdapter(Context c, String[] directories) {
    	this.directories = directories;
        mContext = c;
    }

    public int getCount() {
        return directories.length;
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new Button for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        Button navButton;
        if (convertView == null) {  // if it's not recycled, initialize some attributes
            navButton = new Button(mContext);
            navButton.setLayoutParams(new GridView.LayoutParams(85, 85));
        } else {
            navButton = (Button) convertView;
        }

        navButton.setText(directories[position]);
        return navButton;
    }
}