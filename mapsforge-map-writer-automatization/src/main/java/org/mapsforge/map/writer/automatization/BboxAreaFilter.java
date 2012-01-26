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

// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.03.11 at 03:25:14 PM MEZ 
//

package org.mapsforge.map.writer.automatization;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for bbox-area-filter complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="bbox-area-filter">
 *   &lt;complexContent>
 *     &lt;extension base="{http://mapsforge.org/mapsforge-preprocessing-conf}sink-source">
 *       &lt;attribute name="minlat" use="required" type="{http://mapsforge.org/mapsforge-preprocessing-conf}latitude" />
 *       &lt;attribute name="minlon" use="required" type="{http://mapsforge.org/mapsforge-preprocessing-conf}longitude" />
 *       &lt;attribute name="maxlat" use="required" type="{http://mapsforge.org/mapsforge-preprocessing-conf}latitude" />
 *       &lt;attribute name="maxlon" use="required" type="{http://mapsforge.org/mapsforge-preprocessing-conf}longitude" />
 *       &lt;attribute name="completeWays" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;attribute name="completeRelations" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;attribute name="clipIncompleteEntities" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "bbox-area-filter")
public class BboxAreaFilter extends SinkSource {

	/**
	 * This is the value of the bottom edge of the bounding box.
	 */
	@XmlAttribute(required = true)
	private double minlat;

	/**
	 * This is the value of the left edge of the bounding box.
	 */
	@XmlAttribute(required = true)
	private double minlon;

	/**
	 * This is the value of the top edge of the bounding box.
	 */
	@XmlAttribute(required = true)
	private double maxlat;

	/**
	 * This is the value of the right edge of the bounding box.
	 */
	@XmlAttribute(required = true)
	private double maxlon;

	/**
	 * The parameter to turn on the completeWays function.
	 */
	@XmlAttribute(name = "completeWays")
	private Boolean completeWays;

	/**
	 * The parameter to turn on the completeRelations function.
	 */
	@XmlAttribute(name = "completeRelations")
	private Boolean completeRelations;

	/**
	 * The parameter to turn on the clipIncompleteEntities function.
	 */
	@XmlAttribute(name = "clipIncompleteEntities")
	private Boolean clipIncompleteEntities;

	/**
	 * Gets the value of the minlat property.
	 * 
	 * @return returns the latitude value of the bottom of the bounding box.
	 */
	public double getMinlat() {
		return this.minlat;
	}

	/**
	 * Sets the value of the minlat property.
	 * 
	 * @param value
	 *            the latitude value of the bottom of the bounding box.
	 */
	public void setMinlat(double value) {
		this.minlat = value;
	}

	/**
	 * Gets the value of the minlon property.
	 * 
	 * @return returns the value of the left boarder of the bounding box.
	 */
	public double getMinlon() {
		return this.minlon;
	}

	/**
	 * Sets the value of the minlon property.
	 * 
	 * @param value
	 *            the value of the left boarder of the bounding box.
	 */
	public void setMinlon(double value) {
		this.minlon = value;
	}

	/**
	 * Gets the value of the maxlat property.
	 * 
	 * @return returns the latitude value of the top of the bounding box.
	 */
	public double getMaxlat() {
		return this.maxlat;
	}

	/**
	 * Sets the value of the maxlat property.
	 * 
	 * @param value
	 *            the latitude value of the top of the bounding box.
	 */
	public void setMaxlat(double value) {
		this.maxlat = value;
	}

	/**
	 * Gets the value of the maxlon property.
	 * 
	 * @return returns the longitude value of the right boarder of the bounding box.
	 */
	public double getMaxlon() {
		return this.maxlon;
	}

	/**
	 * Sets the value of the maxlon property.
	 * 
	 * @param value
	 *            the longitude value of the right boarder of the bounding box.
	 */
	public void setMaxlon(double value) {
		this.maxlon = value;
	}

	/**
	 * Gets the value of the completeWays property.
	 * 
	 * @return possible object is {@link Boolean }
	 */
	public boolean isCompleteWays() {
		if (this.completeWays == null) {
			return false;
		}
		return this.completeWays;
	}

	/**
	 * Sets the value of the completeWays property.
	 * 
	 * @param value
	 *            allowed object is {@link Boolean }
	 */
	public void setCompleteWays(Boolean value) {
		this.completeWays = value;
	}

	/**
	 * Gets the value of the completeRelations property.
	 * 
	 * @return possible object is {@link Boolean }
	 */
	public boolean isCompleteRelations() {
		if (this.completeRelations == null) {
			return false;
		}
		return this.completeRelations;
	}

	/**
	 * Sets the value of the completeRelations property.
	 * 
	 * @param value
	 *            allowed object is {@link Boolean }
	 */
	public void setCompleteRelations(Boolean value) {
		this.completeRelations = value;
	}

	/**
	 * Gets the value of the clipIncompleteEntities property.
	 * 
	 * @return possible object is {@link Boolean }
	 */
	public boolean isClipIncompleteEntities() {
		if (this.clipIncompleteEntities == null) {
			return false;
		}
		return this.clipIncompleteEntities;
	}

	/**
	 * Sets the value of the clipIncompleteEntities property.
	 * 
	 * @param value
	 *            allowed object is {@link Boolean }
	 */
	public void setClipIncompleteEntities(Boolean value) {
		this.clipIncompleteEntities = value;
	}

	@Override
	public String generate(List<String> md5List, String absoluteWorkingDirPath, String absoluteOutputDirPath) {

		/*
		 * Generate the string for the procedure call of the osmosis pipeline task to extract a bounding box of an .osm
		 * file. The enlargment of the bounding box is parameterized by the attributes minlon, minlat, maxlon and
		 * maxlat.
		 */

		final StringBuilder sb = new StringBuilder();

		sb.append("--bb").append(" ");
		sb.append("left=").append(this.minlon).append(" ");
		sb.append("right=").append(this.maxlon).append(" ");
		sb.append("bottom=").append(this.minlat).append(" ");
		sb.append("top=").append(this.maxlat).append(" ");

		if (this.completeWays != null) {
			sb.append("completeWays=").append(this.completeWays).append(" ");
		}
		if (this.completeRelations != null) {
			sb.append("completeRelations=").append(this.completeRelations).append(" ");
		}
		if (this.clipIncompleteEntities != null) {
			sb.append("clipIncompleteEntities=").append(this.clipIncompleteEntities).append(" ");
		}

		sb.append(super.generate(md5List, absoluteWorkingDirPath, absoluteOutputDirPath));

		return sb.toString();
	}
}
