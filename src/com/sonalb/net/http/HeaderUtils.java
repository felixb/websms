package com.sonalb.net.http;

import java.net.URLConnection;
import java.util.Iterator;

/**
 * Utility class with methods relevant to HTTP Headers.
 * 
 * @author Sonal Bansal
 */
public final class HeaderUtils {
	private HeaderUtils() {
	}

	/**
	 * Extracts the headers from the input URLConnection, and populates them in
	 * a Header instance.
	 * 
	 * @param uc
	 *            a connected URLConnection
	 * @return the Header; always non-null
	 */
	public static Header extractHeaders(final URLConnection uc) {
		if (uc == null) {
			throw new IllegalArgumentException("Null URLConnection");
		}

		Header h = new Header();

		h.setTopLine(uc.getHeaderField(0));

		for (int i = 1; uc.getHeaderFieldKey(i) != null; i++) {
			h.add(uc.getHeaderFieldKey(i), uc.getHeaderField(i));
		}

		return (h);
	}

	/**
	 * Converts the Header input, into properties in the URLConnection.
	 * 
	 * @param uc
	 *            an un-connected URLConnection
	 * @param h
	 *            a Header
	 */
	public static void setHeaders(final URLConnection uc, final Header h) {
		if (uc == null) {
			throw new IllegalArgumentException("Null URLConnection");
		}

		if (h == null || h.isEmpty()) {
			return;
		}

		// System.out.println(
		// "HeaderUtils.setHeaders(): I've been asked to set the following Headers = "
		// + h);

		HeaderEntry he;
		Iterator iter = h.iterator();

		while (iter.hasNext()) {
			he = (HeaderEntry) iter.next();
			// System.out.println("HeaderUtils.setHeaders(): Setting Entry -:" +
			// he);
			uc.setRequestProperty(he.getKey(), he.getValue());
		}
	}
}