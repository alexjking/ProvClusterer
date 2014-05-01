package uk.co.alexjking.prov.ProvClusterer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openprovenance.prov.model.ProvUtilities;
import org.openprovenance.prov.model.Relation0;

/**
 * Class which implements Brandes 2001 faster algorithm for calculating edge betweenness.
 *
 */
public class EdgeBetweennessCentrality {
	DocumentData dd;
	
	static final ProvUtilities utils = new ProvUtilities();
	static final int processors = Runtime.getRuntime().availableProcessors();
	
	/* Algorithm variables */
	private final Object[] edgeBetweennessLocks; //used to atomically access single array element of: double[] edgeBetweenness
	private double[] edgeBetweenness;
	
	public EdgeBetweennessCentrality(DocumentData dd){
		this.dd = dd;
		this.edgeBetweennessLocks = new Object[dd.relations.size()];
		init();
	}
	
	/**
	 * Initialise
	 */
	private void init(){
		edgeBetweenness = new double[dd.relations.size()];
		for(int i=0; i<dd.relations.size(); i++){
			edgeBetweennessLocks[i] = new Object();
		}
	}
	
	/**
	 * Increment the edge betweenness value for a relation
	 * @param relation Relation in question.
	 * @param incrementValue Value to increment the betweenness value by.
	 */
	private void incrementEdgeBetweenness(final Integer relation, final Double incrementValue) {
		synchronized (edgeBetweennessLocks[relation]) {
			edgeBetweenness[relation] += incrementValue;
		}
	}

	/**
	 * Calculates the edge betweenness for each edge and returns relation with the highest edge betweenness value.
	 * @return Relation with highest edge betweenness.
	 */
	public Relation0 getMostBetweenRelation(){
		ExecutorService executor = null;
		if(processors > 4){
			executor = Executors.newFixedThreadPool(4);
		}else{
			executor = Executors.newFixedThreadPool(processors);
		}
		for (int i=0; i<dd.elements.size(); i++) {
			Runnable worker = new EdgeBetweennessThread(i);
			executor.execute(worker);
		}
		executor.shutdown();
		while(!executor.isTerminated()){
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {}
		}	
		
		
		Integer mostBetweenRelation = null;
		Double mostBetweenValue = null;
		for(int i=0; i<dd.relations.size(); i++){
			if(mostBetweenRelation == null || edgeBetweenness[i] > mostBetweenValue){
				mostBetweenValue = edgeBetweenness[i];
				mostBetweenRelation = i;
			}
		}
		return dd.relations.get(mostBetweenRelation);
	}
	
	
	
	/**
	 * 
	 * A runnable thread which calculates the number of shortest path
	 * to each element from the given root element.
	 * 
	 * @author Alexander King
	 *
	 */
	class EdgeBetweennessThread implements Runnable {
		/* Variables global to the thread which store the state of each element */
		private ElementData[] elementData;
		private int rootElement;
	
		public EdgeBetweennessThread(Integer rootElement) {
			this.rootElement = rootElement;
		}

		
		public void run() {
			elementData = getInitialisedElementData(rootElement);
			Stack<Integer> stack = calculateShortestPaths();	
			calculateBetweennessValues(stack);
		}
		
		/**
		 * Uses a breadth first search tree to calculate the number of shortest paths
		 * travelling through each element from the root node.
		 * 
		 * @return Stack of element ids to calculate the betweenness values
		 */
		private Stack<Integer> calculateShortestPaths(){
			/* Initialise  lists/stacks for use */
			Stack<Integer> stack = new Stack<Integer>();
			LinkedList<Integer> bfsQueue = new LinkedList<Integer>();
			bfsQueue.add(rootElement);
			
			/*
			 * Loop through all elements starting at the root
			 * and adding neighbours as they are encountered
			 * until all elements have been visited
			 */
			
			while(!bfsQueue.isEmpty()){
				int v = bfsQueue.poll();//dequeue
				stack.push(v);//push v onto stack
				
				/*
				 * Loop through each of the current element's neighbours
				 * updating the geodesic distance and number of shortest path values
				 */
				for(int neighbour: dd.getNeighbours(v)){	
					/* Neighbour is replacing w from pseudocode */
					
					/*
					 * If true, we are encountering this neighbour has been encountered for the first time,
					 * so the neighbour's geodesic distance if the geodesic distance of the current element + 1
					 * 
					 * The neighbour is then added to the bfs queue to later calculate the distances of
					 * it's own neighbours
					 */
					int incrementValue = elementData[v].geodesicDistance + 1;
					if(elementData[neighbour].geodesicDistance < 0){
						elementData[neighbour].geodesicDistance = incrementValue;
						bfsQueue.add(neighbour);
					}
					/*
					 * If current bfs element is the parent node of the current neighbour,
					 * then it's number of shortest paths value is updated
					 * 
					 * The current bfs element is then added to the neighbour's parent list
					 */
					if(elementData[neighbour].geodesicDistance == incrementValue){
						elementData[neighbour].numShortestPaths += elementData[v].numShortestPaths;
						/* Append v as a neighbour of parents */
						elementData[neighbour].parents.add(v);
					}
				}
			}
			return stack;
		}
		
		/**
		 * Uses the stack generated by calculateShortestPaths() to calculate the
		 * betweenness values for each edge.
		 * 
		 * @param stack
		 */
		private void calculateBetweennessValues(Stack<Integer> stack){
			/* 	 
			 * Loop through stack
			 * Betweenness values are calculated and updated
			 */
			while(!stack.isEmpty()){
				int w = stack.pop();
				for(int parent: elementData[w].parents){
					//update betweenness value for edge between v and wparent
					double incrementValue = ((double) elementData[parent].numShortestPaths / (double) elementData[w].numShortestPaths) * (1.0 + elementData[w].dependency);
					incrementEdgeBetweenness(dd.getRelation(parent, w), incrementValue); //increment edge betweenness atomically
					elementData[parent].dependency += incrementValue;
				}
			}	
		}
		
		
	}
	
	/////////////////////////////////////////////////////////////////////////////////
	//
	//      Initialisation methods for the Worker Thread
	//      These are placed outside of the worker thread for efficiency purposes
	//
	/////////////////////////////////////////////////////////////////////////////////

	
	
	private class ElementData {

		protected int numShortestPaths;
		protected int geodesicDistance;
		protected List<Integer> parents;
		protected double dependency;
		
		public ElementData(){
			geodesicDistance = -1;
			parents = new ArrayList<Integer>();
		}
	}
	
	
	private ElementData[] getInitialisedElementData(int rootElement){
		ElementData[] elementData = new ElementData[dd.elements.size()];
		for(int i=0; i<dd.elements.size(); i++){
			elementData[i] = new ElementData();
		}
		elementData[rootElement].geodesicDistance = 0;
		elementData[rootElement].numShortestPaths = 1;
		return elementData;
	}
	
	

}
