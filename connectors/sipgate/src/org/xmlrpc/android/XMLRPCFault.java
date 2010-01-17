package org.xmlrpc.android;

/**
 * original source code from http://code.google.com/p/android-xmlrpc/ released
 * under apache licence 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
public class XMLRPCFault extends XMLRPCException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5676562456612956519L;
	private String faultString;
	private int faultCode;

	public XMLRPCFault(final String faultString, final int faultCode) {
		super("XMLRPC Fault: " + faultString + " [code " + faultCode + "]");
		this.faultString = faultString;
		this.faultCode = faultCode;
	}

	public String getFaultString() {
		return this.faultString;
	}

	public int getFaultCode() {
		return this.faultCode;
	}
}
