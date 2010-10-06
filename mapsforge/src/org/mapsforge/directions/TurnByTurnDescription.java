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
package org.mapsforge.directions;

import java.io.FileInputStream;
import java.util.Vector;

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.MercatorProjection;
import org.mapsforge.preprocessing.graph.osm2rg.osmxml.TagHighway;
import org.mapsforge.server.routing.IEdge;
import org.mapsforge.server.routing.IRouter;
import org.mapsforge.server.routing.IVertex;
import org.mapsforge.server.routing.highwayHierarchies.HHRouterServerside;

/**
 * Turn by turn directions contain a way which was found by a routing algorithm. Streets which
 * are of them same name are concatenated, except in case of a U-turn. Street lengths and angles
 * between streets are calculated and saved.
 * 
 * Several methods to get a string representation like GeoJSON, KML or plain text are provided.
 * 
 * @author Eike Send
 */
public class TurnByTurnDescription {
	private static final int NO_MODE = -1;
	private static final int MOTORWAY_MODE = 0;
	private static final int CITY_MODE = 1;
	private static final int REGIONAL_MODE = 2;
	private static final double VERY_SHORT_STREET_LENGTH = 30;
	Vector<TurnByTurnStreet> streets = new Vector<TurnByTurnStreet>();
	LandmarksFromPerst landmarkService;

	/**
	 * Constructs a TurnByTurnDirectionsObject from an array of IEdges as the are provided by
	 * the IRouter
	 * 
	 * @param routeEdges
	 *            is the IEdges array to convert to directions
	 */
	public TurnByTurnDescription(IEdge[] routeEdges) {
		generateDirectionsFromPath(routeEdges);
	}

	void generateDirectionsFromPath(IEdge[] edges) {
		if (edges.length == 0)
			return;
		// These are the edges which are used to make decisions based on local information
		IEdge lastEdge;
		IEdge edgeBeforePoint;
		IEdge edgeAfterPoint;
		IEdge nextEdge;
		// These are both the current decision point, which is at the end of the current edge
		IVertex decisionPointVertex;
		GeoCoordinate decisionPointCoord;
		// These don't change in the process, they are the beginning and end of the route
		GeoCoordinate startPoint = edges[0].getSource().getCoordinate();
		GeoCoordinate endPoint = edges[edges.length - 1].getTarget().getCoordinate();
		// TODO: get start and finishing city with radius
		TurnByTurnCity startCity = getCityFromCoords(startPoint);
		TurnByTurnCity endCity = getCityFromCoords(endPoint);
		// this contains concatenated IEdges and represents the current street / road
		TurnByTurnStreet currentStreet = new TurnByTurnStreet(edges[0]);
		// What navigational mode is the current and what was the last one
		int routingMode = NO_MODE;
		int lastRoutingMode = NO_MODE;
		// The whole point of this method boils down to the question if at a given potential
		// decision point a new instruction is to be generated. This boolean represents that.
		boolean startANewStreet;
		for (int i = 0; i < edges.length; i++) {
			// First setup the "environment" variables, ie the edges and points around the
			// potential decision point
			edgeBeforePoint = edges[i];
			decisionPointVertex = edgeBeforePoint.getTarget();
			decisionPointCoord = decisionPointVertex.getCoordinate();
			if (i > 0) {
				lastEdge = edges[i - 1];
			} else {
				lastEdge = null;
			}
			if (i < edges.length - 1) {
				edgeAfterPoint = edges[i + 1];
			} else {
				edgeAfterPoint = null;
			}
			if (i < edges.length - 2) {
				nextEdge = edges[i + 2];
			} else {
				nextEdge = null;
			}
			// Now the variables are set up.
			// First determine which kind of navigational level we're on
			lastRoutingMode = routingMode;
			// if we're on a motorway
			if (isMotorway(edgeBeforePoint)) {
				routingMode = MOTORWAY_MODE;
			}
			// if we're in the start or destination city, we'll do local navigation
			else if (isInStartOrDestinationCity(startCity, endCity, decisionPointCoord)) {
				routingMode = CITY_MODE;
			}
			// if we're not in the start- or end city but on a primary again its motorway
			// routing
			else if (isPrimary(edgeBeforePoint)) {
				routingMode = MOTORWAY_MODE;
			} else {
				// if we're not in the start- or end city and not on a motorway, trunk or
				// primary we must be in regional mode
				routingMode = REGIONAL_MODE;
			}
			// Now that the mode of travel has been determined we need to figure out if a new
			// street is to be started
			startANewStreet = false;
			switch (routingMode) {
				case CITY_MODE:
					startANewStreet = startNewStreetCityMode(lastEdge, edgeBeforePoint,
							edgeAfterPoint, nextEdge, currentStreet);
					break;
				case REGIONAL_MODE:

					startANewStreet = startNewStreetRegionalMode(lastEdge, edgeBeforePoint,
							edgeAfterPoint, nextEdge, currentStreet);
					break;
				case MOTORWAY_MODE:
					startANewStreet = startNewStreetMotorwayMode(lastEdge, edgeBeforePoint,
							edgeAfterPoint, nextEdge, currentStreet);
					break;
			}
			if (lastRoutingMode == NO_MODE) {
				lastRoutingMode = routingMode;
			}
			if (lastRoutingMode != routingMode) {
				startANewStreet = true;
			}
			if (startANewStreet) {
				if (currentStreet.angleFromStreetLastStreet == -360) {
					double delta = getAngleOfStreets(lastEdge, edgeBeforePoint);
					currentStreet.angleFromStreetLastStreet = delta;
				}
				streets.add(currentStreet);
				if (edgeAfterPoint != null)
					currentStreet = new TurnByTurnStreet(edgeAfterPoint);
			} else {
				currentStreet.appendCoordinatesFromEdge(edgeAfterPoint);
			}
		}
	}

