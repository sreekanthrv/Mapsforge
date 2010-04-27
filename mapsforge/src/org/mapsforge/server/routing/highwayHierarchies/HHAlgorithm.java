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

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.LinkedList;

import org.mapsforge.preprocessing.routing.highwayHierarchies.HHComputation;
import org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures.DistanceTable;
import org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures.HHStaticGraph;
import org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures.HHStaticGraph.HHStaticEdge;
import org.mapsforge.preprocessing.routing.highwayHierarchies.datastructures.HHStaticGraph.HHStaticVertex;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.prioQueue.BinaryMinHeap;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.prioQueue.IBinaryHeapItem;

/**
 * @author Frank Viernau
 * 
 *         Implementation of the highway hierarchies algorithm. Four versions are supported,
 *         combinations of distancetable avail. / NA and downgrade edges yes/ no are supported.
 *         Fastest is distanceTable + downgraded edges. This implementiation should be very
 *         close to the description in the dissertation on routeplanning in road networks from
 *         D. Schultes, see Trac.
 * 
 *         Temporary data is not stored by direct adress tables but by using a hashtable, which
 *         saves memory especially important with regard to parallel queries. Performance loss
 *         not evaluated til now.
 * 
 *         For experimantal verification, two versions of dijkstra are implemented here too.
 */
public class HHAlgorithm {

	private static final int INFINITY_1 = HHComputation.INFINITY_1;
	private static final int INFINITY_2 = HHComputation.INFINITY_2;

	private static final int INITIAL_QUEUE_SIZE = 300;
	private static final int INITIAL_MAP_SIZE = 5000;
	private static final int HEAP_IDX_SETTLED = -123456789;

	private static final int FWD = 0;
	private static final int BWD = 1;

	private BinaryMinHeap<DiscoveredVertex, HeapKey>[] queue;
	private TIntObjectHashMap<DiscoveredVertex>[] discoveredVertices;

	public HHAlgorithm() {
		queue = new BinaryMinHeap[] {
				new BinaryMinHeap<DiscoveredVertex, HeapKey>(INITIAL_QUEUE_SIZE),
				new BinaryMinHeap<DiscoveredVertex, HeapKey>(INITIAL_QUEUE_SIZE) };
		discoveredVertices = new TIntObjectHashMap[] {
				new TIntObjectHashMap<DiscoveredVertex>(INITIAL_MAP_SIZE),
				new TIntObjectHashMap<DiscoveredVertex>(INITIAL_MAP_SIZE) };
	}

	/**
	 * Adds edges along shortest path to the two buffers. Edges in buffFwd are sorted starting
	 * at source to target. Edges in buffBwd are sorted starting at target to source.
	 * 
	 * @param graph
	 * @param sourceId
	 *            vertexId of target, not checked if valid.
	 * @param targetId
	 *            vertexId of target, not checked if valid.
	 * @param dt
	 *            can be null.
	 * @param buffFwd
	 *            must be empty.
	 * @param buffBwd
	 *            must be empty.
	 * @param buffSearchSpace
	 * @return sum of edge costs along shortest path.
	 */
	public int shortestPath(HHStaticGraph graph, int sourceId, int targetId, DistanceTable dt,
			LinkedList<HHStaticEdge> buffFwd, LinkedList<HHStaticEdge> buffBwd,
			LinkedList<HHStaticEdge> buffSearchSpace) {
		if (dt != null) {
			if (graph.getGraphPropterties().downgradedEdges) {
				return shortestPathDtYesDowngradedYes(graph, sourceId, targetId, dt, buffFwd,
						buffBwd, buffSearchSpace);
			}
			return shortestPathDtYesDowngradedNo(graph, sourceId, targetId, dt, buffFwd,
					buffBwd, buffSearchSpace);

		}
		if (graph.getGraphPropterties().downgradedEdges) {
			return shortestPathDtNoDowngradedYes(graph, sourceId, targetId, buffFwd, buffBwd,
					buffSearchSpace);
		}
		return shortestPathDtNoDowngradedNo(graph, sourceId, targetId, buffFwd, buffBwd,
				buffSearchSpace);

	}

