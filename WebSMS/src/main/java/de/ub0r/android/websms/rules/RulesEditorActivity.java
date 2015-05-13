package de.ub0r.android.websms.rules;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import java.util.List;

import de.ub0r.android.websms.R;

/**
 * Rules Editor activity.
 */
public class RulesEditorActivity extends SherlockFragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle(R.string.rules_editor_title);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, new RulesEditorFragment())
                .commit();
        }
    }

    @Override
    public void onBackPressed() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof RulesEditorFragment) {
                ((RulesEditorFragment)fragment).onDone();
            }
        }
        super.onBackPressed();
    }

}
