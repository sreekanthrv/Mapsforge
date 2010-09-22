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
package org.mapsforge.preprocessing.routing.hhmobile;

import java.io.Serializable;
import java.util.Collection;

/**
 * A Clustering represents a set of clusters, which are disjoint sets of vertices.
 */
public interface Clustering extends Serializable {

	/**
	 * Lookup the cluster the given vertex belongs to.
	 * 
	 * @param vertexId
	 *            the vertex whose cluster is queried.
	 * @return the cluster the given vertex belongs to.
	 */
	public Cluster getCluster(int vertexId);

	/**
	 * @return all clusters within this clustering.
	 */
	public Collection<? extends Cluster> getClusters();

	/**
	 * @return the number of clusters.
	 */
	public int size();

}
