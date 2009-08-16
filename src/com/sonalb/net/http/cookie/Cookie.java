package com.sonalb.net.http.cookie;

import java.net.URL;
import java.util.Date;

import com.sonalb.Utils;

/*
 * /1.) Portlist determination from requestURL /2.) MaxAge setting requires Base
 * time /3.) Expires is null only if maxage=0 /4.) secure determined from
 * Protocol (HTTPS eg.) /5.) maxage is -ve only if version=0 /6.) Right now an
 * IP address instead of Fully Qualified Domain Name would throw spanner in
 * domain matching ... must discern between IP and name. 7.) Path matching is
 * case-sensitive. Follows from "Cookie Management" section of RFC2965. 8.) If
 * max-age is set to value other than 0 then discard should be false, unless
 * explicitly set.
 * 
 * NOTE: 1.) A cookie with domain=.foo.com will be sent by client to
 * host=y.x.foo.com . However, a cookie sent by y.x.foo.com with Domain=.foo.com
 * will be rejected. 2.) sendWith(URL) should not take into consideration
 * whether cookie has expired. Document this. 3.) Calling setExpires(Date)
 * explicitly causes Cookie version to be reset to '0' 3.1) Fixed ... USOE
 * thrown if version=1 and setExpires called (To maintain consistency).
 * getExpires is allowed for Version 1. 4.) Right now an UnsupportedOpExc is
 * thrown only when get/setMaxAge is called on a Version 0 cookie. V0 also does
 * not support comment,commentURL,discard,port. Do we need to throw exc for
 * these ? 4.1) Made tough cookie ... throws USOE now. However, comment &
 * commentURL field is allowed for version 0 cookie. Just in case some extra
 * info is required to be stored. 5.) According to RFC2965, if Set-Cookie and
 * set-cookie2 both describe the same cookie, then sc2 should be used. This
 * distinction has not been incorporated.
 */

/**
 * The data structure representing a cookie. Supports both Netscape (Version 0)
 * and RFC2965 (Version 1) cookies. The fields common to both these versions are
 * listed below:
 * <ul>
 * <li>NAME - Must be set, no default value</li>
 * <li>VALUE - Default value: Empty</li>
 * <li>Domain - Must be set, default: Local</li>
 * <li>Path - Must be set, default: / (root)</li>
 * <li>Secure - Optional, default: false</li>
 * </ul>
 * 
 * @author Sonal Bansal
 */
