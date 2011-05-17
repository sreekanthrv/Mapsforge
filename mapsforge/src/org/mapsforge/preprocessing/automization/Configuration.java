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

package org.mapsforge.preprocessing.automization;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for configuration complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="configuration">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="pipeline" type="{http://mapsforge.org/mapsforge-preprocessing-conf}pipeline" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute name="osmosis-home" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="working-dir" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="output-dir" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="logging-dir" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="destination-dir" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="move" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "configuration", propOrder = { "pipeline" })
public class Configuration {

	/**
	 * A list of all pipelines that should run in this configuration.
	 */
	@XmlElement(required = true)
	private List<Pipeline> pipeline;

	/**
	 * The path to the osmosis home directory.
	 */
	@XmlAttribute(name = "osmosis-home", required = true)
	private String osmosisHome;

	/**
	 * The path to the working directory. This is the place where the configuration data (logs,
	 * files, maps, etc.) would be stored.
	 */
	@XmlAttribute(name = "working-dir", required = true)
	private String workingDir;
	/**
	 * The path to the directory where the generated content should be stored.
	 */
	@XmlAttribute(name = "output-dir", required = true)
	private String outputDir;
	@XmlAttribute(name = "logging-dir", required = true)
	private String loggingDir;
	@XmlAttribute(name = "destination-dir")
	private String destinationDir;
	@XmlAttribute
	private Boolean move;

	/**
	 * Gets the value of the pipeline property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any
	 * modification you make to the returned list will be present inside the JAXB object. This
	 * is why there is not a <CODE>set</CODE> method for the pipeline property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getPipeline().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Pipeline }
	 * 
	 * @return a list of all pipelines.
	 * 
	 * 
	 */
	public List<Pipeline> getPipeline() {
		if (pipeline == null) {
			pipeline = new ArrayList<Pipeline>();
		}
		return this.pipeline;
	}

	/**
	 * Gets the value of the osmosisHome property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getOsmosisHome() {
		return osmosisHome;
	}

	/**
	 * Sets the value of the osmosisHome property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setOsmosisHome(String value) {
		this.osmosisHome = value;
	}

	/**
	 * Gets the value of the workingDir property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getWorkingDir() {
		return workingDir;
	}

	/**
	 * Sets the value of the workingDir property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setWorkingDir(String value) {
		this.workingDir = value;
	}

	/**
	 * Gets the value of the outputDir property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getOutputDir() {
		return outputDir;
	}

	/**
	 * Sets the value of the outputDir property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setOutputDir(String value) {
		this.outputDir = value;
	}

	/**
	 * Gets the value of the loggingDir property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getLoggingDir() {
		return loggingDir;
	}

	/**
	 * Sets the value of the loggingDir property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setLoggingDir(String value) {
		this.loggingDir = value;
	}

	/**
	 * Gets the value of the destinationDir property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getDestinationDir() {
		return destinationDir;
	}

	/**
	 * Sets the value of the destinationDir property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setDestinationDir(String value) {
		this.destinationDir = value;
	}

	/**
	 * Gets the value of the move property.
	 * 
	 * @return possible object is {@link Boolean }
	 * 
	 */
	public boolean isMove() {
		if (move == null) {
			return false;
		}
		return move;
	}

	/**
	 * Sets the value of the move property.
	 * 
	 * @param value
	 *            allowed object is {@link Boolean }
	 * 
	 */
	public void setMove(Boolean value) {
		this.move = value;
	}

}
