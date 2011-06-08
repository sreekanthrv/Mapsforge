/*
 * Copyright 2010, 2011 mapsforge.org
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
package org.mapsforge.preprocessing.routingGraph.graphCreation;

import java.util.HashSet;
import java.util.Set;

import org.mapsforge.core.Edge;
import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.Vertex;

/**
 * An edge filled with maximal Data from OpenStreetMap.
 * 
 * @author Michael Bartel
 * 
 */
public class CompleteEdge implements Edge {

	int id;
	Vertex source;
	Vertex target;
	GeoCoordinate[] allWaypoints;
	String name;
	String type;
	boolean roundabout;
	boolean isOneWay;
	String ref;
	String destination;
	int weight;
	HashSet<KeyValuePair> additionalTags;

	/**
	 * The Constructor to create a CompleteEdge-instance
	 * 
	 * @param id
	 *            the OSM-ID
	 * @param source
	 *            The source-vertex of the edge.
	 * @param target
	 *            The target-vertex of the edge.
	 * @param waypoints
	 *            The waypoints excluding source and target.
	 * @param allWaypoints
	 *            The waypoints including source and target.
	 * @param name
	 *            The name of the street.
	 * @param type
	 *            The type of the street.
	 * @param roundabout
	 *            Is this way a roundabout.
	 * @param isOneWay
	 *            Is the street oneway?
	 * @param ref
	 *            This reference means another description of a street e.g "B15".
	 * @param destination
	 *            The destination of motor-links e.g. "Leipzig München".
	 * @param weight
	 *            The weight for routing.
	 * @param additionalTags
	 *            the additional Tags that exist for this way
	 * 
	 */
	public CompleteEdge(int id, Vertex source, Vertex target, GeoCoordinate[] waypoints,
			GeoCoordinate[] allWaypoints, String name, String type, boolean roundabout,
			boolean isOneWay, String ref,
			String destination, int weight, HashSet<KeyValuePair> additionalTags) {
		super();
		this.id = id;
		this.source = source;
		this.target = target;
		this.allWaypoints = allWaypoints;
		this.name = name;
		this.type = type;
		this.roundabout = roundabout;
		this.isOneWay = isOneWay;
		this.ref = ref;
		this.destination = destination;
		this.weight = weight;
		this.additionalTags = additionalTags;
	}

	/**
	 * Adds a new additional Tag key/value pair to this way
	 * 
	 * @param sp
	 *            the new pair to be added
	 */
	public void addAdditionalTags(KeyValuePair sp) {
		this.additionalTags.add(sp);
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public Vertex getSource() {
		return source;
	}

	@Override
	public Vertex getTarget() {
		return target;
	}

	@Override
	public GeoCoordinate[] getWaypoints() {
		GeoCoordinate[] wp = new GeoCoordinate[allWaypoints.length - 2];
		System.arraycopy(allWaypoints, 1, wp, 0, allWaypoints.length - 2);
		return wp;
	}

	@Override
	public GeoCoordinate[] getAllWaypoints() {
		return allWaypoints;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public boolean isRoundabout() {
		return roundabout;
	}

	@Override
	public String getRef() {
		return ref;
	}

	@Override
	public String getDestination() {
		return destination;
	}

	@Override
	public int getWeight() {
		return weight;
	}

	/**
	 * Returns the additional tags for this way
	 * 
	 * @return The set of restrictions for this way
	 */
	public Set<KeyValuePair> getAdditionalTags() {
		return additionalTags;
	}

	/**
	 * Returns true if the street is a oneway street
	 * 
	 * @return true if the street is a oneway street
	 */
	public boolean isOneWay() {
		return isOneWay;
	}

	@Override
	public String toString() {
		String s = "[Way " + this.id;
		s += " source-ID: " + this.source.getId();
		s += " target-ID: " + this.target.getId();
		s += " type: " + this.type;
		s += " WAYPOINTS ";
		for (GeoCoordinate geo : this.allWaypoints)
			s += geo.getLatitude() + " " + geo.getLongitude() + ", ";
		s += " TAGS ";
		for (KeyValuePair kv : this.additionalTags) {
			s += kv.toString() + ", ";
		}
		s += "]";
		return s;
	}

}