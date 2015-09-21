package de.ub0r.android.websms.rules;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.inputmethod.InputMethodManager;


import java.util.List;

import de.ub0r.android.websms.R;

/**
 * Activity for editing new or existing rules.
 */
public class EditRuleActivity extends AppCompatActivity {

    private static final String INTENT_RULE_IDX = "rule_idx";
    private static final String INTENT_RULE     = "rule";


    public static Intent createStartIntent(Context appCtx, int ruleIdx, Rule rule) {
        Intent intent = new Intent(appCtx, EditRuleActivity.class);
        intent.putExtra(INTENT_RULE_IDX, ruleIdx);
        intent.putExtra(INTENT_RULE, rule);
        return intent;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int ruleIdx = getIntent().getIntExtra(INTENT_RULE_IDX, -1);
        getSupportActionBar().setTitle(ruleIdx == -1 ? R.string.new_rule_title : R.string.edit_rule_title);

        if (savedInstanceState == null) {
            Rule rule = (Rule) getIntent().getSerializableExtra(INTENT_RULE);

            EditRuleFragment frag = EditRuleFragment.newInstance(ruleIdx, rule);

            getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, frag)
                .commit();
        }
    }

    @Override
    public void onBackPressed() {
        boolean canLeave = true;
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof EditRuleFragment) {
                canLeave &= ((EditRuleFragment)fragment).onDone();
            }
        }
        if (canLeave) {
            super.onBackPressed();
        }
    }

    @Override
    public void finish() {
        hideSoftKeyboard();
        super.finish();
    }

    private void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    @SuppressWarnings("unchecked")
    public static IndexedRule getResult(Intent result) {
        int ruleIdx = result.getIntExtra(INTENT_RULE_IDX, -1);
        Rule rule = (Rule) result.getSerializableExtra(INTENT_RULE);
        return new IndexedRule(ruleIdx, rule);
    }

    public void setResult(int ruleIdx, Rule rule) {
        Intent data = new Intent();
        data.putExtra(INTENT_RULE_IDX, ruleIdx);
        data.putExtra(INTENT_RULE, rule);
        setResult(Activity.RESULT_OK, data);
    }

}
