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
package org.mapsforge.server.routing.highwayHierarchies;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;

import org.mapsforge.core.DBConnection;
import org.mapsforge.preprocessing.graph.osm2rg.osmxml.TagHighway;
import org.mapsforge.preprocessing.graph.osm2rg.routingGraph.RgDAO;
import org.mapsforge.preprocessing.graph.osm2rg.routingGraph.RgEdge;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.Serializer;

public class RgEdgeNames implements Serializable {

	private static final long serialVersionUID = 2122661604323386224L;

	private final String[] names;
	private final int[] namesIndex;
	private final String[] refs;
	private final int[] refsIndex;
	private final byte[] flags;

	private RgEdgeNames(String[] names, int[] namesIndex, String[] refs, int[] refsIndex,
			byte[] flags) {
		this.names = names;
		this.namesIndex = namesIndex;
		this.refs = refs;
		this.refsIndex = refsIndex;
		this.flags = flags;
	}

	public String getName(int rgEdgeId) {
		if (rgEdgeId < 0 || rgEdgeId >= namesIndex.length || namesIndex[rgEdgeId] == -1) {
			return "";
		}
		return names[namesIndex[rgEdgeId]];
	}

	public String getRef(int rgEdgeId) {
		if (rgEdgeId < 0 || rgEdgeId >= refsIndex.length || refsIndex[rgEdgeId] == -1) {
			return "";
		}
		return refs[refsIndex[rgEdgeId]];
	}

	public boolean isMotorWayLink(int rgEdgeId) {
		return (flags[rgEdgeId] & 1) == 1;
	}

	public boolean isRoundabout(int rgEdgeId) {
		return (flags[rgEdgeId] & 2) == 2;
	}

	public int size() {
		return namesIndex.length;
	}

	public void serialize(OutputStream oStream) throws IOException {
		Serializer.serialize(oStream, this);
	}

	public static RgEdgeNames deserialize(InputStream iStream) throws IOException,
			ClassNotFoundException {
		return Serializer.deserialize(iStream);
	}

	public static RgEdgeNames importFromDb(Connection conn) throws SQLException {
		RgDAO rg = new RgDAO(conn);

		int[] namesIndex = new int[rg.getNumEdges()];
		int[] refsIndex = new int[rg.getNumEdges()];
		byte[] flags = new byte[rg.getNumEdges()];

		int counter = 0;
		// put all names on a map
		TObjectIntHashMap<String> namesMap = new TObjectIntHashMap<String>();
		TObjectIntHashMap<String> refsMap = new TObjectIntHashMap<String>();
		for (Iterator<RgEdge> iter = rg.getEdges().iterator(); iter.hasNext();) {
			RgEdge e = iter.next();
			String name = e.getName();
			String ref = e.getRef();
			if (name != null && !name.isEmpty()) {
				if (!namesMap.containsKey(name)) {
					namesMap.put(name, counter++);
				}
				int offset = namesMap.get(name);
				namesIndex[e.getId()] = offset;
			} else {
				namesIndex[e.getId()] = -1;
			}
			if (ref != null && !ref.isEmpty()) {
				if (!refsMap.containsKey(ref)) {
					refsMap.put(ref, counter++);
				}
				int offset = refsMap.get(ref);
				refsIndex[e.getId()] = offset;
			} else {
				refsIndex[e.getId()] = -1;
			}
			// Set additional flags
			byte flagByte = 0;
			if (e.getHighwayLevel().equals(TagHighway.MOTORWAY_LINK)) {
				flagByte = 1;
			}
			if (e.isRoundabout()) {
				flagByte = (byte) (flagByte + 2);
			}
			flags[e.getId()] = flagByte;
		}

		String[] names = new String[counter];
		String[] refs = new String[counter];
		for (Object s : namesMap.keys()) {
			String s1 = (String) s;
			names[namesMap.get(s)] = s1;
		}
		for (Object s : refsMap.keys()) {
			String s1 = (String) s;
			refs[refsMap.get(s)] = s1;
		}
		return new RgEdgeNames(names, namesIndex, refs, refsIndex, flags);
	}

	public static void main(String[] args) throws SQLException {
		Connection conn = DBConnection.getJdbcConnectionPg("localhost", 5432, "osm_base",
				"osm", "osm");
		RgEdgeNames edgeNames = importFromDb(conn);
		for (int i = 0; i < edgeNames.size(); i++) {

			// System.out.println(edgeNames.getName(i) + " " + edgeNames.getRef(i));
		}
	}
}
