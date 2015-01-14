package de.ub0r.android.websms;

import de.ub0r.android.websms.connector.common.ConnectorSpec;

/**
 * Helper class for building connector selectors.
 * (Implements CharSequence so that it can be passed to AlertDialog.Builder.setItems().)
 */
public class ConnectorLabel
    implements CharSequence {

    private final String name;
    private final ConnectorSpec connector;
    private final ConnectorSpec.SubConnectorSpec subConnector;


    public ConnectorLabel(ConnectorSpec connector, ConnectorSpec.SubConnectorSpec subConnector,
                          boolean isSingleSubConnector) {
        this.connector = connector;
        this.subConnector = subConnector;

        if (isSingleSubConnector) {
            this.name = connector.getName();
        } else {
            this.name = connector.getName() + " - " + subConnector.getName();
        }
    }


    public String getName() {
        return name;
    }

    public ConnectorSpec getConnector() {
        return connector;
    }

    public ConnectorSpec.SubConnectorSpec getSubConnector() {
        return subConnector;
    }


    @Override
    public int length() {
        return name.length();
    }

    @Override
    public char charAt(int index) {
        return name.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return name.subSequence(start, end);
    }

    @Override
    public String toString() {
        return name;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ConnectorLabel)) {
            return false;
        }
        ConnectorLabel that = (ConnectorLabel) o;
        return this.connector.getPackage().equals(that.connector.getPackage())
                && this.subConnector.getID().equals(that.subConnector.getID());
    }

    @Override
    public int hashCode() {
        int result = this.connector.getPackage().hashCode();
        result = 31 * result + this.subConnector.getID().hashCode();
        return result;
    }
}