public class Cookie implements Cloneable, java.io.Serializable, Comparable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private String value;
	private String comment;
	private URL commentURL;
	private boolean discard;
	private String domain;
	private int maxage;
	private String path;
	private String portList;
	private boolean secure;
	private String version;
	private Date expires;

	private Object lockSecure;
	private Object lockDiscard;
	private Object lockMaxage;

	private boolean bExplicitDomain;
	private boolean bExplicitExpires;
	private boolean bExplicitMaxage;
	private boolean bExplicitPath;
	private boolean bExplicitPort;
	private boolean bPortListSpecified;

	private void initializeFields() {
		this.name = new String();
		this.value = new String();
		this.comment = new String();
		this.commentURL = null;
		this.discard = false;
		this.domain = new String();
		this.maxage = -1;
		this.path = new String();
		this.portList = new String();
		this.secure = false;
		this.version = "1";
		this.expires = new Date(0);

		this.lockSecure = new Object();
		this.lockDiscard = new Object();
		this.lockMaxage = new Object();

		this.bExplicitDomain = this.bExplicitExpires = this.bExplicitMaxage = this.bExplicitPath = this.bExplicitPort = this.bPortListSpecified = false;
	}

	public Object clone() throws CloneNotSupportedException {
		return (super.clone());
	}

	/**
	 * Compares one Cookie with another. The natural ordering is such that it
	 * follows the path specificity rule laid down in RFC2695. Thus more
	 * specific path ("/acme/corp") comes before ("is less than") less specific
	 * path ("/acme").
	 */
	public int compareTo(final Object o) {
		if (o == null || !(o instanceof Cookie)) {
			throw new ClassCastException("Object is null or not a Cookie");
		}

		if (this.equals(o)) {
			return (0);
		}

		Cookie c = (Cookie) o;

		if (!this.isValid() || !c.isValid()) {
			throw new IllegalArgumentException(
					"Either I am an invalid cookie or the argument is.");
		}

		int mySlashes, hisSlashes;
		mySlashes = Utils.countInstances(this.getPath(), '/');
		hisSlashes = Utils.countInstances(c.getPath(), '/');

		if (mySlashes == hisSlashes) {
			if (mySlashes == 1) {
				boolean bMine = "/".equals(this.getPath());
				boolean bHis = "/".equals(c.getPath());

				if (bMine || bHis) {
					if (!(bMine && bHis)) {
						if (bMine) {
							return (1);
						} else {
							return (-1);
						}
					}
				}
			}

			return (0);
		} else if (mySlashes > hisSlashes) {
			return (-1);
		}

		return (1);
	}

	/**
	 * Creates cookie instance.
	 * 
	 * @param name
	 *            the Cookie name
	 * @param value
	 *            the Cookie value
	 * @param domain
	 *            the domain in which this Cookie is valid
	 * @param path
	 *            the path for this Cookie
	 */
	public Cookie(final String name, final String value, final String domain,
			final String path) {
		this(name, value, domain, path, null);
	}

	/**
	 * Creates cookie instance. The path and domain are picked up from the
	 * request URL.
	 * 
	 * @param name
	 *            the Cookie name
	 * @param value
	 *            the Cookie value
	 * @param requestURL
	 *            the request URL which resulted in this cookie being received
	 */
	public Cookie(final String name, final String value, final URL requestURL) {
		this(name, value, null, null, requestURL);
	}

	protected Cookie(final String name, final String value,
			final String domain, final String path, final URL requestURL) {
		this.initializeFields();

		this.setName(name);
		this.setValue(value);
		this.setDomain(domain, requestURL);
		this.setPath(path, requestURL);

		if (requestURL != null) {
			this.setPort(requestURL.getPort());

			String protocol = requestURL.getProtocol();
			if (!Utils.isNullOrWhiteSpace(protocol)) {
				protocol = protocol.toLowerCase();

				if ("https".equals(protocol) || "shttp".equals(protocol)) {
					this.secure = true;
				}
			}
		}
	}

	Cookie() {
		this.initializeFields();
	}

	protected boolean isValid() {
		synchronized (this.name) {
			synchronized (this.domain) {
				synchronized (this.path) {
					if (Utils.isNullOrWhiteSpace(this.name)
							|| Utils.isNullOrWhiteSpace(this.domain)
							|| Utils.isNullOrWhiteSpace(this.path)) {
						return (false);
					}

					return (true);
				}
			}
		}
	}

	/**
	 * Sets the Cookie name.
	 * 
	 * @param name
	 *            the Cookie name
	 */
	public void setName(final String name) {
		if (Utils.isNullOrWhiteSpace(name)) {
			throw new IllegalArgumentException("Cookie can't have empty name.");
		}

		synchronized (this.name) {
			this.name = name;
		}
	}

	/**
	 * Sets the Cookie value.
	 * 
	 * @param value
	 *            the Cookie value
	 */
	public void setValue(final String value) {
		synchronized (this.value) {
			this.value = (value == null) ? "" : value;
		}
	}

	protected void setMaxAge(final int maxage, final Date base,
			final boolean bInsider) {
		if (!bInsider) {
			if ("0".equals(this.version)) {
				throw new UnsupportedOperationException(
						"Version 0 cookies do not support Max-Age");
			}

			if (maxage < 0 || base == null) {
				throw new IllegalArgumentException("Can't set max age");
			}
		}

		if (maxage == 0) {
			synchronized (this.lockMaxage) {
				synchronized (this.expires) {
					synchronized (this.lockDiscard) {
						this.bExplicitMaxage = true;
						this.maxage = 0;
						this.bExplicitExpires = false;
						this.expires = new Date(0);
						this.discard = true;
					}
				}
			}

			return;
		}

		synchronized (this.lockMaxage) {
			synchronized (this.expires) {
				this.bExplicitMaxage = true;
				this.maxage = maxage;
				long expiry = base.getTime() + maxage * 1000;
				this.expires = new Date(expiry);
				this.bExplicitExpires = true;
			}
		}
	}

	/**
	 * Sets the lifetime of this Cookie. Applicable only to Version 1 cookies.
	 * 
	 * @param maxage
	 *            the number of seconds from now that this Cookie is valid
	 *            (delta-t)
	 * @throws UnsupportedOperationException
	 *             when called on a Version 0 cookie
	 */
	public void setMaxAge(final int maxage) {
		this.setMaxAge(maxage, new Date());
	}

	/**
	 * Sets the lifetime of this Cookie. Applicable only to Version 1 cookies.
	 * 
	 * @param maxage
	 *            the number of seconds from base that this Cookie is valid
	 *            (delta-t)
	 * @param base
	 *            the Date from which the delta-t should be counted
	 * @throws UnsupportedOperationException
	 *             when called on a Version 0 cookie
	 */
	public void setMaxAge(final int maxage, final Date base) {
		this.setMaxAge(maxage, base, false);
	}

	protected boolean explicitDomain() {
		return (this.bExplicitDomain);
	}

	protected boolean explicitPath() {
		return (this.bExplicitPath);
	}

	protected boolean explicitPort() {
		return (this.bExplicitPort);
	}

	protected boolean explicitExpires() {
		return (this.bExplicitExpires);
	}

	protected boolean explicitMaxage() {
		return (this.bExplicitMaxage);
	}

	protected boolean portListSpecified() {
		return (this.bPortListSpecified);
	}

	/**
	 * Gets the amount of time this Cookie is valid, measured in seconds from
	 * the time the value was set. Applicable only to Version 0 cookies.
	 * 
	 * @return the delta-t that this Cookie holds valid
	 * @throws UnsupportedOperationException
	 *             if this method is called on a Version 0 cookie
	 */
	public int getMaxAge() {
		if ("0".equals(this.version)) {
			throw new UnsupportedOperationException(
					"Version 0 cookies do not support Max-Age");
		}

		if (!this.bExplicitMaxage) {
			return (-1);
		}

		synchronized (this.lockMaxage) {
			return (this.maxage);
		}
	}

	/**
	 * Sets the date-time when this cookie expires. Applicable only to Version 0
	 * cookies.
	 * 
	 * @param expires
	 *            the Date when this cookie expires
	 * @throws UnsupportedOperationException
	 *             when called on a Version 1 cookie
	 */
	public void setExpires(final Date expires) {
		if ("1".equals(this.version)) {
			throw new UnsupportedOperationException(
					"Version 1 cookies do not support Expires. Use Max-Age.");
		}

		synchronized (this.expires) {
			if (expires == null) {
				this.bExplicitExpires = false;
				this.expires = new Date(0);
			} else {
				this.bExplicitExpires = true;
				this.expires = expires;
			}
		}
	}

	/**
	 * Sets the Cookie version. The version determines what fields and methods
	 * are valid for a Cookie instance. It also determines the format in which
	 * the Cookie is sent with a request.
	 * 
	 * @param version
	 *            the Cookie version. Either "0" or "1"
	 */
	public void setVersion(final String version) {
		if (!("0".equals(version) || "1".equals(version))) {
			throw new IllegalArgumentException("Unsupported cookie version");
		}

		synchronized (this.version) {
			this.version = version;
		}

		if ("0".equals(version)) {
			this.bExplicitMaxage = false;
		} else if (this.maxage == -1) {
			this.bExplicitMaxage = false;
		}
	}

	/**
	 * Gets the version of this Cookie.
	 * 
	 * @return the version; either "0" or "1"
	 */
	public String getVersion() {
		synchronized (this.version) {
			return (this.version);
		}
	}

	protected void setPath(String path, final URL requestURL) {
		if (!Utils.isNullOrWhiteSpace(path)) {
			synchronized (this.path) {
				this.bExplicitPath = true;
				path = path.trim();
				if (path.charAt(0) != '/') {
					path = "/" + path;
				}

				this.path = path;
			}
		} else {
			synchronized (this.path) {
				this.bExplicitPath = false;
				this.path = (requestURL == null) ? null : requestURL.getPath();
				if (Utils.isNullOrWhiteSpace(this.path)) {
					this.path = "/";
				}
				if (this.path.charAt(this.path.length() - 1) != '/') {
					this.path = this.path.substring(0, this.path
							.lastIndexOf('/') + 1);
				}
			}
		}
	}

	/**
	 * Sets the path for this Cookie.
	 * 
	 * @param path
	 *            the Path for this Cookie
	 */
	public void setPath(final String path) {
		this.setPath(path, null);
	}

	/**
	 * Sets the path for this Cookie. Path is extracted from the URL.
	 * 
	 * @param requestURL
	 *            the request URL which caused this Cookie to be sent.
	 */
	public void setPath(final URL requestURL) {
		this.setPath(null, requestURL);
	}

	protected void setDomain(String domain, final URL requestURL) {
		synchronized (this.domain) {
			if (!Utils.isNullOrWhiteSpace(domain)) {
				this.bExplicitDomain = true;
				domain = domain.trim();
				if (!Utils.isIPAddress(domain)) {
					if (domain.charAt(0) != '.') {
						domain = "." + domain;
					}
				}

				this.domain = domain;
			} else {
				this.bExplicitDomain = false;
				this.domain = (requestURL == null) ? null : requestURL
						.getHost();
				if (Utils.isNullOrWhiteSpace(this.domain)) {
					throw new IllegalArgumentException(
							"Could not determine cookie domain.");
				}
			}

			if (this.domain.indexOf('.') == -1) {
				this.domain += ".local";
			}
		}
	}

	/**
	 * Sets the domain for this Cookie. The domain determines which hosts can
	 * receive this Cookie.
	 * 
	 * @param domain
	 *            the Cookie domain
	 */
	public void setDomain(final String domain) {
		this.setDomain(domain, null);
	}

	/**
	 * Sets the domain for this Cookie. The domain determines which hosts can
	 * receive this Cookie.
	 * 
	 * @param requestURL
	 *            the request URL which caused this cookie to be sent
	 */
	public void setDomain(final URL requestURL) {
		this.setDomain(null, requestURL);
	}

	/**
	 * Sets the list of ports to which this Cookie can be sent. Applicable only
	 * to Version 1 cookies.
	 * 
	 * @param ports
	 *            the valid ports as array of int; non-positive values ignored
	 * @throws UnsupportedException
	 *             when called on a Version 0 cookie
	 */
	public void setPortList(final int[] ports) {
		if ("0".equals(this.version)) {
			throw new UnsupportedOperationException(
					"Version 0 cookies do not support Port Matching");
		}

		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < ports.length; i++) {
			if (ports[i] > 0) {
				sb.append(ports[i]);
				sb.append(',');
			}
		}

		sb.deleteCharAt(sb.length() - 1);

		if (sb.length() <= 0) {
			this.bExplicitPort = false;
		}

		synchronized (this.portList) {
			this.bExplicitPort = true;
			this.bPortListSpecified = true;
			this.portList = sb.toString();
		}
	}

	/**
	 * Sets the port to which this cookie can be sent. Applicable only to
	 * Version 1 cookies.
	 * 
	 * @param p
	 *            the Port
	 * @throws UnsupportedOperationException
	 *             when called on a Version 0 cookie
	 */
	public void setPort(final int p) {
		this.setPort(p, null);
	}

	/**
	 * Sets the port to which this cookie can be sent. Port is extracted from
	 * URL. Applicable only to Version 1 cookies.
	 * 
	 * @param url
	 *            the request URL
	 * @throws UnsupportedOperationException
	 *             when called on a Version 0 cookie
	 */
	public void setPort(final URL url) {
		this.setPort(-1, url);
	}

	protected void setPort(int p, final URL url) {
		if ("0".equals(this.version)) {
			throw new UnsupportedOperationException(
					"Version 0 cookies do not support Port Matching.");
		}

		if (p < 0) {
			p = (url == null ? 80 : (url.getPort() == -1 ? 80 : url.getPort()));
			this.bExplicitPort = false;
		}

		synchronized (this.portList) {
			this.bExplicitPort = true;
			this.portList = String.valueOf(p);
		}
	}

	/**
	 * Gets the list of ports to which this cookie can be sent. Applicable only
	 * to Version 1 cookies.
	 * 
	 * @return the comma-separated list of valid ports
	 * @throws UnsupportedOperationException
	 *             when called on a Version 0 cookie
	 */
	public String getPortList() {
		if ("0".equals(this.version)) {
			throw new UnsupportedOperationException(
					"Version 0 cookies do not support Port Matching");
		}

		synchronized (this.portList) {
			return (this.portList);
		}
	}

	/**
	 * Sets whether this cookie should be sent only over secure channels.
	 * 
	 * @param bSecure
	 *            secure or not ?
	 */
	public void setSecure(final boolean bSecure) {
		synchronized (this.lockSecure) {
			this.secure = bSecure;
		}
	}

	/**
	 * Gets the name of this cookie.
	 * 
	 * @return the cookie name
	 */
	public String getName() {
		synchronized (this.name) {
			return (this.name);
		}
	}

	/**
	 * Gets the value of this cookie.
	 * 
	 * @return the cookie value
	 */
	public String getValue() {
		synchronized (this.value) {
			return (this.value);
		}
	}

	/**
	 * Gets the comment for this cookie.
	 * 
	 * @return the comment
	 */
	public String getComment() {
		synchronized (this.comment) {
			return (this.comment);
		}
	}

	/**
	 * Sets the comment for this cookie. Comment has no functional value.
	 * 
	 * @param comment
	 *            the comment
	 */
	public void setComment(final String comment) {
		synchronized (this.comment) {
			this.comment = comment;
		}
	}

	/**
	 * Sets the comment URL for this cookie. URL has no functional value.
	 * 
	 * @param url
	 *            the URL
	 */
	public void setCommentURL(final URL url) {
		synchronized (this.commentURL) {
			this.commentURL = url;
		}
	}

	/**
	 * Gets the comment URL for this cookie.
	 * 
	 * @return the comment URL
	 */
	public URL getCommentURL() {
		synchronized (this.commentURL) {
			return (this.commentURL);
		}
	}

	/**
	 * Gets the date-time when this cookie expires. Note that this can be called
	 * on both Version 0 AND Version 1 cookies.
	 * 
	 * @return the date-time when this cookie expires
	 */
	public Date getExpires() {
		if (!this.bExplicitExpires) {
			return (null);
		}

		synchronized (this.expires) {
			return (this.expires);
		}
	}

	/**
	 * Checks whether this cookie can be discarded once the session is over.
	 * This is different from the lifetime of the cookie.
	 * 
	 * @return discardable or not ?
	 */
	public boolean isDiscardable() {
		if ("0".equals(this.version)) {
			throw new UnsupportedOperationException(
					"Version 0 cookies do not support Discard. Use hasExpired() instead");
		}

		synchronized (this.lockDiscard) {
			synchronized (this.lockMaxage) {
				return (this.discard || this.maxage == 0);
			}
		}
	}

	/**
	 * Sets the discard status of this cookie. This determines whether the
	 * cookie is valid after the session is over. It is different from lifetime.
	 * Applicable only to Version 1 cookies.
	 * 
	 * @param bDiscard
	 *            discardable or not ?
	 * @throws UnsupportedOperationException
	 *             when called on a Version 0 cookie
	 */
	public void setDiscard(final boolean bDiscard) {
		if ("0".equals(this.version)) {
			throw new UnsupportedOperationException(
					"Version 0 cookies do not support Discard.");
		}

		synchronized (this.lockDiscard) {
			this.discard = bDiscard;
		}
	}

	/**
	 * Gets the domain in which this cookie is valid.
	 * 
	 * @return the domain
	 */
	public String getDomain() {
		synchronized (this.domain) {
			return (this.domain);
		}
	}

	/**
	 * Gets the path for this cookie.
	 * 
	 * @return the path
	 */
	public String getPath() {
		synchronized (this.path) {
			return (this.path);
		}
	}

	/**
	 * Checks whether this cookie will be sent over secure channels only.
	 * 
	 * @return secure or not ?
	 */
	public boolean isSecure() {
		synchronized (this.lockSecure) {
			return (this.secure);
		}
	}

	/**
	 * Checks whether this cookie's lifetime has expired or not. The lifetime
	 * has expired if:
	 * <ul>
	 * <li>The Max-Age (for Version 1 cookie) was explicitly set to 0
	 * <li>The delta-t seconds set by Max-Age have passed
	 * <li>The date-time now is greater than what was set to be the expiry
	 * date-time
	 * </ul>
	 * The current system time is used for lifetime calculation.<br>
	 * If none of these conditions are satisfied, or if no explicit lifetime
	 * information was set the cookie is deemed to not have expired.
	 * 
	 * @return expired or not ?
	 */
	public boolean hasExpired() {
		return (this.hasExpired(new Date()));
	}

	/**
	 * Checks whether this cookie's lifetime has expired or not. The lifetime
	 * has expired if:
	 * <ul>
	 * <li>The Max-Age (for Version 1 cookie) was explicitly set to 0
	 * <li>The delta-t seconds set by Max-Age have passed
	 * <li>The date-time as specified by the input is greater than what was set
	 * to be the expiry date-time
	 * </ul>
	 * The input date-time is used for lifetime calculation.<br>
	 * If none of these conditions are satisfied, or if no explicit lifetime
	 * information was set the cookie is deemed to not have expired.
	 * 
	 * @return expired or not ?
	 */
	public boolean hasExpired(final Date d) {
		if (d == null) {
			return (this.hasExpired());
		}

		synchronized (this.expires) {
			if (this.bExplicitExpires == false) {
				if (this.bExplicitMaxage && this.maxage == 0) {
					return (true);
				}

				return (false);
			}

			if ("0".equals(this.version)) {
				if (this.expires.getTime() < d.getTime()) {
					return (true);
				}

				return (false);
			} else {
				if (this.expires.getTime() < d.getTime()) {
					return (true);
				}

				return (false);
			}
		}
	}

	/**
	 * Checks whether two cookies are equal. Two cookies are deemed to be equal,
	 * when all of the following conditions are satisfied:
	 * <ul>
	 * <li>They have the same name</li>
	 * <li>They have the same domain</li>
	 * <li>They have the same path</li>
	 * </ul>
	 * Note that the cookie value is NOT used for equality determination.
	 */
	public boolean equals(final Object obj) {
		if ((obj != null) && (obj instanceof Cookie)) {
			Cookie other = (Cookie) obj;
			return (this.name.equalsIgnoreCase(other.getName())
					&& this.domain.equalsIgnoreCase(other.getDomain()) && this.path
					.equals(other.getPath()));
		}

		return (false);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();

		sb.append("[");
		sb.append("VERSION=\"");
		sb.append(this.version);
		sb.append("\" ; NAME=\"");
		sb.append(this.name);
		sb.append("\" ; VALUE=\"");
		sb.append(this.value);
		sb.append("\" ; DOMAIN=\"");
		sb.append(this.domain);
		if ("1".equals(this.version)) {
			sb.append("\" ; PORTLIST=\"");
			sb.append(this.portList);
		}
		sb.append("\" ; PATH=\"");
		sb.append(this.path);
		if ("1".equals(this.version)) {
			sb.append("\" ; MAX-AGE=\"");
			sb.append(this.maxage);
			sb.append("\" ; DISCARD=\"");
			sb.append(this.discard);
			sb.append("\" ; COMMENT=\"");
			sb.append(this.comment);
			sb.append("\" ; COMMENTURL=\"");
			sb.append((this.commentURL == null) ? "null" : this.commentURL
					.toString());
		}
		sb.append("\" ; SECURE=\"");
		sb.append(this.secure);
		sb.append("\" ; EXPIRES=\"");
		sb.append((this.bExplicitExpires ? this.expires.toString() : "null"));
		sb.append("\"]");

		return (sb.toString());
	}
}