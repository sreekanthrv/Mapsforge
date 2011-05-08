/*
 * Copyright 2010, 2011 mapsforge.org
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
package org.mapsforge.android.maps;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;

import android.graphics.Rect;

/**
 * This class place the labels form POIs, area labels and normal labels. The main target is avoiding
 * collisions of these different labels.
 */
class LabelPlacement {
	/**
	 * This class holds the reference positions for the two and four point greedy algorithms.
	 */
	static class ReferencePosition {
		final float height;
		final int nodeNumber;
		SymbolContainer symbol;
		final float width;
		final float x;
		final float y;

		ReferencePosition(float x, float y, int nodeNumber, float width, float height,
				SymbolContainer symbol) {
			this.x = x;
			this.y = y;
			this.nodeNumber = nodeNumber;
			this.width = width;
			this.height = height;
			this.symbol = symbol;
		}
	}

	private static final boolean DEFAULT = false;

	private int labelDistanceToLabel = 2;
	private int labelDistanceToSymbol = 2;
	private int placementOption = 1;
	// You can choose between 2 Position and 4 Position
	// placement Model 0 - 2-Position 1 - 4 Position
	// distance adjustments
	private int startDistanceToSymbols = 4;
	private int symbolDistanceToSymbol = 2;

	final DependencyCache dependencyCache;
	PointTextContainer label;
	Rect rect1;
	Rect rect2;
	ReferencePosition reference;
	SymbolContainer smb;

	LabelPlacement() {
		this.dependencyCache = new DependencyCache();
	}

	/**
	 * Centers the labels.
	 * 
	 * @param labels
	 *            labels to center
	 */
	private void centerLabels(ArrayList<PointTextContainer> labels) {
		for (int i = 0; i < labels.size(); i++) {
			this.label = labels.get(i);
			this.label.x = this.label.x - this.label.boundary.width() / 2;
		}
	}

	/**
	 * Centers labels with a safety margin for default rendering.
	 * 
	 * @param labels
	 *            Labels to center
	 */
	private void centerLabels2(ArrayList<PointTextContainer> labels) {
		for (int i = 0; i < labels.size(); i++) {
			this.label = labels.get(i);
			this.label.x = this.label.x - this.label.boundary.width() / 2;
			if (this.label.symbol != null) {
				this.label.y = this.label.y - this.label.symbol.symbol.getHeight() / 2 - 3;
			}
		}
	}

	private void preprocessAreaLabels(ArrayList<PointTextContainer> areaLabels) {
		centerLabels(areaLabels);

		removeOutOfTileAreaLabels(areaLabels);

		removeOverlappingAreaLabels(areaLabels);

		if (areaLabels.size() != 0) {
			this.dependencyCache.removeAreaLabelsInAlreadyDrawnAreas(areaLabels);
		}
	}

	private void preprocessLabels(ArrayList<PointTextContainer> labels) {
		removeOutOfTileLabels(labels);
	}

	private void preprocessSymbols(ArrayList<SymbolContainer> symbols) {
		removeOutOfTileSymbols(symbols);
		removeOverlappingSymbols(symbols);
		this.dependencyCache.removeSymbolsFromDrawnAreas(symbols);
	}

