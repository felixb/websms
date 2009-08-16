package com.sonalb;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.StringTokenizer;

/**
 * Utility class containing several general-purpose methods for system-wide
 * consumption.
 * 
 * @author Sonal Bansal
 */

public abstract class Utils {
	private Utils() {
	}

	/**
	 * Returns true if argument is either <code>null</code> or is full of
	 * whitespace. The argument is full of whitespace if after calling
	 * <code>String.trim()</code> on it, it equals(""). In other words,
	 * whitespace determination is done by <code>String.trim()</code>.
	 * 
	 * @param s
	 *            the String to be tested.
	 * @see java.lang.String#trim()
	 */
	public static boolean isNullOrWhiteSpace(final String s) {
		if (s == null || "".equals(s.trim())) {
			return (true);
		}

		return (false);
	}

	/**
	 * Returns the String obtained by removing any whitespace at both ends of
	 * the argument. Whitespace is defined as stated by
	 * <code>Character.isWhitespace(char)</code>. It is different from
	 * <code>String.trim()</code> which also removes any control characters.
	 * 
	 * @param s
	 *            the String to be trimmed.
	 * @return the trimmed String.
	 * @see java.lang.Character#isWhitespace(char)
	 * @see java.lang.String#trim()
	 */
	public static String trimWhitespace(final String s) {
		if (s == null) {
			return (s);
		}

		return (trimRightWS(trimLeftWS(s)));
	}

	/**
	 * Returns the String obtained by removing any whitespace at the left hand
	 * side of the argument. Whitespace is defined as stated by
	 * <code>Character.isWhitespace(char)</code>.
	 * 
	 * @see java.lang.Character#isWhitespace(char)
	 * @param s
	 *            the String to be trimmed.
	 * @return the trimmed String.
	 */
	public static String trimLeftWS(final String s) {
		if (s == null) {
			return (s);
		}

		for (int i = 0; i < s.length(); i++) {
			if (!Character.isWhitespace(s.charAt(i))) {
				return (s.substring(i));
			}
		}

		return (s);
	}

	/**
	 * Returns the String obtained by removing any whitespace at the right hand
	 * side of the argument. Whitespace is defined as stated by
	 * <code>Character.isWhitespace(char)</code>.
	 * 
	 * @see java.lang.Character#isWhitespace(char)
	 * @param s
	 *            the String to be trimmed.
	 * @return the trimmed String.
	 */
	public static String trimRightWS(final String s) {
		if (s == null) {
			return (s);
		}

		for (int i = s.length() - 1; i >= 0; i--) {
			if (!Character.isWhitespace(s.charAt(i))) {
				return (s.substring(0, i + 1));
			}
		}

		return (s);
	}

	/**
	 * Convenience synonym for <code>trimWhitespace(String)</code>.
	 * 
	 * @see #trimWhitespace(String)
	 * @param s
	 *            the String to be trimmed.
	 * @return the trimmed String.
	 */
	public static String trimWS(final String s) {
		return (trimWhitespace(s));
	}

	/**
	 * Tokenizes the argument String into several constituent Strings, separated
	 * by commas, and returns the tokens as an array.
	 * 
	 * @see #delimitedStringToArray(String, String)
	 * @param s
	 *            the String to be tokenized.
	 * @return the array of token Strings.
	 */
	public static String[] csvStringToArray(final String s) {
		return (delimitedStringToArray(s, ","));
	}

	/**
	 * Tokenizes the argument String into several constituent Strings, separated
	 * by specified delimiters, and returns the tokens as an array. Every
	 * character of the delimiter String is treated as a token-separator
	 * individually.
	 * 
	 * @param s
	 *            the String to be tokenized.
	 * @param delimiters
	 *            the String of delimiters.
	 * @return the array of token Strings.
	 */
	public static String[] delimitedStringToArray(final String s,
			String delimiters) {
		if (isNullOrWhiteSpace(s)) {
			return (null);
		}

		delimiters = (delimiters == null) ? ",;" : delimiters;

		StringTokenizer st = new StringTokenizer(s, delimiters);
		int num;

		if ((num = st.countTokens()) == 0) {
			return (null);
		}

		String array[] = new String[num];

		for (int i = 0; i < num; i++) {
			array[i] = st.nextToken();
		}

		return (array);
	}

	/**
	 * Checks whether the specified Object exists in the given array. The
	 * <code>Object.equals(Object)</code> method is used to test for equality.
	 * 
	 * @see java.lang.Object#equals(Object)
	 * @param obj
	 *            the Object to be searched for.
	 * @param array
	 *            the array which has to be searched.
	 * @return <code>true</code> if the Object exists in the array;
	 *         <code>false</code> if the Object does not exist in the array, or
	 *         the object, or the array is <code>null</code>.
	 */
	public static boolean isInArray(final Object obj, final Object[] array) {
		if (obj == null || array == null) {
			return (false);
		}

		for (int i = 0; i < array.length; i++) {
			if (array[i].equals(obj)) {
				return (true);
			}
		}

		return (false);
	}

