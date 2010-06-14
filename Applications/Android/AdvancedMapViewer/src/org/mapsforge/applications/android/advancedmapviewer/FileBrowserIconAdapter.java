/*
 * Copyright 2010 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.applications.android.advancedmapviewer;

import java.io.File;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.TextView;

class FileBrowserIconAdapter extends BaseAdapter {
	private Context context;
	private File currentFile;
	private File[] files;
	private boolean hasParentFolder;
	private LayoutParams layoutParams;
	private TextView textView;

	FileBrowserIconAdapter(Context context) {
		this.context = context;
		this.layoutParams = new AbsListView.LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT);
	}

	@Override
	public int getCount() {
		if (this.files == null) {
			return 0;
		}
		return this.files.length;
	}

	@Override
	public Object getItem(int index) {
		return this.files[index];
	}

	@Override
	public long getItemId(int index) {
		return index;
	}

	@Override
	public View getView(int index, View convertView, ViewGroup parent) {
		if (convertView instanceof TextView) {
			// recycle the old view
			this.textView = (TextView) convertView;
		} else {
			// create a new view object
			this.textView = new TextView(this.context);
			this.textView.setEllipsize(TextUtils.TruncateAt.END);
			this.textView.setSingleLine();
			this.textView.setLayoutParams(this.layoutParams);
			this.textView.setGravity(Gravity.CENTER_HORIZONTAL);
			this.textView.setPadding(5, 5, 5, 5);
		}

		if (index == 0 && this.hasParentFolder) {
			// the parent directory of the current folder
			this.textView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_menu_back,
					0, 0);
			this.textView.setText("..");
		} else {
			this.currentFile = this.files[index];
			if (this.currentFile.isDirectory()) {
				this.textView.setCompoundDrawablesWithIntrinsicBounds(0,
						R.drawable.ic_menu_archive, 0, 0);
			} else {
				this.textView.setCompoundDrawablesWithIntrinsicBounds(0,
						R.drawable.ic_menu_mapmode, 0, 0);
			}
			this.textView.setText(this.currentFile.getName());
		}
		return this.textView;
	}

	void updateFiles(File[] newFiles, boolean newHasParentFolder) {
		this.files = newFiles;
		this.hasParentFolder = newHasParentFolder;
	}
}