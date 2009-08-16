package com.sonalb;

/**
 * Convenience implementation of <code>IEnhancedException</code> interface, and
 * subclass of <code>RuntimeException</code>.
 * <p>
 * Setter methods for the error code, causal Exception, class and method of
 * origin, and data Object are provided. However, once these fields have been
 * set, any subsequent calls to the respective setters throws an
 * <code>UnsupportedOperationException</code>.
 * 
 * @author Sonal Bansal
 * @see com.sonalb.IEnhancedException
 * @see java.lang.RuntimeException
 */

public class EnhancedRuntimeException extends RuntimeException implements
		IEnhancedException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Exception causalException;
	private String exceptionCode;
	private String originClass;
	private String originMethod;
	private Object data;

	private boolean bDataSet = false;
	private boolean bClassSet = false;
	private boolean bMethodSet = false;
	private boolean bCodeSet = false;
	private boolean bCausalSet = false;

	/**
	 * Constructs a "plain vanilla" EnhancedRuntimeException.
	 */
	public EnhancedRuntimeException() {
		super();
	}

	/**
	 * Constructs an EnhancedRuntimeException with a short detail message.
	 * 
	 * @param s
	 *            the detail message.
	 */
	public EnhancedRuntimeException(final String s) {
		super(s);
	}

	/**
	 * Constructs an EnhancedRuntimeException with the specified causal
	 * Exception.
	 * 
	 * @param under
	 *            the causal Exception.
	 * @see java.lang.Exception
	 */
	public EnhancedRuntimeException(final Exception under) {
		super();
		this.setCausalException(under);
	}

	/**
	 * Constructs an EnhancedRuntimeException with the specified detail message
	 * and causal Exception.
	 * 
	 * @param under
	 *            the causal Exception.
	 * @param s
	 *            the detail message.
	 * @see java.lang.Exception
	 */
	public EnhancedRuntimeException(final String s, final Exception under) {
		super(s);
		this.setCausalException(under);
	}

	/**
	 * Constructs an EnhancedRuntimeException with a detail message, causal
	 * Exception, error code, class and method of origin. For example,<br>
	 * <code>
	 * ...<br>
	 * throw new EnhancedRuntimeException("Failed to connect to server", excp, "SVR_0090", this, "connect");<br>
	 * ...</code>
	 * 
	 * @param under
	 *            the causal Exception.
	 * @param s
	 *            the detail message.
	 * @param code
	 *            the error-code.
	 * @param o
	 *            the Object from which the class of origin is determined.
	 * @param method
	 *            the method of origin.
	 * @see java.lang.Exception
	 */
	public EnhancedRuntimeException(final String s, final Exception under,
			final String code, final Object o, final String method) {
		super(s);
		this.setCausalException(under);
		this.setCode(code);
		this.setOriginClass(o);
		this.setOriginMethod(method);
	}

	/**
	 * Constructs an EnhancedRuntimeException with an error code and, class and
	 * method of origin. For example,<br>
	 * <code>
	 * ...<br>
	 * throw new EnhancedRuntimeException("SVR_0090", this, "connect");<br>
	 * ...</code>
	 * 
	 * @param code
	 *            the error-code.
	 * @param o
	 *            the Object from which the class of origin is determined.
	 * @param method
	 *            the method of origin.
	 */
	public EnhancedRuntimeException(final String code, final Object o,
			final String method) {
		super();
		this.setCode(code);
		this.setOriginClass(o);
		this.setOriginMethod(method);
	}

	/**
	 * Constructs an EnhancedRuntimeException with a detail message, error code,
	 * class and method of origin. For example,<br>
	 * <code>
	 * ...<br>
	 * throw new EnhancedRuntimeException("Failed to connect to server", "SVR_0090", this, "connect");<br>
	 * ...</code>
	 * 
	 * @param under
	 *            the causal Exception.
	 * @param s
	 *            the detail message.
	 * @param code
	 *            the error-code.
	 * @param o
	 *            the Object from which the class of origin is determined.
	 * @param method
	 *            the method of origin.
	 * @see java.lang.Exception
	 */
	public EnhancedRuntimeException(final String s, final String code,
			final Object o, final String method) {
		super(s);
		this.setCode(code);
		this.setOriginClass(o);
		this.setOriginMethod(method);
	}

	public Exception getCausalException() {
		return (this.causalException);
	}

	public String getOriginClass() {
		return ((this.originClass == null) ? "UNKNOWN" : this.originClass);
	}

	public String getOriginMethod() {
		return ((this.originMethod == null) ? "UNKNOWN" : this.originMethod);
	}

	public String getCode() {
		return ((this.exceptionCode == null) ? "UNSPECIFIED"
				: this.exceptionCode);
	}

	/**
	 * Sets the data Object which will be passed up the call stack.
	 * 
	 * @param o
	 *            the data Object.
	 * @throws UnsupportedOperationException
	 *             Thrown if the data Object has already been set.
	 */
	public void setDataObject(final Object o)
			throws UnsupportedOperationException {
		if (this.bDataSet) {
			throw new UnsupportedOperationException(
					"Data Object has already been set.");
		}

		this.internalSetDataObject(o);
		this.bDataSet = true;
	}

	public Object getDataObject() {
		return (this.data);
	}

	public void removeDataObject() {
		this.data = null;
	}

	/**
	 * Sets the error-code which identifies the particular error condition that
	 * triggered this exception.
	 * 
	 * @param c
	 *            the error code.
	 * @throws UnsupportedOperationException
	 *             Thrown if the error code has already been set.
	 */
	public void setCode(final String c) throws UnsupportedOperationException {
		if (this.bCodeSet) {
			throw new UnsupportedOperationException(
					"Error-code has already been set.");
		}

		this.internalSetCode(c);
		this.bCodeSet = true;
	}

	/**
	 * Sets the class of origin for this instance. The class of origin is taken
	 * as the value returned by o.getClass().getName(). However, if the
	 * parameter is itself an instance of <code>Class</code>, then the class of
	 * origin is taken as o.getName().
	 * 
	 * @param o
	 *            the Object representing the class of origin.
	 * @throws UnsupportedOperationException
	 *             Thrown if the class of origin has already been set.
	 * @see java.lang.Class
	 */
	public void setOriginClass(final Object o)
			throws UnsupportedOperationException {
		if (this.bClassSet) {
			throw new UnsupportedOperationException(
					"Class of origin has already been set.");
		}

		this.internalSetOriginClass(o);
		this.bClassSet = true;
	}

	/**
	 * Sets the method of origin for this instance.
	 * 
	 * @param meth
	 *            the String representing the method of origin.
	 * @throws UnsupportedOperationException
	 *             Thrown if the method of origin has already been set.
	 */
	public void setOriginMethod(final String meth)
			throws UnsupportedOperationException {
		if (this.bMethodSet) {
			throw new UnsupportedOperationException(
					"Method of origin has already been set.");
		}

		this.internalSetOriginMethod(meth);
		this.bMethodSet = true;
	}

	/**
	 * Sets the underlying (causal) Exception for this instance.
	 * 
	 * @param e
	 *            the Exception representing the causal Exception.
	 * @throws UnsupportedOperationException
	 *             Thrown if the causal Exception has already been set.
	 */
	public void setCausalException(final Exception e)
			throws UnsupportedOperationException {
		if (this.bCausalSet) {
			throw new UnsupportedOperationException(
					"Causal Exception has already been set.");
		}

		this.internalSetCausalException(e);
		this.bCausalSet = true;
	}

	/**
	 * Returns a short description of this instance. If this instance does not
	 * contain the error code or the origin info, the returned String is same as
	 * would be returned by <code>RuntimeException.toString()</code>. Otherwise,
	 * the returned String is formed by concatenating the following :-<br>
	 * <ul>
	 * <li>The name of the actual class of this object </li> <li>": " (a colon
	 * and a space) </li> <li>The result of the {@link #getMessage} method for
	 * this object</li> <li>" : Code="</li> <li>The error-code</li> <li>
	 * " : OriginClass="</li> <li>The class of origin</li> <li>
	 * " : OriginMethod="</li> <li>The method of origin</li> <li>
	 * " : CausalException="</li> <li>The name of the class of the causal
	 * Exception (if any)</li>
	 * </ul>
	 * 
	 * @return the <code>String</code> representation of this
	 *         <code>EnhancedRuntimeException</code>.
	 * @see java.lang.RuntimeException#toString()
	 */
	public String toString() {
		if (this.exceptionCode == null && this.originClass == null
				&& this.originMethod == null) {
			return (super.toString());
		}

		StringBuffer sb = new StringBuffer();
		sb.append(this.getClass().getName());
		sb.append(": ");
		sb.append((this.getMessage() == null) ? "" : this.getMessage());
		sb.append(" : Code=");
		sb.append(this.getCode());
		sb.append(" : OriginClass=");
		sb.append(this.getOriginClass());
		sb.append(" : OriginMethod=");
		sb.append(this.getOriginMethod());
		sb.append(" : CausalException=");
		sb.append((this.getCausalException() == null) ? "" : this
				.getCausalException().getClass().getName());

		if (this.getCausalException() != null) {
			try {
				java.io.StringWriter sw = new java.io.StringWriter();
				this.getCausalException().printStackTrace(
						new java.io.PrintWriter(sw));
				sb.append("\n");
				sb.append(sw.toString());
			} catch (Exception e) {
			}
		}

		return (sb.toString());
	}

	/**
	 * Sets the underlying (causal) Exception for this instance. Allows
	 * subclasses to have unrestricted access to "causal exception" field.
	 * 
	 * @param e
	 *            the Exception representing the causal Exception.
	 */
	protected void internalSetCausalException(final Exception e) {
		this.causalException = e;
	}

	/**
	 * Sets the method of origin for this instance. Allows subclasses to have
	 * unrestricted access to "method of origin" field.
	 * 
	 * @param meth
	 *            the String representing the method of origin.
	 */
	protected void internalSetOriginMethod(final String meth) {
		this.originMethod = meth;
	}

	/**
	 * Sets the class of origin for this instance. The class of origin is taken
	 * as the value returned by o.getClass().getName(). However, if the
	 * parameter is itself an instance of <code>Class</code>, then the class of
	 * origin is taken as o.getName(). Allows subclasses to have unrestricted
	 * access to "class of origin" field.
	 * 
	 * @param o
	 *            the Object representing the class of origin.
	 * @see java.lang.Class
	 */
	protected void internalSetOriginClass(final Object o) {
		if (o != null) {
			if (o instanceof Class) {
				this.originClass = ((Class) o).getName();
			} else {
				this.originClass = o.getClass().getName();
			}
		} else {
			this.originClass = null;
		}
	}

	/**
	 * Sets the error-code which identifies the particular error condition that
	 * triggered this exception. Allows subclasses to have unrestricted access
	 * to "error code" field.
	 * 
	 * @param c
	 *            the error code.
	 */
	protected void internalSetCode(final String c) {
		this.exceptionCode = c;
	}

	/**
	 * Sets the data Object which will be passed up the call stack. Allows
	 * subclasses to have unrestricted access to "data Object" field.
	 * 
	 * @param o
	 *            the data Object.
	 */
	protected void internalSetDataObject(final Object o) {
		this.data = o;
	}
}