	private boolean isInStartOrDestinationCity(TurnByTurnCity start, TurnByTurnCity end,
			GeoCoordinate decisionPointCoord) {
		if (start == null || end == null)
			return true;
		return (start.contains(decisionPointCoord) || end.contains(decisionPointCoord));
	}

	private TurnByTurnCity getCityFromCoords(GeoCoordinate point) {
		// TODO Auto-generated method stub
		return null;
	}

	private boolean startNewStreetCityMode(IEdge lastEdge, IEdge edgeBeforePoint,
			IEdge edgeAfterPoint,
			IEdge nextEdge, TurnByTurnStreet currentStreet) {
		// Only one instruction per U-turn is necessary
		// also U-Turns are really the sum of two right angle turns
		if (isUTurn(lastEdge, edgeBeforePoint, edgeAfterPoint)) {
			currentStreet.angleFromStreetLastStreet = 180;
			return false;
		}
		if (haveSameName(edgeBeforePoint, edgeAfterPoint)) {
			// If a U-Turn is performed an instruction is needed
			if (isUTurn(edgeBeforePoint, edgeAfterPoint, nextEdge)) {
				return true;
			}
			return false;
		}
		if (isInTwoLaneJunction(lastEdge, edgeBeforePoint, edgeAfterPoint, nextEdge,
				currentStreet)) {
			return false;
		}
		return true;
	}

	private boolean isInTwoLaneJunction(IEdge lastEdge, IEdge edgeBeforePoint,
			IEdge edgeAfterPoint, IEdge nextEdge, TurnByTurnStreet currentStreet) {
		// If the edge after the decision point is very short and followed by a right angle,
		// the edgeAfterPoint is part of a two lane junction where the name is the one of
		// the street coming from the other direction
		if (isRightAngle(getAngleOfStreets(edgeAfterPoint, nextEdge))
				&& isVeryShortEdge(edgeAfterPoint)) {
			return true;
		}
		// If there was a right angle turn between the last edge and the edge before the
		// decision point and this edge is very short, the edgeBeforePoint is part of a two lane
		// junction and no instruction is needed, only the name should be that of the actual
		// street
		if (isRightAngle(getAngleOfStreets(lastEdge, edgeBeforePoint))
				&& isVeryShortEdge(edgeBeforePoint) && edgeAfterPoint != null) {
			currentStreet.name = edgeAfterPoint.getName();
			return true;
		}
		return false;
	}

	private boolean startNewStreetRegionalMode(IEdge lastEdge, IEdge currentEdge,
			IEdge nextEdge, IEdge secondNextEdge, TurnByTurnStreet currentStreet) {
		return true;
	}

	private boolean startNewStreetMotorwayMode(IEdge lastEdge, IEdge currentEdge,
			IEdge nextEdge, IEdge secondNextEdge, TurnByTurnStreet currentStreet) {
		return true;
	}

	private boolean isVeryShortEdge(IEdge edge) {
		GeoCoordinate source = edge.getSource().getCoordinate();
		GeoCoordinate destination = edge.getTarget().getCoordinate();
		return source.sphericalDistance(destination) < VERY_SHORT_STREET_LENGTH;
	}

	private boolean isRightAngle(double angle) {
		return (90d - 45d < angle && angle < 90d + 45d)
				|| (270d - 45d < angle && angle < 270d + 45d);
	}

	private boolean haveSameName(IEdge edge1, IEdge edge2) {
		if (edge2 == null)
			return false;
		if (edge1 == null)
			return false;
		if (edge1.getName() == null && edge2.getName() == null)
			return true;
		if (edge1.getName() == null || edge2.getName() == null)
			return false;
		return edge1.getName().equalsIgnoreCase(edge2.getName());
	}

	private boolean isMotorway(IEdge curEdge) {
		return curEdge.getType() == TagHighway.MOTORWAY ||
				curEdge.getType() == TagHighway.MOTORWAY_LINK ||
				curEdge.getType() == TagHighway.TRUNK ||
				curEdge.getType() == TagHighway.TRUNK_LINK;
	}

	private boolean isPrimary(IEdge curEdge) {
		return curEdge.getType() == TagHighway.PRIMARY ||
				curEdge.getType() == TagHighway.PRIMARY_LINK;
	}

