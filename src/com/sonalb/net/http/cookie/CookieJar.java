package com.sonalb.net.http.cookie;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import com.sonalb.Utils;

/*
 * According to RFC2965, if Set-Cookie and set-cookie2 both describe the same
 * cookie, then sc2 should be used. This distinction has not been incorporated.
 * 
 * ADD function to getcookies by version
 */

/**
 * Container for <code>Cookie</code> objects. Each CookieJar is independent of
 * any request. This means that a single CookieJar can hold all the cookies for
 * a number of requests and servers.
 * 
 * @author Sonal Bansal
 */

public class CookieJar implements java.util.Collection, java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Vector theJar;
	private int iNumCookies;

	/**
	 * Creates an empty CookieJar.
	 */
	public CookieJar() {
		this.theJar = new Vector();
		this.iNumCookies = 0;
	}

	/**
	 * Creates a CookieJar, and populates it with Cookies from input Collection.
	 * All the objects in the input Collection NEED NOT be Cookie objects.
	 * 
	 * @param c
	 *            the input Collection
	 */
	public CookieJar(final Collection c) {
		this.theJar = new Vector();
		this.iNumCookies = 0;
		this.addAll(c);
	}

	protected CookieJar(final int initialCapacity, final int growthStep) {
		this.theJar = new Vector(initialCapacity, growthStep);
		this.iNumCookies = 0;
	}

	public boolean add(final Object o) {
		if (o == null) {
			throw new IllegalArgumentException("Null cookie.");
		} else if (!(o instanceof Cookie)) {
			throw new ClassCastException("Not a Cookie.");
		}

		Cookie cookie;

		try {
			cookie = (Cookie) ((Cookie) o).clone();
		} catch (CloneNotSupportedException cnse) {
			throw new IllegalArgumentException(
					"Could not add. Object does not support Cloning.");
		}

		if (!cookie.isValid()) {
			throw new IllegalArgumentException("Invalid cookie.");
		}

		int ind = this.getCookieIndex(cookie);

		if (ind == -1) {
			this.theJar.add(cookie);
			this.iNumCookies++;
		} else {
			this.theJar.setElementAt(cookie, ind);
		}

		return (true);
	}

	public boolean addAll(final Collection c) {
		if (c == null) {
			throw new IllegalArgumentException("Null Collection");
		}

		if (!c.isEmpty()) {
			Iterator iter = c.iterator();

			while (iter.hasNext()) {
				try {
					this.add(iter.next());
				} catch (Exception e) {
				}
			}
		} else {
			return (false);
		}

		return (true);
	}

	public Iterator iterator() {
		return (this.theJar.iterator());
	}

	public boolean contains(final Object o) {
		if (o == null) {
			throw new IllegalArgumentException("Null cookie");
		} else if (!(o instanceof Cookie)) {
			throw new ClassCastException("Not a cookie");
		}

		Cookie c = (Cookie) o;

		if (!c.isValid()) {
			throw new IllegalArgumentException("Invalid cookie.");
		}

		return (this.theJar.contains(c));
	}

	public boolean containsAll(final Collection c) {
		if (c != null) {
			Iterator iter = c.iterator();

			while (iter.hasNext()) {
				if (!this.contains(iter.next())) {
					return (false);
				}
			}
		} else {
			throw new IllegalArgumentException("Null collection");
		}

		return (true);
	}

	public Object[] toArray() {
		return (this.theJar.toArray());
	}

	public Object[] toArray(final Object[] array) {
		if (array == null) {
			throw new IllegalArgumentException("Null array.");
		}

		Cookie[] cookieArray = new Cookie[array.length];

		try {
			for (int i = 0; i < array.length; i++) {
				cookieArray[i] = (Cookie) array[i];
			}
		} catch (ClassCastException cce) {
			throw new ArrayStoreException("ClassCastException occurred.");
		}

		return (this.theJar.toArray(cookieArray));
	}

	public void clear() {
		this.theJar.clear();
		this.iNumCookies = 0;
	}

	public boolean removeAll(final Collection c) {
		if (c == null) {
			throw new IllegalArgumentException("Null collection");
		}

		if (!c.isEmpty()) {
			Iterator iter = c.iterator();

			while (iter.hasNext()) {
				this.remove(iter.next());
			}
		} else {
			return (false);
		}

		return (true);
	}

	public boolean retainAll(final Collection c) {
		if (c == null) {
			throw new IllegalArgumentException("Null collection");
		}

		if (!c.isEmpty()) {
			Iterator iter = c.iterator();
			Object o;

			while (iter.hasNext()) {
				o = iter.next();
				if (!this.contains(o)) {
					this.remove(o);
				}
			}
		} else {
			return (false);
		}

		return (true);
	}

	public boolean remove(final Object o) {
		if (o == null) {
			throw new IllegalArgumentException("Null cookie.");
		} else if (!(o instanceof Cookie)) {
			throw new ClassCastException("Not a cookie.");
		}

		Cookie cookie = (Cookie) o;

		if (!cookie.isValid()) {
			throw new IllegalArgumentException("Invalid cookie.");
		}

		return (this.theJar.remove(cookie));
	}

	/**
	 * Removes all cookies that match the given CookieMatcher.
	 * 
	 * @param cm
	 *            the CookieMatcher
	 */
	public void removeCookies(final CookieMatcher cm) {
		if (cm == null) {
			throw new IllegalArgumentException("Null CookieMatcher");
		}

		Cookie c;

		for (int i = 0; i < this.iNumCookies; i++) {
			c = (Cookie) this.theJar.get(i);
			if (cm.doMatch(c)) {
				this.theJar.removeElementAt(i);
				this.iNumCookies--;
			}
		}
	}

	protected int getCookieIndex(final Cookie c) {
		int retVal = -1;

		for (int i = 0; i < this.iNumCookies; i++) {
			if (c.equals(this.theJar.get(i))) {
				retVal = i;
				break;
			}
		}

		return (retVal);
	}

	public int size() {
		if (this.iNumCookies > Integer.MAX_VALUE) {
			return (Integer.MAX_VALUE);
		}

		return (this.iNumCookies);
	}

	public boolean isEmpty() {
		return (this.iNumCookies == 0);
	}

	/**
	 * Gets all Cookies that match the given CookieMatcher.
	 * 
	 * @param cm
	 *            the CookieMatcher
	 * @return the CookieJar with matching cookies; always non-null
	 */
	public CookieJar getCookies(final CookieMatcher cm) {
		if (cm == null) {
			throw new IllegalArgumentException("Invalid CookieMatcher");
		}

		CookieJar cj = new CookieJar();
		Cookie c;

		for (int i = 0; i < this.iNumCookies; i++) {
			c = (Cookie) this.theJar.get(i);
			if (cm.doMatch(c)) {
				cj.add(c);
			}
		}

		return (cj);
	}

	/**
	 * Gets all Cookies with the given name.
	 * 
	 * @param cookieName
	 *            the cookie name
	 * @return the CookieJar with matching cookies; always non-null
	 */
	public CookieJar getCookies(final String cookieName) {
		if (Utils.isNullOrWhiteSpace(cookieName)) {
			throw new IllegalArgumentException("Name cannot be empty");
		}

		CookieJar cj = new CookieJar();
		Cookie c;

		for (int i = 0; i < this.iNumCookies; i++) {
			c = (Cookie) this.theJar.get(i);
			if (cookieName.equalsIgnoreCase(c.getName())) {
				cj.add(c);
			}
		}

		return (cj);
	}

	/**
	 * Gets all Cookies having given version.
	 * 
	 * @param ver
	 *            the version
	 * @return the CookieJar with Cookies; always non-null
	 */
	public CookieJar getVersionCookies(final String ver) {
		CookieJar cj = new CookieJar();
		Cookie c;

		for (int i = 0; i < this.iNumCookies; i++) {
			c = (Cookie) this.theJar.get(i);
			if (c.getVersion().equals(ver)) {
				cj.add(c);
			}
		}

		return (cj);
	}

	public String toString() {
		if (this.isEmpty()) {
			return ("{}");
		}

		StringBuffer sb = new StringBuffer();

		sb.append("{");

		for (int i = 0; i < this.iNumCookies; i++) {
			sb.append(this.theJar.get(i).toString());
			sb.append(",");
		}

		sb.deleteCharAt(sb.length() - 1);
		sb.append("}");

		return (sb.toString());
	}
}