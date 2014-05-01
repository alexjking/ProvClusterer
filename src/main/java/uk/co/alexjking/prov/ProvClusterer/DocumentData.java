package uk.co.alexjking.prov.ProvClusterer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openprovenance.prov.model.ActedOnBehalfOf;
import org.openprovenance.prov.model.Element;
import org.openprovenance.prov.model.HasOther;
import org.openprovenance.prov.model.NamedBundle;
import org.openprovenance.prov.model.Namespace;
import org.openprovenance.prov.model.Other;
import org.openprovenance.prov.model.ProvUtilities;
import org.openprovenance.prov.model.QualifiedName;
import org.openprovenance.prov.model.Relation0;
import org.openprovenance.prov.model.Statement;
import org.openprovenance.prov.model.Type;
import org.openprovenance.prov.model.WasAssociatedWith;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.WasDerivedFrom;
import org.openprovenance.prov.model.WasEndedBy;
import org.openprovenance.prov.model.WasStartedBy;
import org.openprovenance.prov.xml.ProvFactory;

/**
 * Class to store information about a provenance document.
 * Contains functions to help compute Girvan-Newman algorithm.
 *
 */
public class DocumentData {
	static final ProvUtilities utils = new ProvUtilities();
	public static org.openprovenance.prov.model.ProvFactory pFactory = new ProvFactory();
	public static org.openprovenance.prov.model.Name name = pFactory.getName();
	public static final String CLUSTER_NS = "http://prov.alexjking.co.uk/";
	public static final String CLUSTER_PREFIX = "cluster";
	
	protected Document d;
	protected List<Relation0> relations;
	protected List<Element> elements;
	protected List<QualifiedName> qualifiedNames; //list of qualified names for each element
	public List<Relation0> removedRelations; //list of removed relations
	protected ArrayList<ArrayList<Integer>> neighboursList; //getNeighbours(Integer)
	protected int[][] neighbouringElements; //getRelation(Integer, Integer)
	private boolean[] visitedElements;
	
	public DocumentData(Document d){
		this.d = d;
		init();
		initialiseNeighbours();
	}
	
	private void init(){
		removedRelations = new ArrayList<Relation0>();
		relations = utils.getRelations(d);
		elements = new ArrayList<Element>();
		elements.addAll(utils.getActivity(d));
		elements.addAll(utils.getEntity(d));
		elements.addAll(utils.getAgent(d));
		qualifiedNames = new ArrayList<QualifiedName>();
		for(Element e: elements){
			qualifiedNames.add(e.getId());
		}	
	}
	
	private void initialiseNeighbours(){
		neighboursList = new ArrayList<ArrayList<Integer>>();
		neighbouringElements = new int[elements.size()][elements.size()];
		for(int i=0; i<elements.size(); i++){
			neighboursList.add(new ArrayList<Integer>());
		}
		
		for(int i=0; i<relations.size(); i++){
			Relation0 r = relations.get(i);
			/* Get the index of the two elements in the relation */
			int causeIndex = qualifiedNames.indexOf(utils.getCause(r));
			int effectIndex = qualifiedNames.indexOf(utils.getEffect(r));
			if(causeIndex != -1 && effectIndex!= -1){
				neighbouringElements[causeIndex][effectIndex] = i;
				neighbouringElements[effectIndex][causeIndex] = i;
				neighboursList.get(causeIndex).add(effectIndex); //add id of curent relation(i) to the list of neighbours for element i1
				neighboursList.get(effectIndex).add(causeIndex);	
			}else{
				relations.remove(i);
				i--;
			}
		}
	}
	
	/**
	 * Removes a relation from the internal representation and regenerates the neighbour values.
	 * @param r Relation to remove.
	 */
	public void removeRelation(Relation0 r){
		relations.remove(r);
		removedRelations.add(r);
		initialiseNeighbours();
	}
	
	/**
	 * Get a list of neighbours for ith element in elements list.
	 * @param i Element index
	 * @return List of neighbours.
	 */
	public List<Integer> getNeighbours(int i){		
		return neighboursList.get(i);
	}
	
