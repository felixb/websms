/**
 * 
 */
package de.ub0r.android.websms;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import de.ub0r.android.websms.connector.common.Connector;
import de.ub0r.android.websms.connector.common.ConnectorSpec;

/**
 * @author flx
 */
public class CaptchaActivity extends Activity implements OnClickListener {
	/** Tag for output. */
	private static final String TAG = "WebSMS.cpt";

	/** Connector which sent the request. */
	private ConnectorSpec connector = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
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
		this.setTitle(this.connector.getName() + " - "
				+ this.getString(R.string.captcha_));
		this.setContentView(R.layout.captcha);
		final Parcelable p = extras
				.getParcelable(Connector.EXTRA_CAPTCHA_DRAWABLE);
		if (p != null && p instanceof Bitmap) {
			((ImageView) this.findViewById(R.id.captcha))
					.setImageBitmap((Bitmap) p);
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
	public final void onClick(final View v) {
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
	protected final void onDestroy() {
		super.onDestroy();
		final Intent intent = new Intent(this.connector.getPackage()
				+ Connector.ACTION_CAPTCHA_SOLVED);
		final String s = ((EditText) this.findViewById(R.id.solved)).getText()
				.toString();
		if (s.length() > 0) {
			Log.d(TAG, "solved: " + s);
			intent.putExtra(Connector.EXTRA_CAPTCHA_SOLVED, s);
		}
		Log.d(TAG, "send broadcast: " + intent.getAction());
		final List<ResolveInfo> ri = this.getPackageManager()
				.queryBroadcastReceivers(intent, 0);
	}
}
