package de.ub0r.android.websms.rules;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import de.ub0r.android.lib.Base64Coder;
import de.ub0r.android.websms.ConnectorLabel;
import de.ub0r.android.websms.R;
import de.ub0r.android.websms.WebSMS;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.Log;
import de.ub0r.android.websms.connector.common.Utils;
import de.ub0r.android.websms.connector.common.WebSMSException;

/**
 * A pseudo-connector that selects the real connector based on a set of rules.
 *
 * NOTE: do not extend Connector because Connector uses static fields
 *       and they will conflict with ConnectorSMS.
 */
public class PseudoConnectorRules {

    public static final String ID = PseudoConnectorRules.class.getPackage().getName();

    /** Tag for debug output. */
    private static final String TAG = "rules";

    /* Preference keys - must match keys in prefs_rules.xml */
    private static final String PREFS_KEY_ENABLED  = "enable_rules";
    private static final String PREFS_KEY_SHOW_DECISION_TOAST = "rules_show_decision_toast";
    private static final String PREFS_KEY_TEST_ONLY  = "rules_test_only";
    private static final String PREFS_KEY_RULES_LIST  = "rules_list";

    /** Singleton ConnectorSpec. */
    private static ConnectorSpec connectorSpec = null;

    /** Sync access. */
    private static final Object SYNC_UPDATE = new Object();

    /** Cached list of rules. */
    private static List<Rule> rules;

    /**
     * Returns the singleton ConnectorSpec. Initializes and updates it if needed.
     */
    public ConnectorSpec getSpec(final Context context) {
        synchronized (SYNC_UPDATE) {
            if (connectorSpec == null) {
                this.initSpec(context);
            }
            this.updateSpec(context);
            return connectorSpec;
        }
    }

    /**
     * Initializes the singleton ConnectorSpec.
     */
    private void initSpec(final Context context) {
        String name = context.getString(R.string.connector_rules_name);
        connectorSpec = new ConnectorSpec(name);
        connectorSpec.setPackage(ID);
        connectorSpec.setCapabilities(ConnectorSpec.CAPABILITIES_SEND
                | ConnectorSpec.CAPABILITIES_PREFS);
        connectorSpec.setBalance("");
        connectorSpec.addSubConnector("", "", ConnectorSpec.SubConnectorSpec.FEATURE_NONE);
    }

    /**
     * Updates the singleton ConnectorSpec with the current connector status etc.
     */
    public void updateSpec(final Context context) {
        if (isEnabled(context)) {
            connectorSpec.setReady();
        } else {
            connectorSpec.setStatus(ConnectorSpec.STATUS_INACTIVE);
        }
    }

    /**
     * Called when the app is upgraded in case any stored preferences need to be upgraded.
     */
    public void upgrade() {
        // nothing yet
    }

    /**
     * Chooses the connector to use for the given message.
     */
    public ConnectorLabel chooseConnector(Context context, String to, String text) {

        String toNumber = Utils.getRecipientsNumber(to).trim();

        ConnectorLabel[] connectorLabels = WebSMS.getConnectorMenuItems(false /*isIncludePseudoConnectors*/);

        ConnectorLabel foundConnLabel = null;
        for (Rule rule : getRules(context)) {

            ConnectorLabel connLabel = findConnector(connectorLabels,
                    rule.getConnectorId(), rule.getSubConnectorId());
            if (connLabel == null) {
                // rule refers to connector that is not present or not enabled
                continue;
            }

            boolean isMatchingNumber = false;
            if (rule.getPattern() != null) {
                if (rule.getPattern().matcher(toNumber).matches()) {
                    isMatchingNumber = true;
                }
            } else if (rule.getPrefix() != null ) {
                if (toNumber.startsWith(rule.getPrefix())) {
                    isMatchingNumber = true;
                }
            } else {
                isMatchingNumber = true;
            }

            boolean isMatchingText =
                    rule.getCharsetName() == null ||
                    Charset.forName(rule.getCharsetName()).newEncoder().canEncode(text);

            if (isMatchingNumber && isMatchingText) {
                foundConnLabel = connLabel;
                break;
            }
        }

        if (foundConnLabel == null) {
            throw new WebSMSException(context, R.string.err_no_matching_rules);
        }
        return foundConnLabel;
    }

    /**
     * Returns a connector in the list that matches the IDs (or null if not found).
     */
    private ConnectorLabel findConnector(ConnectorLabel[] connectorLabels,
                                         String connectorId, String subConnectorId) {
        for (ConnectorLabel cl : connectorLabels) {
            if (cl.getConnector().getPackage().equals(connectorId)
                    && cl.getSubConnector().getID().equals(subConnectorId)) {
                return cl;
            }
        }
        return null;
    }

    /**
     * Returns whether this connector is enabled.
     */
    public static boolean isEnabled(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREFS_KEY_ENABLED, false);
    }

    /**
     * Returns whether the decision toast should be shown.
     */
    public static boolean isShowDecisionToast(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREFS_KEY_SHOW_DECISION_TOAST, false);
    }

    /**
     * Returns whether the test-only mode is enabled.
     */
    public static boolean isTestOnly(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREFS_KEY_TEST_ONLY, false);
    }

    /**
     * Returns the current list of rules.
     */
    @SuppressWarnings("unchecked")
    public static List<Rule> getRules(Context context) {
        if (PseudoConnectorRules.rules == null) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String rulesStr = prefs.getString(PREFS_KEY_RULES_LIST, null);
            if (rulesStr == null) {
                PseudoConnectorRules.rules = Collections.emptyList();
            } else {
                try {
                    PseudoConnectorRules.rules = (List<Rule>)
                            new ObjectInputStream(
                                    new ByteArrayInputStream(Base64Coder.decode(rulesStr)))
                            .readObject();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load rules", e);
                    PseudoConnectorRules.rules = Collections.emptyList();
                }
            }
        }
        return PseudoConnectorRules.rules;
    }

    /**
     * Saves the new list of rules.
     */
    public static void saveRules(Context context, List<Rule> newRules) {
        try {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(newRules);
            String rulesStr = new String(Base64Coder.encode(baos.toByteArray()));

            prefs.edit()
                .putString(PREFS_KEY_RULES_LIST, rulesStr)
                .commit();

            PseudoConnectorRules.rules = newRules;

        } catch (Exception e) {
            Log.e(TAG, "Failed to save rules", e);
        }
    }

}
