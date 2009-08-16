package com.sonalb.net.http.cookie;

import java.net.URL;
import java.util.Iterator;

/**
 * Provides utility functions for internal consumption.
 * 
 * @author Sonal Bansal
 */
public final class CookieUtils {
	/**
	 * Picks out the <code>Cookie</code>s in a <code>CookieJar</code> that are
	 * eligible to be sent with a request to a particular <code>URL</code>.
	 * 
	 * @param cj
	 *            the CookieJar holding the Cookies
	 * @param cp
	 *            the CookieParser which determines whether each Cookie can be
	 *            sent with given URL
	 * @param url
	 *            the URL for which Cookies have to be picked out
	 * @param bRespectExpires
	 *            whether the lifetime of the Cookies should be taken into
	 *            consideration
	 * @return the CookieJar with eligible Cookies; always non-null
	 */
	public static CookieJar getCookiesForURL(final CookieJar cj,
			final CookieParser cp, final URL url, final boolean bRespectExpires) {
		if (url == null || cj == null || cp == null) {
			throw new IllegalArgumentException("Null Argument.");
		}

		if (cj.isEmpty()) {
			return (cj);
		}

		Iterator i = cj.iterator();
		CookieJar jar = new CookieJar();
		Cookie c;

		while (i.hasNext()) {
			c = (Cookie) i.next();
			if (cp.sendCookieWithURL(c, url, bRespectExpires)) {
				jar.add(c);
			}
		}

		return (jar);
	}
}