	private int shortestPathDtYesDowngradedYes(HHStaticGraph graph, int sourceId, int targetId,
			DistanceTable dt, LinkedList<HHStaticEdge> buffFwd,
			LinkedList<HHStaticEdge> buffBwd, LinkedList<HHStaticEdge> buffSearchSpace) {
		HHStaticVertex source = graph.getVertex(sourceId);
		HHStaticVertex target = graph.getVertex(targetId);

		// tentative shortest distance (upper bound)
		int d = Integer.MAX_VALUE;
		DiscoveredVertex minSearchScopeHit = null;

		// clear queue
		LinkedList<DiscoveredVertex>[] I = new LinkedList[] {
				new LinkedList<DiscoveredVertex>(), new LinkedList<DiscoveredVertex>() };
		queue[FWD].clear();
		queue[BWD].clear();
		discoveredVertices[FWD].clear();
		discoveredVertices[BWD].clear();

		// enqueue source and target
		DiscoveredVertex s = new DiscoveredVertex(source, null, null, new HeapKey(0, 0, source
				.getNeighborhood(0)));
		DiscoveredVertex t = new DiscoveredVertex(target, null, null, new HeapKey(0, 0, target
				.getNeighborhood(0)));
		queue[FWD].insert(s);
		queue[BWD].insert(t);
		discoveredVertices[FWD].put(source.getId(), s);
		discoveredVertices[BWD].put(target.getId(), t);

		int direction = FWD;
		while (!queue[FWD].isEmpty() || !queue[BWD].isEmpty()) {
			// switch search direction if queue of current direction is empty
			if (queue[direction].isEmpty()) {
				direction = (direction + 1) % 2;
			}

			// dequeue vertex u
			DiscoveredVertex u = queue[direction].extractMin();
			u.heapIdx = HEAP_IDX_SETTLED;

			// abort criteria for current direction
			if (u.key.distance > d) {
				queue[direction].clear();
				continue;
			}

			// adjust lower bound if u was settled in both directions
			DiscoveredVertex u_ = discoveredVertices[(direction + 1) % 2].get(u.vertex.getId());
			if (u_ != null && u_.heapIdx == HEAP_IDX_SETTLED) {
				int d_ = u.key.distance + u_.key.distance;
				if (d_ < d) {
					minSearchScopeHit = u;
					d = d_;
				}
			}

			if (u.key.gap >= INFINITY_2) {
				// reached top level core ?
				if (u.key.gap == INFINITY_2) {
					I[direction].add(u);
					continue;
				}
				u.key.gap = u.vertex.getNeighborhood(u.key.level);
			}
			// relax adjacent edges
			for (HHStaticEdge e : u.vertex.getAdjacentEdges(u.key.level)) {
				// if edge is not in graph for current direction -> skip
				if (!e.getDirection(direction))
					continue;
				int gap_ = u.key.gap;

				// switch to next level
				int lvl = u.key.level;
				while (e.getWeight() > gap_ && lvl < u.vertex.getLevel()) {
					lvl++;
					gap_ = u.vertex.getNeighborhood(lvl);
				}

				// check if edge's level is high enough
				if (!e.isLvlGEQ(lvl)) {
					continue;
				}
				// restriction 1 (only local search)
				if (e.getWeight() > gap_) {
					continue;
				}

				if (gap_ < INFINITY_2) {
					gap_ = gap_ - e.getWeight();
				}
				// only for debug
				buffSearchSpace.add(e);

				// adjust v's heap key, enqueue if not already on heap (relax the edge)
				HeapKey key = new HeapKey(u.key.distance + e.getWeight(), lvl, gap_);
				DiscoveredVertex v = discoveredVertices[direction].get(e.getTarget().getId());
				if (v != null) {
					if (key.compareTo(v.key) < 0) {
						queue[direction].decreaseKey(v, key);
						v.parent = u;
						v.edgeToParent = e;
					}
				} else {
					v = new DiscoveredVertex(e.getTarget(), e, u, key);
					discoveredVertices[direction].put(v.vertex.getId(), v);
					queue[direction].insert(v);
				}
			}
			direction = (direction + 1) % 2;
		}

		int d_ = getShortestDistanceByTable(I[FWD], I[BWD], dt);
		if (d_ < d) {
			// shortest path is between top level core vertices
			addEdgesToAllParents(I[FWD].getFirst(), buffFwd);
			getShortestPathByTable(graph, I[FWD].getFirst().vertex, I[BWD].getFirst().vertex,
					dt, buffFwd);
			addEdgesToAllParents(I[BWD].getFirst(), buffBwd);
		} else if (d != Integer.MAX_VALUE) {
			// shortest path is found in lower levels
			addEdgesToAllParents(discoveredVertices[FWD].get(minSearchScopeHit.vertex.getId()),
					buffFwd);
			addEdgesToAllParents(discoveredVertices[BWD].get(minSearchScopeHit.vertex.getId()),
					buffFwd);
		}
		return Math.min(d, d_);
	}

