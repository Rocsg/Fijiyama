package com.vitimage;
import org.jgrapht.Graph;
import org.jgrapht.alg.flow.mincost.*;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.*;
import org.jgrapht.alg.connectivity.*;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm.*;
import org.jgrapht.alg.interfaces.*;
import org.jgrapht.alg.shortestpath.*;
import org.jgrapht.graph.*;

import java.util.*;

public class MaxFlowMinCost extends CapacityScalingMinimumCostFlow<Object, Object>{

	
	public static void main (String[]args) {
		Graph<Integer,DefaultWeightedEdge>graph=new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
		Integer v0=new Integer(0);
		Integer v1=new Integer(1);
		graph.addVertex(v0);
		graph.addVertex(v1);
		graph.addEdge(v0,v1);
		//CapacityScalingMinimumCostFlow<Integer, E>;
        // constructs a directed graph with the specified vertices and edges
	}
	public static void testDijkstra() {
		Graph<String, DefaultEdge> directedGraph = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
        directedGraph.addVertex("a");
        directedGraph.addVertex("b");
        directedGraph.addVertex("c");
        directedGraph.addVertex("d");
        directedGraph.addVertex("e");
        directedGraph.addVertex("f");
        directedGraph.addVertex("g");
        directedGraph.addVertex("h");
        directedGraph.addVertex("i");
        directedGraph.addEdge("a", "b");
        directedGraph.addEdge("b", "d");
        directedGraph.addEdge("d", "c");
        directedGraph.addEdge("c", "a");
        directedGraph.addEdge("e", "d");
        directedGraph.addEdge("e", "f");
        directedGraph.addEdge("f", "g");
        directedGraph.addEdge("g", "e");
        directedGraph.addEdge("h", "e");
        directedGraph.addEdge("i", "h");
        System.out.println(directedGraph);
        // computes all the strongly connected components of the directed graph
        StrongConnectivityAlgorithm<String, DefaultEdge> scAlg =
            new KosarajuStrongConnectivityInspector<>(directedGraph);
        List<Graph<String, DefaultEdge>> stronglyConnectedSubgraphs =
            scAlg.getStronglyConnectedComponents();

        // prints the strongly connected components
        System.out.println("Strongly connected components:");
        for (int i = 0; i < stronglyConnectedSubgraphs.size(); i++) {
            System.out.println(stronglyConnectedSubgraphs.get(i));
        }
        System.out.println();

        // Prints the shortest path from vertex i to vertex c. This certainly
        // exists for our particular directed graph.
        System.out.println("Shortest path from i to c:");
        DijkstraShortestPath<String, DefaultEdge> dijkstraAlg =
            new DijkstraShortestPath<>(directedGraph);
        SingleSourcePaths<String, DefaultEdge> iPaths = dijkstraAlg.getPaths("i");
        System.out.println(iPaths.getPath("c") + "\n");

        // Prints the shortest path from vertex c to vertex i. This path does
        // NOT exist for our particular directed graph. Hence the path is
        // empty and the result must be null.
        System.out.println("Shortest path from c to i:");
        SingleSourcePaths<String, DefaultEdge> cPaths = dijkstraAlg.getPaths("c");
        System.out.println(cPaths.getPath("i"));			
	}
	
	
	
	
	
	
	public static void firstTrial() {
		/* Build following
		 *            P1 C1 
		 *    P0 C3        
		 * 
		 * 
		 */
		
	}
	
	
	
	public static void makeTheThing() {
		//Convert problem into min cost max flow
		
		//Build the graph, Z by Z
			//From down to up
		
		
	}
	
	
	
	
	
	public MaxFlowMinCost() {
		// TODO Auto-generated constructor stub
	}

}
