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
package org.mapsforge.android.maps.theme;

import java.util.List;

import org.xml.sax.Attributes;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

final class Circle extends RenderingInstruction {
	static Circle create(String elementName, Attributes attributes, int level) {
		float radius = 0;
		boolean scaleRadius = false;
		int fill = Color.TRANSPARENT;
		int stroke = Color.TRANSPARENT;
		float strokeWidth = 0;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("r".equals(name)) {
				radius = Float.parseFloat(value);
			} else if ("scale-radius".equals(name)) {
				scaleRadius = Boolean.parseBoolean(value);
			} else if ("fill".equals(name)) {
				fill = Color.parseColor(value);
			} else if ("stroke".equals(name)) {
				stroke = Color.parseColor(value);
			} else if ("stroke-width".equals(name)) {
				strokeWidth = Float.parseFloat(value);
			} else {
				RenderThemeHandler.logUnknownAttribute(elementName, name, value, i);
			}
		}

		return new Circle(radius, scaleRadius, fill, stroke, strokeWidth, level);
	}

	private final Paint fill;
	private final int level;
	private final Paint outline;
	private final float radius;
	private float renderRadius;
	private final boolean scaleRadius;
	private final float strokeWidth;

	private Circle(float radius, boolean scaleRadius, int fill, int stroke, float strokeWidth, int level) {
		super();

		this.radius = radius;
		this.scaleRadius = scaleRadius;

		if (fill == Color.TRANSPARENT) {
			this.fill = null;
		} else {
			this.fill = new Paint(Paint.ANTI_ALIAS_FLAG);
			this.fill.setStyle(Style.FILL);
			this.fill.setColor(fill);
		}

		if (stroke == Color.TRANSPARENT) {
			this.outline = null;
		} else {
			this.outline = new Paint(Paint.ANTI_ALIAS_FLAG);
			this.outline.setStyle(Style.STROKE);
			this.outline.setColor(stroke);
		}

		this.strokeWidth = strokeWidth;
		this.level = level;

		if (!this.scaleRadius) {
			this.renderRadius = this.radius;
			if (this.outline != null) {
				this.outline.setStrokeWidth(this.strokeWidth);
			}
		}
	}

	@Override
	public void onDestroy() {
		// do nothing
	}

	@Override
	public void renderNode(RenderThemeCallback renderThemeCallback, List<Tag> tags) {
		if (this.outline != null) {
			renderThemeCallback.addNodeCircle(this.renderRadius, this.outline, this.level);
		}
		if (this.fill != null) {
			renderThemeCallback.addNodeCircle(this.renderRadius, this.fill, this.level);
		}
	}

	@Override
	public void renderWay(RenderThemeCallback renderThemeCallback, List<Tag> tags) {
		// do nothing
	}

	@Override
	public void scaleStrokeWidth(float scaleFactor) {
		if (this.scaleRadius) {
			this.renderRadius = this.radius * scaleFactor;
			if (this.outline != null) {
				this.outline.setStrokeWidth(this.strokeWidth * scaleFactor);
			}
		}
	}

	@Override
	public void scaleTextSize(float scaleFactor) {
		// do nothing
	}
}