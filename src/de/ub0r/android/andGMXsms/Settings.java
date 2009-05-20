package de.ub0r.android.andGMXsms;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class Settings extends Activity {
	Settings me;

	private static String pr_user;
	private static String pr_password;
	private static String pr_sender;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.settings);

		this.me = this;

		Button button = (Button) this.findViewById(R.id.help);
		button.setOnClickListener(this.help);
		button = (Button) this.findViewById(R.id.ok);
		button.setOnClickListener(this.ok);
		button = (Button) this.findViewById(R.id.cancel);
		button.setOnClickListener(this.cancel);
	}

	@Override
	public void onResume() {
		super.onResume();

		if (pr_user == null) {
			pr_user = AndGMXsms.prefs_user;
		}
		if (pr_password == null) {
			pr_password = AndGMXsms.prefs_password;
		}
		if (pr_sender == null) {
			pr_sender = AndGMXsms.prefs_sender;
		}

		EditText et = (EditText) this.findViewById(R.id.user);
		et.setText(pr_user);
		et = (EditText) this.findViewById(R.id.password);
		et.setText(pr_password);
		et = (EditText) this.findViewById(R.id.sender);
		et.setText(pr_sender);
	}

	@Override
	public void onPause() {
		super.onPause();

		EditText et = (EditText) this.findViewById(R.id.user);
		pr_user = et.getText().toString();
		et = (EditText) this.findViewById(R.id.password);
		pr_password = et.getText().toString();
		et = (EditText) this.findViewById(R.id.sender);
		pr_sender = et.getText().toString();
	}

	// Create an anonymous implementation of OnClickListener
	private OnClickListener help = new OnClickListener() {
		public void onClick(final View v) {
			Settings.this.me.startActivity(new Intent(Settings.this.me,
					Help.class));
		}
	};

	// Create an anonymous implementation of OnClickListener
	private OnClickListener ok = new OnClickListener() {
		public void onClick(final View v) {
			EditText et = (EditText) Settings.this.findViewById(R.id.user);
			pr_user = et.getText().toString();
			et = (EditText) Settings.this.findViewById(R.id.password);
			pr_password = et.getText().toString();
			et = (EditText) Settings.this.findViewById(R.id.sender);
			pr_sender = et.getText().toString();

			AndGMXsms.prefs_user = pr_user;
			AndGMXsms.prefs_password = pr_password;
			AndGMXsms.prefs_sender = pr_sender;

			Settings.this.finish();
		}
	};

	// Create an anonymous implementation of OnClickListener
	private OnClickListener cancel = new OnClickListener() {
		public void onClick(final View v) {
			pr_user = AndGMXsms.prefs_user;
			pr_password = AndGMXsms.prefs_password;
			pr_sender = AndGMXsms.prefs_sender;

			EditText et = (EditText) Settings.this.findViewById(R.id.user);
			et.setText(pr_user);
			et = (EditText) Settings.this.findViewById(R.id.password);
			et.setText(pr_password);
			et = (EditText) Settings.this.findViewById(R.id.sender);
			et.setText(pr_sender);
			Settings.this.finish();
		}
	};
}
