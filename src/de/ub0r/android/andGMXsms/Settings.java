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

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.settings);

		this.me = this;

		EditText et = (EditText) this.findViewById(R.id.user);
		et.setText(AndGMXsms.prefs_user);
		et = (EditText) this.findViewById(R.id.password);
		et.setText(AndGMXsms.prefs_password);
		et = (EditText) this.findViewById(R.id.sender);
		et.setText(AndGMXsms.prefs_sender);

		Button button = (Button) this.findViewById(R.id.help);
		button.setOnClickListener(this.help);
	}

	@Override
	public void onPause() {
		super.onPause();

		EditText et = (EditText) this.findViewById(R.id.user);
		AndGMXsms.prefs_user = et.getText().toString();
		et = (EditText) this.findViewById(R.id.password);
		AndGMXsms.prefs_password = et.getText().toString();
		et = (EditText) this.findViewById(R.id.sender);
		AndGMXsms.prefs_sender = et.getText().toString();
	}

	// Create an anonymous implementation of OnClickListener
	private OnClickListener help = new OnClickListener() {
		public void onClick(final View v) {
			Settings.this.me.startActivity(new Intent(Settings.this.me,
					Help.class));
		}
	};
}
