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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;

abstract class Rule {
	private static final Pattern SPLIT_PATTERN = Pattern.compile("\\|");
	private static final String WILDCARD_STRING = "*";

	private static ClosedMatcher getClosedMatcher(Closed closed) {
		switch (closed) {
			case YES:
				return ClosedWayMatcher.getInstance();
			case NO:
				return LinearWayMatcher.getInstance();
			case ANY:
				return AnyMatcher.getInstance();
			default:
				throw new IllegalArgumentException("unknown enum value: " + closed);
		}
	}

	private static ElementMatcher getElementMatcher(Element element) {
		switch (element) {
			case NODE:
				return ElementNodeMatcher.getInstance();
			case WAY:
				return ElementWayMatcher.getInstance();
			case ANY:
				return AnyMatcher.getInstance();
			default:
				throw new IllegalArgumentException("unknown enum value: " + element);
		}
	}

	private static AttributeMatcher getKeyMatcher(List<String> keysList) {
		if (WILDCARD_STRING.equals(keysList.get(0))) {
			return AnyMatcher.getInstance();
		}
		return new KeyMatcher(keysList);
	}

	private static AttributeMatcher getValueMatcher(List<String> valuesList) {
		if (WILDCARD_STRING.equals(valuesList.get(0))) {
			return AnyMatcher.getInstance();
		}
		return new ValueMatcher(valuesList);
	}

	private static void validate(String elementName, Element element, String keys, String values,
			byte zoomMin, byte zoomMax) {
		if (element == null) {
			throw new IllegalArgumentException("missing attribute e for element: " + elementName);
		} else if (keys == null) {
			throw new IllegalArgumentException("missing attribute k for element: " + elementName);
		} else if (values == null) {
			throw new IllegalArgumentException("missing attribute v for element: " + elementName);
		} else if (zoomMin < 0) {
			throw new IllegalArgumentException("zoom-min must not be negative: " + zoomMin);
		} else if (zoomMax < 0) {
			throw new IllegalArgumentException("zoom-max must not be negative: " + zoomMax);
		} else if (zoomMin > zoomMax) {
			throw new IllegalArgumentException("zoom-min must be less or equal zoom-max: " + zoomMin);
		}
	}

	static Rule create(String elementName, Attributes attributes, Stack<Rule> ruleStack) {
		Element element = null;
		String keys = null;
		String values = null;
		Closed closed = Closed.ANY;
		byte zoomMin = 0;
		byte zoomMax = Byte.MAX_VALUE;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("e".equals(name)) {
				element = Element.valueOf(value.toUpperCase(Locale.ENGLISH));
			} else if ("k".equals(name)) {
				keys = value;
			} else if ("v".equals(name)) {
				values = value;
			} else if ("closed".equals(name)) {
				closed = Closed.valueOf(value.toUpperCase(Locale.ENGLISH));
			} else if ("zoom-min".equals(name)) {
				zoomMin = Byte.parseByte(value);
			} else if ("zoom-max".equals(name)) {
				zoomMax = Byte.parseByte(value);
			} else {
				RenderThemeHandler.logUnknownAttribute(elementName, name, value, i);
			}
		}

		validate(elementName, element, keys, values, zoomMin, zoomMax);

		ElementMatcher elementMatcher = getElementMatcher(element);
		ClosedMatcher closedMatcher = getClosedMatcher(closed);
		List<String> keyList = new ArrayList<String>(Arrays.asList(SPLIT_PATTERN.split(keys)));
		List<String> valueList = new ArrayList<String>(Arrays.asList(SPLIT_PATTERN.split(values)));

		elementMatcher = RuleOptimizer.optimize(elementMatcher, ruleStack);
		closedMatcher = RuleOptimizer.optimize(closedMatcher, ruleStack);

		if (valueList.remove("~")) {
			AttributeMatcher keyMatcher = new NegativeMatcher(keyList, valueList);

			return new NegativeRule(elementMatcher, closedMatcher, zoomMin, zoomMax, keyMatcher);
		}