	/**
	 * This method uses an adapted greedy strategy for the fixed four position model, above, under left
	 * and right form the point of interest. It uses no priority search tree, because it will not
	 * function with symbols only with points. Instead it uses two minimum heaps. They work similar to a
	 * sweep line algorithm but have not a O(n log n +k) runtime. To find the rectangle that has the top
	 * edge, I use also a minimum Heap. The rectangles are sorted by their y coordinates.
	 * 
	 * @param labels
	 *            label positions and text
	 * @param symbols
	 *            symbol positions
	 * @param areaLabels
	 *            area label positions and text
	 * @return list of labels without overlaps with symbols and other labels by the four fixed position
	 *         greedy strategy
	 */
	private ArrayList<PointTextContainer> processFourPointGreedy(
			ArrayList<PointTextContainer> labels, ArrayList<SymbolContainer> symbols,
			ArrayList<PointTextContainer> areaLabels) {
		ArrayList<PointTextContainer> resolutionSet = new ArrayList<PointTextContainer>();

		// Array for the generated reference positions around the points of interests
		ReferencePosition[] refPos = new ReferencePosition[(labels.size()) * 4];

		// lists that sorts the reference points after the minimum top edge y position
		PriorityQueue<ReferencePosition> priorUp = new PriorityQueue<ReferencePosition>(labels
				.size()
				* 4 * 2 + labels.size() / 10 * 2, new Comparator<ReferencePosition>() {
			@Override
			public int compare(ReferencePosition x, ReferencePosition y) {
				if (x.y < y.y) {
					return -1;
				}

				if (x.y > y.y) {
					return 1;
				}

				return 0;
			}
		});
		// lists that sorts the reference points after the minimum bottom edge y position
		PriorityQueue<ReferencePosition> priorDown = new PriorityQueue<ReferencePosition>(
				labels.size() * 4 * 2 + labels.size() / 10 * 2,
				new Comparator<ReferencePosition>() {
					@Override
					public int compare(ReferencePosition x, ReferencePosition y) {
						if (x.y - x.height < y.y - y.height) {
							return -1;
						}

						if (x.y - x.height > y.y - y.height) {
							return 1;
						}
						return 0;
					}
				});

		PointTextContainer tmp;
		int dis = this.startDistanceToSymbols;

		// creates the reference positions
		for (int z = 0; z < labels.size(); z++) {
			if (labels.get(z) != null) {
				if (labels.get(z).symbol != null) {
					tmp = labels.get(z);

					// up
					refPos[z * 4] = new ReferencePosition(tmp.x - tmp.boundary.width() / 2,
							tmp.y - tmp.symbol.symbol.getHeight() / 2 - dis, z, tmp.boundary
									.width(), tmp.boundary.height(), tmp.symbol);
					// down
					refPos[z * 4 + 1] = new ReferencePosition(tmp.x - tmp.boundary.width() / 2,
							tmp.y + tmp.symbol.symbol.getHeight() / 2 + tmp.boundary.height()
									+ dis, z, tmp.boundary.width(), tmp.boundary.height(),
							tmp.symbol);
					// left
					refPos[z * 4 + 2] = new ReferencePosition(tmp.x
							- tmp.symbol.symbol.getWidth() / 2 - tmp.boundary.width() - dis,
							tmp.y + tmp.boundary.height() / 2, z, tmp.boundary.width(),
							tmp.boundary.height(), tmp.symbol);
					// right
					refPos[z * 4 + 3] = new ReferencePosition(tmp.x
							+ tmp.symbol.symbol.getWidth() / 2 + dis, tmp.y
							+ tmp.boundary.height() / 2 - 0.1f, z, tmp.boundary.width(),
							tmp.boundary.height(), tmp.symbol);
				} else {
					refPos[z * 4] = new ReferencePosition(labels.get(z).x
							- ((labels.get(z).boundary.width()) / 2), labels.get(z).y, z,
							labels.get(z).boundary.width(), labels.get(z).boundary.height(),
							null);
					refPos[z * 4 + 1] = null;
					refPos[z * 4 + 2] = null;
					refPos[z * 4 + 3] = null;
				}
			}
		}

		removeNonValidateReferencePosition(refPos, symbols, areaLabels);

		// do while it gives reference positions
		for (int i = 0; i < refPos.length; i++) {
			this.reference = refPos[i];
			if (this.reference != null) {
				priorUp.add(this.reference);
				priorDown.add(this.reference);
			}
		}

		while (priorUp.size() != 0) {
			this.reference = priorUp.remove();

			this.label = labels.get(this.reference.nodeNumber);

			resolutionSet.add(new PointTextContainer(this.label.text, this.reference.x,
					this.reference.y, this.label.paintFront, this.label.paintBack,
					this.label.symbol));

			if (priorUp.size() == 0) {
				return resolutionSet;
			}

			priorUp.remove(refPos[this.reference.nodeNumber * 4 + 0]);
			priorUp.remove(refPos[this.reference.nodeNumber * 4 + 1]);
			priorUp.remove(refPos[this.reference.nodeNumber * 4 + 2]);
			priorUp.remove(refPos[this.reference.nodeNumber * 4 + 3]);

			priorDown.remove(refPos[this.reference.nodeNumber * 4 + 0]);
			priorDown.remove(refPos[this.reference.nodeNumber * 4 + 1]);
			priorDown.remove(refPos[this.reference.nodeNumber * 4 + 2]);
			priorDown.remove(refPos[this.reference.nodeNumber * 4 + 3]);

			LinkedList<ReferencePosition> linkedRef = new LinkedList<ReferencePosition>();

			while (priorDown.size() != 0) {
				if (priorDown.peek().x < this.reference.x + this.reference.width) {
					linkedRef.add(priorDown.remove());
				} else {
					break;
				}
			}
			// brute Force collision test (faster then sweep line for a small amount of
			// objects)
			for (int i = 0; i < linkedRef.size(); i++) {
				if ((linkedRef.get(i).x <= this.reference.x + this.reference.width)
						&& (linkedRef.get(i).y >= this.reference.y - linkedRef.get(i).height)
						&& (linkedRef.get(i).y <= this.reference.y + linkedRef.get(i).height)) {
					priorUp.remove(linkedRef.get(i));
					linkedRef.remove(i);
					i--;
				}
			}
			priorDown.addAll(linkedRef);
		}

		return resolutionSet;
	}

