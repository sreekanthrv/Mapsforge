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
package org.mapsforge.preprocessing.graph.interpreter.util;

import java.io.File;

public class FileLoader {

	public FileLoader() {
	}

	public File getOsmFile(String url) {
		/*
		 * vorerst sollen hier ein pfad angegeben werden wo sich die datei befindet, die
		 * aufgerufen werden soll alternativa kann man auch nur die datei angeben und dann wird
		 * in einem relativen pfad danach gesucht, also ein default ordner
		 * 
		 * sp�ter kann man �berlegen ob an dieser stelle auch files von geofabrik geladen werden
		 * soll
		 */
		url = "U:\\berlin.osm\\berlin.osm";
		/*
		 * url = "http://download.geofabrik.de/osm/"; File p = new File(url); try { URL u =
		 * p.toURI().toURL(); System.out.println(u.getProtocol()); } catch
		 * (MalformedURLException e) { // TODO Auto-generated catch block e.printStackTrace(); }
		 */
		return new File(url);
	}

	public static void main(String[] args) {
		FileLoader fl = new FileLoader();
		File f = fl.getOsmFile("");
		System.out.println(f.getPath());
	}

}