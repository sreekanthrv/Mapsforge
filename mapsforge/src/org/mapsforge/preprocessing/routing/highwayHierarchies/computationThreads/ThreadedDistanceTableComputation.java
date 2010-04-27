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
package org.mapsforge.preprocessing.routing.highwayHierarchies.computationThreads;

import java.util.Iterator;
import java.util.LinkedList;

import org.mapsforge.preprocessing.routing.highwayHierarchies.algorithm.DijkstraAlgorithm;
import org.mapsforge.preprocessing.routing.highwayHierarchies.algorithm.DijkstraAlgorithm.DijkstraTreeVertex;
import org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures.DistanceTable;
import org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures.HHDynamicGraph;
import org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures.HHDynamicGraph.HHDynamicVertex;

/**
 * @author Frank Viernau
 */
public class ThreadedDistanceTableComputation extends ComputationThread {

	private final Iterator<HHDynamicVertex> iter;
	private final DistanceTable table;
	private final int lvl;
	private volatile int progress;

	private ThreadedDistanceTableComputation(Iterator<HHDynamicVertex> iter, int lvl,
			DistanceTable dt) {
		this.iter = iter;
		this.lvl = lvl;
		this.table = dt;
	}

	public static DistanceTable computeCoreDistanceTable(HHDynamicGraph graph, int lvl,
			int numThreads) {
		LinkedList<Integer> coreVertices = new LinkedList<Integer>();
		for (Iterator<HHDynamicVertex> iter = graph.getVertices(lvl); iter.hasNext();) {
			HHDynamicVertex v = iter.next();
			if (v.getNeighborhood(lvl) != Integer.MAX_VALUE) {
				coreVertices.add(v.getId());
			}
		}

		Iterator<HHDynamicVertex> iter = graph.getVertices(lvl);
		DistanceTable table = new DistanceTable(coreVertices);
		ThreadedDistanceTableComputation[] threads = new ThreadedDistanceTableComputation[numThreads];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new ThreadedDistanceTableComputation(iter, lvl, table);
		}
		ThreadedComputation.executeThreads(threads, coreVertices.size(),
				"computeDistanceTable(lvl=" + lvl + ",size=" + coreVertices.size() + "x"
						+ coreVertices.size() + ")");
		return table;
	}

	@Override
	public void run() {
		while (iter.hasNext()) {
			HHDynamicVertex v = iter.next();
			if (v != null && v.getNeighborhood(lvl) != Integer.MAX_VALUE) {
				LinkedList<DijkstraTreeVertex> spTree = DijkstraAlgorithm.shortestPathTree(v,
						true, false, lvl);
				for (DijkstraTreeVertex dtv : spTree) {
					if (dtv.vertex.getNeighborhood(lvl) != Integer.MAX_VALUE) {
						table.set(v.getId(), dtv.vertex.getId(), dtv.distance);
					}
				}
				progress++;
			}
		}
	}

	@Override
	public int progress() {
		return progress;
	}
}