	/**
	 * This method uses an adapted greedy strategy for the fixed two position model, above and under. It
	 * uses no priority search tree, because it will not function with symbols only with points. Instead
	 * it uses two minimum heaps. They work similar to a sweep line algorithm but have not a O(n log n
	 * +k) runtime. To find the rectangle that has the leftest edge, I use also a minimum Heap. The
	 * rectangles are sorted by their x coordinates.
	 * 
	 * @param labels
	 *            label positions and text
	 * @param symbols
	 *            symbol positions
	 * @param areaLabels
	 *            area label positions and text
	 * @return list of labels without overlaps with symbols and other labels by the two fixed position
	 *         greedy strategy
	 */
	private ArrayList<PointTextContainer> processTwoPointGreedy(
			ArrayList<PointTextContainer> labels, ArrayList<SymbolContainer> symbols,
			ArrayList<PointTextContainer> areaLabels) {
		ArrayList<PointTextContainer> resolutionSet = new ArrayList<PointTextContainer>();
		// Array for the generated reference positions around the points of interests
		ReferencePosition[] refPos = new ReferencePosition[labels.size() * 2];

		// lists that sorts the reference points after the minimum right edge x position
		PriorityQueue<ReferencePosition> priorRight = new PriorityQueue<ReferencePosition>(
				labels.size() * 2 + labels.size() / 10 * 2,
				new Comparator<ReferencePosition>() {
					@Override
					public int compare(ReferencePosition x, ReferencePosition y) {
						if (x.x + x.width < y.x + y.width) {
							return -1;
						}

						if (x.x + x.width > y.x + y.width) {
							return 1;
						}

						return 0;
					}
				});
		// lists that sorts the reference points after the minimum left edge x position
		PriorityQueue<ReferencePosition> priorLeft = new PriorityQueue<ReferencePosition>(
				labels.size() * 2 + labels.size() / 10 * 2,
				new Comparator<ReferencePosition>() {
					@Override
					public int compare(ReferencePosition x, ReferencePosition y) {
						if (x.x < y.x) {
							return -1;
						}

						if (x.x > y.x) {
							return 1;
						}

						return 0;
					}
				});

		// creates the reference positions
		for (int z = 0; z < labels.size(); z++) {
			this.label = labels.get(z);

			if (this.label.symbol != null) {
				refPos[z * 2] = new ReferencePosition(this.label.x
						- (this.label.boundary.width() / 2) - 0.1f, this.label.y
						- this.label.boundary.height() - this.startDistanceToSymbols, z,
						this.label.boundary.width(), this.label.boundary.height(),
						this.label.symbol);
				refPos[z * 2 + 1] = new ReferencePosition(this.label.x
						- (this.label.boundary.width() / 2), this.label.y
						+ this.label.symbol.symbol.getHeight() + this.startDistanceToSymbols,
						z, this.label.boundary.width(), this.label.boundary.height(),
						this.label.symbol);
			} else {
				refPos[z * 2] = new ReferencePosition(this.label.x
						- (this.label.boundary.width() / 2) - 0.1f, this.label.y, z,
						this.label.boundary.width(), this.label.boundary.height(), null);
				refPos[z * 2 + 1] = null;
			}
		}

		// removes reference positions that overlaps with other symbols or dependency objects
		removeNonValidateReferencePosition(refPos, symbols, areaLabels);

		for (int i = 0; i < refPos.length; i++) {
			this.reference = refPos[i];
			if (this.reference != null) {
				priorLeft.add(this.reference);
				priorRight.add(this.reference);
			}
		}

		while (priorRight.size() != 0) {
			this.reference = priorRight.remove();

			this.label = labels.get(this.reference.nodeNumber);

			resolutionSet.add(new PointTextContainer(this.label.text, this.reference.x,
					this.reference.y, this.label.paintFront, this.label.paintBack,
					this.reference.symbol));

			// Removes the other position that is a possible position for the label of one point
			// of interest

			priorRight.remove(refPos[this.reference.nodeNumber * 2 + 1]);

			if (priorRight.size() == 0) {
				return resolutionSet;
			}

			priorLeft.remove(this.reference);
			priorLeft.remove(refPos[this.reference.nodeNumber * 2 + 1]);

			// find overlapping labels and deletes the reference points and delete them
			LinkedList<ReferencePosition> linkedRef = new LinkedList<ReferencePosition>();

			while (priorLeft.size() != 0) {
				if (priorLeft.peek().x < this.reference.x + this.reference.width) {
					linkedRef.add(priorLeft.remove());
				} else {
					break;
				}
			}

			// brute Force collision test (faster then sweep line for a small amount of
			// objects)
			for (int i = 0; i < linkedRef.size(); i++) {
				if ((linkedRef.get(i).x <= this.reference.x + this.reference.width)
						&& (linkedRef.get(i).y >= this.reference.y - linkedRef.get(i).height)
						&& (linkedRef.get(i).y <= this.reference.y + linkedRef.get(i).height)) {
					priorRight.remove(linkedRef.get(i));
					linkedRef.remove(i);
					i--;
				}
			}
			priorLeft.addAll(linkedRef);
		}

		return resolutionSet;
	}

