package org.xmlrpc.android;

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