	/**
	 * Get the relation between two elements.
	 * @param e1 Element index
	 * @param e2 Element index
	 * @return Relation between two elements.
	 */
	public int getRelation(int e1, int e2){	
 		return neighbouringElements[e1][e2];
	}
	
	
	/**
	 * Generates communities using the neighbours list.
	 * A community consists of elements which are all interconnected, but have no connections to other groups of elements.
	 * @return Set of Set of element indexes. (Set of clusters)
	 */
	public Set<Set<Integer>> generateCommunities(){
		visitedElements = new boolean[elements.size()];
		Set<Set<Integer>> communities = new HashSet<Set<Integer>>();	
		for(int i=0; i<elements.size(); i++){
			if(!visitedElements[i]){ //add a new community if the element hasn't been visited
				communities.add(generateCommunitiesRecurse(i));
			}
		}
		return communities;
	}
	
	/**
	 * Helper function for generateCommunities()
	 * @param elementIndex
	 * @return
	 */
	private Set<Integer> generateCommunitiesRecurse(Integer elementIndex){
		Set<Integer> community = new HashSet<Integer>();
		List<Integer> neighbours = neighboursList.get(elementIndex); //get current element's neighbours
		visitedElements[elementIndex] = true; //set the current element as visited
		community.add(elementIndex);
		/*
		 * Loop through the current element's neighbours,
		 * recursing on it if it hasn't been visited already
		 */
		for(Integer neighbour: neighbours){
			if(!visitedElements[neighbour]){
				community.addAll(generateCommunitiesRecurse(neighbour));
			}	
		}
		return community;
	}
	
	/**
	 * Generates a document from the internal representation.
	 * @return
	 */
	public Document generateClusteredDocument(){
		Set<Set<Integer>> communities = generateCommunities();
		Set<Statement> statements = new HashSet<Statement>();
		/* Loop through sets of communities adding cluster meta-data */
		int communityCount = 0;
		Double hue = 0.000;
		Double increment = 1.000/communities.size();
		Type elementType = pFactory.newType( pFactory.newQualifiedName(CLUSTER_NS, "element", CLUSTER_PREFIX), name.XSD_QNAME);
		/* loop through each commmunity  calculating the colour and adding elements within the communities*/
		for(Set<Integer> community: communities){
			DecimalFormat df = new DecimalFormat("0.000");
			String currentHue = df.format(hue) + " 0.600 1.000";
			/* Loop through each element in a community updating its attributes */
			for(Integer element: community){
				Element e = elements.get(element);
				Other id = pFactory.newOther(CLUSTER_NS, "id", CLUSTER_PREFIX, ""+communityCount, name.XSD_INT);
				Other colour = pFactory.newOther("http://openprovenance.org/Toolbox/dot#", "fillcolor", "dot", currentHue, name.XSD_STRING);
				if(e instanceof HasOther){
					((HasOther) e).getOther().add(id);
					((HasOther) e).getOther().add(colour);	
				}
				e.getType().add(elementType);
				statements.add(e);
				
			}
			communityCount++;
			hue += increment;
		}
		/* Add relations to the list of statements */
		for(Relation0 r:relations){
			if(r instanceof WasAssociatedWith)
				((WasAssociatedWith) r).setPlan(null);
			else if(r instanceof WasDerivedFrom)
				((WasDerivedFrom) r).setActivity(null);
			else if(r instanceof WasStartedBy)
				((WasStartedBy) r).setStarter(null);
			else if(r instanceof WasEndedBy )
				((WasEndedBy) r).setEnder(null);
			else if(r instanceof ActedOnBehalfOf)
				((ActedOnBehalfOf) r).setActivity(null);
			statements.add(r);
		}

				
		/* Add removed relations to the list of statements - white attribute added so that they appear to be invisible */
		for(Relation0 r: removedRelations){
			if(r instanceof WasAssociatedWith)
				((WasAssociatedWith) r).setPlan(null);
			else if(r instanceof WasDerivedFrom)
				((WasDerivedFrom) r).setActivity(null);
			else if(r instanceof WasStartedBy)
				((WasStartedBy) r).setStarter(null);
			else if(r instanceof WasEndedBy )
				((WasEndedBy) r).setEnder(null);
			else if(r instanceof ActedOnBehalfOf)
				((ActedOnBehalfOf) r).setActivity(null);
			statements.add(r);
		}
	
		/* Update the namespace of the new document using the namespace of the original document */
		Namespace ns = d.getNamespace();
		ns.addKnownNamespaces();
		ns.register(CLUSTER_PREFIX, CLUSTER_NS); 
		ns.register("dot", "http://openprovenance.org/Toolbox/dot#");
		Document newDocument = pFactory.newDocument(ns, statements, new HashSet<NamedBundle>());
		
		return newDocument;
	}
}