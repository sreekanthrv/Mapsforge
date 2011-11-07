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
package org.mapsforge.android.maps.mapdatabase;

import org.mapsforge.android.maps.MercatorProjection;

import android.graphics.Rect;

/**
 * Holds all parameters of a sub-file.
 */
class SubFileParameter {
	/**
	 * Divisor for converting coordinates stored as integers to double values.
	 */
	private static final double COORDINATES_DIVISOR = 1000000d;

	/**
	 * Stores the hash value of this object.
	 */
	private final int hashCode;

	/**
	 * Base zoom level of the map file, which equals to one block.
	 */
	final byte baseZoomLevel;

	/**
	 * Size of the entries table at the beginning of each block in bytes.
	 */
	final int blockEntriesTableSize;

	/**
	 * Vertical amount of blocks in the grid.
	 */
	final long blocksHeight;

	/**
	 * Horizontal amount of blocks in the grid.
	 */
	final long blocksWidth;

	/**
	 * Y number of the tile at the bottom boundary in the grid.
	 */
	final long boundaryTileBottom;

	/**
	 * X number of the tile at the left boundary in the grid.
	 */
	final long boundaryTileLeft;

	/**
	 * X number of the tile at the right boundary in the grid.
	 */
	final long boundaryTileRight;

	/**
	 * Y number of the tile at the top boundary in the grid.
	 */
	final long boundaryTileTop;

	/**
	 * Absolute start address of the index in the enclosing file.
	 */
	final long indexStartAddress;

	/**
	 * Total number of blocks in the grid.
	 */
	final long numberOfBlocks;

	/**
	 * Absolute start address of the sub-file in the enclosing file.
	 */
	final long startAddress;

	/**
	 * Size of the sub-file in bytes.
	 */
	final long subFileSize;

	/**
	 * Maximum zoom level for which the block entries tables are made.
	 */
	final byte zoomLevelMax;

	/**
	 * Minimum zoom level for which the block entries tables are made.
	 */
	final byte zoomLevelMin;

	/**
	 * Constructs an immutable SubFileParameter with the given values.
	 * 
	 * @param startAddress
	 *            the start address of the sub-file.
	 * @param indexStartAddress
	 *            the start address of the index.
	 * @param subFileSize
	 *            the size of the sub-file.
	 * @param baseZoomLevel
	 *            the base zoom level of the sub-file.
	 * @param tileZoomLevelMin
	 *            the minimum zoom level of the sub-file.
	 * @param tileZoomLevelMax
	 *            the maximum zoom level of the sub-file.
	 * @param mapBoundary
	 *            the boundary of the sub-file.
	 */
	SubFileParameter(long startAddress, long indexStartAddress, long subFileSize,
			byte baseZoomLevel, byte tileZoomLevelMin, byte tileZoomLevelMax, Rect mapBoundary) {
		this.startAddress = startAddress;
		this.indexStartAddress = indexStartAddress;
		this.subFileSize = subFileSize;
		this.baseZoomLevel = baseZoomLevel;
		this.zoomLevelMin = tileZoomLevelMin;
		this.zoomLevelMax = tileZoomLevelMax;
		this.hashCode = calculateHashCode();

		// calculate the XY numbers of the boundary tiles in this map file
		this.boundaryTileTop = MercatorProjection.latitudeToTileY(mapBoundary.bottom
				/ COORDINATES_DIVISOR, this.baseZoomLevel);
		this.boundaryTileLeft = MercatorProjection.longitudeToTileX(mapBoundary.left
				/ COORDINATES_DIVISOR, this.baseZoomLevel);
		this.boundaryTileBottom = MercatorProjection.latitudeToTileY(mapBoundary.top
				/ COORDINATES_DIVISOR, this.baseZoomLevel);
		this.boundaryTileRight = MercatorProjection.longitudeToTileX(mapBoundary.right
				/ COORDINATES_DIVISOR, this.baseZoomLevel);

		// calculate the horizontal and vertical amount of blocks in this map file
		this.blocksWidth = this.boundaryTileRight - this.boundaryTileLeft + 1;
		this.blocksHeight = this.boundaryTileBottom - this.boundaryTileTop + 1;

		// calculate the total amount of blocks in this map file
		this.numberOfBlocks = this.blocksWidth * this.blocksHeight;

		// calculate the size of the tile entries table
		this.blockEntriesTableSize = 2 * (this.zoomLevelMax - this.zoomLevelMin + 1) * 2;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof SubFileParameter)) {
			return false;
		}
		SubFileParameter other = (SubFileParameter) obj;
		if (this.startAddress != other.startAddress) {
			return false;
		} else if (this.subFileSize != other.subFileSize) {
			return false;
		} else if (this.baseZoomLevel != other.baseZoomLevel) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	/**
	 * Calculates the hash value of this object.
	 * 
	 * @return the hash value of this object.
	 */
	private int calculateHashCode() {
		int result = 7;
		result = 31 * result + (int) (this.startAddress ^ (this.startAddress >>> 32));
		result = 31 * result + (int) (this.subFileSize ^ (this.subFileSize >>> 32));
		result = 31 * result + this.baseZoomLevel;
		return result;
	}
}