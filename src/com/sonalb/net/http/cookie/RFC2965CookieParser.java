package com.sonalb.net.http.cookie;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import com.sonalb.Utils;
import com.sonalb.net.http.Header;
import com.sonalb.net.http.HeaderEntry;

/**
 * Implementation for CookieParser that conforms to cookie specification
 * RFC-2965.
 * 
 * @author Sonal Bansal
 */
public class RFC2965CookieParser implements CookieParser {
	public Header getCookieHeaders(final CookieJar cj) {
		if (cj == null) {
			throw new IllegalArgumentException("Null CookieJar");
		}

		if (cj.isEmpty()) {
			return (null);
		}

		CookieJar eligibleV1Cookies = sortCookiesByPathSpecificity(cj
				.getVersionCookies("1"));
		CookieJar eligibleV0Cookies = sortCookiesByPathSpecificity(cj
				.getVersionCookies("0"));

		Header headers = new Header();
		// headers.add("Cookie2", "1");

		StringBuilder sb;
		boolean bFirstElement;
		Iterator iter;

		if (!eligibleV1Cookies.isEmpty()) {
			sb = new StringBuilder();
			bFirstElement = true;
			iter = eligibleV1Cookies.iterator();

			while (iter.hasNext()) {
				if (bFirstElement) {
					sb.append(toCookieHeaderForm((Cookie) iter.next(), true));
					bFirstElement = false;
				} else {
					sb.append(toCookieHeaderForm((Cookie) iter.next(), false));
				}

				sb.append(";");
			}

			sb.deleteCharAt(sb.length() - 1);
			headers.add("Cookie", sb.toString());
		}

		if (!eligibleV0Cookies.isEmpty()) {
			sb = new StringBuilder();
			bFirstElement = true;
			iter = eligibleV0Cookies.iterator();

			while (iter.hasNext()) {
				if (bFirstElement) {
					sb.append(toCookieHeaderForm((Cookie) iter.next(), true));
					bFirstElement = false;
				} else {
					sb.append(toCookieHeaderForm((Cookie) iter.next(), false));
				}

				sb.append("; ");
			}

			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			headers.add("Cookie", sb.toString());
		}

		return (headers);
	}

	public boolean allowedCookie(final Cookie c, final URL url) {
		try {
			return (allowedCookie(c, url, true));
		} catch (MalformedCookieException mce) {
			return (false);
		}
	}

	public CookieJar parseCookies(final Header header, final URL url)
			throws MalformedCookieException {
		if (header == null || header.isEmpty()) {
			throw new IllegalArgumentException("No Headers");
		}

		CookieJar cj = new CookieJar();

		if (header.containsKey("set-cookie")) {
			// System.out.println(
			// "Client.getCookies(): There are set-cookie headers.");
			cj.addAll(parseSetCookieV0(header, url, true));
		}

		if (header.containsKey("set-cookie2")
				|| header.containsKey("set-cookie")) {
			// System.out.println(
			// "Client.getCookies(): There are set-cookie2 headers.");
			cj.addAll(parseSetCookieV1(header, url, true));
		}

		//System.out.println("Client.getCookies(): Parsed. JAR="+cj.toString());
		return (cj);
	}

