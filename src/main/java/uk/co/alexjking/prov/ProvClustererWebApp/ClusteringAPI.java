package uk.co.alexjking.prov.ProvClustererWebApp;


import java.io.IOException;

import org.openprovenance.prov.model.Document;

import uk.co.alexjking.prov.ProvClusterer.ClusterCollapseExpand;
import uk.co.alexjking.prov.ProvClusterer.DocumentConverter;
import uk.co.alexjking.prov.ProvClusterer.ProvClusterer;


/**
 * Utility class for the API.
 * Uses classes within uk.co.alexjking.prov.cluster.bfs
 *
 */
public class ClusteringAPI {
	
	DocumentConverter documentConverter = new DocumentConverter();

	
	/**
	 * Clusters a String in PROV-JSON format returning clustered document in PROV-JSON
	 * 
	 * @param provJSON PROV-JSON String
	 * @param numCommunities Number of communities to divide the document into
	 * @return PROV-JSON String representing clustered document.
	 * @throws IOException
	 */
	public String clusterProvJSON(String provJSON, int numCommunities) throws IOException{
		Document doc = documentConverter.convertProvJSONToDocument(provJSON);
		Document clusteredDoc = clusterDocument(doc, numCommunities);
		return  documentConverter.convertDocumentToProvJSON(clusteredDoc);
	}
	
	/**
	 * Cluster a document using ProvClusterer
	 * 
	 * @param doc Document to cluster
	 * @param numCommunities Number of communities to divide the document into
	 * @return Clustered document
	 */
	private Document clusterDocument(Document doc, int numCommunities){
		ProvClusterer clusterer = new ProvClusterer();
		return clusterer.clusterDocument(doc, numCommunities);
	}
	
	
	/**
	 * Cluster PROV-JSON String and convert to SVG
	 * 
	 * @param provJSON String containing PROV-JSON Document
	 * @param numCommunities Number of communities to cluster the document into
	 * @return SVG String
	 * @throws IOException
	 */
	public String clusterProvJSONIntoSVG(String provJSON, int numCommunities) throws IOException{
		Document doc = documentConverter.convertProvJSONToDocument(provJSON);
		Document clusteredDoc = clusterDocument(doc, numCommunities);		
		return documentConverter.convertDocumentToSVG(clusteredDoc);		
	}
	
	
	/**
	 * Converts an PROV-JSON string to SVG
	 * 
	 * @param provJSON STring containing document in PROV-JSON
	 * @return String containing SVG
	 * @throws IOException
	 */
	public String convertProvJSONToSVG(String provJSON) throws IOException{
		Document doc = documentConverter.convertProvJSONToDocument(provJSON);
		return documentConverter.convertDocumentToSVG(doc);
	}
	
	/**
	 * Collapse all clusters in a document
	 * 
	 * @param provJSON String containing clustered document in PROV-JSON
	 * @return String containing collapsed document in PROV-JSON
	 * @throws IOException
	 */
	public String collapseAllProvJSON(String provJSON) throws IOException{
		Document doc = documentConverter.convertProvJSONToDocument(provJSON);
		ClusterCollapseExpand collapse = new ClusterCollapseExpand(doc);
		Document collapsedDocument = collapse.collapseAll();
		return documentConverter.convertDocumentToProvJSON(collapsedDocument);
	}

	/**
	 * Collapse a cluster within a clustered document encoded in PROV-JSON
	 * @return String containing collapsed document in PROV-JSON
	 */
	public String collapseClusteredProvJSON(String provJSON, String clusterID, String clusterLabel) throws IOException{
		Document doc = documentConverter.convertProvJSONToDocument(provJSON);
		ClusterCollapseExpand collapse = new ClusterCollapseExpand(doc);
		Document collapsedDocument = collapse.collapseCluster(clusterID, clusterLabel);
		return documentConverter.convertDocumentToProvJSON(collapsedDocument);
	}
	
	
	/**
	 * Expand a cluster within a clustered document encoded in PROV-JSON
	 * 
	 * @param provJSON
	 * @param originalProvJSON
	 * @param clusterID
	 * @return String containing expanded document in PROV-JSON
	 * @throws IOException
	 */
	public String expandClusteredProvJSON(String provJSON, String originalProvJSON, String clusterID) throws IOException{
		Document clusteredDocument = documentConverter.convertProvJSONToDocument(provJSON);
		Document originalDocument = documentConverter.convertProvJSONToDocument(originalProvJSON);
		ClusterCollapseExpand expand = new ClusterCollapseExpand(clusteredDocument);
		Document expandedDocument = expand.expandCluster(originalDocument, clusterID);
		return documentConverter.convertDocumentToProvJSON(expandedDocument);	
	}
	
}
