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

import android.view.KeyEvent;
import android.view.View;

public final class MapController implements android.view.View.OnKeyListener {
	private final MapView mapView;

	MapController(MapView mapView) {
		this.mapView = mapView;
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		Logger.d("onKey called / MapController");
		return false;
	}

	public void setCenter(GeoPoint point) {
		this.mapView.setCenter(point);
	}

	public int setZoom(byte zoomLevel) {
		return this.mapView.setZoom(zoomLevel);
	}

	public boolean zoomIn() {
		return this.mapView.zoomIn();
	}

	public boolean zoomOut() {
		return this.mapView.zoomOut();
	}
}