	/**
	 * Determines whether a <code>Cookie</code> is eligible to be sent alongwith
	 * the request for a given <code>URL</code>. This method may or may not take
	 * into consideration the lifetime (indicated by cookie parameters). It
	 * discerns between Version 0 (Netscape) and Version 1 (RFC 2965) cookies.
	 * For Version 0 cookies, the relevant portion of Netscape's draft:
	 * <p>
	 *"When searching the cookie list for valid cookies, a comparison of the
	 * domain attributes of the cookie is made with the Internet domain name of
	 * the host from which the URL will be fetched. If there is a tail match,
	 * then the cookie will go through path matching to see if it should be
	 * sent."
	 * <p>
	 * For Version 1 cookies, the relevant portion of Netscape's draft:
	 * <p>
	 *"The user agent applies the following rules to choose applicable
	 * cookie-values to send in Cookie request headers from among all the
	 * cookies it has received.
	 * <p>
	 * Domain Selection<br>
	 * The origin server's effective host name MUST domain-match the Domain
	 * attribute of the cookie.
	 * <p>
	 * Port Selection<br>
	 * There are three possible behaviors, depending on the Port attribute in
	 * the Set-Cookie2 response header:
	 * <p>
	 * &nbsp;&nbsp;&nbsp;&nbsp;1. By default (no Port attribute), the cookie MAY
	 * be sent to any port.<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;2. If the attribute is present but has no value
	 * (e.g., Port), the cookie MUST only be sent to the request-port it was
	 * received from.<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;3. If the attribute has a port-list, the cookie
	 * MUST only be returned if the new request-port is one of those listed in
	 * port-list.
	 * <p>
	 * Path Selection<br>
	 * The request-URI MUST path-match the Path attribute of the cookie.
	 * <p>
	 * Max-Age Selection<br>
	 * Cookies that have expired should have been discarded and thus are not
	 * forwarded to an origin server."
	 * <p>
	 * Note: For both Versions 0 and 1 cookies, if the cookie is marked as
	 * "secure", it can be sent only over a secure transport. This
	 * implementation considers HTTPS and SHTTP protocols as secure.
	 * 
	 * @see #pathMatch
	 * @see #tailMatch
	 * @see #domainMatch
	 * @see #portMatch
	 * @see Cookie#hasExpired
	 * @see Cookie#isValid
	 * @param c
	 *            the <code>Cookie</code> to be tested.
	 * @param url
	 *            the <code>URL</code> for which to test.
	 * @param bRespectExpires
	 *            whether or not to take expiry information into consideration.
	 * @returns <code>true</code> if the cookie can be sent to the request-url;
	 *          false otherwise. Also, <code>false</code> if either
	 *          <code>url</code> or <code>c</code> are null.
	 * @throws IllegalArgumentException
	 *             Thrown if the input <code>Cookie</code> is not valid, that
	 *             is, it is incomplete.
	 */
	public boolean sendCookieWithURL(final Cookie c, final URL url,
			final boolean bRespectExpires) {
		if (url == null || c == null) {
			// System.out.println(
			// "RFC2965CookieParser.sendCookieWithURL(): URL or Cookie is Null"
			// );
			return (false);
		} else if (!c.isValid()) {
			// System.out.println(
			// "RFC2965CookieParser.sendCookieWithURL(): Invalid Cookie");
			throw new IllegalArgumentException("Invalid/Bad cookie.");
		}

		if (c.isSecure()) {
			String protocol = url.getProtocol();

			if (Utils.isNullOrWhiteSpace(protocol)) {
				return (false);
			}

			protocol = protocol.toLowerCase();

			if (!("https".equals(protocol) || "shttp".equals(protocol))) {
				return (false);
			}
		}

		String domain = c.getDomain();
		String path = c.getPath();

		if ("0".equals(c.getVersion())) {
			return (tailMatch(url, domain) && pathMatch(url, path) && (bRespectExpires ? !c
					.hasExpired()
					: true));
		} else {
			String portList = c.getPortList();
			return (domainMatch(url, domain) && portMatch(url, portList)
					&& pathMatch(url, path) && (bRespectExpires ? !c
					.hasExpired() : true));
		}
	}

	// ******************************************************************
	// Methods for internal use follow
	// ******************************************************************

	/**
	 * Sorts the <code>Cookie</code>s in the input <code>CookieJar</code>, with
	 * the cookie having most specific path attribute first. This is used while
	 * determining the order in which cookies must be sent back to the server.
	 * <p>
	 * Note: The input <code>CookieJar</code> is NOT modified, instead, a new
	 * CookieJar is returned with the sorted Cookies.
	 * 
	 * @param cj
	 *            the CookieJar with the cookies to be sorted.
	 * @returns the new sorted CookieJar, or the input CookieJar <code>cj</code>
	 *          if <code>cj</code> is null, empty, or has only one cookie.
	 * @see CookieJar
	 */
	public static CookieJar sortCookiesByPathSpecificity(final CookieJar cj) {
		if (cj == null || cj.isEmpty() || cj.size() == 1) {
			return (cj);
		}

		Vector v = new Vector();
		v.addAll(cj);

		Collections.sort(v);
		CookieJar sortedCJ = new CookieJar(v);

		return (sortedCJ);
	}

	/**
	 * Performs "path matching" of URL to cookie path, as specified by RFC 2965.
	 * The <code>URL</code>'s path is obtained by <code>URL.getPath()</code>.
	 * The relevant portion of RFC 2965 :
	 * <p>
	 *"For two strings that represent paths, P1 and P2, P1 path-matches P2 if
	 * P2 is a prefix of P1 (including the case where P1 and P2 string-compare
	 * equal). Thus, the string /tec/waldo path-matches /tec."
	 * 
	 * @see URL
	 * @param url
	 *            the <code>URL</code> which must be matched.
	 * @param path
	 *            the cookie path.
	 * @return <code>true</code> if the URL path-matched the cookie path;
	 *         <code>false</code> otherwise.
	 */
	public static boolean pathMatch(final URL url, final String path) {
		// System.out.println("RFC2965CookieParser.pathMatch(): URL=" + url +
		// ",PATH=" + path);
		String upath = url.getPath();

		if (Utils.isNullOrWhiteSpace(upath)) {
			upath = "/";
		}

		// System.out.println("RFC2965CookieParser.pathMatch(): URL Path=" +
		// upath);

		if (upath.equals(path) || upath.startsWith(path)) {
			//System.out.println("RFC2965CookieParser.pathMatch(): Match TRUE");
			return (true);
		}

		// System.out.println("RFC2965CookieParser.pathMatch(): Match FALSE");
		return (false);
	}

