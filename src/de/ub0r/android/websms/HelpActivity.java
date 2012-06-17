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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

import de.ub0r.android.lib.Market;
import de.ub0r.android.lib.Utils;

/**
 * {@link SherlockActivity} showing intro.
 * 
 * @author flx
 */
public final class HelpActivity extends SherlockActivity implements
		OnClickListener {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.help);
		Utils.fixActionBarBackground(this.getSupportActionBar(),
				this.getResources(), R.drawable.bg_striped,
				R.drawable.bg_striped_img);
		this.setTitle(R.string.help_title);

		this.findViewById(R.id.ok).setOnClickListener(this);
		this.findViewById(R.id.connectors).setOnClickListener(this);

		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (TextUtils.isEmpty(p.getString(WebSMS.PREFS_SENDER, null))
				|| TextUtils.isEmpty(p.getString(WebSMS.PREFS_DEFPREFIX, null))) {
			this.findViewById(R.id.help_prefs).setVisibility(View.VISIBLE);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			this.finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(final View v) {
		switch (v.getId()) {
		case R.id.ok:
			this.finish();
			return;
		case R.id.connectors:
			Market.searchApp(this, "websms+connector",
					"http://code.google.com/p/websmsdroid/downloads"
							+ "/list?can=2&q=label%3AConnector");
			return;
		default:
			return;
		}
	}
}
