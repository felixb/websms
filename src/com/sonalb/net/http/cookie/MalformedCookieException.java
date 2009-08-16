package com.sonalb.net.http.cookie;

/**
 * Indicates some problem caused by a bad or malformed cookie. For constructor
 * descriptions, see the documentation for superclass.
 * 
 * @author Sonal Bansal
 */

public class MalformedCookieException extends com.sonalb.EnhancedIOException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MalformedCookieException() {
		super();
	}

	public MalformedCookieException(final String s) {
		super(s);
	}

	public MalformedCookieException(final Exception under) {
		super(under);
	}

	public MalformedCookieException(final String s, final Exception under) {
		super(s, under);
	}

	public MalformedCookieException(final String s, final Exception under,
			final String code, final Object o, final String method) {
		super(s, under, code, o, method);
	}

	public MalformedCookieException(final String code, final Object o,
			final String method) {
		super(code, o, method);
	}

	public MalformedCookieException(final String s, final String code,
			final Object o, final String method) {
		super(s, code, o, method);
	}

	public String getCode() {
		return (super.getCode().equals("UNSPECIFIED") ? "SBCL_0000" : super
				.getCode());
	}
}