	private int shortestPathDtYesDowngradedNo(HHStaticGraph graph, int sourceId, int targetId,
			DistanceTable dt, LinkedList<HHStaticEdge> buffFwd,
			LinkedList<HHStaticEdge> buffBwd, LinkedList<HHStaticEdge> buffSearchSpace) {

		HHStaticVertex source = graph.getVertex(sourceId);
		HHStaticVertex target = graph.getVertex(targetId);

		DiscoveredVertex minSearchScopeHit = null;

		// tentative shortest distance (upper bound)
		int d = Integer.MAX_VALUE;

		// clear queue
		LinkedList<DiscoveredVertex>[] I = new LinkedList[] {
				new LinkedList<DiscoveredVertex>(), new LinkedList<DiscoveredVertex>() };
		queue[FWD].clear();
		queue[BWD].clear();
		discoveredVertices[FWD].clear();
		discoveredVertices[BWD].clear();

		// enqueue source and target
		DiscoveredVertex s = new DiscoveredVertex(source, null, null, new HeapKey(0, 0, source
				.getNeighborhood(0)));
		DiscoveredVertex t = new DiscoveredVertex(target, null, null, new HeapKey(0, 0, target
				.getNeighborhood(0)));
		queue[FWD].insert(s);
		queue[BWD].insert(t);
		discoveredVertices[FWD].put(source.getId(), s);
		discoveredVertices[BWD].put(target.getId(), t);

		int direction = FWD;
		while (!queue[FWD].isEmpty() || !queue[BWD].isEmpty()) {
			// switch search direction if queue of current direction is empty
			if (queue[direction].isEmpty()) {
				direction = (direction + 1) % 2;
			}

			// dequeue vertex u
			DiscoveredVertex u = queue[direction].extractMin();
			u.heapIdx = HEAP_IDX_SETTLED;

			// abort criteria for current direction
			if (u.key.distance > d) {
				queue[direction].clear();
				continue;
			}

			// adjust lower bound if u was settled in both directions
			DiscoveredVertex u_ = discoveredVertices[(direction + 1) % 2].get(u.vertex.getId());
			if (u_ != null && u_.heapIdx == HEAP_IDX_SETTLED) {
				int d_ = u.key.distance + u_.key.distance;
				if (d_ < d) {
					minSearchScopeHit = u;
					d = d_;
				}
			}

			if (u.key.gap >= INFINITY_2) {
				// reached top level core ?
				if (u.key.gap == INFINITY_2) {
					I[direction].add(u);
					continue;
				}
				u.key.gap = u.vertex.getNeighborhood(u.key.level);
			}
			// relax adjacent edges
			for (HHStaticEdge e : u.vertex.getAdjacentEdges(u.key.level)) {
				// if edge is not in graph for current direction -> skip
				if (!e.getDirection(direction))
					continue;
				int gap_ = u.key.gap;

				// switch to next level
				int lvl = u.key.level;
				while (e.getWeight() > gap_ && lvl < u.vertex.getLevel()) {
					lvl++;
					gap_ = u.vertex.getNeighborhood(lvl);
				}

				// check if edge's level is high enough
				if (!e.isLvlGEQ(lvl)) {
					continue;
				}
				// restriction 1 (only local search)
				if (e.getWeight() > gap_) {
					continue;
				}

				// restriction 2 (don't leave core)
				if (u.vertex.getNeighborhood(lvl) < INFINITY_2
						&& e.getTarget().getNeighborhood(lvl) == INFINITY_1) {
					continue;
				}

				if (gap_ < INFINITY_2) {
					gap_ = gap_ - e.getWeight();
				}

				// only for debug
				buffSearchSpace.add(e);

				// adjust v's heap key, enqueue if not already on heap (relax the edge)
				HeapKey key = new HeapKey(u.key.distance + e.getWeight(), lvl, gap_);
				DiscoveredVertex v = discoveredVertices[direction].get(e.getTarget().getId());
				if (v != null) {
					if (key.compareTo(v.key) < 0) {
						queue[direction].decreaseKey(v, key);
						v.parent = u;
						v.edgeToParent = e;
					}
				} else {
					v = new DiscoveredVertex(e.getTarget(), e, u, key);
					discoveredVertices[direction].put(v.vertex.getId(), v);
					queue[direction].insert(v);
				}
			}
			direction = (direction + 1) % 2;
		}
		int d_ = getShortestDistanceByTable(I[FWD], I[BWD], dt);
		if (d_ < d) {
			// shortest path is between top level core vertices
			addEdgesToAllParents(I[FWD].getFirst(), buffFwd);
			getShortestPathByTable(graph, I[FWD].getFirst().vertex, I[BWD].getFirst().vertex,
					dt, buffFwd);
			addEdgesToAllParents(I[BWD].getFirst(), buffBwd);
		} else if (d != Integer.MAX_VALUE) {
			// shortest path is found in lower levels
			addEdgesToAllParents(discoveredVertices[FWD].get(minSearchScopeHit.vertex.getId()),
					buffFwd);
			addEdgesToAllParents(discoveredVertices[BWD].get(minSearchScopeHit.vertex.getId()),
					buffFwd);
		}
		return Math.min(d, d_);
	}

