package uk.co.alexjking.prov.ProvClusterer;

import java.util.Set;


import org.openprovenance.prov.model.ProvUtilities;
import org.openprovenance.prov.model.Relation0;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.xml.ProvFactory;

/**
 * Class which implements Girvan-Newman 2002 for community detection.
 *
 */
public class ProvClusterer {
	static final ProvUtilities utils = new ProvUtilities();
	public static org.openprovenance.prov.model.ProvFactory pFactory = new ProvFactory();
	public static org.openprovenance.prov.model.Name name = pFactory.getName();
	public static final String CLUSTER_NS = "http://prov.alexjking.co.uk/";
	public static final String CLUSTER_PREFIX = "cluster";
	
	/**
	 * Girvan-Newman algorithm implementation
	 * 
	 * @param doc Provenance document to cluster.
	 * @param numCommunities Number of communities to divide the document into.
	 * @return PRovenance document where each element is labelled with the cluster it belongs to.
	 */
	public Document clusterDocument(Document doc, int numCommunities){
		/* Generate document information */
		DocumentData dd = new DocumentData(doc);
		
		Set<Set<Integer>> communities = dd.generateCommunities();
		int currentNumCommunities = communities.size();
		
		while(currentNumCommunities < numCommunities && currentNumCommunities < dd.elements.size()){
			EdgeBetweennessCentrality betweenness = new EdgeBetweennessCentrality(dd);
			Relation0 mostBetweenRelation = betweenness.getMostBetweenRelation(); //calculate edge betweenness for all edges
			dd.removeRelation(mostBetweenRelation);
			
			/* calculate the current set of communities to see if we need to remove any more edges */
			communities = dd.generateCommunities();
			currentNumCommunities = communities.size();
		}
		
		/* Generate a new document from these communities */
		Document newDocument = dd.generateClusteredDocument();
		return newDocument;
		
	}
	

}






