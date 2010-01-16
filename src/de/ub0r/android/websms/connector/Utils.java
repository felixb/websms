/*
 * Copyright (C) 2010 Felix Bechstein
 * 
 * This file is part of WebSMS.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */

package de.ub0r.android.websms.connector;

/**
 * General Utils calls.
 * 
 * @author flx
 */
public final class Utils {
	/**
	 * No Constructor needed here.
	 */
	private Utils() {
		return;
	}

	/**
	 * Parse a String of "name <number>, name <number>, number, ..." to an array
	 * of "name <number>".
	 * 
	 * @param recipients
	 *            recipients
	 * @return array of recipients
	 */
	public static String[] parseRecipients(final String recipients) {
		String s = recipients.trim();
		if (s.endsWith(",")) {
			s = s.substring(0, s.length() - 1);
		}
		return s.split(",");
	}

	/**
	 * Get a recipient's number.
	 * 
	 * @param recipient
	 *            recipient
	 * @return recipient's number
	 */
	public static String getRecipientsNumber(final String recipient) {
		final int i = recipient.lastIndexOf('<');
		if (i >= 0) {
			final int j = recipient.indexOf('>', i);
			if (j > 0) {
				return recipient.substring(i + 1, j);
			}
		}
		return recipient;
	}

	/**
	 * Get a recipient's name.
	 * 
	 * @param recipient
	 *            recipient
	 * @return recipient's name
	 */
	public static String getRecipientsName(final String recipient) {
		final int i = recipient.lastIndexOf('<');
		if (i >= 0) {
			return recipient.substring(0, i - 1);
		}
		return recipient;
	}

	/**
	 * Clean recipient's phone number from [ -.()].
	 * 
	 * @param recipient
	 *            recipient's mobile number
	 * @return clean number
	 */
	public static String cleanRecipient(final String recipient) {
		if (recipient == null) {
			return "";
		}
		return recipient.replace(" ", "").replace("-", "").replace(".", "")
				.replace("(", "").replace(")", "").replace("<", "").replace(
						">", "").trim();
	}

	/**
	 * Convert international number to national.
	 * 
	 * @param defPrefix
	 *            defualt prefix
	 * @param number
	 *            international number
	 * @return national number
	 */
	public static String international2national(final String defPrefix,
			final String number) {
		if (number.startsWith(defPrefix)) {
			return '0' + number.substring(defPrefix.length());
		}
		return number;
	}

	/**
	 * Convert international number to old format. Eg. +49123 to 0049123
	 * 
	 * @param number
	 *            international number starting with +
	 * @return international number in old format starting with 00
	 */
	public static String international2oldformat(final String number) {
		if (number.startsWith("+")) {
			return "00" + number.substring(1);
		}
		return number;
	}
}