	private int shortestPathDtNoDowngradedYes(HHStaticGraph graph, int sourceId, int targetId,
			LinkedList<HHStaticEdge> buffFwd, LinkedList<HHStaticEdge> buffBwd,
			LinkedList<HHStaticEdge> buffSearchSpace) {

		HHStaticVertex source = graph.getVertex(sourceId);
		HHStaticVertex target = graph.getVertex(targetId);

		// tentative shortest distance (upper bound)
		int d = Integer.MAX_VALUE;
		DiscoveredVertex minSearchScopeHit = null;

		// clear queue
		queue[FWD].clear();
		queue[BWD].clear();
		discoveredVertices[FWD].clear();
		discoveredVertices[BWD].clear();

		// enqueue source and target
		DiscoveredVertex s = new DiscoveredVertex(source, null, null, new HeapKey(0, 0, source
				.getNeighborhood(0)));
		DiscoveredVertex t = new DiscoveredVertex(target, null, null, new HeapKey(0, 0, target
				.getNeighborhood(0)));
		queue[FWD].insert(s);
		queue[BWD].insert(t);
		discoveredVertices[FWD].put(source.getId(), s);
		discoveredVertices[BWD].put(target.getId(), t);

		int direction = FWD;
		while (!queue[FWD].isEmpty() || !queue[BWD].isEmpty()) {
			// switch search direction if queue of current direction is empty
			if (queue[direction].isEmpty()) {
				direction = (direction + 1) % 2;
			}

			// dequeue vertex u
			DiscoveredVertex u = queue[direction].extractMin();
			u.heapIdx = HEAP_IDX_SETTLED;

			// abort criteria for current direction
			if (u.key.distance > d) {
				queue[direction].clear();
				continue;
			}

			// adjust lower bound if u was settled in both directions
			DiscoveredVertex u_ = discoveredVertices[(direction + 1) % 2].get(u.vertex.getId());
			if (u_ != null && u_.heapIdx == HEAP_IDX_SETTLED) {
				int d_ = u.key.distance + u_.key.distance;
				if (d_ < d) {
					minSearchScopeHit = u;
					d = d_;
				}
			}

			if (u.key.gap >= INFINITY_2) {
				u.key.gap = u.vertex.getNeighborhood(u.key.level);
			}

			// relax adjacent edges
			for (HHStaticEdge e : u.vertex.getAdjacentEdges(u.key.level)) {
				// if edge is not in graph for current direction -> skip
				if (!e.getDirection(direction))
					continue;

				int gap_ = u.key.gap;
				// switch to next level
				int lvl = u.key.level;
				while (e.getWeight() > gap_ && lvl < u.vertex.getLevel()) {
					lvl++;
					gap_ = u.vertex.getNeighborhood(lvl);
				}

				// check if edge's level is high enough
				if (!e.isLvlGEQ(lvl)) {
					continue;
				}
				// restriction 1 (only local search)
				if (e.getWeight() > gap_) {
					continue;
				}

				if (gap_ < INFINITY_2) {
					gap_ = gap_ - e.getWeight();
				}
				// only for debug
				buffSearchSpace.add(e);

				// adjust v's heap key, enqueue if not already on heap
				HeapKey key = new HeapKey(u.key.distance + e.getWeight(), lvl, gap_);
				DiscoveredVertex v = discoveredVertices[direction].get(e.getTarget().getId());
				if (v != null) {
					if (key.compareTo(v.key) < 0) {
						queue[direction].decreaseKey(v, key);
						v.parent = u;
						v.edgeToParent = e;
					}
				} else {
					v = new DiscoveredVertex(e.getTarget(), e, u, key);
					discoveredVertices[direction].put(v.vertex.getId(), v);
					queue[direction].insert(v);
				}
			}
			direction = (direction + 1) % 2;
		}
		if (d != Integer.MAX_VALUE) {
			addEdgesToAllParents(discoveredVertices[FWD].get(minSearchScopeHit.vertex.getId()),
					buffFwd);
			addEdgesToAllParents(discoveredVertices[BWD].get(minSearchScopeHit.vertex.getId()),
					buffFwd);
		}
		return d;
	}

