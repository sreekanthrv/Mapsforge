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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;

/**
 * This class represents an overlay which may be displayed over the {@link #mapView}
 * 
 * @author Karsten Groll
 * @author Sebastian Schlaak
 * 
 */
public abstract class Overlay extends Thread {

	/**
	 * This is where the overlays are drawn on before the canvas is touched.
	 */
	protected Bitmap bmp;

	/**
	 * The reference to the mapview class.
	 */
	protected MapView mapView;

	/**
	 * The shadows x-offset. This feature is not yet implemented!
	 */
	protected static float SHADOW_X_SKEW = -0.8999999761581421f;

	/**
	 * The shadows y-offset. This feature is not yet implemented!
	 */
	protected static float SHADOW_Y_SKEW = 0.5f;

	/**
	 * Draws the overlay on the {@link MapView}.
	 * 
	 * @param canvas
	 *            the canvas the overlay will be thrown onto.
	 * 
	 * @param mapview
	 *            the {@link MapView} that called the draw-method.
	 * 
	 * @param shadow
	 *            not yet implemented!
	 */
	public void draw(Canvas canvas, MapView mapview, boolean shadow) {
		// overwritten
	}

	/**
	 * Calls {@link Overlay#draw(Canvas, MapView, boolean)} and returns false.
	 * 
	 * @param canvas
	 *            the canvas the overlay will be drawn onto.
	 * 
	 * @param mapview
	 *            the {@link MapView} that called the draw-method.
	 * 
	 * @param shadow
	 *            not yet implemented!
	 * @param when
	 *            not yet implemented!
	 * @return false
	 */
	public boolean draw(android.graphics.Canvas canvas, MapView mapview, boolean shadow,
			long when) {
		draw(canvas, mapView, shadow);
		return false;
	}

	/**
	 * Handles behavior on keypress(Does nothing by default).
	 * 
	 * @param keyCode
	 *            the keyCode of the event.
	 * @param event
	 *            the event.
	 * @param mapview
	 *            {@link MapView} that triggered the event.
	 * @return false
	 */
	public boolean onKeyDown(int keyCode, android.view.KeyEvent event, MapView mapview) {
		return false;
	}

	/**
	 * Handles behavior on keyrelease (Does nothing by default).
	 * 
	 * @param keyCode
	 *            the keyCode of the event.
	 * @param event
	 *            the event.
	 * @param mapview
	 *            {@link MapView} that triggered the event.
	 * @return false
	 */
	public boolean onKeyUp(int keyCode, android.view.KeyEvent event, MapView mapview) {
		return false;
	}

	/**
	 * Handles a touch event (Does nothing by default).
	 * 
	 * @param event
	 *            the event.
	 * @param mapview
	 *            {@link MapView} that triggered the event.
	 * @return true if the event was handled by the overlay.
	 */
	public boolean onTouchEvent(android.view.MotionEvent event, MapView mapview) {
		return false;
	}

	/**
	 * Handles a trackball event.
	 * 
	 * @param event
	 *            the event.
	 * @param mapview
	 *            {@link MapView} that triggered the event.
	 * @return true if the event was handled by the overlay.
	 */
	public boolean onTrackballEvent(android.view.MotionEvent event, MapView mapview) {
		return false;
	}

	/**
	 * Prepares this overlay for drawing.
	 * 
	 * @param mapview
	 *            the parent mapview.
	 */
	protected abstract void prepareOverlayBitmap(MapView mapview);

	/**
	 * 
	 * Inits the overlay-bitmap and the related canvas.
	 * 
	 * @param width
	 *            the width of the bitmap.
	 * @param height
	 *            the height of the bitmap.
	 */
	protected abstract void createOverlayBitmapsAndCanvas(int width, int height);

	/**
	 * Returns true if mapview is set.
	 * 
	 * @return true if mapview is set.
	 */
	protected boolean isMapViewSet() {
		boolean ready = false;
		if (this.mapView == null)
			ready = false;
		else {
			ready = true;
		}
		return ready;
	}

	/**
	 * Sets a reference to the mapview.
	 * 
	 * @param mapView
	 *            a reference to the mapview class.
	 */
	protected void setMapViewAndCreateOverlayBitmaps(MapView mapView) {
		this.mapView = mapView;
		createOverlayBitmapsAndCanvas(mapView.getWidth(), mapView.getHeight());
	}

	/**
	 * Returns the matrix of this overlay.
	 * 
	 * @return the matrix of this overlay.
	 */
	protected abstract Matrix getMatrix();

	@Override
	public final void run() {
		while (!isInterrupted()) {
			synchronized (this) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					interrupt();
				}
			}
			if (isInterrupted()) {
				break;
			}
			prepareOverlayBitmap(this.mapView);
		}
		if (this.bmp != null)
			this.bmp.recycle();
	}
}
