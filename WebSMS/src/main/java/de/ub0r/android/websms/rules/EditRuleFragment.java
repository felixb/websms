package de.ub0r.android.websms.rules;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.ub0r.android.websms.ConnectorLabel;
import de.ub0r.android.websms.HelpHtmlActivity;
import de.ub0r.android.websms.R;
import de.ub0r.android.websms.WebSMS;

/**
 * Fragment for editing new or existing rules.
 */
public class EditRuleFragment
        extends SherlockDialogFragment
        implements ConfirmDialogFragment.DialogListener {

    private static final String ARG_RULE_IDX = "rule_idx";
    private static final String ARG_RULE     = "rule";

    private static final String SAVE_WAS_CHANGED      = "EditRuleFragment$wasChanged";
    private static final String SAVE_PREV_SPINNER_POS = "EditRuleFragment$prevSpinnerPosition";

    private ConnectorLabel[] connectorLabels;

    private RadioButton rbNumberPrefix;
    private EditText etNumberPrefix;
    private RadioButton rbNumberPattern;
    private EditText etNumberPattern;
    private RadioButton rbNumberAny;
    private RadioButton rbCharsetAny;
    private RadioButton rbCharsetAscii;
    private Spinner spinner;

    private boolean wasChanged = false;
    private int prevSpinnerPosition = 0;


    public static EditRuleFragment newInstance(int ruleIdx, Rule rule) {
        EditRuleFragment fragment = new EditRuleFragment();

        Bundle args = new Bundle();
        args.putInt(ARG_RULE_IDX, ruleIdx);
        args.putSerializable(ARG_RULE, rule);
        fragment.setArguments(args);

        return fragment;
    }

    private int getRuleIdx() {
        return getArguments().getInt(ARG_RULE_IDX);
    }

    private Rule getRule() {
        return (Rule) getArguments().getSerializable(ARG_RULE);
    }


    @Override
    @SuppressWarnings("unchecked")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        this.connectorLabels = WebSMS.getConnectorMenuItems(false /*isIncludePseudoConnectors*/);

        if (savedInstanceState != null) {
            this.wasChanged = savedInstanceState.getBoolean(SAVE_WAS_CHANGED);
            this.prevSpinnerPosition = savedInstanceState.getInt(SAVE_PREV_SPINNER_POS);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean(SAVE_WAS_CHANGED, this.wasChanged);
        bundle.putInt(SAVE_PREV_SPINNER_POS, this.prevSpinnerPosition);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.edit_rule, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.rbNumberPrefix = (RadioButton) view.findViewById(R.id.radio_number_prefix);
        this.etNumberPrefix = (EditText) view.findViewById(R.id.prefix);
        this.rbNumberPattern = (RadioButton) view.findViewById(R.id.radio_number_pattern);
        this.etNumberPattern = (EditText) view.findViewById(R.id.pattern);
        this.rbNumberAny = (RadioButton) view.findViewById(R.id.radio_number_any);
        this.rbCharsetAny = (RadioButton) view.findViewById(R.id.radio_charset_any);
        this.rbCharsetAscii = (RadioButton) view.findViewById(R.id.radio_charset_ascii);
        this.spinner = (Spinner) view.findViewById(R.id.connector_spinner);

        this.rbNumberPrefix.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    etNumberPrefix.setEnabled(true);
                    etNumberPattern.setEnabled(false);
                    etNumberPrefix.requestFocus();
                }
                onChanged();
            }
        });

        this.etNumberPrefix.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                onChanged();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void afterTextChanged(Editable s) {
            }
        });

        this.rbNumberPattern.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    etNumberPrefix.setEnabled(false);
                    etNumberPattern.setEnabled(true);
                    etNumberPattern.requestFocus();
                }
                onChanged();
            }
        });

        this.etNumberPattern.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                onChanged();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void afterTextChanged(Editable s) {
            }
        });

        this.rbNumberAny.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    etNumberPrefix.setEnabled(false);
                    etNumberPattern.setEnabled(false);
                }
                onChanged();
            }
        });

        this.rbCharsetAny.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onChanged();
            }
        });
        this.rbCharsetAscii.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onChanged();
            }
        });

        ArrayAdapter<ConnectorLabel> adapter = new ArrayAdapter<ConnectorLabel>(
                getActivity().getApplicationContext(),
                android.R.layout.simple_spinner_item,
                connectorLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.spinner.setAdapter(adapter);

        this.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != prevSpinnerPosition) {
                    prevSpinnerPosition = position;
                    onChanged();
                }
            }
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // populate view from the rule if available
        Rule rule = getRule();
        if (rule != null) {
            if (rule.getPattern() != null) {
                this.rbNumberPattern.setChecked(true);
                this.etNumberPattern.setText(rule.getPattern().pattern());
            } else if (rule.getPrefix() != null) {
                this.rbNumberPrefix.setChecked(true);
                this.etNumberPrefix.setText(rule.getPrefix());
            } else {
                this.rbNumberAny.setChecked(true);
            }
            if (rule.getCharsetName() == null) {
                this.rbCharsetAny.setChecked(true);
            } else {
                // only single US-ASCII charset is currently supported
                this.rbCharsetAscii.setChecked(true);
            }
            this.spinner.setSelection(findConnectorIdx(rule, this.connectorLabels));
        } else {
            this.rbNumberPrefix.setChecked(true);
            this.rbCharsetAny.setChecked(true);
        }
    }

    @Override
    public void onDestroyView() {
        this.rbNumberPrefix = null;
        this.etNumberPrefix = null;
        this.rbNumberPattern = null;
        this.etNumberPattern = null;
        this.rbNumberAny = null;
        this.rbCharsetAny = null;
        this.rbCharsetAscii = null;
        this.spinner.setAdapter(null);
        this.spinner = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.edit_rule_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cancel:
                // finish without saving the rule
                getActivity().finish();
                return true;
            case R.id.help:
                this.startActivity(
                    HelpHtmlActivity.createStartIntent(getActivity().getApplicationContext(),
                            getString(R.string.help_html_title,
                                    getSherlockActivity().getSupportActionBar().getTitle()),
                            getString(R.string.edit_rule_help)));
                return true;
        }
        return false;
    }

    @Override
    public void onConfirmOk(int dialogId) {
        // continue editing
    }

    @Override
    public void onConfirmCancelled(int dialogId) {
        // finish without saving
        getActivity().finish();
    }


    public boolean onDone() {
        if (!wasChanged) {
            // if the user just looked and went back
            //   then do not save the new/old rule and do not complain
            return true;
        }

        // populate rule from dialog fields (and validate)
        Rule newRule = new Rule();
        boolean isValid = true;

        if (this.rbNumberPattern.isChecked()) {
            String patternStr = this.etNumberPattern.getText().toString();
            if (TextUtils.isEmpty(patternStr)) {
                this.etNumberPattern.setError(getString(R.string.rule_invalid_field_empty));
                isValid = false;
            } else {
                try {
                    newRule.setPattern(Pattern.compile(patternStr));
                    newRule.setPrefix(null);
                } catch (PatternSyntaxException e) {
                    this.etNumberPattern.setError(getString(R.string.rule_invalid_field_regex));
                    isValid = false;
                }
            }

        } else if (this.rbNumberPrefix.isChecked()) {
            String prefix = this.etNumberPrefix.getText().toString();
            if (TextUtils.isEmpty(prefix)) {
                this.etNumberPrefix.setError(getString(R.string.rule_invalid_field_empty));
                isValid = false;
            } else {
                newRule.setPattern(null);
                newRule.setPrefix(prefix);
            }

        } else {
            newRule.setPattern(null);
            newRule.setPrefix(null);
        }

        // only single US-ASCII charset is currently supported
        if (this.rbCharsetAscii.isChecked()) {
            newRule.setCharsetName("US-ASCII");
        } else {
            newRule.setCharsetName(null);
        }

        ConnectorLabel connector = (ConnectorLabel) this.spinner.getSelectedItem();
        newRule.setConnectorId(connector.getConnector().getPackage());
        newRule.setSubConnectorId(connector.getSubConnector().getID());

        if (!isValid) {
            ConfirmDialogFragment dialogFragment = ConfirmDialogFragment.newInstance(0,
                    getString(R.string.rule_invalid),
                    android.R.string.ok, android.R.string.cancel,
                    ConfirmDialogFragment.ACTION_OK,
                    this);
            dialogFragment.show(getSherlockActivity().getSupportFragmentManager(),
                    ConfirmDialogFragment.FRAG_TAG);
            return false;
        } else {
            // return the new rule to the Rules Editor
            ((EditRuleActivity) getActivity()).setResult(getRuleIdx(), newRule);
            return true;
        }
    }

    private void onChanged() {
        if (isResumed()) {
            wasChanged = true;
            this.etNumberPrefix.setError(null);
            this.etNumberPattern.setError(null);
        }
    }

    private int findConnectorIdx(Rule rule, ConnectorLabel[] connectorLabels) {
        for (int idx = 0; idx < connectorLabels.length; idx++) {
            ConnectorLabel connLabel = connectorLabels[idx];
            if (connLabel.getConnector().getPackage().equals(rule.getConnectorId())
                    && connLabel.getSubConnector().getID().equals(rule.getSubConnectorId())) {
                return idx;
            }
        }
        return -1;
    }

}