	private int shortestPathDtNoDowngradedNo(HHStaticGraph graph, int sourceId, int targetId,
			LinkedList<HHStaticEdge> buffFwd, LinkedList<HHStaticEdge> buffBwd,
			LinkedList<HHStaticEdge> buffSearchSpace) {

		HHStaticVertex source = graph.getVertex(sourceId);
		HHStaticVertex target = graph.getVertex(targetId);

		// tentative shortest distance (upper bound)
		int d = Integer.MAX_VALUE;
		DiscoveredVertex minSearchScopeHit = null;

		// clear queue
		queue[FWD].clear();
		queue[BWD].clear();
		discoveredVertices[FWD].clear();
		discoveredVertices[BWD].clear();

		// enqueue source and target
		DiscoveredVertex s = new DiscoveredVertex(source, null, null, new HeapKey(0, 0, source
				.getNeighborhood(0)));
		DiscoveredVertex t = new DiscoveredVertex(target, null, null, new HeapKey(0, 0, target
				.getNeighborhood(0)));
		queue[FWD].insert(s);
		queue[BWD].insert(t);
		discoveredVertices[FWD].put(source.getId(), s);
		discoveredVertices[BWD].put(target.getId(), t);

		int direction = FWD;
		while (!queue[FWD].isEmpty() || !queue[BWD].isEmpty()) {
			// switch search direction if queue of current direction is empty
			if (queue[direction].isEmpty()) {
				direction = (direction + 1) % 2;
			}

			// dequeue vertex u
			DiscoveredVertex u = queue[direction].extractMin();
			u.heapIdx = HEAP_IDX_SETTLED;

			// abort criteria for current direction
			if (u.key.distance > d) {
				queue[direction].clear();
				continue;
			}

			// adjust lower bound if u was settled in both directions
			DiscoveredVertex u_ = discoveredVertices[(direction + 1) % 2].get(u.vertex.getId());
			if (u_ != null && u_.heapIdx == HEAP_IDX_SETTLED) {
				int d_ = u.key.distance + u_.key.distance;
				if (d_ < d) {
					minSearchScopeHit = u;
					d = d_;
				}
			}

			if (u.key.gap >= INFINITY_2) {
				u.key.gap = u.vertex.getNeighborhood(u.key.level);
			}
			// relax adjacent edges
			for (HHStaticEdge e : u.vertex.getAdjacentEdges(u.key.level)) {
				// if edge is not in graph for current direction -> skip
				if (!e.getDirection(direction))
					continue;

				int gap_ = u.key.gap;
				// switch to next level
				int lvl = u.key.level;
				while (e.getWeight() > gap_ && lvl < u.vertex.getLevel()) {
					lvl++;
					gap_ = u.vertex.getNeighborhood(lvl);
				}

				// check if edge's level is high enough
				if (!e.isLvlGEQ(lvl)) {
					continue;
				}

				// restriction 1 (only local search)
				if (e.getWeight() > gap_) {
					continue;
				}

				// restriction 2 (don't leave core)
				if (u.vertex.getNeighborhood(lvl) < INFINITY_2
						&& e.getTarget().getNeighborhood(lvl) == INFINITY_1) {
					continue;
				}
				if (gap_ < INFINITY_2) {
					gap_ = gap_ - e.getWeight();
				}
				// only for debug
				buffSearchSpace.add(e);

				// adjust v's heap key, enqueue if not already on heap
				HeapKey key = new HeapKey(u.key.distance + e.getWeight(), lvl, gap_);
				DiscoveredVertex v = discoveredVertices[direction].get(e.getTarget().getId());
				if (v != null) {
					if (key.compareTo(v.key) < 0) {
						queue[direction].decreaseKey(v, key);
						v.parent = u;
						v.edgeToParent = e;
					}
				} else {
					v = new DiscoveredVertex(e.getTarget(), e, u, key);
					discoveredVertices[direction].put(v.vertex.getId(), v);
					queue[direction].insert(v);
				}
			}
			direction = (direction + 1) % 2;
		}
		if (d != Integer.MAX_VALUE) {
			addEdgesToAllParents(discoveredVertices[FWD].get(minSearchScopeHit.vertex.getId()),
					buffFwd);
			addEdgesToAllParents(discoveredVertices[BWD].get(minSearchScopeHit.vertex.getId()),
					buffFwd);
		}

		return d;
	}

