package de.ub0r.android.websms.rules;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import de.ub0r.android.websms.ConnectorLabel;
import de.ub0r.android.websms.HelpHtmlActivity;
import de.ub0r.android.websms.R;
import de.ub0r.android.websms.WebSMS;

/**
 * Rules Editor fragment.
 */
public class RulesEditorFragment extends SherlockListFragment {

    private static final String SAVE_RULES                = "RulesEditorFragment$rules";
    private static final String SAVE_ACTION_MODE_RULE_IDX = "RulesEditorFragment$actionModeRuleIdx";

    private static final int REQ_CODE_NEW  = 1001;
    private static final int REQ_CODE_EDIT = 1002;

    private ConnectorLabel[] connectorLabels;
    private List<Rule> rules;

    private ActionMode actionMode;
    private int actionModeRuleIdx = -1;

    @Override
    @SuppressWarnings("unchecked")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        this.connectorLabels = WebSMS.getConnectorMenuItems(false /*isIncludePseudoConnectors*/);

        if (savedInstanceState == null) {
            // clone current rules, so that the user could undo the changes
            List<Rule> currentRules = PseudoConnectorRules.getRules(getActivity().getApplicationContext());
            this.rules = new ArrayList<Rule>(currentRules.size());
            for (Rule rule : currentRules) {
                this.rules.add(rule.clone());
            }
            filterValidRules();
        } else {
            this.rules = (List<Rule>) savedInstanceState.getSerializable(SAVE_RULES);
            this.actionModeRuleIdx = savedInstanceState.getInt(SAVE_ACTION_MODE_RULE_IDX);

            if (this.actionModeRuleIdx != -1) {
                this.actionMode = getSherlockActivity().startActionMode(new ActionModeCallback());
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putSerializable(SAVE_RULES, new ArrayList<Rule>(this.rules));
        bundle.putInt(SAVE_ACTION_MODE_RULE_IDX, this.actionModeRuleIdx);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.rules_editor, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setListAdapter(new RulesListAdapter());

        ListView listView = getListView();
        listView.setItemsCanFocus(false);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (actionMode != null) {
                    return false;
                }
                actionModeRuleIdx = position;
                actionMode = getSherlockActivity().startActionMode(new ActionModeCallback());
                getListAdapter().notifyDataSetChanged();    // to update selection
                return true;
            }
        });

        // NOTE: listView.setChoiceMode(CHOICE_MODE_SINGLE) does not quite work
        //       because in this mode listView changes the checked state on each single click
        //       while I need the checked state to reflect the current item for the action mode
        //       so I manage this in RulesListAdapter.getView() instead
    }

    @Override
    public void onDestroyView() {
        setListAdapter(null);
        super.onDestroyView();
    }

    @Override
    public RulesListAdapter getListAdapter() {
        return (RulesListAdapter) super.getListAdapter();
    }

    public void onDone() {
        PseudoConnectorRules.saveRules(getActivity().getApplicationContext(), this.rules);
    }

    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.rules_editor_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_rule:
                startActivityForResult(
                        EditRuleActivity.createStartIntent(getActivity().getApplicationContext(),
                                -1, null),
                        REQ_CODE_NEW);
                return true;
            case R.id.cancel:
                // finish without saving the rules
                getActivity().finish();
                return true;
            case R.id.help:
                startActivity(
                        HelpHtmlActivity.createStartIntent(getActivity().getApplicationContext(),
                                getString(R.string.help_html_title,
                                        getSherlockActivity().getSupportActionBar().getTitle()),
                                getString(R.string.rules_editor_help)));
                return true;
        }
        return false;
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        super.onListItemClick(list, view, position, id);
        if (actionMode != null) {
            return;
        }
        startActivityForResult(
                EditRuleActivity.createStartIntent(getActivity().getApplicationContext(),
                        position, this.rules.get(position)),
                REQ_CODE_EDIT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQ_CODE_NEW) {
                IndexedRule newRule = EditRuleActivity.getResult(data);
                this.rules.add(newRule.getRule());
                getListAdapter().notifyDataSetChanged();

            } else if (requestCode == REQ_CODE_EDIT) {
                IndexedRule newRule = EditRuleActivity.getResult(data);
                this.rules.set(newRule.getIdx(), newRule.getRule());
                getListAdapter().notifyDataSetChanged();
            }
        }
    }


    private void filterValidRules() {
        List<Rule> filteredRules = new ArrayList<Rule>(this.rules.size());
        for (Rule rule : this.rules) {
            if (findConnectorIdx(rule) >= 0) {
                filteredRules.add(rule);
            }
        }
        this.rules = filteredRules;
    }

    private int findConnectorIdx(Rule rule) {
        for (int idx = 0; idx < this.connectorLabels.length; idx++) {
            ConnectorLabel connLabel = this.connectorLabels[idx];
            if (connLabel.getConnector().getPackage().equals(rule.getConnectorId())
                    && connLabel.getSubConnector().getID().equals(rule.getSubConnectorId())) {
                return idx;
            }
        }
        return -1;
    }


    private final class ActionModeCallback
            implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            getSherlockActivity().getSupportMenuInflater()
                .inflate(R.menu.rules_editor_context_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (mode == actionMode) {
                actionMode = null;
                actionModeRuleIdx = -1;
                getListAdapter().notifyDataSetChanged();    // to update selection
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.rule_delete) {
                rules.remove(actionModeRuleIdx);
                actionModeRuleIdx = -1;
                mode.finish();
                getListAdapter().notifyDataSetChanged();
                return true;

            } else if (item.getItemId() == R.id.rule_up) {
                if (actionModeRuleIdx > 0) {
                    Rule rule = rules.remove(actionModeRuleIdx);
                    --actionModeRuleIdx;
                    rules.add(actionModeRuleIdx, rule);
                    getListAdapter().notifyDataSetChanged();
                }
                return true;

            } else if (item.getItemId() == R.id.rule_down) {
                if (actionModeRuleIdx < rules.size() - 1) {
                    Rule rule = rules.remove(actionModeRuleIdx);
                    ++actionModeRuleIdx;
                    rules.add(actionModeRuleIdx, rule);
                    getListAdapter().notifyDataSetChanged();
                }
                return true;
            }
            return false;
        }
    }


    private class RulesListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return rules.size();
        }

        @Override
        public Object getItem(int position) {
            return rules.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = getActivity().getLayoutInflater().inflate(R.layout.rules_editor_row, parent, false);
            }

            Rule rule = rules.get(position);
            String connectorName = connectorLabels[findConnectorIdx(rule)].getName();

            StringBuilder ruleSummary = new StringBuilder();
            if (rule.getPattern() != null) {
                ruleSummary.append(getString(R.string.rule_summary_pattern, rule.getPattern().pattern()));
            } else if (rule.getPrefix() != null) {
                ruleSummary.append(getString(R.string.rule_summary_prefix, rule.getPrefix()));
            } else {
                ruleSummary.append(getString(R.string.rule_summary_default));
            }
            if (rule.getCharsetName() != null) {
                // only single US-ASCII charset is currently supported
                ruleSummary.append(getString(R.string.rule_summary_ascii));
            }
            ruleSummary.append(getString(R.string.rule_summary_connector, connectorName));

            TextView tvRuleSummary = (TextView) view.findViewById(R.id.rule_summary);
            tvRuleSummary.setText(Html.fromHtml(ruleSummary.toString()));

            // see the NOTE in onViewCreated()
            ((Checkable)view).setChecked(position == actionModeRuleIdx);

            return view;
        }
    }

}
