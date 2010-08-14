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
package org.mapsforge.android.routing.hh;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import org.mapsforge.android.routing.hh.ObjectPool.PoolableFactory;
import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.preprocessing.routing.hhmobile.util.Utils;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.prioQueue.BinaryMinHeap;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.prioQueue.IBinaryHeapItem;
import org.mapsforge.preprocessing.routing.highwayHierarchies.util.renderer.RendererV2;
import org.mapsforge.server.routing.RouterFactory;

final class HighwayHierarchiesAlgorithm {

	private static final int INITIAL_HH_QUEUE_SIZE = 300;
	private static final int INITIAL_HH_MAP_SIZE = 2000;
	private static final int INITIAL_DIJKSTRA_QUEUE_SIZE = 50;
	private static final int INITIAL_DIJKSTRA_MAP_SIZE = 100;
	private static final int FWD = 0;
	private static final int BWD = 1;
	private static final int HEAP_IDX_SETTLED = -123456789;

	private final RoutingGraph graph;
	private final HHQueue[] queue;
	private final HHMap[] discovered;
	private final BinaryMinHeap<DijkstraHeapItem, DijkstraHeapItem> queueDijkstra;
	private final TIntObjectHashMap<DijkstraHeapItem> discoveredDijkstra;
	private int[][] numSettled;

	private final ObjectPool<HHVertex> poolVertices;
	private final ObjectPool<HHEdge> poolEdges;

	public HighwayHierarchiesAlgorithm(RoutingGraph graph, ObjectPool<HHVertex> poolVertices,
			ObjectPool<HHEdge> poolEdges) {
		this.graph = graph;
		this.queue = new HHQueue[] { new HHQueue(INITIAL_HH_QUEUE_SIZE),
				new HHQueue(INITIAL_HH_QUEUE_SIZE) };
		this.discovered = new HHMap[] { new HHMap(INITIAL_HH_MAP_SIZE),
				new HHMap(INITIAL_HH_MAP_SIZE) };
		this.queueDijkstra = new BinaryMinHeap<DijkstraHeapItem, DijkstraHeapItem>(
				INITIAL_DIJKSTRA_QUEUE_SIZE);
		this.discoveredDijkstra = new TIntObjectHashMap<DijkstraHeapItem>(
				INITIAL_DIJKSTRA_MAP_SIZE);

		this.poolEdges = poolEdges;
		this.poolVertices = poolVertices;
	}

	public int getShortestPath(int sourceId, int targetId, LinkedList<HHEdge> shortestPathBuff)
			throws IOException {
		Utils.setZero(graph.numBlockReads, 0, graph.numBlockReads.length);
		System.out.println("\ngetShortestPath " + sourceId + " -> " + targetId);
		graph.ioTime = 0;
		graph.shiftTime = 0;
		long startTime = System.currentTimeMillis();

		queue[FWD].clear();
		queue[BWD].clear();
		discovered[FWD].clear();
		discovered[BWD].clear();

		int direction = FWD;
		int distance = Integer.MAX_VALUE;
		int searchScopeHitId = -1;

		numSettled = new int[][] { new int[graph.numLevels()], new int[graph.numLevels()] };

		{ // scope for s
			HHVertex s = poolVertices.borrow();
			graph.getVertex(sourceId, s);
			HHHeapItem _s = new HHHeapItem(0, 0, s.neighborhood, sourceId, sourceId, -1, -1, -1);
			queue[FWD].insert(_s);
			discovered[FWD].put(s.idZeroLvl, _s);
			poolVertices.release(s);
		}
		{ // scope for t
			HHVertex t = poolVertices.borrow();
			graph.getVertex(targetId, t);
			HHHeapItem _t = new HHHeapItem(0, 0, t.neighborhood, targetId, targetId, -1, -1, -1);
			queue[BWD].insert(_t);
			discovered[BWD].put(t.idZeroLvl, _t);
			poolVertices.release(t);
		}

		while (!queue[FWD].isEmpty() || !queue[BWD].isEmpty()) {
			if (queue[direction].isEmpty()) {
				direction = (direction + 1) % 2;
			}
			HHHeapItem uItem = queue[direction].extractMin();
			uItem.heapIdx = HEAP_IDX_SETTLED;
			numSettled[direction][uItem.level]++;

			if (uItem.distance > distance) {
				queue[direction].clear();
				continue;
			}

			HHHeapItem uItem_ = discovered[(direction + 1) % 2].get(uItem.idLvlZero);
			if (uItem_ != null && uItem_.heapIdx == HEAP_IDX_SETTLED) {
				if (distance > uItem.distance + uItem_.distance) {
					distance = uItem.distance + uItem_.distance;
					searchScopeHitId = uItem.idLvlZero;
				}
			}

			{ // scope for u
				HHVertex u = poolVertices.borrow();
				graph.getVertex(uItem.id, u);
				if (uItem.gap == Integer.MAX_VALUE) {
					uItem.gap = u.neighborhood;
				}
				int lvl = uItem.level;
				int gap = uItem.gap;
				while (!relaxAdjacentEdges(uItem, u, direction, lvl, gap) && u.idNextLvl != -1) {
					// switch to next level
					lvl++;
					graph.getVertex(u.idNextLvl, u);
					uItem.id = u.id;
					gap = u.neighborhood;
				}
				direction = (direction + 1) % 2;
				poolVertices.release(u);
			}
		}
		if (searchScopeHitId != -1) {
			System.out.println("got shortest distance " + distance + " "
					+ (System.currentTimeMillis() - startTime) + "ms");
			startTime = System.currentTimeMillis();
			System.out.println("settled : " + Utils.arrToString(numSettled[0]) + " | "
					+ Utils.arrToString(numSettled[1]));
			System.out.println("blockReads : " + Utils.arrToString(graph.numBlockReads));
			System.out.println("ioTime : " + (graph.ioTime / 1000000) + "ms");
			System.out.println("shiftTime : " + (graph.shiftTime / 1000000) + "ms");
			Utils.setZero(graph.numBlockReads, 0, graph.numBlockReads.length);

			graph.ioTime = 0;
			graph.shiftTime = 0;

			System.out.print("expanding shortcuts...");
			expandEdges(discovered[FWD].get(searchScopeHitId), discovered[BWD]
					.get(searchScopeHitId), shortestPathBuff);
			System.out.println((System.currentTimeMillis() - startTime) + "ms");
			System.out.println("blockReads : " + Utils.arrToString(graph.numBlockReads));
			System.out.println("ioTime : " + (graph.ioTime / 1000000) + "ms");
			System.out.println("shiftTime : " + (graph.shiftTime / 1000000) + "ms");

			graph.ioTime = 0;
			graph.shiftTime = 0;
			Utils.setZero(graph.numBlockReads, 0, graph.numBlockReads.length);
		}
		return distance;
	}

