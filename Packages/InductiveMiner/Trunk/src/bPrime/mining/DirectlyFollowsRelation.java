package bPrime.mining;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import bPrime.Pair;
import bPrime.ProcessTreeModelParameters;

public class DirectlyFollowsRelation {
	private DefaultDirectedGraph<XEventClass, DefaultEdge> graph;
	private Set<XEventClass> startActivities;
	private Set<XEventClass> endActivities;
	private HashMap<XEventClass, Set<XEventClass>> minimumSelfDistancesBetween;
	private HashMap<XEventClass, Integer> minimumSelfDistances;
	private boolean tauPresent;
	private int longestTrace;
	
	public DirectlyFollowsRelation(Filteredlog log, ProcessTreeModelParameters parameters) {
		//initialise
		graph = new DefaultDirectedGraph<XEventClass, DefaultEdge>(DefaultEdge.class);
		startActivities = new HashSet<XEventClass>();
		endActivities = new HashSet<XEventClass>();
		minimumSelfDistances = new HashMap<XEventClass, Integer>();
		minimumSelfDistancesBetween = new HashMap<XEventClass, Set<XEventClass>>();
		tauPresent = false;
		
		XEventClass secondFromEventClass;
		XEventClass fromEventClass;
		XEventClass toEventClass;
		Set<XEventClass> lengthOneLoops = new HashSet<XEventClass>();
		Set<Pair<XEventClass, XEventClass>> lengthTwoLoops = new HashSet<Pair<XEventClass, XEventClass>>();
		longestTrace = 0;
		
		//add the nodes to the graph
		for (XEventClass a : log.getEventClasses()) {
			graph.addVertex(a);
		}
		
		//walk trough the log
		log.initIterator();
		HashMap<XEventClass, Integer> eventSeenAt;
		List<XEventClass> readTrace;
		
		while (log.hasNextTrace()) {
			log.nextTrace();
		
			toEventClass = null;
			fromEventClass = null;
			secondFromEventClass = null;
			
			int traceSize = 0;
			eventSeenAt = new HashMap<XEventClass, Integer>();
			readTrace = new LinkedList<XEventClass>();
			
			while(log.hasNextEvent()) {

				secondFromEventClass = fromEventClass;
				fromEventClass = toEventClass;
				toEventClass = log.nextEvent();
				
				readTrace.add(toEventClass);
				
				if (eventSeenAt.containsKey(toEventClass)) {
					//we have detected an activity for the second time
					//check whether this is shorter than what we had already seen
					if (!minimumSelfDistances.containsKey(toEventClass) || traceSize - eventSeenAt.get(toEventClass) < minimumSelfDistances.get(toEventClass)) {
						//keep the new minimum self distance
						minimumSelfDistances.put(toEventClass, traceSize - eventSeenAt.get(toEventClass));
						
						//store the new activities in between
						minimumSelfDistancesBetween.put(toEventClass, new HashSet<XEventClass>(readTrace.subList(eventSeenAt.get(toEventClass)+1, traceSize)));
					}
				}
				eventSeenAt.put(toEventClass, traceSize);
				
				if (fromEventClass != null) {
					
					//add edge to directly-follows graph
					graph.addEdge(fromEventClass, toEventClass);
					
					//check whether we found a witness of a length-one-loop
					if (fromEventClass == toEventClass) {
						lengthOneLoops.add(fromEventClass);
					}
					
					//check whether we found a witness of a length-two-loop
					if (secondFromEventClass != null && secondFromEventClass == toEventClass) {
						lengthTwoLoops.add(new Pair<XEventClass, XEventClass>(secondFromEventClass, fromEventClass));
					}
				} else {
					startActivities.add(toEventClass);
				}
				
				traceSize += 1;
			}
			
			//update the longest-trace-counter
			if (traceSize > longestTrace) {
				longestTrace = traceSize;
			}
			
			if (toEventClass != null) {
				endActivities.add(toEventClass);
			}
			
			if (traceSize == 0) {
				tauPresent = true;
			}
		}
		System.out.println(minimumSelfDistancesBetween.toString());
	}
	
	public String debugGraph() {
		String result = "nodes: " + graph.vertexSet().toString();
		result += "\nedges: " + graph.edgeSet().toString();
		return result;
	}

	public DefaultDirectedGraph<XEventClass, DefaultEdge> getGraph() {
		return graph;
	}

	public Set<XEventClass> getStartActivities() {
		return startActivities;
	}

	public Set<XEventClass> getEndActivities() {
		return endActivities;
	}
	
	public boolean getTauPresent() {
		return tauPresent;
	}
	
	public int getLongestTrace() {
		return longestTrace;
	}
	
	public Set<XEventClass> getMinimumSelfDistanceBetween(XEventClass activity) {
		if (!minimumSelfDistances.containsKey(activity)) {
			return new HashSet<XEventClass>();
		}
		return minimumSelfDistancesBetween.get(activity);
	}
	
}