		AttributeMatcher keyMatcher = getKeyMatcher(keyList);
		AttributeMatcher valueMatcher = getValueMatcher(valueList);

		keyMatcher = RuleOptimizer.optimize(keyMatcher, ruleStack);
		valueMatcher = RuleOptimizer.optimize(valueMatcher, ruleStack);

		return new PositiveRule(elementMatcher, closedMatcher, zoomMin, zoomMax, keyMatcher,
				valueMatcher);
	}

	private final ArrayList<RenderingInstruction> renderingInstructions;
	private final ArrayList<Rule> subRules;

	final ClosedMatcher closedMatcher;
	final ElementMatcher elementMatcher;
	final byte zoomMax;
	final byte zoomMin;

	Rule(ElementMatcher elementMatcher, ClosedMatcher closedMatcher, byte zoomMin, byte zoomMax) {
		this.elementMatcher = elementMatcher;
		this.closedMatcher = closedMatcher;
		this.zoomMin = zoomMin;
		this.zoomMax = zoomMax;

		this.renderingInstructions = new ArrayList<RenderingInstruction>(4);
		this.subRules = new ArrayList<Rule>(4);
	}

	void addRenderingInstruction(RenderingInstruction renderingInstruction) {
		this.renderingInstructions.add(renderingInstruction);
	}

	void addSubRule(Rule rule) {
		this.subRules.add(rule);
	}

	abstract boolean matchesNode(List<Tag> tags, byte zoomLevel);

	abstract boolean matchesWay(List<Tag> tags, byte zoomLevel, Closed closed);

	void matchNode(RenderThemeCallback renderThemeCallback, List<Tag> tags, byte zoomLevel) {
		if (matchesNode(tags, zoomLevel)) {
			for (int i = 0, n = this.renderingInstructions.size(); i < n; ++i) {
				this.renderingInstructions.get(i).renderNode(renderThemeCallback, tags);
			}

			for (int i = 0, n = this.subRules.size(); i < n; ++i) {
				this.subRules.get(i).matchNode(renderThemeCallback, tags, zoomLevel);
			}
		}
	}

	void matchWay(RenderThemeCallback renderThemeCallback, List<Tag> tags, byte zoomLevel, Closed closed) {
		if (matchesWay(tags, zoomLevel, closed)) {
			for (int i = 0, n = this.renderingInstructions.size(); i < n; ++i) {
				this.renderingInstructions.get(i).renderWay(renderThemeCallback, tags);
			}

			for (int i = 0, n = this.subRules.size(); i < n; ++i) {
				this.subRules.get(i).matchWay(renderThemeCallback, tags, zoomLevel, closed);
			}
		}
	}

	void onComplete() {
		this.renderingInstructions.trimToSize();
		this.subRules.trimToSize();
		for (int i = 0, n = this.subRules.size(); i < n; ++i) {
			this.subRules.get(i).onComplete();
		}
	}

	void onDestroy() {
		for (int i = 0, n = this.renderingInstructions.size(); i < n; ++i) {
			this.renderingInstructions.get(i).onDestroy();
		}
		for (int i = 0, n = this.subRules.size(); i < n; ++i) {
			this.subRules.get(i).onDestroy();
		}
	}

	void scaleStrokeWidth(float scaleFactor) {
		for (int i = 0, n = this.renderingInstructions.size(); i < n; ++i) {
			this.renderingInstructions.get(i).scaleStrokeWidth(scaleFactor);
		}
		for (int i = 0, n = this.subRules.size(); i < n; ++i) {
			this.subRules.get(i).scaleStrokeWidth(scaleFactor);
		}
	}

	void scaleTextSize(float scaleFactor) {
		for (int i = 0, n = this.renderingInstructions.size(); i < n; ++i) {
			this.renderingInstructions.get(i).scaleTextSize(scaleFactor);
		}
		for (int i = 0, n = this.subRules.size(); i < n; ++i) {
			this.subRules.get(i).scaleTextSize(scaleFactor);
		}
	}
}