package com.sonalb.net.http.cookie;

import java.net.URL;

import com.sonalb.net.http.Header;

/**
 * Interface definition for cookie-parsing and specification implementations.
 * 
 * @see Client#setCookieParser(CookieParser)
 * @author Sonal Bansal
 */
public interface CookieParser {
	/**
	 * Converts the <code>Cookie</code>s in the <code>CookieJar</code> to a set
	 * of headers suitable to be sent along with an HTTP request.
	 */
	public Header getCookieHeaders(CookieJar cj);

	/**
	 * Checks whether a request to the given URL is allowed to return the
	 * specified Cookie.
	 */
	public boolean allowedCookie(Cookie c, URL url);

	/**
	 * Converts the headers in an HTTP response into a <code>CookieJar</code> of
	 * <code>Cookie</code> objects.
	 */
	public CookieJar parseCookies(Header h, URL url)
			throws MalformedCookieException;

	/**
	 * Checks whether the given Cookie can be sent alongwith a request for the
	 * given URL.
	 */
	public boolean sendCookieWithURL(Cookie c, URL url, boolean bRespectExpires);
}
