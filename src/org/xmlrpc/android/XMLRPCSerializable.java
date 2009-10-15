package org.xmlrpc.android;

/**
 * original source code from http://code.google.com/p/android-xmlrpc/ released
 * under apache licence 2.0 http://www.apache.org/licenses/LICENSE-2.0 Allows to
 * pass any XMLRPCSerializable object as input parameter. When implementing
 * getSerializable() you should return one of XMLRPC primitive types (or another
 * XMLRPCSerializable: be careful not going into recursion by passing this
 * object reference!)
 */
public interface XMLRPCSerializable {

	/**
	 * Gets XMLRPC serialization object
	 * 
	 * @return object to serialize This object is most likely one of XMLRPC
	 *         primitive types, however you can return also another
	 *         XMLRPCSerializable
	 */
	Object getSerializable();
}
