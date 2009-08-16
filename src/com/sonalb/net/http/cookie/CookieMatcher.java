package com.sonalb.net.http.cookie;

/**
 * Allows custom matching of Cookies. Applications can use their matching logic
 * to pick out Cookies.
 * 
 * @see CookieJar#removeCookies(CookieMatcher)
 * @see CookieJar#getCookies(CookieMatcher)
 * @author Sonal Bansal
 */
public interface CookieMatcher {
	/**
	 * Checks whether the given Cookie satisfies the custom criteria.
	 * 
	 * @param the
	 *            Cookie to be checked
	 * @return satisfies or not ?
	 */
	public boolean doMatch(Cookie cookie);
}