	private boolean relaxAdjacentEdges(HHHeapItem uItem, HHVertex u, int direction, int lvl,
			int gap) throws IOException {
		boolean result = true;
		boolean forward = (direction == FWD);

		int n = u.getOutboundDegree();
		for (int i = 0; i < n; i++) {
			HHEdge e = poolEdges.borrow();
			graph.getOutboundEdge(u, i, e);
			if (forward && !e.isForward()) {
				poolEdges.release(e);
				continue;
			}
			if (!forward && !e.isBackward()) {
				poolEdges.release(e);
				continue;
			}

			int gap_ = gap;
			if (gap != Integer.MAX_VALUE) {
				gap_ = gap - e.weight;
				if (!e.isCore()) {
					// don't leave the core
					poolEdges.release(e);
					continue;
				}
				if (gap_ < 0) {
					// edge crosses neighborhood of entry point, don't relax it
					result = false;
					poolEdges.release(e);
					continue;
				}
			}

			HHHeapItem vItem = discovered[direction].get(e.targetIdZeroLvl);
			if (vItem == null) {
				vItem = new HHHeapItem(uItem.distance + e.weight, lvl, gap_, e.getTargetId(),
						e.targetIdZeroLvl, u.idZeroLvl, u.id, e.getTargetId());
				discovered[direction].put(e.targetIdZeroLvl, vItem);
				queue[direction].insert(vItem);
			} else if (vItem.compareTo(uItem.distance + e.weight, lvl, gap_) > 0) {
				vItem.distance = uItem.distance + e.weight;
				vItem.level = lvl;
				vItem.id = e.getTargetId();
				vItem.gap = gap_;
				vItem.parentIdLvlZero = u.idZeroLvl;
				vItem.eSrcId = u.id;
				vItem.eTgtId = e.getTargetId();
				queue[direction].decreaseKey(vItem, vItem);
			}
			poolEdges.release(e);
		}

		return result;
	}

	private void expandEdges(HHHeapItem fwd, HHHeapItem bwd, LinkedList<HHEdge> buff)
			throws IOException {
		while (fwd.eSrcId != -1) {
			expandEdgeRec(fwd.eSrcId, fwd.eTgtId, buff, true);
			fwd = discovered[FWD].get(fwd.parentIdLvlZero);
		}
		while (bwd.eSrcId != -1) {
			expandEdgeRec(bwd.eSrcId, bwd.eTgtId, buff, false);
			bwd = discovered[BWD].get(bwd.parentIdLvlZero);
		}
	}

