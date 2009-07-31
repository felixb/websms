package de.ub0r.android.andGMXsms;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Preferences.
 * 
 * @author flx
 */
public class Preferences extends PreferenceActivity {
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndGMXsms.doPreferences = true;
		this.addPreferencesFromResource(R.xml.prefs);
	}
}
