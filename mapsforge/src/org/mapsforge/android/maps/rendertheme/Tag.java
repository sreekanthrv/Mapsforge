/*
 * Copyright 2010, 2011 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.maps.rendertheme;

/**
 * A tag represents a key-value pair.
 */
public class Tag {
	private static final char KEY_VALUE_SEPARATOR = '=';

	private final int hashCodeValue;
	private final String key;
	private final String value;

	/**
	 * Constructs a new tag from the given string.
	 * 
	 * @param tag
	 *            the textual representation of the tag.
	 */
	public Tag(String tag) {
		int splitPosition = tag.indexOf(KEY_VALUE_SEPARATOR);
		this.key = tag.substring(0, splitPosition);
		this.value = tag.substring(splitPosition + 1);
		this.hashCodeValue = calculateHashCode();
	}

	/**
	 * Constructs a new tag with the given key and value.
	 * 
	 * @param key
	 *            the key of the tag.
	 * @param value
	 *            the value of the tag.
	 */
	public Tag(String key, String value) {
		this.key = key;
		this.value = value;
		this.hashCodeValue = calculateHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof Tag)) {
			return false;
		}
		Tag other = (Tag) obj;
		if (this.key == null && other.key != null) {
			return false;
		} else if (this.key != null && !this.key.equals(other.key)) {
			return false;
		} else if (this.value == null && other.value != null) {
			return false;
		} else if (this.value != null && !this.value.equals(other.value)) {
			return false;
		}
		return true;
	}

	/**
	 * @return the key of this tag.
	 */
	public String getKey() {
		return this.key;
	}

	/**
	 * @return the value of this tag.
	 */
	public String getValue() {
		return this.value;
	}

	@Override
	public int hashCode() {
		return this.hashCodeValue;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Tag [key=");
		stringBuilder.append(this.key);
		stringBuilder.append(", value=");
		stringBuilder.append(this.value);
		stringBuilder.append("]");
		return stringBuilder.toString();
	}

	/**
	 * Calculates the hash code of this object.
	 * 
	 * @return the hash code of this object.
	 */
	private int calculateHashCode() {
		int result = 7;
		result = 31 * result + ((this.key == null) ? 0 : this.key.hashCode());
		result = 31 * result + ((this.value == null) ? 0 : this.value.hashCode());
		return result;
	}
}