	private void expandEdgeRec(int src, int tgt, LinkedList<HHEdge> buff, boolean fwd)
			throws IOException {
		HHVertex s = new HHVertex();
		graph.getVertex(src, s);
		HHVertex t = new HHVertex();
		graph.getVertex(tgt, t);

		HHEdge e = extractEdge(s, t, fwd);
		if (s.idPrevLvl == -1) {
			// edge level == 0
			if (fwd) {
				buff.addFirst(e);
			} else {
				e = extractEdge(t, s, true);
				buff.addLast(e);
			}
		} else if (!e.isShortcut()) {
			// jump directly to level 0
			expandEdgeRec(s.idPrevLvl, t.idZeroLvl, buff, fwd);
		} else {
			// use dijkstra within the core of subjacent level
			discoveredDijkstra.clear();
			queueDijkstra.clear();
			DijkstraHeapItem sItem = new DijkstraHeapItem(0, s.idPrevLvl, null);
			discoveredDijkstra.put(s.idPrevLvl, sItem);
			queueDijkstra.insert(sItem);

			while (!queueDijkstra.isEmpty()) {
				DijkstraHeapItem uItem = queueDijkstra.extractMin();
				if (uItem.id == t.idPrevLvl) {
					// found target
					break;
				}
				HHVertex u = new HHVertex();
				graph.getVertex(uItem.id, u);

				// relax edges
				int n = u.getOutboundDegree();
				for (int i = 0; i < n; i++) {
					HHEdge e_ = new HHEdge();
					graph.getOutboundEdge(u, i, e_);
					if (!e_.isCore() || (fwd && !e_.isForward()) || (!fwd && !e_.isBackward())) {
						// -skip edge if it is not applicable for current search direction
						// -skip non core edges
						continue;
					}
					DijkstraHeapItem vItem = discoveredDijkstra.get(e_.getTargetId());
					if (vItem == null) {
						vItem = new DijkstraHeapItem(uItem.distance + e_.weight, e_
								.getTargetId(), uItem);
						discoveredDijkstra.put(e_.getTargetId(), vItem);
						queueDijkstra.insert(vItem);
					} else if (vItem.distance > uItem.distance + e_.weight) {
						vItem.distance = uItem.distance + e_.weight;
						vItem.parent = uItem;
					}
				}
			}
			DijkstraHeapItem i = discoveredDijkstra.get(t.idPrevLvl);
			while (i.parent != null) {
				int s_ = i.parent.id;
				int t_ = i.id;
				expandEdgeRec(s_, t_, buff, fwd);
				i = i.parent;
			}
		}
	}

	private HHEdge extractEdge(HHVertex s, HHVertex t, boolean fwd) throws IOException {
		int minWeight = Integer.MAX_VALUE;
		int n = s.getOutboundDegree();
		HHEdge eMinWeight = new HHEdge();
		for (int i = 0; i < n; i++) {
			HHEdge e = new HHEdge();
			graph.getOutboundEdge(s, i, e);
			if (e.getTargetId() == t.id && e.weight < minWeight
					&& ((fwd && e.isForward()) || (!fwd && e.isBackward()))) {
				minWeight = e.weight;
				eMinWeight = e;
			}
		}
		return eMinWeight;
	}