	private void removeEmptySymbolReferences(ArrayList<PointTextContainer> nodes,
			ArrayList<SymbolContainer> symbols) {
		for (int i = 0; i < nodes.size(); i++) {
			this.label = nodes.get(i);
			if (!symbols.contains(this.label.symbol)) {
				this.label.symbol = null;
			}
		}
	}

	/**
	 * The greedy algorithms need possible label positions, to choose the best among them. This method
	 * removes the reference points, that are not validate. Not validate means, that the Reference
	 * overlap with another symbol or label or is outside of the tile.
	 * 
	 * @param refPos
	 *            list of the potential positions
	 * @param symbols
	 *            actual list of the symbols
	 * @param areaLabels
	 *            actual list of the area labels
	 */
	private void removeNonValidateReferencePosition(ReferencePosition[] refPos,
			ArrayList<SymbolContainer> symbols, ArrayList<PointTextContainer> areaLabels) {
		int dis = this.labelDistanceToSymbol;

		for (int i = 0; i < symbols.size(); i++) {
			this.smb = symbols.get(i);
			this.rect1 = new android.graphics.Rect((int) this.smb.x - dis, (int) this.smb.y
					- dis, (int) this.smb.x + this.smb.symbol.getWidth() + dis,
					(int) this.smb.y + this.smb.symbol.getHeight() + dis);

			for (int y = 0; y < refPos.length; y++) {
				if (refPos[y] != null) {

					this.rect2 = new android.graphics.Rect((int) refPos[y].x,
							(int) (refPos[y].y - refPos[y].height),
							(int) (refPos[y].x + refPos[y].width), (int) (refPos[y].y));

					if (android.graphics.Rect.intersects(this.rect2, this.rect1)) {
						refPos[y] = null;
					}
				}
			}
		}

		dis = this.labelDistanceToLabel;

		for (PointTextContainer areaLabel : areaLabels) {

			this.rect1 = new android.graphics.Rect((int) areaLabel.x - dis, (int) areaLabel.y
					- areaLabel.boundary.height() - dis, (int) areaLabel.x
					+ areaLabel.boundary.width() + dis, (int) areaLabel.y + dis);

			for (int y = 0; y < refPos.length; y++) {
				if (refPos[y] != null) {

					this.rect2 = new android.graphics.Rect((int) refPos[y].x,
							(int) (refPos[y].y - refPos[y].height),
							(int) (refPos[y].x + refPos[y].width), (int) (refPos[y].y));

					if (android.graphics.Rect.intersects(this.rect2, this.rect1)) {
						refPos[y] = null;
					}
				}
			}
		}

		this.dependencyCache.removeReferencePointsFromDependencyCache(refPos);
	}

