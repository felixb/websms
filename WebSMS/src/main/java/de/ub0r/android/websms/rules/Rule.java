package de.ub0r.android.websms.rules;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

public class Rule
    implements Serializable, Cloneable {

    private static final long serialVersionUID = 1451431417810332955L;

    private String prefix;
    private Pattern pattern;
    private String charsetName;
    private String connectorId;
    private String subConnectorId;


    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Pattern getPattern() {
        return this.pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public String getCharsetName() {
        return this.charsetName;
    }

    public void setCharsetName(String charsetName) {
        this.charsetName = charsetName;
    }

    public String getConnectorId() {
        return this.connectorId;
    }

    public void setConnectorId(String connectorId) {
        this.connectorId = connectorId;
    }

    public String getSubConnectorId() {
        return this.subConnectorId;
    }

    public void setSubConnectorId(String subConnectorId) {
        this.subConnectorId = subConnectorId;
    }


    @Override
    public Rule clone() {
        try {
            return (Rule) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new IllegalStateException();
        }
    }

}
