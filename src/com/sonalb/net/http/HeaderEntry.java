package com.sonalb.net.http;

/**
 * Represents a single name-value pair of an HTTP Header.
 * 
 * @author Sonal Bansal
 */
public class HeaderEntry implements Cloneable {
	private String key;
	private String value;

	private HeaderEntry() {
	}

	/**
	 * Creates a HeaderEntry with specified key and value.
	 * 
	 * @param key
	 *            the name; must be non-null
	 * @param value
	 *            the value
	 */
	public HeaderEntry(final String key, final String value) {
		if (key == null) {
			throw new IllegalArgumentException("The Key can't be null");
		}

		this.key = key;
		this.value = value;
	}

	/**
	 * Gets the Key/Name.
	 */
	public String getKey() {
		return (this.key);
	}

	/**
	 * Gets the Value.
	 */
	public String getValue() {
		return (this.value);
	}

	public boolean equals(final Object o) {
		int i = 0;

		if (o instanceof HeaderEntry) {
			HeaderEntry x = (HeaderEntry) o;

			if (this.key.equalsIgnoreCase(x.getKey())) {
				i++;
			}

			if (this.value != null) {
				if (this.value.equals(x.getValue())) {
					i++;
				}
			} else if (x.getValue() == null) {
				i++;
			}
		}

		if (i != 2) {
			return (false);
		}

		return (true);
	}

	public String toString() {
		return (this.key + ":" + this.value);
	}

	public Object clone() throws CloneNotSupportedException {
		return (super.clone());
	}
}