	private static class HHHeapItem implements IBinaryHeapItem<HHHeapItem>,
			Comparable<HHHeapItem> {

		int heapIdx;
		// the key
		public int distance;
		public int level;
		public int gap;
		//
		public int id;
		public int idLvlZero;

		public int parentIdLvlZero;
		public int eSrcId;
		public int eTgtId;

		public HHHeapItem(int distance, int level, int gap, int id, int idLvlZero,
				int parentIdLvlZero, int eSrcId, int eTgtId) {
			this.heapIdx = -1;
			this.distance = distance;
			this.level = level;
			this.gap = gap;

			this.id = id;
			this.idLvlZero = idLvlZero;

			this.parentIdLvlZero = parentIdLvlZero;
			this.eSrcId = eSrcId;
			this.eTgtId = eTgtId;
		}

		@Override
		public int getHeapIndex() {
			return heapIdx;
		}

		@Override
		public void setHeapIndex(int idx) {
			this.heapIdx = idx;
		}

		@Override
		public void setHeapKey(HHHeapItem key) {
			this.distance = key.distance;
			this.level = key.level;
			this.gap = key.gap;
		}

		@Override
		public int compareTo(HHHeapItem other) {
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

		public int compareTo(int _distance, int _level, int _gap) {
			if (distance < _distance) {
				return -3;
			} else if (distance > _distance) {
				return 3;
			} else if (level < _level) {
				return -2;
			} else if (level > _level) {
				return 2;
			} else if (gap < _gap) {
				return -1;
			} else if (gap > _gap) {
				return 1;
			} else {
				return 0;
			}
		}

		@Override
		public HHHeapItem getHeapKey() {
			return this;
		}
	}

	private static class DijkstraHeapItem implements IBinaryHeapItem<DijkstraHeapItem>,
			Comparable<DijkstraHeapItem> {

		public int heapIdx;
		public int distance;
		public int id;
		public DijkstraHeapItem parent;

		public DijkstraHeapItem(int distance, int id, DijkstraHeapItem parent) {
			this.distance = distance;
			this.id = id;
			this.parent = parent;
		}

		@Override
		public int getHeapIndex() {
			return heapIdx;
		}

		@Override
		public DijkstraHeapItem getHeapKey() {
			return this;
		}

		@Override
		public void setHeapIndex(int idx) {
			this.heapIdx = idx;

		}

		@Override
		public void setHeapKey(DijkstraHeapItem key) {
			this.distance = key.distance;

		}

		@Override
		public int compareTo(DijkstraHeapItem other) {
			return distance - other.distance;
		}

	}

	private static class HHMap extends TIntObjectHashMap<HHHeapItem> {
		// need class without parameter to allow array creation without warning
		public HHMap(int initialCapacity) {
			super(initialCapacity);
		}

	}

	private static class HHQueue extends BinaryMinHeap<HHHeapItem, HHHeapItem> {
		// need class without parameter to allow array creation without warning
		public HHQueue(int initialSize) {
			super(initialSize);
		}
	}

	public static void main(String[] args) throws IOException {
		String map = "germany";
		int n = 1;

		RoutingGraph graph = new RoutingGraph(new File(map + ".hhmobile"), 1024 * 1000);
		ObjectPool<HHVertex> poolVertices = new ObjectPool<HHVertex>(
				new PoolableFactory<HHVertex>() {

					@Override
					public HHVertex makeObject() {
						return new HHVertex();
					}

				}, 100);
		ObjectPool<HHEdge> poolEdges = new ObjectPool<HHEdge>(new PoolableFactory<HHEdge>() {
			@Override
			public HHEdge makeObject() {
				return new HHEdge();
			}
		}, 100);

		HighwayHierarchiesAlgorithm hh = new HighwayHierarchiesAlgorithm(graph, poolVertices,
				poolEdges);
		DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(graph);

		RendererV2 renderer = new RendererV2(1024, 768, RouterFactory.getRouter(), Color.BLACK,
				Color.WHITE);
		LinkedList<HHEdge> sp1 = new LinkedList<HHEdge>();
		LinkedList<HHEdge> sp2 = new LinkedList<HHEdge>();

		long time = System.currentTimeMillis();
		for (int i = 0; i < n; i++) {
			HHVertex s = new HHVertex();
			graph.getNearestVertex(new GeoCoordinate(52.509769, 13.4567655), 300, s);
			// graph.getRandomVertex(0, s);
			HHVertex t = new HHVertex();
			graph.getNearestVertex(new GeoCoordinate(52.4556941, 13.2918805), 300, t);
			// graph.getRandomVertex(0, t);
			int d1 = hh.getShortestPath(s.id, t.id, sp1);
			// System.out.println("cache misses : " + cache.getNumCacheMisses());
			// System.out.println("bytes read : " + cache.getNumBytesRead());
			graph.clearCache();

			int d2 = dijkstra.getShortestPath(s.id, t.id, new LinkedList<HHVertex>());
			if (d1 != d2) {
				System.out.println(d1 + " != " + d2);
			} else {
				System.out.println("distance = " + d1);
			}
			int j = 1;
			GeoCoordinate[] coords = new GeoCoordinate[sp1.size() + 1];
			coords[0] = new GeoCoordinate(s.getLatitudeE6(), s.getLongitudeE6());
			for (HHEdge e : sp1) {
				HHVertex v = new HHVertex();
				graph.getVertex(e.getTargetId(), v);
				coords[j] = new GeoCoordinate(v.getLatitudeE6(), v.getLongitudeE6());
				if (coords[j].getLongitudeE6() == coords[j - 1].getLongitudeE6()
						&& coords[j].getLatitudeE6() == coords[j - 1].getLatitudeE6()) {
					System.out.println("error " + j);
				}
				j++;
			}
			renderer.addMultiLine(coords, Color.RED);
			//
			renderer.addCircle(new GeoCoordinate(s.getLatitudeE6(), s.getLongitudeE6()),
					Color.GREEN);
			renderer.addCircle(new GeoCoordinate(t.getLatitudeE6(), t.getLongitudeE6()),
					Color.GREEN);
			sp1.clear();
		}
		System.out.println("num routes : " + n);
		System.out.println("exec time : " + (System.currentTimeMillis() - time) + "ms.");
	}
}