	/**
	 * Check 3 edges to see if they form a U-turn.
	 * 
	 * @param edge1
	 *            second last Edge before the current edge
	 * @param edge2
	 *            last Edge before the current edge
	 * @param edge3
	 *            current Edge
	 * @return true if the edges form a u-turn around the 2nd edge
	 */
	boolean isUTurn(IEdge edge1, IEdge edge2, IEdge edge3) {
		if (edge1 == null || edge2 == null || edge3 == null)
			return false;
		double angleSum = (getAngleOfStreets(edge1, edge2) + getAngleOfStreets(
				edge2, edge3)) % 360;
		if (haveSameName(edge1, edge3)
				&& (170 < angleSum && angleSum < 190)
				&& isVeryShortEdge(edge2)) {
			return true;
		}
		return false;
	}

	/**
	 * Calculate the angle between two IEdge objects / streets
	 * 
	 * @param edge1
	 *            the IEdge of the street before the crossing
	 * @param edge2
	 *            the IEdge of the street after the crossing
	 * @return the angle between the given streets
	 */
	static double getAngleOfStreets(IEdge edge1, IEdge edge2) {
		double delta = -360.0;
		if (edge1 != null && edge2 != null) {
			// Let's see if i can get the angle between the last street and this
			// This is the crossing
			GeoCoordinate crossingCoordinate = edge2.getAllWaypoints()[0];
			// The following is the last coordinate before the crossing
			GeoCoordinate lastCoordinate = edge1.getAllWaypoints()[edge1
					.getAllWaypoints().length - 2];
			// Take a coordinate further away from the crossing if it's too close
			if (lastCoordinate.sphericalDistance(crossingCoordinate) < 10
					&& edge1.getAllWaypoints().length > 2) {
				lastCoordinate = edge1.getAllWaypoints()[edge1
						.getAllWaypoints().length - 3];
			}
			// Here comes the first coordinate after the crossing
			GeoCoordinate firstCoordinate = edge2.getAllWaypoints()[1];
			if (firstCoordinate.sphericalDistance(crossingCoordinate) < 10
					&& edge2.getAllWaypoints().length > 2) {
				firstCoordinate = edge2.getAllWaypoints()[2];
			}
			// calculate angles of the incoming street
			double deltaY = MercatorProjection.latitudeToMetersY(crossingCoordinate
					.getLatitude())
					- MercatorProjection.latitudeToMetersY(lastCoordinate.getLatitude());
			double deltaX = MercatorProjection.longitudeToMetersX(crossingCoordinate
					.getLongitude())
					- MercatorProjection.longitudeToMetersX(lastCoordinate.getLongitude());
			double alpha = java.lang.Math.toDegrees(java.lang.Math.atan(deltaX / deltaY));
			if (deltaY < 0)
				alpha += 180; // this compensates for the atan result being between -90 and +90
			// deg
			// calculate angles of the outgoing street
			deltaY = MercatorProjection.latitudeToMetersY(firstCoordinate.getLatitude())
					- MercatorProjection.latitudeToMetersY(crossingCoordinate.getLatitude());
			deltaX = MercatorProjection.longitudeToMetersX(firstCoordinate.getLongitude())
					- MercatorProjection.longitudeToMetersX(crossingCoordinate.getLongitude());
			double beta = java.lang.Math.toDegrees(java.lang.Math.atan(deltaX / deltaY));
			if (deltaY < 0)
				beta += 180; // this compensates for the atan result being between -90 and +90
			// deg
			// the angle difference is angle of the turn,
			delta = alpha - beta;
			// For some reason the angle is conterclockwise, so it's turned around
			delta = 360 - delta;
			// make sure there are no values above 360 or below 0
			delta = java.lang.Math.round((delta + 360) % 360);
		}
		return delta;
	}

	/**
	 * @param args
	 *            unused
	 */
	public static void main(String[] args) {
		try {
			long time = System.currentTimeMillis();
			FileInputStream iStream = new FileInputStream("C:/uni/berlin_car.hh");
			IRouter router = HHRouterServerside.deserialize(iStream);
			iStream.close();
			time = System.currentTimeMillis() - time;
			System.out.println("Loaded Router in " + time + " ms");
			time = System.currentTimeMillis();
			String filename = "c:/uni/berlin_landmarks.dbs.clustered";
			LandmarksFromPerst landmarkService = new LandmarksFromPerst(filename);
			TurnByTurnStreet.landmarkService = landmarkService;
			time = System.currentTimeMillis() - time;
			System.out.println("Loaded LandmarkBuilder in " + time + " ms");
			int source = router
						.getNearestVertex(new GeoCoordinate(52.53156, 13.40274)).getId();
			int target = router.getNearestVertex(
						new GeoCoordinate(52.49246, 13.41722)).getId();
			IEdge[] shortestPath = router.getShortestPath(source, target);

			time = System.currentTimeMillis() - time;
			TurnByTurnDescription directions = new TurnByTurnDescription(shortestPath);
			time = System.currentTimeMillis() - time;
			System.out.println("Route directions built in " + time + " ms");
			System.out.println();
			System.out.println(new TurnByTurnDescriptionToString(directions));
			landmarkService.persistenceManager.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}