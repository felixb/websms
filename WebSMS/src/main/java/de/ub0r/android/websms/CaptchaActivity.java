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
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.Log;

/**
 * @author flx
 */
public final class CaptchaActivity extends SherlockActivity implements
		OnClickListener {
	/** Tag for output. */
	private static final String TAG = "cpt";

	/** Connector which sent the request. */
	private ConnectorSpec connector = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		this.setTheme(PreferencesActivity.getTheme(this));
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate();");
		final Bundle extras = this.getIntent().getExtras();
		if (extras == null) {
			this.finish();
			return;
		}
		this.connector = new ConnectorSpec(this.getIntent());
		if (this.connector == null) {
			this.finish();
			return;
		}
		this.setContentView(R.layout.captcha);
		this.setTitle(this.connector.getName() + " - "
				+ this.getString(R.string.captcha_));
		this.getSupportActionBar().setHomeButtonEnabled(true);
		WebSMSApp.fixActionBarBackground(this.getSupportActionBar(),
				this.getResources(), R.drawable.bg_striped,
				R.drawable.bg_striped_img);

		final Parcelable p = extras.getParcelable(Connector.EXTRA_CAPTCHA_DRAWABLE);
		if (p != null && p instanceof Bitmap) {
            final ImageView ivCaptcha = (ImageView) this.findViewById(R.id.captcha);
            final ImageView ivCaptchaFull = (ImageView) this.findViewById(R.id.captcha_full);
            ivCaptcha.setImageBitmap((Bitmap) p);

            ivCaptcha.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    ivCaptcha.setVisibility(View.GONE);
                    ivCaptchaFull.setVisibility(View.VISIBLE);

                    if (ivCaptchaFull.getDrawable() == null) {
                        ivCaptchaFull.setImageBitmap((Bitmap) p);
                    }
                }
            });
            ivCaptchaFull.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    ivCaptcha.setVisibility(View.VISIBLE);
                    ivCaptchaFull.setVisibility(View.GONE);
                }
            });

		} else {
			this.finish();
			return;
		}

		final String t = extras.getString(Connector.EXTRA_CAPTCHA_MESSAGE);
		if (t != null) {
			((TextView) this.findViewById(R.id.text)).setText(t);
		}
		this.findViewById(R.id.ok).setOnClickListener(this);
		this.findViewById(R.id.cancel).setOnClickListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	public void onClick(final View v) {
		switch (v.getId()) {
		case R.id.cancel:
			((EditText) this.findViewById(R.id.solved)).setText("");
		default:
			break;
		}
		this.finish();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		final Intent intent = new Intent(this.connector.getPackage()
				+ Connector.ACTION_CAPTCHA_SOLVED);
		final String s = ((EditText) this.findViewById(R.id.solved)).getText()
				.toString();
		if (s.length() > 0) {
			Log.d(TAG, "solved: " + s);
			intent.putExtra(Connector.EXTRA_CAPTCHA_SOLVED, s);
		}
		intent.setFlags(intent.getFlags()
				| Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
		Log.d(TAG, "send broadcast: " + intent.getAction());
		this.sendBroadcast(intent);
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
