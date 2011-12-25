package org.sqlite.android;

/**
 * String encoder/decoder for SQLite.
 * 
 * This module was kindly donated by Eric van der Maarel of Nedap N.V.
 * 
 * This encoder was implemented based on an original idea from an anonymous
 * author in the source code of the SQLite distribution. I feel obliged to
 * provide a quote from the original C-source code:
 * 
 * "The author disclaims copyright to this source code. In place of a legal
 * notice, here is a blessing:
 * 
 * May you do good and not evil. May you find forgiveness for yourself and
 * forgive others. May you share freely, never taking more than you give."
 * 
 */

public class StringEncoder {

	/**
	 * Encodes the given byte array into a string that can be used by the SQLite
	 * database. The database cannot handle null (0x00) and the character '\''
	 * (0x27). The encoding consists of escaping these characters with a
	 * reserved character (0x01). The escaping is applied after determining and
	 * applying a shift that minimizes the number of escapes required. With this
	 * encoding the data of original size n is increased to a maximum of
	 * 1+(n*257)/254. For sufficiently large n the overhead is thus less than
	 * 1.2%.
	 * 
	 * @param a
	 *            the byte array to be encoded. A null reference is handled as
	 *            an empty array.
	 * @return the encoded bytes as a string. When an empty array is provided a
	 *         string of length 1 is returned, the value of which is bogus. When
	 *         decoded with this class' <code>decode</code> method a string of
	 *         size 1 will return an empty byte array.
	 */

	public static String encode(byte[] a) {
		// check input
		if (a == null || a.length == 0) {
			// bogus shift, no data
			return "x";
		}
		// determine count
		int[] cnt = new int[256];
		for (int i = 0; i < a.length; i++) {
			cnt[a[i] & 0xff]++;
		}
		// determine shift for minimum number of escapes
		int shift = 1;
		int nEscapes = a.length;
		for (int i = 1; i < 256; i++) {
			if (i == '\'') {
				continue;
			}
			int sum = cnt[i] + cnt[(i + 1) & 0xff] + cnt[(i + '\'') & 0xff];
			if (sum < nEscapes) {
				nEscapes = sum;
				shift = i;
				if (nEscapes == 0) {
					// cannot become smaller
					break;
				}
			}
		}
		// construct encoded output
		int outLen = a.length + nEscapes + 1;
		StringBuffer out = new StringBuffer(outLen);
		out.append((char) shift);
		for (int i = 0; i < a.length; i++) {
			// apply shift
			char c = (char) ((a[i] - shift) & 0xff);
			// insert escapes
			if (c == 0) { // forbidden
				out.append((char) 1);
				out.append((char) 1);
			} else if (c == 1) { // escape character
				out.append((char) 1);
				out.append((char) 2);
			} else if (c == '\'') { // forbidden
				out.append((char) 1);
				out.append((char) 3);
			} else {
				out.append(c);
			}
		}
		return out.toString();
	}

	/**
	 * Decodes the given string that is assumed to be a valid encoding of a byte
	 * array. Typically the given string is generated by this class'
	 * <code>encode</code> method.
	 * 
	 * @param s
	 *            the given string encoding.
	 * @return the byte array obtained from the decoding.
	 * @throws IllegalArgumentException
	 *             when the string given is not a valid encoded string for this
	 *             encoder.
	 */

