package org.xmlrpc.android;

/**
 * original source code from http://code.google.com/p/android-xmlrpc/ released
 * under apache licence 2.0 http://www.apache.org/licenses/LICENSE-2.0
 */
public class XMLRPCException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7499675036625522379L;

	public XMLRPCException(final Exception e) {
		super(e);
	}

	public XMLRPCException(final String string) {
		super(string);
	}
}