	private void addEdgesToAllParents(DiscoveredVertex v, LinkedList<HHStaticEdge> buff) {
		while (v.edgeToParent != null) {
			buff.addFirst(v.edgeToParent);
			v = v.parent;
		}
	}

	private void getShortestPathByTable(HHStaticGraph graph, HHStaticVertex s,
			HHStaticVertex t, DistanceTable dt, LinkedList<HHStaticEdge> buff) {
		int distance = dt.get(s.getId(), t.getId());
		int lvl = graph.numLevels() - 1;
		HHStaticVertex s_ = s;
		while (s_.getId() != t.getId()) {
			for (HHStaticEdge e : s.getAdjacentEdges(lvl)) {
				s_ = e.getTarget();
				if (s_.getNeighborhood(lvl) < INFINITY_1
						&& distance - e.getWeight() == dt.get(s_.getId(), t.getId())) {
					s = s_;
					distance = distance - e.getWeight();
					buff.addLast(e);
					break;
				}
			}
		}
	}

	private int getShortestDistanceByTable(LinkedList<DiscoveredVertex> fwd,
			LinkedList<DiscoveredVertex> bwd, DistanceTable dt) {
		int d = Integer.MAX_VALUE;
		DiscoveredVertex u, v;
		u = v = null;
		for (DiscoveredVertex s : fwd) {
			for (DiscoveredVertex t : bwd) {
				int d_st = dt.get(s.vertex.getId(), t.vertex.getId());
				int d_ = d_st + s.key.distance + t.key.distance;
				if (d_st == Integer.MAX_VALUE) {
					continue;
				}
				if (d_ < d) {
					u = s;
					v = t;
					d = d_;
				}
			}
		}
		fwd.clear();
		bwd.clear();
		if (u != null) {
			fwd.add(u);
			bwd.add(v);
		}
		return d;
	}