	/**
	 * This method removes the area labels, that are not visible in the actual tile.
	 * 
	 * @param areaLabels
	 *            area Labels from the actual tile
	 */
	private void removeOutOfTileAreaLabels(ArrayList<PointTextContainer> areaLabels) {
		for (int i = 0; i < areaLabels.size(); i++) {
			this.label = areaLabels.get(i);

			if (this.label.x > Tile.TILE_SIZE) {
				areaLabels.remove(i);

				i--;
			} else if (this.label.y - this.label.boundary.height() > Tile.TILE_SIZE) {
				areaLabels.remove(i);

				i--;
			} else if (this.label.x + this.label.boundary.width() < 0.0f) {
				areaLabels.remove(i);

				i--;
			} else if (this.label.y + this.label.boundary.height() < 0.0f) {
				areaLabels.remove(i);

				i--;
			}
		}
	}

	/**
	 * This method removes the labels, that are not visible in the actual tile.
	 * 
	 * @param labels
	 *            Labels from the actual tile
	 */
	private void removeOutOfTileLabels(ArrayList<PointTextContainer> labels) {
		for (int i = 0; i < labels.size();) {
			this.label = labels.get(i);

			if (this.label.x - this.label.boundary.width() / 2 > Tile.TILE_SIZE) {
				labels.remove(i);
				this.label = null;
			} else if (this.label.y - this.label.boundary.height() > Tile.TILE_SIZE) {
				labels.remove(i);
				this.label = null;
			} else if ((this.label.x - this.label.boundary.width() / 2 + this.label.boundary
					.width()) < 0.0f) {
				labels.remove(i);
				this.label = null;
			} else if (this.label.y < 0.0f) {
				labels.remove(i);
				this.label = null;
			} else {
				i++;
			}
		}
	}

	/**
	 * This method removes the Symbols, that are not visible in the actual tile.
	 * 
	 * @param symbols
	 *            Symbols from the actual tile
	 */
	private void removeOutOfTileSymbols(ArrayList<SymbolContainer> symbols) {
		for (int i = 0; i < symbols.size();) {
			this.smb = symbols.get(i);

			if (this.smb.x > Tile.TILE_SIZE) {
				symbols.remove(i);

			} else if (this.smb.y > Tile.TILE_SIZE) {
				symbols.remove(i);

			} else if (this.smb.x + this.smb.symbol.getWidth() < 0.0f) {
				symbols.remove(i);

			} else if (this.smb.y + this.smb.symbol.getHeight() < 0.0f) {
				symbols.remove(i);

			} else {
				i++;
			}
		}
	}

	/**
	 * This method removes all the area labels, that overlap each other. So that the output is collision
	 * free
	 * 
	 * @param areaLabels
	 *            area labels from the actual tile
	 */
	private void removeOverlappingAreaLabels(ArrayList<PointTextContainer> areaLabels) {
		int dis = this.labelDistanceToLabel;

		for (int x = 0; x < areaLabels.size(); x++) {
			this.label = areaLabels.get(x);
			this.rect1 = new android.graphics.Rect((int) this.label.x - dis, (int) this.label.y
					- dis, (int) (this.label.x + this.label.boundary.width()) + dis,
					(int) (this.label.y + this.label.boundary.height() + dis));

			for (int y = x + 1; y < areaLabels.size(); y++) {
				if (y != x) {
					this.label = areaLabels.get(y);
					this.rect2 = new android.graphics.Rect((int) this.label.x,
							(int) this.label.y, (int) (this.label.x + this.label.boundary
									.width()), (int) (this.label.y + this.label.boundary
									.height()));

					if (android.graphics.Rect.intersects(this.rect1, this.rect2)) {
						areaLabels.remove(y);

						y--;
					}
				}
			}
		}
	}

	/**
	 * Removes the the symbols that overlap with area labels.
	 * 
	 * @param symbols
	 *            list of symbols
	 * @param pTC
	 *            list of labels
	 */
	private void removeOverlappingSymbolsWithAreaLabels(ArrayList<SymbolContainer> symbols,
			ArrayList<PointTextContainer> pTC) {
		int dis = this.labelDistanceToSymbol;

		for (int x = 0; x < pTC.size(); x++) {
			this.label = pTC.get(x);

			this.rect1 = new android.graphics.Rect((int) this.label.x - dis,
					(int) (this.label.y - this.label.boundary.height()) - dis,
					(int) (this.label.x + this.label.boundary.width() + dis),
					(int) (this.label.y + dis));

			for (int y = 0; y < symbols.size(); y++) {
				this.smb = symbols.get(y);

				this.rect2 = new android.graphics.Rect((int) this.smb.x, (int) this.smb.y,
						(int) (this.smb.x + this.smb.symbol.getWidth()),
						(int) (this.smb.y + this.smb.symbol.getHeight()));

				if (android.graphics.Rect.intersects(this.rect1, this.rect2)) {
					symbols.remove(y);
					y--;
				}
			}
		}
	}

