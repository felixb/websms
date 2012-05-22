/*
 * Copyright (C) 2009-2012 Felix Bechstein
 * 
 * This file is part of WebSMS.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.android.websms;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

import de.ub0r.android.lib.Utils;
import de.ub0r.android.websms.connector.common.ConnectorSpec;

/**
 * Display About Activity.
 * 
 * @author flx
 */
public final class AboutActivity extends SherlockActivity {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.about);
		this.getSupportActionBar().setHomeButtonEnabled(true);
		Utils.fixActionBarBackground(this.getSupportActionBar(),
				this.getResources(), R.drawable.bg_striped,
				R.drawable.bg_striped_img);
		this.setTitle(this.getString(R.string.about_) + " "
				+ this.getString(R.string.app_name) + " v"
				+ this.getString(R.string.app_version));
		StringBuffer authors = new StringBuffer();
		final ConnectorSpec[] css = WebSMS.getConnectors(
				ConnectorSpec.CAPABILITIES_NONE, ConnectorSpec.STATUS_INACTIVE);
		String a;
		for (ConnectorSpec cs : css) {
			a = cs.getAuthor();
			if (a != null && a.length() > 0) {
				authors.append(cs.getName());
				authors.append(":\t");
				authors.append(a);
				authors.append("\n");
			}
		}
		a = null;
		((TextView) this.findViewById(R.id.author_connectors)).setText(authors
				.toString().trim());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// app icon in Action Bar clicked; go home
			Intent intent = new Intent(this, WebSMS.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			this.startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
