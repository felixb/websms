/*
 * Copyright (C) 2010 Felix Bechstein
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

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import de.ub0r.android.websms.connector.common.Utils;

/**
 * Display send IMEI hash, read signature..
 * 
 * @author flx
 */
public class DonationHelper extends Activity {
	/** Tag for output. */
	private static final String TAG = "WebSMS.dh";

	/** Crypto algorithm for signing UID hashs. */
	private static final String ALGO = "RSA";
	/** Crypto hash algorithm for signing UID hashs. */
	private static final String SIGALGO = "SHA1with" + ALGO;
	/** My public key for verifying UID hashs. */
	private static final String KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNAD"
			+ "CBiQKBgQCgnfT4bRMLOv3rV8tpjcEqsNmC1OJaaEYRaTHOCC"
			+ "F4sCIZ3pEfDcNmrZZQc9Y0im351ekKOzUzlLLoG09bsaOeMd"
			+ "Y89+o2O0mW9NnBch3l8K/uJ3FRn+8Li75SqoTqFj3yCrd9IT"
			+ "sOJC7PxcR5TvNpeXsogcyxxo3fMdJdjkafYwIDAQAB";

	/** Hashed IMEI. */
	private static String imeiHash = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// this.setContentView(R.layout.about);

		final Intent i = this.getIntent();
		final Uri u = i.getData();
		final String p = u.getPath();

		if (p == null || p.length() == 0) {
			// send imei hash via mail
			sendImeiHash(this);
		} else {
			// check signature encoded in path
			final boolean ret = DonationHelper.checkSig(this, p);
			Log.d(TAG, "put: " + ret);
			final SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(this);
			prefs.edit().putBoolean(WebSMS.PREFS_HIDEADS, ret).commit();
			// notify user
			int text = R.string.sig_loaded;
			if (!ret) {
				text = R.string.sig_failed;
			}
			Toast.makeText(this, text, Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Send a mail with user's IMEI hash.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	public static void sendImeiHash(final Context context) {
		final Intent in = new Intent(Intent.ACTION_SEND);
		in.putExtra(Intent.EXTRA_EMAIL, new String[] {
				context.getString(R.string.donate_mail), "" });
		// FIXME: "" is a k9 hack. This is fixed in market
		// on 26.01.10. wait some more time..
		final StringBuilder buf = new StringBuilder();
		buf.append(context.getString(R.string.app_name).split(" ", 2)[0]
				.toLowerCase());
		buf.append(':');
		in.putExtra(Intent.EXTRA_TEXT, getImeiHash(context));
		buf.append(':');
		buf.append(context.getString(R.string.lang));
		in.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name)
				+ " " + context.getString(R.string.donate_subject));
		in.setType("text/plain");
		context.startActivity(in);
	}

	/**
	 * Get MD5 hash of the IMEI (device id).
	 * 
	 * @param context
	 *            {@link Context}
	 * @return MD5 hash of IMEI
	 */
	public static String getImeiHash(final Context context) {
		if (imeiHash == null) {
			// get imei
			TelephonyManager mTelephonyMgr = (TelephonyManager) context
					.getSystemService(TELEPHONY_SERVICE);
			final String did = mTelephonyMgr.getDeviceId();
			if (did != null) {
				imeiHash = Utils.md5(did);
			}
		}
		return imeiHash;
	}

	/**
	 * Check for signature updates.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param s
	 *            signature
	 * @return true if ads should be hidden
	 */
	public static boolean checkSig(final Context context, final String s) {
		Log.d(TAG, "checkSig(ctx, " + s + ")");
		boolean ret = false;
		try {
			final byte[] publicKey = Base64Coder.decode(KEY);
			final KeyFactory keyFactory = KeyFactory.getInstance(ALGO);
			PublicKey pk = keyFactory.generatePublic(new X509EncodedKeySpec(
					publicKey));
			final String h = getImeiHash(context);
			Log.d(TAG, "hash: " + h);
			Log.d(TAG, "read sig: " + s);
			try {
				byte[] signature = Base64Coder.decode(s);
				Signature sig = Signature.getInstance(SIGALGO);
				sig.initVerify(pk);
				sig.update(h.getBytes());
				ret = sig.verify(signature);
				Log.d(TAG, "ret: " + ret);
			} catch (IllegalArgumentException e) {
				Log.w(TAG, "error reading signature", e);
			}
		} catch (Exception e) {
			Log.e(TAG, "error reading signatures", e);
		}
		return ret;
	}
}
