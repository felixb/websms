package de.ub0r.android.websms.rules;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import de.ub0r.android.websms.R;
import de.ub0r.android.websms.WebSMS;

/**
 * Preferences for the pseudo-connector Rules.
 */
public class PreferencesRulesActivity extends PreferenceActivity {

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("deprecation")
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.addPreferencesFromResource(R.xml.prefs_rules);
        this.setTitle(R.string.settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean onOptionsItemSelected(final android.view.MenuItem item) {
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