	/**
	 * Performs "tail matching" of URL host/domain to cookie domain, for Version
	 * 0 cookies as specified by Netscape's draft. The <code>URL</code>'s host
	 * is obtained by <code>URL.getHost()</code>. The relevant portion of
	 * Netscape's draft :
	 * <p>
	 * ""Tail matching" means that domain attribute is matched against the tail
	 * of the fully qualified domain name of the host. A domain attribute of
	 * "acme.com" would match host names "anvil.acme.com" as well as
	 * "shipping.crate.acme.com".
	 * <p>
	 * Only hosts within the specified domain can set a cookie for a domain and
	 * domains must have at least two (2) or three (3) periods in them to
	 * prevent domains of the form: ".com", ".edu", and "va.us". Any domain that
	 * fails within one of the seven special top level domains listed below only
	 * require two periods. Any other domain requires at least three. The seven
	 * special top level domains are: "COM", "EDU", "NET", "ORG", "GOV", "MIL",
	 * and "INT"".
	 * 
	 * @see URL
	 * @param url
	 *            the <code>URL</code> which must be matched.
	 * @param domain
	 *            the cookie domain.
	 * @return <code>true</code> if the URL tail-matched the cookie domain;
	 *         <code>false</code> otherwise.
	 */
	public static boolean tailMatch(final URL url, final String domain) {
		// System.out.println("RFC2965CookieParser.tailMatch(): URL=" + url +
		// ",DOMAIN=" + domain);
		String host = url.getHost();

		if (Utils.isNullOrWhiteSpace(host)) {
			//System.out.println("RFC2965CookieParser.tailMatch(): Match TRUE");
			return (false);
		}

		if (host.indexOf('.') == -1) {
			host += ".local";
			// System.out.println("RFC2965CookieParser.tailMatch(): Match " +
			// host.toLowerCase().endsWith(domain.toLowerCase()));
			return (host.toLowerCase().endsWith(domain.toLowerCase()));
		}

		String specialTLDs[] = { "com", "edu", "net", "org", "gov", "mil",
				"int", "de", "biz" };
		int dots = countTheDots(domain);
		String tld = domain.substring(domain.lastIndexOf('.') + 1);

		if (Utils.isInArray(tld.toLowerCase(), specialTLDs)) {
			if (dots >= 2) {
				// System.out.println("RFC2965CookieParser.tailMatch(): Match "
				// + host.toLowerCase().endsWith(domain.toLowerCase()));
				return (host.toLowerCase().endsWith(domain.toLowerCase()));
			}
		} else {
			if (dots >= 3) {
				// System.out.println("RFC2965CookieParser.tailMatch(): Match "
				// + host.toLowerCase().endsWith(domain.toLowerCase()));
				return (host.toLowerCase().endsWith(domain.toLowerCase()));
			}
		}

		// System.out.println("RFC2965CookieParser.tailMatch(): Match FALSE");
		return (false);
	}