	public int dijkstra(HHStaticGraph graph, int sourceId, int targetId, int lvl) {

		HHStaticVertex source = graph.getVertex(sourceId);
		HHStaticVertex target = graph.getVertex(targetId);

		int numSettled = 0;

		// clear queue
		queue[FWD].clear();
		discoveredVertices[FWD].clear();

		// enqueue source and target
		DiscoveredVertex s = new DiscoveredVertex(source, null, null, new HeapKey(0, 0, source
				.getNeighborhood(0)));
		queue[FWD].insert(s);
		discoveredVertices[FWD].put(source.getId(), s);

		while (!queue[FWD].isEmpty()) {
			DiscoveredVertex u = queue[FWD].extractMin();
			numSettled++;

			if (u.vertex.getId() == target.getId()) {
				return u.key.distance;
			}
			for (HHStaticEdge e : u.vertex.getAdjacentEdges(lvl)) {
				if (e.getDirection(FWD) && !e.isShortcut()) {
					DiscoveredVertex v = discoveredVertices[FWD].get(e.getTarget().getId());
					HeapKey key = new HeapKey(u.key.distance + e.getWeight(), 0, 0);
					if (v == null) {
						v = new DiscoveredVertex(e.getTarget(), e, u, key);
						queue[FWD].insert(v);
						discoveredVertices[FWD].put(v.vertex.getId(), v);
					} else if (key.compareTo(v.key) < 0) {
						queue[FWD].decreaseKey(v, key);
						v.parent = u;
						v.edgeToParent = e;
					}
				}
			}
		}
		return Integer.MAX_VALUE;
	}

	public LinkedList<HHStaticEdge> dijkstraPath(HHStaticGraph graph, int sourceId,
			int targetId, int lvl) {

		HHStaticVertex source = graph.getVertex(sourceId);
		HHStaticVertex target = graph.getVertex(targetId);

		int numSettled = 0;

		// clear queue
		queue[FWD].clear();
		discoveredVertices[FWD].clear();

		// enqueue source and target
		DiscoveredVertex s = new DiscoveredVertex(source, null, null, new HeapKey(0, 0, source
				.getNeighborhood(0)));
		queue[FWD].insert(s);
		discoveredVertices[FWD].put(source.getId(), s);
		while (!queue[FWD].isEmpty()) {
			DiscoveredVertex u = queue[FWD].extractMin();
			numSettled++;

			if (u.vertex.getId() == target.getId()) {
				LinkedList<HHStaticEdge> edges = new LinkedList<HHStaticEdge>();
				while (u.parent != null) {
					edges.addFirst(u.edgeToParent);
					u = u.parent;
				}
				return edges;
			}
			for (HHStaticEdge e : u.vertex.getAdjacentEdges(lvl)) {
				if (e.getDirection(FWD) && !e.isShortcut()) {
					DiscoveredVertex v = discoveredVertices[FWD].get(e.getTarget().getId());
					HeapKey key = new HeapKey(u.key.distance + e.getWeight(), 0, 0);
					if (v == null) {
						v = new DiscoveredVertex(e.getTarget(), e, u, key);
						queue[FWD].insert(v);
						discoveredVertices[FWD].put(v.vertex.getId(), v);
					} else if (key.compareTo(v.key) < 0) {
						queue[FWD].decreaseKey(v, key);
						v.parent = u;
						v.edgeToParent = e;
					}
				}
			}
		}
		return null;
	}

