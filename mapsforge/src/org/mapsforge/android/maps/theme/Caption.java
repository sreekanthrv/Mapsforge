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
import java.util.Locale;

import org.xml.sax.Attributes;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;

final class Caption extends RenderingInstruction {
	static Caption create(String elementName, Attributes attributes) {
		TextKey textKey = null;
		float dy = 0;
		FontFamily fontFamily = FontFamily.DEFAULT;
		FontStyle fontStyle = FontStyle.NORMAL;
		float fontSize = 0;
		int fill = Color.BLACK;
		int stroke = Color.BLACK;
		float strokeWidth = 0;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("k".equals(name)) {
				textKey = TextKey.getInstance(value);
			} else if ("dy".equals(name)) {
				dy = Float.parseFloat(value);
			} else if ("font-family".equals(name)) {
				fontFamily = FontFamily.valueOf(value.toUpperCase(Locale.ENGLISH));
			} else if ("font-style".equals(name)) {
				fontStyle = FontStyle.valueOf(value.toUpperCase(Locale.ENGLISH));
			} else if ("font-size".equals(name)) {
				fontSize = Float.parseFloat(value);
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

		if (textKey == null) {
			throw new IllegalArgumentException("missing attribute: k");
		}

		return new Caption(textKey, dy, fontFamily, fontStyle, fontSize, fill, stroke, strokeWidth);
	}

	private final float dy;
	private final float fontSize;
	private final Paint paint;
	private final Paint stroke;
	private final TextKey textKey;

	private Caption(TextKey textKey, float dy, FontFamily fontFamily, FontStyle fontStyle,
			float fontSize, int fill, int stroke, float strokeWidth) {
		super();

		this.textKey = textKey;
		this.dy = dy;
		Typeface typeface = Typeface.create(fontFamily.toTypeface(), fontStyle.toInt());

		this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		this.paint.setTextAlign(Align.LEFT);
		this.paint.setTypeface(typeface);
		this.paint.setColor(fill);

		this.stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
		this.stroke.setStyle(Style.STROKE);
		this.stroke.setTextAlign(Align.LEFT);
		this.stroke.setTypeface(typeface);
		this.stroke.setColor(stroke);
		this.stroke.setStrokeWidth(strokeWidth);

		this.fontSize = fontSize;
	}

	@Override
	public void onDestroy() {
		// do nothing
	}

	@Override
	public void renderNode(RenderThemeCallback renderThemeCallback, List<Tag> tags) {
		String caption = this.textKey.getValue(tags);
		if (caption == null) {
			return;
		}
		renderThemeCallback.addNodeCaption(caption, this.dy, this.paint, this.stroke);
	}

	@Override
	public void renderWay(RenderThemeCallback renderThemeCallback, List<Tag> tags) {
		String caption = this.textKey.getValue(tags);
		if (caption == null) {
			return;
		}
		renderThemeCallback.addAreaCaption(caption, this.dy, this.paint, this.stroke);
	}

	@Override
	public void scaleStrokeWidth(float scaleFactor) {
		// do nothing
	}

	@Override
	public void scaleTextSize(float scaleFactor) {
		this.paint.setTextSize(this.fontSize * scaleFactor);
		this.stroke.setTextSize(this.fontSize * scaleFactor);
	}
}