	/**
	 * Performs "domain matching" of URL host/domain to cookie domain, for
	 * Version 1 cookies as specified by RFC 2965. The <code>URL</code>'s host
	 * is obtained by <code>URL.getHost()</code>. The relevant portion of RFC
	 * 2965 :
	 * <p>
	 *"Host names can be specified either as an IP address or a HDN [host
	 * domain name] string. Sometimes we compare one host name with another.
	 * (Such comparisons SHALL be case-insensitive.) Host A's name
	 * domain-matches host B's if
	 * <p>
	 * 
	 * <pre>
	 *     *  their host name strings string-compare equal; or
	 *     *  A is a HDN string and has the form NB, where N is a non-empty 
	 *        name string, B has the form .B', and B' is a HDN string. 
	 *        (So, x.y.com domain-matches .Y.com but not Y.com.)
	 * </pre>
	 * 
	 * Note that domain-match is not a commutative operation: a.b.c.com
	 * domain-matches .c.com, but not the reverse."
	 * 
	 * @see URL
	 * @param url
	 *            the <code>URL</code> which must be matched.
	 * @param domain
	 *            the cookie domain.
	 * @return <code>true</code> if the URL domain-matched the cookie domain;
	 *         <code>false</code> otherwise.
	 */
	public static boolean domainMatch(final URL url, final String domain) {
		// System.out.println("RFC2965CookieParser.domainMatch(): URL="+url+
		// ",DOMAIN="+domain);
		try {
			String host = url.getHost();
			//System.out.println("RFC2965CookieParser.domainMatch(): URL host="+
			// host);

			if (Utils.isNullOrWhiteSpace(host)) {
				// System.out.println(
				// "RFC2965CookieParser.domainMatch(): Null host. FALSE.");
				return (false);
			}

			if (host.indexOf('.') == -1) {
				host += ".local";
			}

			// System.out.println(
			// "RFC2965CookieParser.domainMatch(): Equivalent host="+host);

			if (host.equalsIgnoreCase(domain)) {
				// System.out.println(
				// "RFC2965CookieParser.domainMatch(): Host equals Domain. TRUE."
				// );
				return (true);
			}

			if (Utils.isIPAddress(domain)) {
				// System.out.println(
				// "RFC2965CookieParser.domainMatch(): Domain is IP.");
				if (Utils.isIPAddress(host)) {
					// System.out.println(
					// "RFC2965CookieParser.domainMatch(): Host is also IP." +
					// host.equals(domain));
					return (host.equals(domain));
				} else {
					// System.out.println(
					// "RFC2965CookieParser.domainMatch(): Host is not IP.");
					InetAddress ia = InetAddress.getByName(host);
					// System.out.println(
					// "RFC2965CookieParser.domainMatch(): Host IP="
					// +ia.getHostAddress());
					return (domain.equals(ia.getHostAddress()));
				}
			}

			if (domain.charAt(0) != '.') {
				// System.out.println(
				// "RFC2965CookieParser.domainMatch(): Explicit domain doesn't have '.'.FALSE"
				// );
				return (false);
			}

			String bdash = domain.substring(1);
			if (bdash.indexOf(".") != -1 || bdash.equalsIgnoreCase("local")) {
				return (host.toLowerCase().endsWith(bdash.toLowerCase()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return (false);
	}

	/**
	 * Performs "port matching" of URL to cookie portlist, for Version 1
	 * cookies, as specified by RFC 2965. The given <code>URL</code>
	 * port-matches the given portlist, if the port returned by
	 * <code>URL.getPort()</code> exists in the portlist. The portlist itself is
	 * a comma-separated list of allowed ports for that cookie. If
	 * <code>URL.getPort()</code> returns a value less than 0, the default port
	 * of 80 is assumed.
	 * 
	 * @see URL
	 * @param url
	 *            the <code>URL</code> which must be matched.
	 * @param portList
	 *            the comma-separated list of acceptable ports.
	 * @return <code>true</code> if the URL port exists in portList, or if
	 *         portList is empty; <code>false</code> otherwise.
	 */
	public static final boolean portMatch(final URL url, final String portList) {
		// System.out.println("RFC2965CookieParser.portMatch(): URL=" + url +
		// ",PortList=" + portList);
		int p = url.getPort();
		if (p < 0) {
			p = 80;
		}

		String port = String.valueOf(p);

		//System.out.println("RFC2965CookieParser.portMatch(): Host port="+port)
		// ;

		if (!Utils.isNullOrWhiteSpace(portList)) {
			StringTokenizer st = new StringTokenizer(portList, ",");

			while (st.hasMoreTokens()) {
				if (port.equals(st.nextToken().trim())) {
					// System.out.println(
					// "RFC2965CookieParser.portMatch(): Match TRUE");
					return (true);
				}
			}

			//System.out.println("RFC2965CookieParser.portMatch(): Match FALSE")
			// ;
			return (false);
		}

		// System.out.println("RFC2965CookieParser.portMatch(): Match TRUE");
		return (true);
	}

	/**
	 * Converts a single Cookie to a form suitable for sending with a request,
	 * back to the server.
	 * 
	 * @param c
	 *            the Cookie to be converted
	 * @param bIncludeVersion
	 *            whether the version number should be included
	 * @return the String representing the formatted Cookie
	 */
	public static String toCookieHeaderForm(final Cookie c,
			final boolean bIncludeVersion) {
		if (c == null || !c.isValid()) {
			throw new IllegalArgumentException(
					"Cookie is null OR cookie is invalid");
		}

		StringBuilder sb = new StringBuilder();

		// if ("0".equals(c.getVersion())) {
		sb.append(c.getName());
		sb.append("=");
		sb.append(c.getValue());
		/*
		 * } else { if (bIncludeVersion) { sb.append("$Version=");
		 * sb.append(c.getVersion()); sb.append(";"); }
		 * 
		 * sb.append(c.getName()); sb.append("="); sb.append("\"");
		 * sb.append(c.getValue()); sb.append("\"");
		 * 
		 * if (c.explicitPath()) { sb.append(";$Path="); sb.append(c.getPath());
		 * }
		 * 
		 * if (c.explicitDomain()) { sb.append(";$Domain=");
		 * sb.append(c.getDomain()); }
		 * 
		 * if (c.explicitPort()) { sb.append(";$Port"); if
		 * (c.portListSpecified()) { sb.append("=\"");
		 * sb.append(c.getPortList()); sb.append("\""); } } }
		 */
		return (sb.toString());
	}

	private static boolean allowedCookie(final Cookie c, final URL url,
			final boolean bStrict) throws MalformedCookieException {
		if (c == null || url == null) {
			throw new IllegalArgumentException("Null cookie or URL");
		}

		if (!c.isValid()) {
			if (bStrict) {
				throw new MalformedCookieException("Invalid cookie",
						"SBCL_0012", RFC2965CookieParser.class, "allowedCookie");
			}

			return (false);
		}

		if ("1".equals(c.getVersion())) {
			// System.out.println(
			// "RFC2965CookieParser.allowedCookie(): Version 1 Cookie");
			if (domainMatch(url, c.getDomain())) {
				// System.out.println(
				// "RFC2965CookieParser.allowedCookie(): Domain Matches.");
				if (pathMatch(url, c.getPath())) {
					// System.out.println(
					// "RFC2965CookieParser.allowedCookie(): Path Matches.");
					if (portMatch(url, c.getPortList())) {
						// System.out.println(
						// "RFC2965CookieParser.allowedCookie(): Port Matches."
						// );

						String host = url.getHost();

						host = host.toLowerCase().trim();

						if (host.indexOf('.') == -1) {
							host += ".local";
						}

						String d = c.getDomain().toLowerCase().trim();
						String h = host.substring(0, host.lastIndexOf(d));
						// System.out.println(
						// "RFC2965CookieParser.allowedCookie(): HOST="
						// +host+",D="+d+",H="+h);

						if (countTheDots(h) > 0) {
							if (bStrict) {
								throw new MalformedCookieException(
										"X.Y.Z.COM tried to set domain=Y.COM",
										"SBCL_0018", RFC2965CookieParser.class,
										"allowedCookie");
							}

							return (false);
						}

						// System.out.println(
						// "RFC2965CookieParser.allowedCookie(): Security clearance granted."
						// );
						return (true);
					} else {
						if (bStrict) {
							throw new MalformedCookieException(
									"PortMatch failed", "SBCL_0016",
									RFC2965CookieParser.class, "allowedCookie");
						}
					}
				} else {
					if (bStrict) {
						throw new MalformedCookieException("PathMatch failed",
								"SBCL_0017", RFC2965CookieParser.class,
								"allowedCookie");
					}
				}
			} else {
				if (bStrict) {
					throw new MalformedCookieException("DomainMatch failed",
							"SBCL_0015", RFC2965CookieParser.class,
							"allowedCookie");
				}
			}
		} else if ("0".equals(c.getVersion())) {
			// System.out.println(
			// "RFC2965CookieParser.allowedCookie(): Version 0 cookie");

			if (tailMatch(url, c.getDomain())) {
				return (true);
			} else {
				if (bStrict) {
					throw new MalformedCookieException("TailMatch",
							"SBCL_0019", RFC2965CookieParser.class,
							"allowedCookie");
				}
			}
		} else {
			if (bStrict) {
				throw new MalformedCookieException(
						"Security Violated. Unknown reason.", "SBCL_0013",
						RFC2965CookieParser.class, "allowedCookie");
			}
		}

		// System.out.println(
		// "RFC2965CookieParser.allowedCookie(): Security Violated. FALSE.");
		return (false);
	}

	/**
	 * Parses headers into Version 1 Cookies.
	 */
	public static final CookieJar parseSetCookieV1(final Header responseHeader,
			final URL url, final boolean bStrict)
			throws MalformedCookieException {
		/*
		 * 1.) WILL FAIL IF PORTLIST IS NOT QUOTED AS PER RFC2965 2.) Must parse
		 * both "set-cookie" (RFC2109) and "set-cookie2" (RFC2965)
		 */

		if (responseHeader == null || responseHeader.isEmpty()) {
			throw new IllegalArgumentException("No Headers");
		}

		if (url == null) {
			throw new IllegalArgumentException("Null source URL");
		}

		if (!responseHeader.containsKey("set-cookie2")
				&& !responseHeader.containsKey("set-cookie")) {
			// System.out.println(
			// "RFC2965CookieParser.parseSetCookieV1(): No valid headers.");
			return (null);
		}

		String cookieToken = "", key, value;
		HeaderEntry he;
		CookieJar cj = new CookieJar();
		Cookie c;
		StringTokenizer st;

		Iterator it = responseHeader.iterator();

		while (it.hasNext()) {
			he = (HeaderEntry) it.next();
			key = he.getKey();
			value = he.getValue();

			// System.out.println(
			// "RFC2965CookieParser.parseSetCookieV1(): HEADERKEY="+key);

			if (Utils.isNullOrWhiteSpace(key)) {
				continue;
			}

			if (!(key.equalsIgnoreCase("set-cookie2") || key
					.equalsIgnoreCase("set-cookie"))) {
				continue;
			}

			// System.out.println(
			// "RFC2965CookieParser.parseSetCookieV1(): HEADERVALUE="+value);

			if (!Utils.matchQuotes(value)) {
				if (bStrict) {
					throw new MalformedCookieException(
							"Unmatched quotes throughout header.", "SBCL_0009",
							RFC2965CookieParser.class, "parseSetCookieV1");
				}
			}

			st = new StringTokenizer(value, ",");

			while (st.hasMoreTokens()) {
				cookieToken += st.nextToken();
				// System.out.println(
				// "RFC2965CookieParser.parseSetCookieV1(): cookieToken="
				// +cookieToken);

				if (!Utils.matchQuotes(cookieToken)) {
					// System.out.println(
					// "RFC2965CookieParser.parseSetCookieV1(): Comma is inside quotes."
					// );
					cookieToken += ",";
					continue;
				}

				try {
					// System.out.println(
					// "RFC2965CookieParser.parseSetCookieV1(): Parsing single cookie."
					// );
					c = parseSingleCookieV1(cookieToken, url, bStrict);
					// System.out.println(
					// "RFC2965CookieParser.parseSetCookieV1(): Parsed.COOKIE="
					// +(c==null?"null":c.toString()));
				} catch (MalformedCookieException mce) {
					mce.printStackTrace();

					if (bStrict) {
						throw mce;
					}

					c = null;
				}

				if (c != null) {
					cj.add(c);
				}

				cookieToken = "";
			}
		}

		// System.out.println(
		// "RFC2965CookieParser.parseSetCookieV1(): Parsed all. COOKIEJAR="
		// +cj.toString());
		return (cj);
	}

	/**
	 * Parses headers into Version 1 Cookies.
	 */
	public static final CookieJar parseSetCookieV0(final Header responseHeader,
			final URL url, final boolean bStrict)
			throws MalformedCookieException {
		if (responseHeader == null || responseHeader.isEmpty()) {
			throw new IllegalArgumentException("No Headers");
		}

		if (url == null) {
			throw new IllegalArgumentException("Null source URL");
		}

		if (!responseHeader.containsKey("set-cookie")) {
			// System.out.println(
			// "RFC2965CookieParser.parseSetCookieV0(): No Set-Cookie header.");
			return (null);
		}

		String key, value, cookieToken = "";
		HeaderEntry he;
		CookieJar cj = new CookieJar();
		Cookie c;
		StringTokenizer st;

		Iterator it = responseHeader.iterator();

		while (it.hasNext()) {
			he = (HeaderEntry) it.next();
			key = he.getKey();
			value = he.getValue();

			// System.out.println(
			// "RFC2965CookieParser.parseSetCookieV0(): HEADERKEY: " + key);

			if (Utils.isNullOrWhiteSpace(key)) {
				continue;
			}

			if (!key.equalsIgnoreCase("set-cookie")) {
				continue;
			}

			// System.out.println(
			// "RFC2965CookieParser.parseSetCookieV0(): HEADERVALUE: " + value);

			if (!Utils.matchQuotes(value)) {
				if (bStrict) {
					throw new MalformedCookieException(
							"Unmatched quotes throughout header.", "SBCL_0009",
							RFC2965CookieParser.class, "parseSetCookieV0");
				}
			}

			st = new StringTokenizer(value, ",");

			while (st.hasMoreTokens()) {
				cookieToken += st.nextToken();
				// System.out.println(
				// "RFC2965CookieParser.parseSetCookieV0(): cookieToken="
				// +cookieToken);

				if (!Utils.matchQuotes(cookieToken)) {
					cookieToken += ",";
					continue;
				}

				// The expires value may have comma
				if (cookieToken.toLowerCase().indexOf("expires") != -1) {
					// System.out.println(
					// "RFC2965CookieParser.parseSetCookieV0(): Found comma and expires."
					// );
					int eq = cookieToken.lastIndexOf("=");

					if (eq != -1) {
						// System.out.println(
						// "RFC2965CookieParser.parseSetCookieV0(): = sign exists."
						// );
						String last = cookieToken.substring(eq + 1);
						// System.out.println(
						// "RFC2965CookieParser.parseSetCookieV0(): Last word="
						// +last);

						if (isWeekDay(Utils.trimWhitespace(last))) {
							// System.out.println(
							// "RFC2965CookieParser.parseSetCookieV0(): False alarm."
							// );
							cookieToken += ",";
							continue;
						}
					}
				}

				try {
					c = parseSingleCookieV0(cookieToken, url, bStrict);
					// System.out.println(
					// "RFC2965CookieParser.parseSetCookieV0(): Parsed single cookie. C="
					// +c);
				} catch (MalformedCookieException mce) {
					mce.printStackTrace();

					if (bStrict) {
						throw mce;
					}

					c = null;
				}

				if (c != null) {
					cj.add(c);
				}

				cookieToken = "";
			}
		}

		// System.out.println(
		// "RFC2965CookieParser.parseSetCookieV0(): All processed. CJ="+cj);
		return (cj);
	}

	private static final boolean isWeekDay(final String str) {
		final String weekdays[] = { "sun", "sunday", "mon", "monday", "tue",
				"tuesday", "wed", "wednesday", "thu", "thursday", "fri",
				"friday", "sat", "saturday" };

		if (Utils.isNullOrWhiteSpace(str)) {
			return (false);
		}

		String s = str.trim().toLowerCase();
		for (int i = 0; i < weekdays.length; i++) {
			if (s.equals(weekdays[i])) {
				return (true);
			}
		}

		return (false);
	}

	/**
	 * Converts a single cookie-string into a Cookie object, for Version 0
	 */
	public static Cookie parseSingleCookieV0(final String s, final URL url,
			final boolean bStrict) throws MalformedCookieException {
		/*
		 * If expires value can't be parsed into valid date, continues quietly.
		 */

		if (Utils.isNullOrWhiteSpace(s) || isRFC2965CookieString(s)) {
			return (null);
		}

		if (!Utils.matchQuotes(s)) {
			if (bStrict) {
				throw new MalformedCookieException(
						"Unmatched quotes in cookie.", "SBCL_0010",
						RFC2965CookieParser.class, "parseSingleCookieV0");
			}
		}

		StringTokenizer st = new StringTokenizer(s, ";");
		Cookie c = new Cookie();
		String av = "", attr, val;
		int i;
		boolean bValisQuoted = false;

		c.setDomain(url);
		c.setPath(url);
		c.setVersion("0");

		while (st.hasMoreTokens()) {
			av += st.nextToken();
			attr = "";
			val = "";
			bValisQuoted = false;

			if (!Utils.matchQuotes(av)) {
				av += ";";
				continue;
			}

			if (Utils.isNullOrWhiteSpace(av)) {
				av = "";
				continue;
			}

			av = Utils.trimWhitespace(av);
			i = av.indexOf('=');

			if (i == -1) {
				i = av.length();
			}

			attr = Utils.trimWhitespace(av.substring(0, i));

			if (Utils.isNullOrWhiteSpace(attr)) {
				if (bStrict) {
					throw new MalformedCookieException("Wierd cookie.",
							"SBCL_0002", RFC2965CookieParser.class,
							"parseSingleCookieV0");
				}

				av = "";
				continue;
			}

			if (i == av.length()) {
				val = "";
			} else {
				val = av.substring(i + 1);
			}

			val = Utils.trimWhitespace(val);
			bValisQuoted = Utils.isQuoted(val);
			val = Utils.stripQuotes(val);

			if (Utils.isEmpty(val)) {
				if (Utils.isNullOrWhiteSpace(c.getName()) && i != av.length()) {
					c.setName(attr);
					c.setValue(val);
				} else if ("secure".equalsIgnoreCase(attr)) {
					c.setSecure(true);
				} else {
					// Unrecognised attribute in AVPair with empty RHS
					// Do what ?
				}

				av = "";
				continue;
			}

			if ("domain".equalsIgnoreCase(attr)) {
				c.setDomain(val);
			} else if ("path".equalsIgnoreCase(attr)) {
				c.setPath(val);
			} else if ("expires".equalsIgnoreCase(attr)) {
				Date d = Utils.parseHttpDateStringToDate(val);
				if (d == null) {
					d = new Date();
					d.setTime(d.getTime() + 1000 * 60 * 60 * 24);
				}
				if (d == null && bStrict) {
					throw new MalformedCookieException("Unparseable expires.",
							"SBCL_0011", RFC2965CookieParser.class,
							"parseSingleCookieV0");
				}

				c.setExpires(d);
			} else if (Utils.isNullOrWhiteSpace(c.getName())) {
				c.setName(attr);
				c.setValue(bValisQuoted ? "\"" + val + "\"" : val);
			} else {
				// Unrecognised attribute in AVPair.
				// Do nothing.
			}

			av = "";
		}

		if (s.toLowerCase().indexOf("expires") == -1) {
			c.setExpires(null);
		}

		if (!c.isValid()) {
			if (bStrict) {
				throw new MalformedCookieException("Invalid cookie.",
						"SBCL_0012", RFC2965CookieParser.class,
						"parseSingleCookieV0");
			}
		} else if (!allowedCookie(c, url, bStrict)) {
		} else {
			return (c);
		}

		return (null);
	}

	/**
	 * Converts a single cookie-string into a Cookie object, for Version 1
	 */
	public static Cookie parseSingleCookieV1(final String s, final URL url,
			final boolean bStrict) throws MalformedCookieException {
		/*
		 * This parser is both strict and lenient. Lenient :- 1.) If commentURL
		 * is not valid URL (MalformedURLException), it continues parsing 2.) If
		 * Max-Age is not valid number (NumberFormatException), it continues
		 * parsing 3.) If PortList has invalid number (NFE), continues Strict :-
		 * 1.) NAME-VALUE pair must be first AVPair 2.) Version must be set. If
		 * Cookie doesn't support that version Exception is thrown up. 3.) The
		 * NAME must not begin with '$' 4.) The server must have permission to
		 * set cookie with given params. (Domain,path,port)
		 * 
		 * NOTE: Must add code to distinguish between V0 and V1 (RFC2109). This
		 * is done using Version attr. If it is there, then V1, else V0
		 */

		// System.out.println(
		// "RFC2965CookieParser.parseSingleCookieV1(): Parsing. S="
		// +s+",URL="+url);
		if (Utils.isNullOrWhiteSpace(s) || !isRFC2965CookieString(s)) {
			return (null);
		}

		if (!Utils.matchQuotes(s)) {
			if (bStrict) {
				throw new MalformedCookieException(
						"Unmatched quotes in cookie.", "SBCL_0010",
						RFC2965CookieParser.class, "parseSingleCookieV1");
			}
		}

		StringTokenizer st = new StringTokenizer(s, ";");
		Cookie c = new Cookie();
		String av = "", attr, val;
		int i;
		boolean bGotVersion = false;

		c.setDomain(url);
		c.setPath(url);
		c.setVersion("1");

		while (st.hasMoreTokens()) {
			av += st.nextToken();
			attr = "";
			val = "";

			if (!Utils.matchQuotes(av)) {
				av += ";";
				continue;
			}

			if (Utils.isNullOrWhiteSpace(av)) {
				av = "";
				continue;
			}

			av = Utils.trimWhitespace(av);
			i = av.indexOf('=');

			if (i == -1) {
				if (Utils.isNullOrWhiteSpace(c.getName())) {
					throw new MalformedCookieException(
							"Non-conforming cookie.", "SBCL_0001",
							RFC2965CookieParser.class, "parseSingleCookieV1");
				}

				i = av.length();
			}

			attr = Utils.trimWhitespace(av.substring(0, i));

			if (Utils.isNullOrWhiteSpace(attr)) {
				if (bStrict) {
					throw new MalformedCookieException("Wierd cookie.",
							"SBCL_0002", RFC2965CookieParser.class,
							"parseSingleCookieV0");
				}

				av = "";
				continue;
			}

			if (i == av.length()) {
				val = "";
			} else {
				val = av.substring(i + 1);
			}

			val = Utils.stripQuotes(Utils.trimWhitespace(val));

			if (Utils.isNullOrWhiteSpace(c.getName())) {
				if (attr.startsWith("$")) {
					throw new MalformedCookieException(
							"Non-conforming cookie.", "SBCL_0003",
							RFC2965CookieParser.class, "parseSingleCookieV1");
				}

				c.setName(attr);
				c.setValue(val);
				av = "";
				continue;
			}

			if (Utils.isEmpty(val)) {
				if ("port".equalsIgnoreCase(attr)) {
					c.setPort(url);
				} else if ("secure".equalsIgnoreCase(attr)) {
					c.setSecure(true);
				} else if ("discard".equalsIgnoreCase(attr)) {
					c.setDiscard(true);
				} else {
					// Unrecognised attribute in AVPair with empty RHS
					// RFC2965 says should ignore, so do nothing.
				}

				av = "";
				continue;
			}

			if ("comment".equalsIgnoreCase(attr)) {
				c.setComment(val);
			} else if ("commenturl".equalsIgnoreCase(attr)) {
				try {
					c.setCommentURL(new URL(val));
				} catch (MalformedURLException mue) {
					if (bStrict) {
						throw new MalformedCookieException(
								"Invalid data in Cookie.", mue, "SBCL_0004",
								RFC2965CookieParser.class,
								"parseSingleCookieV1");
					}
				}
			} else if ("domain".equalsIgnoreCase(attr)) {
				c.setDomain(val);
			} else if ("max-age".equalsIgnoreCase(attr)) {
				try {
					c.setMaxAge(Integer.parseInt(val));
				} catch (NumberFormatException nfe) {
					if (bStrict) {
						throw new MalformedCookieException(
								"Invalid data in Cookie.", nfe, "SBCL_0005",
								RFC2965CookieParser.class,
								"parseSingleCookieV1");
					}
				}
			} else if ("path".equalsIgnoreCase(attr)) {
				c.setPath(val);
			} else if ("port".equalsIgnoreCase(attr)) {
				try {
					String[] strPorts = Utils.csvStringToArray(val);

					if (strPorts != null) {
						int ports[] = new int[strPorts.length];

						for (int j = 0; j < strPorts.length; j++) {
							ports[j] = Integer.parseInt(strPorts[j]);
						}

						c.setPortList(ports);
					}
				} catch (NumberFormatException nfe) {
					if (bStrict) {
						throw new MalformedCookieException(
								"Invalid data in Cookie.", nfe, "SBCL_0006",
								RFC2965CookieParser.class,
								"parseSingleCookieV1");
					}
				}
			} else if ("version".equalsIgnoreCase(attr)) {
				c.setVersion(val);
				bGotVersion = true;
			} else {
				// Unrecognised attribute in AVPair.
				// RFC2965 says should ignore, so do nothing.
			}

			av = "";
		}

		// System.out.println(
		// "RFC2965CookieParser.parseSingleCookieV1(): Processing done. COOKIE="
		// +c.toString());

		if (!c.isValid()) {
			if (bStrict) {
				throw new MalformedCookieException("Invalid cookie.",
						"SBCL_0012", RFC2965CookieParser.class,
						"parseSingleCookieV1");
			}
		} else if (!bGotVersion) {
			if (bStrict) {
				throw new MalformedCookieException(
						"Version not explicitly stated.", "SBCL_0014",
						RFC2965CookieParser.class, "parseSingleCookieV1");
			}
		} else if (!allowedCookie(c, url, bStrict)) {
		} else {
			// System.out.println(
			// "RFC2965CookieParser.parseSingleCookieV1(): QC Passed.C="
			// +c.toString());
			return (c);
		}

		// System.out.println(
		// "RFC2965CookieParser.parseSingleCookieV1(): QC FAILED.C="
		// +c.toString());
		return (null);
	}

	/**
	 * Determines whether a cookie string is for Version 0 or Version 1 cookie.
	 */
	public static boolean isRFC2965CookieString(final String c)
			throws MalformedCookieException {
		/*
		 * Differentiates betn V0 and V1 RFC2109.
		 */

		//System.out.println("RFC2965CookieParser.isRFC2965CookieString(): C="+c
		// );
		String s[] = Utils.delimitedStringToArray(c, ";");

		if (s == null) {
			throw new MalformedCookieException("Non-conforming Cookie.",
					"SBCL_0008", RFC2965CookieParser.class,
					"isRFC2965CookieString");
		}

		for (int i = 0; i < s.length; i++) {
			if (Utils.trimWhitespace(s[i]).toLowerCase().startsWith("version")
					&& s[i].indexOf('=') != -1) {
				// System.out.println(
				// "RFC2965CookieParser.isRFC2965CookieString(): isV1 TRUE");
				return (true);
			}
		}

		// System.out.println(
		// "RFC2965CookieParser.isRFC2965CookieString(): isV1 FALSE");
		return (false);
	}

	private static final int countTheDots(final String s) {
		// System.out.println("RFC2965CookieParser.countTheDots(): S=" + s +
		// ",DOTS=" + Utils.countInstances(s,'.'));
		return (Utils.countInstances(s, '.'));
	}
}
