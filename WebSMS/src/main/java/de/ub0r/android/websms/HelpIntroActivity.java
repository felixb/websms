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

import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * Activity showing intro.
 *
 * @author flx
 */
public final class HelpIntroActivity extends AppCompatActivity implements
		OnClickListener {
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.help_intro);
		this.setTitle(R.string.help_title);

		this.findViewById(R.id.ok).setOnClickListener(this);
		View v = this.findViewById(R.id.connectors);
		if (v != null) {
			v.setOnClickListener(this);
		}
		v = this.findViewById(R.id.connectors_de);
		if (v != null) {
			v.setOnClickListener(this);
		}

		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (TextUtils.isEmpty(p.getString(WebSMS.PREFS_SENDER, null))
				|| TextUtils.isEmpty(p.getString(WebSMS.PREFS_DEFPREFIX, null))) {
			this.findViewById(R.id.help_prefs).setVisibility(View.VISIBLE);
		}
	}

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

	@Override
	public void onClick(final View v) {
		switch (v.getId()) {
		case R.id.ok:
			this.finish();
			return;
		case R.id.connectors:
			startActivity(new Intent(Intent.ACTION_VIEW,
					Uri.parse("https://play.google.com/store/search?q=websms+connector")));
			return;
		case R.id.connectors_de:
			Builder b = new Builder(this);
			b.setItems(R.array.get_connectors_items,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog,
								final int which) {
							switch (which) {
							case 0:
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                                        "https://play.google.com/store/apps/details?id=de.ub0r.android.websms.connector.smsflatratenet")));
                                break;
							case 1:
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                                        "https://play.google.com/store/search?q=websms+connector")));
                                break;
							default:
								throw new IllegalStateException(
										"invalid option selected: " + which);
							}
						}
					});
			b.show();
			return;
		default:
			return;
		}
	}
}