	public static byte[] decode(String s) {
		char[] a = s.toCharArray();
		if (a.length > 2 && a[0] == 'X' && a[1] == '\''
				&& a[a.length - 1] == '\'') {
			// SQLite3 BLOB syntax
			byte[] result = new byte[(a.length - 3) / 2];
			for (int i = 2, k = 0; i < a.length - 1; i += 2, k++) {
				byte tmp;
				switch (a[i]) {
				case '0':
					tmp = 0;
					break;
				case '1':
					tmp = 1;
					break;
				case '2':
					tmp = 2;
					break;
				case '3':
					tmp = 3;
					break;
				case '4':
					tmp = 4;
					break;
				case '5':
					tmp = 5;
					break;
				case '6':
					tmp = 6;
					break;
				case '7':
					tmp = 7;
					break;
				case '8':
					tmp = 8;
					break;
				case '9':
					tmp = 9;
					break;
				case 'A':
				case 'a':
					tmp = 10;
					break;
				case 'B':
				case 'b':
					tmp = 11;
					break;
				case 'C':
				case 'c':
					tmp = 12;
					break;
				case 'D':
				case 'd':
					tmp = 13;
					break;
				case 'E':
				case 'e':
					tmp = 14;
					break;
				case 'F':
				case 'f':
					tmp = 15;
					break;
				default:
					tmp = 0;
					break;
				}
				result[k] = (byte) (tmp << 4);
				switch (a[i + 1]) {
				case '0':
					tmp = 0;
					break;
				case '1':
					tmp = 1;
					break;
				case '2':
					tmp = 2;
					break;
				case '3':
					tmp = 3;
					break;
				case '4':
					tmp = 4;
					break;
				case '5':
					tmp = 5;
					break;
				case '6':
					tmp = 6;
					break;
				case '7':
					tmp = 7;
					break;
				case '8':
					tmp = 8;
					break;
				case '9':
					tmp = 9;
					break;
				case 'A':
				case 'a':
					tmp = 10;
					break;
				case 'B':
				case 'b':
					tmp = 11;
					break;
				case 'C':
				case 'c':
					tmp = 12;
					break;
				case 'D':
				case 'd':
					tmp = 13;
					break;
				case 'E':
				case 'e':
					tmp = 14;
					break;
				case 'F':
				case 'f':
					tmp = 15;
					break;
				default:
					tmp = 0;
					break;
				}
				result[k] |= tmp;
			}
			return result;
		}
		// first element is the shift
		byte[] result = new byte[a.length - 1];
		int i = 0;
		int shift = s.charAt(i++);
		int j = 0;
		while (i < s.length()) {
			int c;
			if ((c = s.charAt(i++)) == 1) { // escape character found
				if ((c = s.charAt(i++)) == 1) {
					c = 0;
				} else if (c == 2) {
					c = 1;
				} else if (c == 3) {
					c = '\'';
				} else {
					throw new IllegalArgumentException(
							"invalid string passed to decoder: " + j);
				}
			}
			// do shift
			result[j++] = (byte) ((c + shift) & 0xff);
		}
		int outLen = j;
		// provide array of correct length
		if (result.length != outLen) {
			result = byteCopy(result, 0, outLen, new byte[outLen]);
		}
		return result;
	}

	/**
	 * Copies count elements from source, starting at element with index offset,
	 * to the given target.
	 * 
	 * @param source
	 *            the source.
	 * @param offset
	 *            the offset.
	 * @param count
	 *            the number of elements to be copied.
	 * @param target
	 *            the target to be returned.
	 * @return the target being copied to.
	 */

	private static byte[] byteCopy(byte[] source, int offset, int count,
			byte[] target) {
		for (int i = offset, j = 0; i < offset + count; i++, j++) {
			target[j] = source[i];
		}
		return target;
	}

	static final char[] xdigits = { '0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	/**
	 * Encodes the given byte array into SQLite3 blob notation, ie X'..'
	 * 
	 * @param a
	 *            the byte array to be encoded. A null reference is handled as
	 *            an empty array.
	 * @return the encoded bytes as a string.
	 */

	public static String encodeX(byte[] a) {
		// check input
		if (a == null || a.length == 0) {
			return "X''";
		}
		int outLen = a.length * 2 + 3;
		StringBuffer out = new StringBuffer(outLen);
		out.append('X');
		out.append('\'');
		for (int i = 0; i < a.length; i++) {
			out.append(xdigits[(a[i] >> 4) & 0x0F]);
			out.append(xdigits[a[i] & 0x0F]);
		}
		out.append('\'');
		return out.toString();
	}
}