	/**
	 * Counts the number of times the specified character appears in the input
	 * String. For example, <code>countInstances("abcabcdabcde",'a')</code>
	 * returns 3.
	 * 
	 * @param s
	 *            the String to be counted in.
	 * @param c
	 *            the character to be counted.
	 * @return the number of occurrences of <code>c</code> in <code>s</code> ;
	 *         -1 if <code>s</code> is <code>null</code>.
	 */
	public static int countInstances(final String s, final char c) {
		if (isNullOrWhiteSpace(s)) {
			return (-1);
		}

		int count = 0;

		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == c) {
				count++;
			}
		}

		return (count);
	}

	/**
	 * Counts the number of times the specified String appears in the input
	 * String. For example, <code>countInstances("abcabcdabcde","cd")</code>
	 * returns 2.
	 * 
	 * @param s
	 *            the String to be counted in.
	 * @param c
	 *            the String to be counted.
	 * @return the number of occurrences of <code>c</code> in <code>s</code> ;
	 *         -1 if <code>s</code> or <code>c</code> is <code>null</code>.
	 */
	public static int countInstances(final String s, final String c) {
		if (isNullOrWhiteSpace(s) || isNullOrWhiteSpace(c)) {
			return (-1);
		}

		int count = 0;
		int len = c.length();

		for (int i = 0; i < s.length(); i += len) {
			if ((i = s.indexOf(c, i)) != -1) {
				count++;
			} else {
				break;
			}
		}

		return (count);
	}

	/**
	 * Checks whether the input String is a valid IP address (quad).
	 * 
	 * @param s
	 *            the String to be checked.
	 * @return <code>true</code> if the String IS an IP address ;
	 *         <code>false</code> if it is not, or <code>s</code> is
	 *         <code>null</code>.
	 * @see java.net.InetAddress
	 */
	public static boolean isIPAddress(String s) {
		if (isNullOrWhiteSpace(s) || s.indexOf('.') == -1) {
			return (false);
		}

		s = s.trim();

		int index, dotCount = 0, dotPos[] = new int[5];
		char ch;

		dotPos[0] = -1;
		dotPos[4] = s.length();

		for (index = 0; index < s.length() && dotCount < 4; index++) {
			ch = s.charAt(index);
			if (!Character.isDigit(ch) && ch != '.') {
				return (false);
			} else if (ch == '.') {
				dotPos[++dotCount] = index;
			}
		}

		if (dotCount != 3) {
			return (false);
		}

		try {
			String num;
			int number;

			for (dotCount = 1; dotCount < 5; dotCount++) {
				index = dotPos[dotCount - 1] + 1;
				num = s.substring(index, dotPos[dotCount]);
				number = Integer.valueOf(num).intValue();

				if (number < 0 || number > 255) {
					return (false);
				}
			}
		} catch (Exception e) {
			return (false);
		}

		return (true);
	}

	/**
	 * Converts the input Date into a String formatted as specified by RFC 1123
	 * of the IETF. The output String is in the format
	 * "Sun, 06 Nov 1994 08:49:37 GMT".
	 * 
	 * @param d
	 *            the Date to be converted.
	 * @return the String representation of the date as specified by RFC1123 ;
	 *         <code>null</code> if input is <code>null</code>.
	 * @see java.util.Date
	 */
	public static String convertToHttpDate(final Date d) {
		if (d == null) {
			return (null);
		}

		SimpleDateFormat sdf = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss zzz");
		sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
		return (sdf.format(d));
	}

	/**
	 * Parses the input date String into a Date object. The input date String is
	 * expected to be in one of the following formats (note the spaces) :-
	 * <p>
	 * <ul>
	 * <li>"Sun, 06 Nov 1994 08:49:37 GMT" ; RFC 822, updated by RFC 1123</li>
	 * <li>"Sunday, 06-Nov-94 08:49:37 GMT" ; RFC 850, obsoleted by RFC 1036</li>
	 * <li>"Sunday, 06-Nov-1994 08:49:37 GMT" ; RFC 1036</li>
	 * <li>"Sun Nov  6 08:49:37 1994" ; ANSI C's asctime() format</li>
	 * </ul>
	 * 
	 * @param date
	 *            the HTTP date String to be converted.
	 * @return the parsed Date object ; <code>null</code> if parsing was
	 *         unsuccessful or input was <code>null</code>.
	 */
	public static Date parseHttpDateStringToDate(final String date) {
		if (date == null) {
			return (null);
		}

		StringTokenizer st = new StringTokenizer(date.trim(), " ");
		int iNumTokens = st.countTokens();
		String format = null;

		if (iNumTokens == 5) // Its an asctime date ... wkday SP month SP (
		// 2DIGIT | ( SP 1DIGIT )) SP time SP 4DIGIT
		{ // ; month day (e.g., Jun 2)
			// Sun Nov 6 08:49:37 1994
			format = "EEE MMM dd HH:mm:ss yyyy";
		} else if (iNumTokens == 4) // Its an RFC850 date ... weekday "," SP
		// 2DIGIT "-" month "-" 2DIGIT SP time SP
		// "GMT"
		{ // ; day-month-year (e.g., 02-Jun-82)
			String dtok = null; // may have 2 or 4-digit year
			st.nextToken();
			dtok = st.nextToken().trim();
			if (dtok.length() == 9) // has 2-digit year
			{
				// Sunday, 06-Nov-94 08:49:37 GMT
				format = "EEEE, dd-MMM-yy HH:mm:ss zzz";
			} else if (dtok.length() == 11) // has 4-digit year
			{
				// Sunday, 06-Nov-1994 08:49:37 GMT
				format = "EEEE, dd-MMM-yyyy HH:mm:ss zzz";
			} else {
				return (null);
			}
		} else if (iNumTokens == 6) // Its an RFC1123 date ... wkday "," SP
		// 2DIGIT SP month SP 4DIGIT SP time SP
		// "GMT"
		{ // ; day-month-year (e.g., 02 Jun 1982)
			// Sun, 06 Nov 1994 08:49:37 GMT
			format = "EEE, dd MMM yyyy HH:mm:ss zzz";
		} else {
			return (null);
		}

		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
		Date d = sdf.parse(date, new ParsePosition(0));

		return (d);
	}

	/**
	 * Returns true if argument is either <code>null</code> or it equals(""). It
	 * does not trim the input, unlike <code>isNullOrWhiteSpace(String)</code>.
	 * 
	 * @param s
	 *            the String to be tested.
	 * @see #isNullOrWhiteSpace(String)
	 */
	public static boolean isEmpty(final String s) {
		return (s == null || s.equals(""));
	}

	/**
	 * Replaces all occurrences of a String in the input String with another
	 * String.
	 * 
	 * @param source
	 *            the String in which the replacements are to be carried out.
	 * @param find
	 *            the String which must be searched for.
	 * @param replace
	 *            the String which replaces the search String.
	 * @param bIgnoreCase
	 *            determines whether case-sensitive search is done.
	 * @return the processed String if any replacements were made ; the original
	 *         String if search String was not found or search String is empty.
	 * @throws IllegalArgumentException
	 *             Thrown if source String is empty.
	 */
	public static String replaceAll(String source, String find, String replace,
			final boolean bIgnoreCase) throws IllegalArgumentException {
		if (isEmpty(source)) {
			throw new IllegalArgumentException("Empty source String");
		} else if (isEmpty(find)) {
			return (source);
		}

		if (replace == null) {
			replace = "";
		}

		StringBuffer sb = new StringBuffer(source);
		StringBuffer mod;
		boolean bDone = false;
		int prevIndex = 0, currIndex = 0, i = 0;

		if (bIgnoreCase) {
			source = source.toLowerCase();
			find = find.toLowerCase();
		}

		mod = new StringBuffer(source);

		while (!bDone) {
			if ((currIndex = mod.toString().indexOf(find, prevIndex)) != -1) {
				sb = sb.replace(currIndex, currIndex + find.length(), replace);
				mod = mod
						.replace(currIndex, currIndex + find.length(), replace);
				prevIndex = currIndex + replace.length();
			} else {
				bDone = true;
			}
		}

		return (sb.toString());
	}

	/**
	 * Determines whether every double-quote has its closing partner in the
	 * input String.
	 * 
	 * @param s
	 *            the String to be tested.
	 * @return <code>true</code> if quotes match or no quotes exist ;
	 *         <code>false</code> otherwise.
	 */
	public static boolean matchQuotes(final String s) {
		int numQuotes = 0, numEscapedQuotes = 0;

		numQuotes = countInstances(s, '\"');
		numEscapedQuotes = countInstances(s, "\\\"");

		if (numQuotes == -1) {
			return (true);
		}

		if (numEscapedQuotes == -1) {
			numEscapedQuotes = 0;
		}

		return (((numQuotes - numEscapedQuotes) % 2) == 0);
	}

	/**
	 * Removes the outermost double-quotes pair from the input String. Trims the
	 * input String before processing.
	 * 
	 * @param s
	 *            the String to be stripped.
	 * @return the stripped String ; the original String if it is not quoted or
	 *         quotes don't match.
	 */
	public static String stripQuotes(final String s) {
		if (isNullOrWhiteSpace(s) || !matchQuotes(s)) {
			return (s);
		}

		String s2 = trimWhitespace(s);

		if (isQuoted(s2)) {
			return (s2.substring(1, s2.length() - 1));
		}

		return (s);
	}

	/**
	 * Determines whether the input String is enclosed in double-quotes. Trims
	 * the input String first.
	 * 
	 * @param s
	 *            the String to be tested.
	 * @return <code>true</code> if input String is quoted ; <code>false</code>
	 *         if it isn't or it is empty.
	 * @see #trimWhitespace(String)
	 */
	public static boolean isQuoted(final String s) {
		if (isNullOrWhiteSpace(s) || !matchQuotes(s)) {
			return (false);
		}

		String s2 = trimWhitespace(s);

		return (s2.charAt(0) == '\"' && s.charAt(s2.length() - 1) == '\"');
	}

	/**
	 * Converts the input integer into a more readable comma-separated String. A
	 * comma is inserted after every three digits, starting from the unit's
	 * place. For example,
	 * <p>
	 * 
	 * <pre>
	 * 	12345		is converted to 	12,345
	 * 	987654321	is converted to 	987,654,321
	 * </pre>
	 * 
	 * @param i
	 *            the integer to be converted.
	 * @return the String representation of the formatted integer ; if the input
	 *         lies between -1000 & 1000 (not inclusive) , the returned String
	 *         is the same as <code>String.valueOf(int)</code>
	 * @see java.lang.String#valueOf(int)
	 */
	public static String commaFormatInteger(int i) {
		boolean bNegative = (i < 0);

		if (i == 0 || (i > 0 && i < 1000) || (i < 0 && i > -1000)) {
			return (String.valueOf(i));
		}

		i = Math.abs(i);

		StringBuffer sb = new StringBuffer(String.valueOf(i));
		sb.reverse();

		int l = sb.length();
		int iter = l / 3;

		if (l % 3 == 0) {
			iter--;
		}

		for (int c = 1; c <= iter; c++) {
			l = 3 * c;
			sb.insert(l + c - 1, ',');
		}

		if (bNegative) {
			sb.append('-');
		}

		return (sb.reverse().toString());
	}

	/**
	 * Returns the number which would be in the n<sup>th</sup> place if the
	 * input array were sorted in descending order. For arrays with distinct
	 * elements, this returns the n<sup>th</sup> largest number. For example, in
	 * the array {1,2,3,4,5,6}, <code>findOrderedMax(list, 3)</code> would
	 * return 4. However, the result is quite different in arrays with repeated
	 * elements. For example, in the array {1,2,6,2}, both
	 * <code>findOrderedMax(list,2)</code> and
	 * <code>findOrderedMax(list,3)</code> return 2.
	 * 
	 * @see #findMax(int[],int)
	 * @throws IllegalArgumentException
	 *             Thrown when the number of elements in the input array is less
	 *             than the desired order, that is, when <code>n</code> exceeds
	 *             <code>inputlist.length</code>. Or, when the order
	 *             <code>n</code> is less than or equal to 0.
	 * @param inputlist
	 *            the array of numbers.
	 * @param n
	 *            the desired position of sorted array. For example, 1 indicates
	 *            first from top of sorted array; 2 indicates second from top,
	 *            and so on.
	 */
	public static int findOrderedMax(final int inputlist[], final int n) {
		if (inputlist.length < n) {
			throw new IllegalArgumentException(
					"Input array should have atleast input order numbers.");
		}

		if (n <= 0) {
			throw new IllegalArgumentException(
					"Order cannot be less than or equal to zero.");
		}

		int numIters, i;
		int maxIndex, temp;
		int list[] = new int[inputlist.length];

		System.arraycopy(inputlist, 0, list, 0, list.length);

		for (numIters = 0; numIters < n; numIters++) {
			maxIndex = 0;

			for (i = 0; i < (list.length - numIters); i++) {
				if (list[i] > list[maxIndex]) {
					maxIndex = i;
				}
			}

			temp = list[i - 1];
			list[i - 1] = list[maxIndex];
			list[maxIndex] = temp;
		}

		return (list[list.length - n]);
	}

	/**
	 * Returns the n<sup>th</sup> largest number in the input array. This method
	 * disregards duplicate elements unlike the <code>findOrderedMax</code>
	 * method. For example, in the array {1,2,3,4,5,6},
	 * <code>findMax(list, 3)</code> would return 4. In the array {1,2,6,2},
	 * <code>findMax(list,2)</code> would return 2, and and
	 * <code>findMax(list,3)</code> would return 1.
	 * <p>
	 * Note that in an array with repeating elements, it is possible that the
	 * required order may not exist. For example, in the array {1,2,2},
	 * <code>findMax(list,3)</code>, there is no third-largest number. In such
	 * cases, this method returns the number with highest order less than or
	 * equal to <code>n</code>. In other words, in the above example, this
	 * method would return 1, which is the same value as would be returned by
	 * <code>findMax(list,2)</code>.
	 * 
	 * @see #findOrderedMax(int[],int)
	 * @throws IllegalArgumentException
	 *             Thrown when the number of elements in the input array is less
	 *             than the desired order, that is, when <code>n</code> exceeds
	 *             <code>inputlist.length</code>. Or, when the order
	 *             <code>n</code> is less than or equal to 0.
	 * @param inputlist
	 *            the array of numbers.
	 * @param n
	 *            the desired order. For example, 1 indicates largest number; 2
	 *            indicates second-largest number, and so on.
	 */
	public static int findMax(final int inputlist[], final int n) {
		if (inputlist.length < n) {
			throw new IllegalArgumentException(
					"Input array should have atleast input order numbers.");
		}

		if (n <= 0) {
			throw new IllegalArgumentException(
					"Order cannot be less than or equal to zero.");
		}

		int numIters, i;
		int maxIndex, temp;
		int order[] = new int[n];
		int oindex = 0;
		int currIter = 0;
		int list[] = new int[inputlist.length];

		System.arraycopy(inputlist, 0, list, 0, list.length);

		for (numIters = 0; numIters < list.length; numIters++) {
			maxIndex = 0;

			for (i = 0; i < (list.length - numIters); i++) {
				if (list[i] > list[maxIndex]) {
					maxIndex = i;
				}
			}

			temp = list[maxIndex];
			list[maxIndex] = list[i - 1];
			list[i - 1] = temp;

			if (oindex == 0 || order[oindex - 1] != temp) {
				order[oindex++] = temp;
				currIter++;
			}

			if (currIter == n) {
				break;
			}
		}

		return (order[oindex - 1]);
	}

	/**
	 * Returns the number which would be in the n<sup>th</sup> place if the
	 * input array were sorted in ascending order. For arrays with distinct
	 * elements, this returns the n<sup>th</sup> smallest number. For example,
	 * in the array {1,2,3,4,5,6}, <code>findOrderedMin(list, 3)</code> would
	 * return 3. However, the result is quite different in arrays with repeated
	 * elements. For example, in the array {1,2,6,2}, both
	 * <code>findOrderedMin(list,2)</code> and
	 * <code>findOrderedMin(list,3)</code> return 2.
	 * 
	 * @see #findMin(int[],int)
	 * @throws IllegalArgumentException
	 *             Thrown when the number of elements in the input array is less
	 *             than the desired order, that is, when <code>n</code> exceeds
	 *             <code>inputlist.length</code>. Or, when the order
	 *             <code>n</code> is less than or equal to 0.
	 * @param inputlist
	 *            the array of numbers.
	 * @param n
	 *            the desired position of sorted array. For example, 1 indicates
	 *            first from top of sorted array; 2 indicates second from top,
	 *            and so on.
	 */
	public static int findOrderedMin(final int inputlist[], final int n) {
		if (inputlist.length < n) {
			throw new IllegalArgumentException(
					"Input array should have atleast input order numbers.");
		}

		if (n <= 0) {
			throw new IllegalArgumentException(
					"Order cannot be less than or equal to zero.");
		}

		int numIters, i;
		int minIndex, temp;
		int list[] = new int[inputlist.length];

		System.arraycopy(inputlist, 0, list, 0, list.length);

		for (numIters = 0; numIters < n; numIters++) {
			minIndex = 0;

			for (i = 0; i < (list.length - numIters); i++) {
				if (list[i] < list[minIndex]) {
					minIndex = i;
				}
			}

			temp = list[i - 1];
			list[i - 1] = list[minIndex];
			list[minIndex] = temp;
		}

		return (list[list.length - n]);
	}

	/**
	 * Returns the n<sup>th</sup> smallest number in the input array. This
	 * method disregards duplicate elements unlike the
	 * <code>findOrderedMin</code> method. For example, in the array
	 * {1,2,3,4,5,6}, <code>findMin(list, 3)</code> would return 3. In the array
	 * {1,2,6,2}, <code>findMin(list,2)</code> would return 2, and and
	 * <code>findMin(list,3)</code> would return 6.
	 * <p>
	 * Note that in an array with repeating elements, it is possible that the
	 * required order may not exist. For example, in the array {1,2,2},
	 * <code>findMin(list,3)</code>, there is no third-smallest number. In such
	 * cases, this method returns the number with highest order less than or
	 * equal to <code>n</code>. In other words, in the above example, this
	 * method would return 2, which is the same value as would be returned by
	 * <code>findMax(list,2)</code>.
	 * 
	 * @see #findOrderedMin(int[],int)
	 * @throws IllegalArgumentException
	 *             Thrown when the number of elements in the input array is less
	 *             than the desired order, that is, when <code>n</code> exceeds
	 *             <code>inputlist.length</code>. Or, when the order
	 *             <code>n</code> is less than or equal to 0.
	 * @param inputlist
	 *            the array of numbers.
	 * @param n
	 *            the desired order. For example, 1 indicates smallest number; 2
	 *            indicates second-smallest number, and so on.
	 */
	public static int findMin(final int inputlist[], final int n) {
		if (inputlist.length < n) {
			throw new IllegalArgumentException(
					"Input array should have atleast input order numbers.");
		}

		if (n <= 0) {
			throw new IllegalArgumentException(
					"Order cannot be less than or equal to zero.");
		}

		int numIters, i;
		int minIndex, temp;
		int order[] = new int[n];
		int oindex = 0;
		int currIter = 0;
		int list[] = new int[inputlist.length];

		System.arraycopy(inputlist, 0, list, 0, list.length);

		for (numIters = 0; numIters < list.length; numIters++) {
			minIndex = 0;

			for (i = 0; i < (list.length - numIters); i++) {
				if (list[i] < list[minIndex]) {
					minIndex = i;
				}
			}

			temp = list[minIndex];
			list[minIndex] = list[i - 1];
			list[i - 1] = temp;

			if (oindex == 0 || order[oindex - 1] != temp) {
				order[oindex++] = temp;
				currIter++;
			}

			if (currIter == n) {
				break;
			}
		}

		return (order[oindex - 1]);
	}

	/**
	 * Returns the object which would be in the n<sup>th</sup> place if the
	 * input array were sorted in descending order. This method allows
	 * comparison of <code>Object</code>s. It follows similar semantics as the
	 * <code>findOrderedMax(int[], int)</code> method.
	 * 
	 * @see #findMax(Comparable[],int)
	 * @see #findOrderedMax(int[],int)
	 * @throws IllegalArgumentException
	 *             Thrown when the number of elements in the input array is less
	 *             than the desired order, that is, when <code>n</code> exceeds
	 *             <code>inputlist.length</code>. Or, when the order
	 *             <code>n</code> is less than or equal to 0.
	 * @param inputlist
	 *            the array of <code>Comparable</code> objects. All elements
	 *            must be <i>mutually Comparable</i> (that is, e1.compareTo(e2)
	 *            must not throw a ClassCastException for any elements e1 and e2
	 *            in the array).
	 * @param n
	 *            the desired position of sorted array. For example, 1 indicates
	 *            first from top of sorted array; 2 indicates second from top,
	 *            and so on.
	 */
	public static Object findOrderedMax(final Comparable inputlist[],
			final int n) {
		if (inputlist.length < n) {
			throw new IllegalArgumentException(
					"Input array should have atleast input order numbers.");
		}

		if (n <= 0) {
			throw new IllegalArgumentException(
					"Order cannot be less than or equal to zero.");
		}

		int numIters, i;
		int maxIndex;
		Comparable temp;
		Comparable list[] = new Comparable[inputlist.length];

		System.arraycopy(inputlist, 0, list, 0, list.length);

		for (numIters = 0; numIters < n; numIters++) {
			maxIndex = 0;

			for (i = 0; i < (list.length - numIters); i++) {
				if (list[i].compareTo(list[maxIndex]) > 0) {
					maxIndex = i;
				}
			}

			temp = list[i - 1];
			list[i - 1] = list[maxIndex];
			list[maxIndex] = temp;
		}

		return (list[list.length - n]);
	}

	/**
	 * Returns the n<sup>th</sup> largest object in the input array. This method
	 * allows comparison of <code>Object</code>s. It follows similar semantics
	 * as the <code>findMax(int[], int)</code> method.
	 * 
	 * @see #findOrderedMax(Comparable[],int)
	 * @see #findMax(int[],int)
	 * @throws IllegalArgumentException
	 *             Thrown when the number of elements in the input array is less
	 *             than the desired order, that is, when <code>n</code> exceeds
	 *             <code>inputlist.length</code>. Or, when the order
	 *             <code>n</code> is less than or equal to 0.
	 * @param inputlist
	 *            the array of <code>Comparable</code> objects. All elements
	 *            must be <i>mutually Comparable</i> (that is, e1.compareTo(e2)
	 *            must not throw a ClassCastException for any elements e1 and e2
	 *            in the array).
	 * @param n
	 *            the desired order. For example, 1 indicates largest object; 2
	 *            indicates second-largest object, and so on.
	 */
	public static Object findMax(final Comparable inputlist[], final int n) {
		if (inputlist.length < n) {
			throw new IllegalArgumentException(
					"Input array should have atleast input order numbers.");
		}

		if (n <= 0) {
			throw new IllegalArgumentException(
					"Order cannot be less than or equal to zero.");
		}

		int numIters, i;
		int maxIndex;
		Comparable temp;
		Comparable order[] = new Comparable[n];
		int oindex = 0;
		int currIter = 0;
		Comparable list[] = new Comparable[inputlist.length];

		System.arraycopy(inputlist, 0, list, 0, list.length);

		for (numIters = 0; numIters < list.length; numIters++) {
			maxIndex = 0;

			for (i = 0; i < (list.length - numIters); i++) {
				if (list[i].compareTo(list[maxIndex]) > 0) {
					maxIndex = i;
				}
			}

			temp = list[maxIndex];
			list[maxIndex] = list[i - 1];
			list[i - 1] = temp;

			if (oindex == 0 || !order[oindex - 1].equals(temp)) {
				order[oindex++] = temp;
				currIter++;
			}

			if (currIter == n) {
				break;
			}
		}

		return (order[oindex - 1]);
	}

	/**
	 * Returns the object which would be in the n<sup>th</sup> place if the
	 * input array were sorted in ascending order. This method allows comparison
	 * of <code>Object</code>s. It follows similar semantics as the
	 * <code>findOrderedMin(int[], int)</code> method.
	 * 
	 * @see #findMin(Comparable[],int)
	 * @see #findOrderedMin(int[],int)
	 * @throws IllegalArgumentException
	 *             Thrown when the number of elements in the input array is less
	 *             than the desired order, that is, when <code>n</code> exceeds
	 *             <code>inputlist.length</code>. Or, when the order
	 *             <code>n</code> is less than or equal to 0.
	 * @param inputlist
	 *            the array of <code>Comparable</code> objects. All elements
	 *            must be <i>mutually Comparable</i> (that is, e1.compareTo(e2)
	 *            must not throw a ClassCastException for any elements e1 and e2
	 *            in the array).
	 * @param n
	 *            the desired position of sorted array. For example, 1 indicates
	 *            first from top of sorted array; 2 indicates second from top,
	 *            and so on.
	 */
	public static Object findOrderedMin(final Comparable inputlist[],
			final int n) {
		if (inputlist.length < n) {
			throw new IllegalArgumentException(
					"Input array should have atleast input order numbers.");
		}

		if (n <= 0) {
			throw new IllegalArgumentException(
					"Order cannot be less than or equal to zero.");
		}

		int numIters, i;
		int minIndex;
		Comparable temp;
		Comparable list[] = new Comparable[inputlist.length];

		System.arraycopy(inputlist, 0, list, 0, list.length);

		for (numIters = 0; numIters < n; numIters++) {
			minIndex = 0;

			for (i = 0; i < (list.length - numIters); i++) {
				if (list[i].compareTo(list[minIndex]) < 0) {
					minIndex = i;
				}
			}

			temp = list[i - 1];
			list[i - 1] = list[minIndex];
			list[minIndex] = temp;
		}

		return (list[list.length - n]);
	}

	/**
	 * Returns the n<sup>th</sup> smallest object in the input array. This
	 * method allows comparison of <code>Object</code>s. It follows similar
	 * semantics as the <code>findMin(int[], int)</code> method.
	 * 
	 * @see #findOrderedMin(Comparable[],int)
	 * @see #findMin(int[],int)
	 * @throws IllegalArgumentException
	 *             Thrown when the number of elements in the input array is less
	 *             than the desired order, that is, when <code>n</code> exceeds
	 *             <code>inputlist.length</code>. Or, when the order
	 *             <code>n</code> is less than or equal to 0.
	 * @param inputlist
	 *            the array of <code>Comparable</code> objects. All elements
	 *            must be <i>mutually Comparable</i> (that is, e1.compareTo(e2)
	 *            must not throw a ClassCastException for any elements e1 and e2
	 *            in the array).
	 * @param n
	 *            the desired order. For example, 1 indicates smallest object; 2
	 *            indicates second-smallest object, and so on.
	 */
	public static Object findMin(final Comparable inputlist[], final int n) {
		if (inputlist.length < n) {
			throw new IllegalArgumentException(
					"Input array should have atleast input order numbers.");
		}

		if (n <= 0) {
			throw new IllegalArgumentException(
					"Order cannot be less than or equal to zero.");
		}

		int numIters, i;
		int minIndex;
		Comparable temp;
		Comparable order[] = new Comparable[n];
		int oindex = 0;
		int currIter = 0;
		Comparable list[] = new Comparable[inputlist.length];

		System.arraycopy(inputlist, 0, list, 0, list.length);

		for (numIters = 0; numIters < list.length; numIters++) {
			minIndex = 0;

			for (i = 0; i < (list.length - numIters); i++) {
				if (list[i].compareTo(list[minIndex]) < 0) {
					minIndex = i;
				}
			}

			temp = list[minIndex];
			list[minIndex] = list[i - 1];
			list[i - 1] = temp;

			if (oindex == 0 || !order[oindex - 1].equals(temp)) {
				order[oindex++] = temp;
				currIter++;
			}

			if (currIter == n) {
				break;
			}
		}

		return (order[oindex - 1]);
	}

	/**
	 * Returns the object which would be in the n<sup>th</sup> place if the
	 * input array were sorted in descending order. This method allows
	 * comparison of <code>Object</code>s. The comparison logic is provided by
	 * the <code>Comparator</code> argument. It follows similar semantics as the
	 * <code>findOrderedMax(int[], int)</code> method.
	 * 
	 * @see #findMax(Object[],int,Comparator)
	 * @see #findOrderedMax(int[],int)
	 * @throws IllegalArgumentException
	 *             Thrown when the number of elements in the input array is less
	 *             than the desired order, that is, when <code>n</code> exceeds
	 *             <code>inputlist.length</code>. Or, when the order
	 *             <code>n</code> is less than or equal to 0.
	 * @param c
	 *            the <code>Comparator</code> that provides the ordering logic.
	 * @param inputlist
	 *            the array of <code>Object</code>s.
	 * @param n
	 *            the desired position of sorted array. For example, 1 indicates
	 *            first from top of sorted array; 2 indicates second from top,
	 *            and so on.
	 */
	public static Object findOrderedMax(final Object inputlist[], final int n,
			final Comparator c) {
		if (inputlist.length < n) {
			throw new IllegalArgumentException(
					"Input array should have atleast input order numbers.");
		}

		if (n <= 0) {
			throw new IllegalArgumentException(
					"Order cannot be less than or equal to zero.");
		}

		int numIters, i;
		int maxIndex;
		Object temp;
		Object list[] = new Object[inputlist.length];

		System.arraycopy(inputlist, 0, list, 0, list.length);

		for (numIters = 0; numIters < n; numIters++) {
			maxIndex = 0;

			for (i = 0; i < (list.length - numIters); i++) {
				if (c.compare(list[i], list[maxIndex]) > 0) {
					maxIndex = i;
				}
			}

			temp = list[i - 1];
			list[i - 1] = list[maxIndex];
			list[maxIndex] = temp;
		}

		return (list[list.length - n]);
	}

	/**
	 * Returns the n<sup>th</sup> largest object in the input array. This method
	 * allows comparison of <code>Object</code>s. The comparison logic is
	 * provided by the <code>Comparator</code> argument. It follows similar
	 * semantics as the <code>findMax(int[], int)</code> method.
	 * 
	 * @see #findOrderedMax(Object[],int,Comparator)
	 * @see #findMax(int[],int)
	 * @throws IllegalArgumentException
	 *             Thrown when the number of elements in the input array is less
	 *             than the desired order, that is, when <code>n</code> exceeds
	 *             <code>inputlist.length</code>. Or, when the order
	 *             <code>n</code> is less than or equal to 0.
	 * @param c
	 *            the <code>Comparator</code> that provides the ordering logic.
	 * @param inputlist
	 *            the array of <code>Object</code>s.
	 * @param n
	 *            the desired order. For example, 1 indicates largest object; 2
	 *            indicates second-largest object, and so on.
	 */
	public static Object findMax(final Object inputlist[], final int n,
			final Comparator c) {
		if (inputlist.length < n) {
			throw new IllegalArgumentException(
					"Input array should have atleast input order numbers.");
		}

		if (n <= 0) {
			throw new IllegalArgumentException(
					"Order cannot be less than or equal to zero.");
		}

		int numIters, i;
		int maxIndex;
		Object temp;
		Object order[] = new Object[n];
		int oindex = 0;
		int currIter = 0;
		Object list[] = new Object[inputlist.length];

		System.arraycopy(inputlist, 0, list, 0, list.length);

		for (numIters = 0; numIters < list.length; numIters++) {
			maxIndex = 0;

			for (i = 0; i < (list.length - numIters); i++) {
				if (c.compare(list[i], list[maxIndex]) > 0) {
					maxIndex = i;
				}
			}

			temp = list[maxIndex];
			list[maxIndex] = list[i - 1];
			list[i - 1] = temp;

			if (oindex == 0 || !(c.compare(order[oindex - 1], temp) == 0)) {
				order[oindex++] = temp;
				currIter++;
			}

			if (currIter == n) {
				break;
			}
		}

		return (order[oindex - 1]);
	}

	/**
	 * Returns the object which would be in the n<sup>th</sup> place if the
	 * input array were sorted in ascending order. This method allows comparison
	 * of <code>Object</code>s. The comparison logic is provided by the
	 * <code>Comparator</code> argument. It follows similar semantics as the
	 * <code>findOrderedMin(int[], int)</code> method.
	 * 
	 * @see #findMin(Object[],int,Comparator)
	 * @see #findOrderedMin(int[],int)
	 * @throws IllegalArgumentException
	 *             Thrown when the number of elements in the input array is less
	 *             than the desired order, that is, when <code>n</code> exceeds
	 *             <code>inputlist.length</code>. Or, when the order
	 *             <code>n</code> is less than or equal to 0.
	 * @param c
	 *            the <code>Comparator</code> that provides the ordering logic.
	 * @param inputlist
	 *            the array of <code>Object</code>s.
	 * @param n
	 *            the desired position of sorted array. For example, 1 indicates
	 *            first from top of sorted array; 2 indicates second from top,
	 *            and so on.
	 */
	public static Object findOrderedMin(final Object inputlist[], final int n,
			final Comparator c) {
		if (inputlist.length < n) {
			throw new IllegalArgumentException(
					"Input array should have atleast input order numbers.");
		}

		if (n <= 0) {
			throw new IllegalArgumentException(
					"Order cannot be less than or equal to zero.");
		}

		int numIters, i;
		int minIndex;
		Object temp;
		Object list[] = new Object[inputlist.length];

		System.arraycopy(inputlist, 0, list, 0, list.length);

		for (numIters = 0; numIters < n; numIters++) {
			minIndex = 0;

			for (i = 0; i < (list.length - numIters); i++) {
				if (c.compare(list[i], list[minIndex]) < 0) {
					minIndex = i;
				}
			}

			temp = list[i - 1];
			list[i - 1] = list[minIndex];
			list[minIndex] = temp;
		}

		return (list[list.length - n]);
	}

	/**
	 * Returns the n<sup>th</sup> smallest object in the input array. This
	 * method allows comparison of <code>Object</code>s. The comparison logic is
	 * provided by the <code>Comparator</code> argument. It follows similar
	 * semantics as the <code>findMin(int[], int)</code> method.
	 * 
	 * @see #findOrderedMin(Object[],int,Comparator)
	 * @see #findMin(int[],int)
	 * @throws IllegalArgumentException
	 *             Thrown when the number of elements in the input array is less
	 *             than the desired order, that is, when <code>n</code> exceeds
	 *             <code>inputlist.length</code>. Or, when the order
	 *             <code>n</code> is less than or equal to 0.
	 * @param c
	 *            the <code>Comparator</code> that provides the ordering logic.
	 * @param inputlist
	 *            the array of <code>Objects</code>.
	 * @param n
	 *            the desired order. For example, 1 indicates smallest object; 2
	 *            indicates second-smallest object, and so on.
	 */
	public static Object findMin(final Object inputlist[], final int n,
			final Comparator c) {
		if (inputlist.length < n) {
			throw new IllegalArgumentException(
					"Input array should have atleast input order numbers.");
		}

		if (n <= 0) {
			throw new IllegalArgumentException(
					"Order cannot be less than or equal to zero.");
		}

		int numIters, i;
		int minIndex;
		Object temp;
		Object order[] = new Object[n];
		int oindex = 0;
		int currIter = 0;
		Object list[] = new Object[inputlist.length];

		System.arraycopy(inputlist, 0, list, 0, list.length);

		for (numIters = 0; numIters < list.length; numIters++) {
			minIndex = 0;

			for (i = 0; i < (list.length - numIters); i++) {
				if (c.compare(list[i], list[minIndex]) < 0) {
					minIndex = i;
				}
			}

			temp = list[minIndex];
			list[minIndex] = list[i - 1];
			list[i - 1] = temp;

			if (oindex == 0 || !(c.compare(order[oindex - 1], temp) == 0)) {
				order[oindex++] = temp;
				currIter++;
			}

			if (currIter == n) {
				break;
			}
		}

		return (order[oindex - 1]);
	}
}
