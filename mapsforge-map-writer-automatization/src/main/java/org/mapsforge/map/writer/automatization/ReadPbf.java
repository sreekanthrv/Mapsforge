/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.03.11 at 03:25:14 PM MEZ 
//

package org.mapsforge.map.writer.automatization;

import java.io.File;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for read-pbf complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="read-pbf">
 *   &lt;complexContent>
 *     &lt;extension base="{http://mapsforge.org/mapsforge-preprocessing-conf}source">
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "read-pbf")
public class ReadPbf extends Source {

	@Override
	public String generate(List<String> md5List, String absoluteWorkingDirPath, String absoluteOutputDirPath) {

		// check input file
		File inputFile = null;
		inputFile = FileOperation.createReadFile(absoluteWorkingDirPath, getFile());

		if (inputFile == null) {
			throw new RuntimeException("An unexpected error occured. File is null.");
		}

		// generate osmosis call
		final StringBuilder sb = new StringBuilder();
		sb.append("--rb file=").append(inputFile.getAbsolutePath()).append(" ");
		sb.append(super.generate(md5List, absoluteWorkingDirPath, absoluteOutputDirPath));

		return sb.toString();
	}
}