	private class DiscoveredVertex implements IBinaryHeapItem<HeapKey> {
		private HHStaticVertex vertex;
		private HHStaticEdge edgeToParent;
		private DiscoveredVertex parent;
		private HeapKey key;
		private int heapIdx;

		public DiscoveredVertex(HHStaticVertex vertex, HHStaticEdge edgeToParent,
				DiscoveredVertex parent, HeapKey key) {
			this.vertex = vertex;
			this.edgeToParent = edgeToParent;
			this.parent = parent;
			this.key = key;
		}

		@Override
		public int getHeapIndex() {
			return heapIdx;
		}

		@Override
		public HeapKey getHeapKey() {
			return key;
		}

		@Override
		public void setHeapIndex(int idx) {
			heapIdx = idx;
		}

		@Override
		public void setHeapKey(HeapKey key) {
			this.key = key;
		}
	}

	private class HeapKey implements Comparable<HeapKey> {
		private int distance, level, gap;

		public HeapKey(int distance, int level, int gap) {
			this.distance = distance;
			this.level = level;
			this.gap = gap;
		}

		@Override
		public String toString() {
			return "key : distance=" + distance + " lvl=" + level + " gap=" + gap;
		}

		@Override
		public int compareTo(HeapKey other) {
			if (distance < other.distance) {
				return -3;
			} else if (distance > other.distance) {
				return 3;
			} else if (level < other.level) {
				return -2;
			} else if (level > other.level) {
				return 2;
			} else if (gap < other.gap) {
				return -1;
			} else if (gap > other.gap) {
				return 1;
			} else {
				return 0;
			}
		}
	}

	// public static void main(String[] args) throws SQLException, IOException,
	// ClassNotFoundException {
	// DistanceTable dt = DistanceTable.getFromSerialization(new File("dt"));
	// System.out.println(dt);
	// HHStaticGraph graph = HHStaticGraph.getFromSerialization(new File("graph"));
	// System.out.println(graph);

	// Connection conn = DbConnection.getGermanyDbConn();
	// HHStaticGraph graph = HHStaticGraph.getFromHHDb(conn);
	// graph.serialize(new File("graph"));
	// DistanceTable dt = DistanceTable.getFromHHDb(conn);
	// dt.serialize(new File("dt"));
	// int[] eMinLvl = getEMinLvlFromHHDb(conn);
	// HHEdgeExpanderRecursive edgeExpander = HHEdgeExpanderRecursive.createIndex(graph,
	// eMinLvl);
	// HHAlgorithm algo = new HHAlgorithm();
	// Random rnd = new Random();
	// long time = System.currentTimeMillis();
	// for(int i=0;i<10000;i++) {
	// int s = rnd.nextInt(graph.numVertices());
	// int t = rnd.nextInt(graph.numVertices());
	// int d1 = algo.shortestPath(graph, s, t, dt, new LinkedList<HHStaticEdge>(), new
	// LinkedList<HHStaticEdge>());
	// LinkedList<HHStaticEdge> edges = algo.shortestPathDtYesDowngradedYes(graph, s, t, dt, new
	// LinkedList<HHStaticEdge>());
	// if(edges != null)System.out.println(d1 + " " + edges.size());
	// int d1 = algo.shortestDistanceDtNoDowngradedYes(graph, s, t);
	// int d2 = algo.dijkstra(graph.getVertex(s), graph.getVertex(t), 0);
	// System.out.println(d1 + " " + d2);
	// if(d1 != d2)System.out.println("error");
	// }
	// System.out.println((System.currentTimeMillis() - time) + "ms");
	// }
	//	
	// private static int[] getEMinLvlFromHHDb(Connection conn) throws SQLException{
	// HHDbReader reader = new HHDbReader(conn);
	// int i = 0;
	// int[] eMinLvl = new int[reader.numEdges()];
	// for(Iterator<HHEdge> iter = reader.getEdges();iter.hasNext();) {
	// eMinLvl[i++] = iter.next().minLvl;
	// }
	// return eMinLvl;
	// }
}