/*
 * Copyright 2010 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.preprocessing.map.osmosis;

import java.security.InvalidParameterException;

/**
 * This static class converts numbers to byte arrays. Byte order is big-endian.
 */
public class Serializer {
	/**
	 * The maximum integer value that is representable with three bytes which is 2^23.
	 */
	public static final int MAX_VALUE_THREE_BYTES = 8388607;

	/**
	 * Converts an int number to a byte array.
	 * 
	 * @param value
	 *            the int value.
	 * @return an array with four bytes.
	 */
	static final byte[] getBytes(int value) {
		return new byte[] { (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8),
				(byte) value };
	}

	/**
	 * Converts a long number to a byte array.
	 * 
	 * @param value
	 *            the long value.
	 * @return an array with eight bytes.
	 */
	static byte[] getBytes(long value) {
		return new byte[] { (byte) (value >> 56), (byte) (value >> 48), (byte) (value >> 40),
				(byte) (value >> 32), (byte) (value >> 24), (byte) (value >> 16),
				(byte) (value >> 8), (byte) value };
	}

	/**
	 * Converts a short number to a byte array.
	 * 
	 * @param value
	 *            the short value.
	 * @return an array with two bytes.
	 */
	static byte[] getBytes(short value) {
		return new byte[] { (byte) (value >> 8), (byte) value };
	}

	/**
	 * Converts the lowest five bytes of a long number to a byte array.
	 * 
	 * @param value
	 *            the long value.
	 * @return an array with five bytes.
	 */
	static byte[] getFiveBytes(long value) {
		return new byte[] { (byte) (value >> 32), (byte) (value >> 24), (byte) (value >> 16),
				(byte) (value >> 8), (byte) value };
	}

	/**
	 * Converts the lowest three bytes of an int number to a byte array. The sign of the int
	 * number is preserved in the most significant bit of the first byte.
	 * 
	 * The original int number can be exactly restored if its absolute value was < 2^23.
	 * 
	 * @param value
	 *            the int value.
	 * @return an array with three bytes.
	 */
	static final byte[] getSignedThreeBytes(int value) {
		// check the sign
		if (value >= 0) {
			// positive number, set the first bit in the first byte to 0
			return new byte[] { (byte) (value >> 16 & 0x7F), (byte) (value >> 8), (byte) value };
		}
		// negative number, set the first bit in the first byte to 1
		return new byte[] { (byte) (value >> 16 | 0x80), (byte) (value >> 8), (byte) value };
	}

	/**
	 * Converts a positive number to a variable length byte array.
	 * 
	 * @param value
	 *            the int value, must not be negative.
	 * @return an array with 1-5 bytes.
	 */
	static final byte[] variableLengthEncode(int value) {
		// the first bit is used for continuation info, the other seven bits for data
		if (value < 0) {
			throw new InvalidParameterException("negative value not allowed: " + value);
		} else if (value < 128) { // 2^7
			// encode the number in a single byte
			return new byte[] { (byte) value };
		} else if (value < 16384) { // 2^14
			// encode the number in two bytes
			return new byte[] { (byte) ((value & 0x7f) | 0x80), (byte) (value >> 7) };
		} else if (value < 2097152) { // 2^21
			// encode the number in three bytes
			return new byte[] { (byte) ((value & 0x7f) | 0x80),
					(byte) (((value >> 7) & 0x7f) | 0x80), (byte) (value >> 14) };
		} else if (value < 268435456) { // 2^28
			// encode the number in four bytes
			return new byte[] { (byte) ((value & 0x7f) | 0x80),
					(byte) (((value >> 7) & 0x7f) | 0x80),
					(byte) (((value >> 14) & 0x7f) | 0x80), (byte) (value >> 21) };
		} else {
			// encode the number in five bytes
			return new byte[] { (byte) ((value & 0x7f) | 0x80),
					(byte) (((value >> 7) & 0x7f) | 0x80),
					(byte) (((value >> 14) & 0x7f) | 0x80),
					(byte) (((value >> 21) & 0x7f) | 0x80), (byte) (value >> 28) };
		}
	}
}