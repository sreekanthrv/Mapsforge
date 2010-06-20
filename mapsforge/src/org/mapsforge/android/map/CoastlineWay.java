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
package org.mapsforge.android.map;

class CoastlineWay {
	private static final byte BOTTOM = 1;
	private static final byte LEFT = 2;
	private static final byte RIGHT = 0;
	private static final byte TOP = 3;

	/**
	 * Calculates the angle for a given coastline point.
	 * 
	 * @param x
	 *            the x coordinate of the coastline point.
	 * @param y
	 *            the y coordinate of the coastline point.
	 * @return the angle, always between 0 and 2π.
	 */
	private static double calculateAngle(float x, float y) {
		double angle = Math.atan2(y - (Tile.TILE_SIZE >> 1), x - (Tile.TILE_SIZE >> 1));
		if (angle < 0) {
			return angle + 2 * Math.PI;
		}
		return angle;
	}

	/**
	 * Calculates the side of the tile that corresponds to a given angle.
	 * 
	 * @param angle
	 *            the angle, must be between 0 and 2π.
	 * @return the corresponding side of the tile.
	 */
	private static byte calculateSide(double angle) {
		if (angle < Math.PI * 0.25) {
			return RIGHT;
		} else if (angle < Math.PI * 0.75) {
			return BOTTOM;
		} else if (angle < Math.PI * 1.25) {
			return LEFT;
		} else if (angle < Math.PI * 1.75) {
			return TOP;
		} else {
			return RIGHT;
		}
	}

	/**
	 * Shortens a coastline segment by removing all way points from the begin and end that are
	 * outside of the tile and therefore invisible.
	 * 
	 * @param coastline
	 *            the coordinates of the coastline segment.
	 * @return the coordinates of the shortened coastline segment.
	 */
	private static float[] shortenCoastlineSegment(float[] coastline) {
		int skipStart = 0;
		float x1 = coastline[0];
		float y1 = coastline[1];
		float x2;
		float y2;
		double[] clippedSegment;
		// find the first way segment that intersects with the tile
		for (int i = 2; i < coastline.length; i += 2) {
			x2 = coastline[i];
			y2 = coastline[i + 1];
			clippedSegment = CohenSutherlandClipping.clipLineToRectangle(x1, y1, x2, y2, 0, 0,
					Tile.TILE_SIZE, Tile.TILE_SIZE);
			if (clippedSegment != null) {
				coastline[i - 2] = (float) clippedSegment[0];
				coastline[i - 1] = (float) clippedSegment[1];
				coastline[i] = (float) clippedSegment[2];
				coastline[i + 1] = (float) clippedSegment[3];
				break;
			}
			x1 = x2;
			y1 = y2;
			++skipStart;
		}

		int skipEnd = 0;
		x1 = coastline[coastline.length - 2];
		y1 = coastline[coastline.length - 1];
		// find the last way segment that intersects with the tile
		for (int i = coastline.length - 4; i >= 0; i -= 2) {
			x2 = coastline[i];
			y2 = coastline[i + 1];
			clippedSegment = CohenSutherlandClipping.clipLineToRectangle(x1, y1, x2, y2, 0, 0,
					Tile.TILE_SIZE, Tile.TILE_SIZE);
			if (clippedSegment != null) {
				coastline[i + 2] = (float) clippedSegment[0];
				coastline[i + 3] = (float) clippedSegment[1];
				coastline[i] = (float) clippedSegment[2];
				coastline[i + 1] = (float) clippedSegment[3];
				break;
			}
			x1 = x2;
			y1 = y2;
			++skipEnd;
		}

		// copy the subset of the old coastline segment to a new segment
		float[] newCoastline = new float[coastline.length - 2 * skipStart - 2 * skipEnd];
		System.arraycopy(coastline, 2 * skipStart, newCoastline, 0, newCoastline.length);
		return newCoastline;
	}

	/**
	 * Returns a WayContainer for a given coastline segment.
	 * 
	 * @param coastline
	 *            the coordinates of the coastline segment.
	 * @return the WayContainer.
	 */
	static WayContainer getWayContainer(float[] coastline) {
		float[][] wayCoordinates = new float[1][coastline.length];
		System.arraycopy(coastline, 0, wayCoordinates[0], 0, coastline.length);
		return new WayContainer(wayCoordinates);
	}

	/**
	 * Calculates the orientation of a coastline segments by calculating the signed area. As the
	 * origin is in the top left corner, a positive area means clockwise.
	 * 
	 * @param coastline
	 *            the coordinates of the coastline segment.
	 * @return true if the orientation is clockwise, false otherwise.
	 */
	static boolean isClockWise(float[] coastline) {
		double area = 0;
		int nextNode;
		for (int currentNode = 0; currentNode < coastline.length; currentNode += 2) {
			nextNode = (currentNode + 2) % coastline.length;
			area += (coastline[currentNode] + coastline[nextNode])
					* (coastline[nextNode + 1] - coastline[currentNode + 1]);
		}
		return area > 0;
	}

	/**
	 * Checks if a given coastline segment is closed.
	 * 
	 * @param coastline
	 *            the coordinates of the coastline segment.
	 * @return true if the given coastline segment is closed, false otherwise.
	 */
	static boolean isClosed(float[] coastline) {
		return coastline[0] == coastline[coastline.length - 2]
				&& coastline[1] == coastline[coastline.length - 1];
	}

	/**
	 * Checks if a given coastline segment starts and ends outside of the tile.
	 * 
	 * @param coastline
	 *            the coordinates of the coastline segment.
	 * @return true, if first and last point are outside tile, false otherwise.
	 */
	static boolean isValid(float[] coastline) {
		return (coastline[0] <= 0 || coastline[0] >= Tile.TILE_SIZE || coastline[1] <= 0 || coastline[1] >= Tile.TILE_SIZE)
				&& (coastline[coastline.length - 2] <= 0
						|| coastline[coastline.length - 2] >= Tile.TILE_SIZE
						|| coastline[coastline.length - 1] <= 0 || coastline[coastline.length - 1] >= Tile.TILE_SIZE);
	}

	final float[] data;
	final double entryAngle;
	final byte entrySide;
	final double exitAngle;
	final byte exitSide;

	CoastlineWay(float[] coastlineCoordinates) {
		this.data = shortenCoastlineSegment(coastlineCoordinates);
		this.entryAngle = calculateAngle(this.data[0], this.data[1]);
		this.exitAngle = calculateAngle(this.data[this.data.length - 2],
				this.data[this.data.length - 1]);
		this.entrySide = calculateSide(this.entryAngle);
		this.exitSide = calculateSide(this.exitAngle);
	}
}