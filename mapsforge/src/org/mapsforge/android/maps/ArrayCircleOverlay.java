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
import java.util.Collection;

import org.mapsforge.android.maps.MapView.TextField;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.graphics.Paint;

/**
 * ArrayCircleOverlay is a thread-safe implementation of the {@link CircleOverlay} class using an
 * {@link ArrayList} as internal data structure. Default paints for all {@link OverlayCircle
 * OverlayCircles} without individual paints can be defined via the constructor.
 * <p>
 * The ArrayCircleOverlay handles tap events on CircleOverlays by displaying their title in an
 * {@link AlertDialog}. To change this behavior, override the {@link #onTap(int)} method.
 */
public class ArrayCircleOverlay extends CircleOverlay<OverlayCircle> {
	private static final int ARRAY_LIST_INITIAL_CAPACITY = 8;
	private static final String THREAD_NAME = "ArrayCircleOverlay";

	private final Context context;
	private final ArrayList<OverlayCircle> overlayCircles;

	/**
	 * Constructs a new ArrayCircleOverlay.
	 * 
	 * @param defaultPaintFill
	 *            the default paint which will be used to fill the circles (may be null).
	 * @param defaultPaintOutline
	 *            the default paint which will be used to draw the circle outlines (may be null).
	 * @param context
	 *            the reference to the application context.
	 */
	public ArrayCircleOverlay(Paint defaultPaintFill, Paint defaultPaintOutline, Context context) {
		super(defaultPaintFill, defaultPaintOutline);
		this.context = context;
		this.overlayCircles = new ArrayList<OverlayCircle>(ARRAY_LIST_INITIAL_CAPACITY);
	}

	/**
	 * Adds the given circle to the overlay.
	 * 
	 * @param overlayCircle
	 *            the circle that should be added to the overlay.
	 */
	public void addCircle(OverlayCircle overlayCircle) {
		synchronized (this.overlayCircles) {
			this.overlayCircles.add(overlayCircle);
		}
		populate();
	}

	/**
	 * Adds all circles of the given collection to the overlay.
	 * 
	 * @param c
	 *            collection whose circles should be added to the overlay.
	 */
	public void addCircles(Collection<? extends OverlayCircle> c) {
		synchronized (this.overlayCircles) {
			this.overlayCircles.addAll(c);
		}
		populate();
	}

	/**
	 * Removes all circles from the overlay.
	 */
	public void clear() {
		synchronized (this.overlayCircles) {
			this.overlayCircles.clear();
		}
		populate();
	}

	@Override
	public String getThreadName() {
		return THREAD_NAME;
	}

	/**
	 * Removes the given circle from the overlay.
	 * 
	 * @param overlayCircle
	 *            the circle that should be removed from the overlay.
	 */
	public void removeCircle(OverlayCircle overlayCircle) {
		synchronized (this.overlayCircles) {
			this.overlayCircles.remove(overlayCircle);
		}
		populate();
	}

	@Override
	public int size() {
		synchronized (this.overlayCircles) {
			return this.overlayCircles.size();
		}
	}

	@Override
	protected OverlayCircle createCircle(int i) {
		synchronized (this.overlayCircles) {
			if (i >= this.overlayCircles.size()) {
				return null;
			}
			return this.overlayCircles.get(i);
		}
	}

	/**
	 * Handles a tap event.
	 */
	@Override
	protected boolean onTap(int index) {
		synchronized (this.overlayCircles) {
			OverlayCircle circle = this.overlayCircles.get(index);
			if (circle != null && circle.getTitle() != null) {
				Builder builder = new AlertDialog.Builder(this.context);
				builder.setIcon(android.R.drawable.ic_menu_info_details);
				builder.setTitle(circle.getTitle());
				builder.setPositiveButton(this.internalMapView.getText(TextField.OKAY), null);
				builder.show();
			}
			return true;
		}
	}
}