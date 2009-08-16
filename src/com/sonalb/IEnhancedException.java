package com.sonalb;

/**
 * Allows implementing classes to contain an application-specific error code,
 * the class and method of origin, a data <code>Object</code>, and any
 * underlying (causal) <code>Exception</code>.
 * <p>
 * These enhancements are described below :-<br>
 * <ul>
 * <li><b>Error Code</b>: A unique code is assigned to each application-specific
 * error condition. This code may be useful for troubleshooting, or it may be
 * used as a reference to allow the user to obtain more detailed information
 * about the particular error condition.<br>
 * <br>
 * </li>
 * <li><b>Class of origin</b>:</li>
 * <li><b>Method of origin</b>: These two allow the convenient determination of
 * the class and the method where this <code>IEnhancedException</code> instance
 * was <u><i>created</i></u>.<br>
 * <br>
 * </li>
 * <li><b>Data Object</b>: This <code>Object</code> allows this
 * <code>IEnhancedException</code> instance to pass some information up the
 * stack.<br>
 * <br>
 * </li>
 * <li><b>Causal Exception</b>: This is the underlying exception which is the
 * "root cause" of this <code>IEnhancedException</code>. "Exception chaining" is
 * possible if the causal exception is also an instance of
 * <code>IEnhancedException</code>.<br>
 * <br>
 * </li>
 * </ul>
 * <p>
 * Following are some guidelines for implementations :-<br>
 * <ul>
 * <li>In order to be useful, any implementing class should extend
 * <code>Exception</code> (or one of its subclasses).</li>
 * <li>It is up to the implementing class to determine how it internally stores
 * and represents the contained data (error code, data object, origin etc).
 * These fields may be populated at construction time (via appropriate
 * constructor arguments), or using "setter" methods, or both.</li>
 * <li>In keeping with the purpose of the above enhancements, implementations
 * are expected to take suitable measures to ensure that the fields are
 * populated only once, at or near the time that this instance is created. The
 * data Object can be removed at any time, even higher up in the call stack.</li>
 * </ul>
 * <p>
 * <b>NOTE:</b> Direct implementing classes DO NOT follow the same hierarchy as
 * followed by the <code>Exception</code>s in the core Java packages. For
 * example, <code>EnhancedIOException</code> is NOT a direct subclass of
 * <code>EnhancedException</code>, even though <code>IOException</code> is a
 * direct subclass of <code>Exception</code>. In other words,
 * <code>(EnhancedIOException instanceof EnhancedException)</code> returns
 * <code>false</code> even though
 * <code>(IOException instanceof Exception)</code> returns <code>true</code>.
 * 
 * @author Sonal Bansal
 * @see java.lang.Exception
 * @see com.sonalb.EnhancedException
 * @see com.sonalb.EnhancedIOException
 */

public interface IEnhancedException {
	/**
	 * Returns the fully-qualified name of the class which constructed this
	 * instance.
	 * 
	 * @return the <code>String</code> representing the class of origin ;
	 *         "UNKNOWN" if it was not set.
	 */
	public String getOriginClass();

	/**
	 * Returns the name of the method in which this instance was constructed.
	 * 
	 * @return the <code>String</code> representing the method of origin ;
	 *         "UNKNOWN" if it was not set.
	 */
	public String getOriginMethod();

	/**
	 * Returns the application-specific error code associated with this
	 * instance.
	 * 
	 * @return the <code>String</code> representing the error code ;
	 *         "UNSPECIFIED" if it was not set.
	 */
	public String getCode();

	/**
	 * Returns the underlying <code>Exception</code> (if any) for this instance.
	 * 
	 * @return the underlying (causal) <code>Exception</code> ;
	 *         <code>null</code> if there is none.
	 */
	public Exception getCausalException();

	/**
	 * Returns the data object (if any) set by the originator.
	 * 
	 * @return the <code>Object</code> ; <code>null</code> if there is none.
	 */
	public Object getDataObject();

	/**
	 * Removes the data object (if any) contained in this instance.
	 */
	public void removeDataObject();
}