	int getLabelDistanceToLabel() {
		return this.labelDistanceToLabel;
	}

	int getLabelDistanceToSymbol() {
		return this.labelDistanceToSymbol;
	}

	int getPlacementOption() {
		return this.placementOption;
	}

	int getStartDistanceToSymbols() {
		return this.startDistanceToSymbols;
	}

	int getSymbolDistanceToSymbol() {
		return this.symbolDistanceToSymbol;
	}

	/**
	 * The inputs are all the label and symbol objects of the current tile. The output is overlap free
	 * label and symbol placement with the greedy strategy. The placement model is either the two fixed
	 * point or the four fixed point model.
	 * 
	 * @param labels
	 *            labels from the current tile.
	 * @param symbols
	 *            symbols of the current tile.
	 * @param areaLabels
	 *            area labels from the current tile.
	 * @param cT
	 *            current tile with the x,y- coordinates and the zoom level.
	 * @return the processed list of labels.
	 */
	ArrayList<PointTextContainer> placeLabels(ArrayList<PointTextContainer> labels,
			ArrayList<SymbolContainer> symbols, ArrayList<PointTextContainer> areaLabels,
			Tile cT) {
		ArrayList<PointTextContainer> returnLabels = labels;
		if (!DEFAULT) {
			this.dependencyCache.generateTileAndDependencyOnTile(cT);

			preprocessAreaLabels(areaLabels);

			preprocessLabels(returnLabels);

			preprocessSymbols(symbols);

			removeEmptySymbolReferences(returnLabels, symbols);

			removeOverlappingSymbolsWithAreaLabels(symbols, areaLabels);

			this.dependencyCache.removeOverlappingObjectsWithDependencyOnTile(returnLabels,
					areaLabels, symbols);

			if (returnLabels.size() != 0) {
				switch (this.placementOption) {
					case 0:
						returnLabels = processTwoPointGreedy(returnLabels, symbols, areaLabels);
						break;
					case 1:
						returnLabels = processFourPointGreedy(returnLabels, symbols, areaLabels);
						break;
					default:
						break;
				}
			}

			this.dependencyCache.fillDependencyOnTile(returnLabels, symbols, areaLabels);
		} else {
			centerLabels(areaLabels);
			centerLabels2(returnLabels);
		}
		return returnLabels;
	}

	/**
	 * This method removes all the Symbols, that overlap each other. So that the output is collision
	 * free.
	 * 
	 * @param symbols
	 *            symbols from the actual tile
	 */
	void removeOverlappingSymbols(ArrayList<SymbolContainer> symbols) {
		int dis = this.symbolDistanceToSymbol;

		for (int x = 0; x < symbols.size(); x++) {
			this.smb = symbols.get(x);
			this.rect1 = new android.graphics.Rect((int) this.smb.x - dis, (int) this.smb.y
					- dis, (int) this.smb.x + this.smb.symbol.getWidth() + dis,
					(int) this.smb.y + this.smb.symbol.getHeight() + dis);

			for (int y = x + 1; y < symbols.size(); y++) {
				if (y != x) {
					this.smb = symbols.get(y);
					this.rect2 = new android.graphics.Rect((int) this.smb.x, (int) this.smb.y,
							(int) this.smb.x + this.smb.symbol.getWidth(), (int) this.smb.y
									+ this.smb.symbol.getHeight());

					if (android.graphics.Rect.intersects(this.rect2, this.rect1)) {
						symbols.remove(y);
						y--;
					}
				}
			}
		}
	}

	void setLabelDistanceToLabel(int labelDistanceToLabel) {
		this.labelDistanceToLabel = labelDistanceToLabel;
	}

	void setLabelDistanceToSymbol(int labelDistanceToSymbol) {
		this.labelDistanceToSymbol = labelDistanceToSymbol;
	}

	void setStartDistanceToSymbols(int startDistanceToSymbols) {
		this.startDistanceToSymbols = startDistanceToSymbols;
	}

	void setSymbolDistanceToSymbol(int symbolDistanceToSymbol) {
		this.symbolDistanceToSymbol = symbolDistanceToSymbol;
	}
}
