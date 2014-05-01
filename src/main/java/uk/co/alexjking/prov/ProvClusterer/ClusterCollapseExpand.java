package uk.co.alexjking.prov.ProvClusterer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openprovenance.prov.model.Activity;
import org.openprovenance.prov.model.Agent;
import org.openprovenance.prov.model.Attribute;
import org.openprovenance.prov.model.Document;
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
import org.openprovenance.prov.xml.Entity;
import org.openprovenance.prov.xml.ProvFactory;


/**
 * Class which provides functions to expand and collapse clusters within a clustered document
 *
 */
public class ClusterCollapseExpand {
	
	private Document doc;
	static final ProvUtilities utils = new ProvUtilities();
	public static org.openprovenance.prov.model.ProvFactory pFactory = new ProvFactory();
	public static org.openprovenance.prov.model.Name name = pFactory.getName();
	public static final String CLUSTER_NS = "http://prov.alexjking.co.uk/";
	public static final String CLUSTER_PREFIX = "cluster";

	public ClusterCollapseExpand(Document doc){
		this.doc = doc;
	}
	

	
	/**
	 * Expands the specified cluster of the internal document. 
	 * Requires the same document without any clusters collapsed to generate extra relations/elements.
	 * 
	 * @param originalDocument Originally clustered document
	 * @param clusterID ID of cluster to expand.
	 * @return Same document with specified cluster expanded
	 */
	public Document expandCluster(Document originalDocument, String clusterID){
		ClusteredDocumentData originalDocumentData = new ClusteredDocumentData(originalDocument);
		ClusteredDocumentData clusteredDocumentData = new ClusteredDocumentData(doc);
		
		/* check if specified element is a clustered element */
		Other idOther = pFactory.newOther(CLUSTER_NS, "id", CLUSTER_PREFIX, clusterID, name.XSD_STRING);
		Type elementType = pFactory.newType( pFactory.newQualifiedName(CLUSTER_NS, "cluster", CLUSTER_PREFIX), name.XSD_QNAME);
		Element clusterElement = null;
		int clusterElementIndex = -1;
		
		/* Get the cluster element */
		for(int i=0;i<clusteredDocumentData.elements.size(); i++){
			Element e = clusteredDocumentData.elements.get(i);
			if(((HasOther) e).getOther().contains(idOther)){
				/* Return the document as the element has the wrong type */
				clusterElement = e;
				clusterElementIndex = i;
				break;
			}
		}
		/* return if element doesn't exist or if it's type isnt a cluster */
		if(clusterElement == null || !clusterElement.getType().contains(elementType)){
			System.err.println("Error: Cluster element doesn't exist");
			return doc;
		}
		/* Generate a list of statements to generate the new document with */
		Set<Statement> statements = new HashSet<Statement>();
		statements.addAll(clusteredDocumentData.elements);
		statements.addAll(clusteredDocumentData.relations);
		
		/* Find internal cluster elements from old document and add to new document */
		List<Integer> originalClusterElementIndexes = originalDocumentData.getElementsBelongingToCluster(clusterID);
		for(int i: originalClusterElementIndexes){
			statements.add(originalDocumentData.elements.get(i));
		}
		
		/* Remove the cluster element from the new document */
		statements.remove(clusterElement);
		
		/* Remove external relationships from cluster element */
		List<Integer> clusterElementNeighbourIndexes = clusteredDocumentData.neighboursList.get(clusterElementIndex);
		for(int neighbourIndex: clusterElementNeighbourIndexes){
			Relation0 r =clusteredDocumentData.relations.get(clusteredDocumentData.neighbouringElements[clusterElementIndex][neighbourIndex]);
			statements.remove(r); //remove relation from new document
		}
	
		/* Find internal relationships and add to document */
		int[] relationOccurrences = originalDocumentData.getRelationOccurrences(originalClusterElementIndexes);
		List<Integer> internalRelationIndexes = originalDocumentData.getInternalRelationIndexes(relationOccurrences);
		for(int relationIndex: internalRelationIndexes){
			statements.add(originalDocumentData.relations.get(relationIndex));
		}
		
		/* Find external relationships and add to document */
		List<Integer> externalRelationIndexes = originalDocumentData.getExternalRelationIndexes(relationOccurrences);
		for(int relationIndex: externalRelationIndexes){
			Relation0 r = originalDocumentData.relations.get(relationIndex);
			int causeIndex = originalDocumentData.qualifiedNames.indexOf(utils.getCause(r));
			int effectIndex = originalDocumentData.qualifiedNames.indexOf(utils.getEffect(r));
			Element effectElement = originalDocumentData.elements.get(effectIndex);
			Element causeElement = originalDocumentData.elements.get(causeIndex);
			/* Find whether the cause or the effect is the external element */
			if(originalClusterElementIndexes.contains(causeIndex)){
				/* The external element is the effect */
				/* Check if the effect is in the new document */
				if(clusteredDocumentData.elements.contains(effectElement)){
					statements.add(r);
				}else{
					/* We need to rewrite the relationship from the cause to the new clustered element */
					String externalClusterID = getClusterID(effectElement);
					Element externalElement = getElementByID(clusteredDocumentData, externalClusterID);
					statements.add(generateClusterRelation(causeElement, externalElement, r));
				}
			}else if(originalClusterElementIndexes.contains(effectIndex)){
				/* The external element is the cause */
				/* Check if the cause is in the new document */
				if(clusteredDocumentData.elements.contains(causeElement)){
					statements.add(r);
				}else{
					/* We need to rewrite the relationship from the effect to the new clustered element */
					String externalClusterID = getClusterID(causeElement);
					Element externalElement = getElementByID(clusteredDocumentData, externalClusterID);
					statements.add(generateClusterRelation(externalElement, effectElement, r));
				}
			}else{
				System.err.println("Error: Relation which should be linked to cluster is not linked to cluster in expandCluster()");
			}

			
		}
			

		Namespace ns = doc.getNamespace();
		ns.addKnownNamespaces();
		Document newDocument = pFactory.newDocument(ns, statements, new HashSet<NamedBundle>());
		doc = newDocument;
		
		
		DocumentData newDD = new DocumentData(newDocument);
		List<Statement> newStatements = new ArrayList<Statement>();
		newStatements.addAll(newDD.elements);
		newStatements.addAll(newDD.relations);
		Document newerDocument = pFactory.newDocument(ns, newStatements, new HashSet<NamedBundle>());
		doc = newerDocument;
		return newerDocument;
	}
	
	/**
	 * Generates a cluster element for collapsing documents
	 * @param clusterID Id of the cluster element.
	 * @param clusterElements List of elements with specified cluster ID.
	 * @param clusterLabel String to label generated cluster element.
	 * @return Element representing the whole cluster.
	 */
	private Element generateClusterElement(String clusterID, List<Element> clusterElements, String clusterLabel){
		
		/* Set cluster name */
		if(clusterLabel.equals("")){
			clusterLabel = "Cluster-"+clusterID;
		}else{
			clusterLabel += "-";
			clusterLabel += clusterID;
		}
		
		List<Attribute> attributes = new ArrayList<Attribute>();
		int agentCount=0, entityCount=0;
		/* Add id attribute */
		Other idAtt = pFactory.newOther(CLUSTER_NS, "id", CLUSTER_PREFIX, clusterID, name.XSD_STRING);
		attributes.add((Attribute) idAtt);
				
		/* Add sub-element attributes  and count the types of each element in the cluster */
		for(Element element: clusterElements){
			Other otherAtt = pFactory.newOther(CLUSTER_NS, "sub-element", CLUSTER_PREFIX, element.getId(), name.XSD_STRING);
			attributes.add((Attribute) otherAtt);
			/* Increment the type counter for this element */
			if(element instanceof Agent)
				agentCount++;
			else if(element instanceof Entity)
				entityCount++;
		}
		/* Generate new element */
		Element clusterElement = null;
		if(entityCount == clusterElements.size()){
			clusterElement = pFactory.newEntity(pFactory.newQualifiedName(CLUSTER_NS, clusterLabel, CLUSTER_PREFIX), attributes);	
		}else if(agentCount == clusterElements.size()){
			clusterElement = pFactory.newAgent(pFactory.newQualifiedName(CLUSTER_NS, clusterLabel, CLUSTER_PREFIX), attributes);
		}else{
			clusterElement = pFactory.newActivity(pFactory.newQualifiedName(CLUSTER_NS, clusterLabel, CLUSTER_PREFIX));
			for(Attribute a: attributes)
				((HasOther)clusterElement).getOther().add((Other)a);
		}
		/* Add cluster type attribute */
		Type clusterElementType = pFactory.newType( pFactory.newQualifiedName(CLUSTER_NS, "cluster", CLUSTER_PREFIX), name.XSD_QNAME);
		clusterElement.getType().add(clusterElementType);
		
		/* Add colour attribute from original elements */
		for(Other other : ((HasOther) clusterElements.get(0)).getOther()){
			if(other.getElementName().getPrefix().equals("dot") && other.getElementName().getLocalPart().equals("fillcolor")){
				Other colourAtt = pFactory.newOther("http://openprovenance.org/Toolbox/dot#", "fillcolor", "dot", other.getValue(), name.XSD_STRING);
				((HasOther) clusterElement).getOther().add(colourAtt);
				break;
			}
		}
		return clusterElement;
	}
		
	/**
	 * Collapse every cluster within the internal document
	 * @return Internal document with every cluster collapsed.
	 */
	public Document collapseAll(){
		/* Generate list of clusterIDs */
		Set<String> clusterIdList = new HashSet<String>();
		DocumentData dd = new DocumentData(doc);
		for(Element e: dd.elements){
			HasOther o = (HasOther) e;
			for(Other other : o.getOther()){
				if(other.getElementName().getPrefix().equals("cluster") && other.getElementName().getLocalPart().equals("id")){
					clusterIdList.add((String) other.getConvertedValue());
					continue;
				}
			}
		}
		
		/* Collapse each cluster found */
		for(String id: clusterIdList){
			collapseCluster(id, "");
		}
		return doc;
	}
	
	
	/**
	 * Expands all clusters within the internal document
	 * 
	 * @param originalDocument Original clustered document without any clusters collapsed, to generate the new elements and relations. 
	 * @return Internal document with all elements expanded.
	 */
	public Document expandAll(Document originalDocument){
		/* Generate list of clusterIDs */
		Set<String> clusterIdList = new HashSet<String>();
		DocumentData dd = new DocumentData(doc);
		for(Element e: dd.elements){
			HasOther o = (HasOther) e;
			for(Other other : o.getOther()){
				if(other.getElementName().getPrefix().equals("cluster") && other.getElementName().getLocalPart().equals("id")){
					clusterIdList.add((String) other.getConvertedValue());
					continue;
				}
			}
		}
		/* expand each cluster found */
		for(String id: clusterIdList){
			expandCluster(originalDocument, id);
		}
		return doc;
	}
	
	/**
	 * Collapses the specified cluster within the internal document.
	 * 
	 * @param clusterID Id of cluster to collapse.
	 * @param clusterLabel Label to assign the cluster element.
	 * @return Internal document with specified cluster collapsed.
	 */
	public Document collapseCluster(String clusterID, String clusterLabel){
		ClusteredDocumentData dd = new ClusteredDocumentData(doc);
		/* get elements and element indexes belonging to the cluster */
		List<Integer> clusterElementIndexes = dd.getElementsBelongingToCluster(clusterID);
		List<Element> clusterElements = new ArrayList<Element>();
		for(int elementIndex: clusterElementIndexes){
			clusterElements.add(dd.elements.get(elementIndex));
		}
		
		/* Get the number of times a relation has occurred from cluster elements */
		int[] relationOccurrences = dd.getRelationOccurrences(clusterElementIndexes);
		
		/* Get internal and external relations */
		List<Integer> internalRelationIndexes = dd.getInternalRelationIndexes(relationOccurrences);
		List<Integer> externalRelationIndexes = dd.getExternalRelationIndexes(relationOccurrences);
		/*
		 * Generate a new document
		 */
		
		Set<Statement> statements = new HashSet<Statement>();
		/* Add all relations and elements to the list of statements */
		statements.addAll(dd.relations);
		statements.addAll(dd.elements);
		
		/* Remove all internal relations and external relations*/
		for(int relationIndex: internalRelationIndexes){
			statements.remove(dd.relations.get(relationIndex));
		}
		for(int relationIndex: externalRelationIndexes){
			statements.remove(dd.relations.get(relationIndex));
		}
			
		
		/* Remove all elements from within the cluster */
		for(int elementIndex: clusterElementIndexes)
			statements.remove(dd.elements.get(elementIndex));
		
		/* Generate and add the cluster element to the list of statements */
		Element clusterElement = generateClusterElement(clusterID, clusterElements, clusterLabel);
		statements.add(clusterElement);
		
		/*
		 * add external relations
		 */
		for(int relationIndex: externalRelationIndexes){
			//find whether the internal element is the cause or the effect
			Relation0 relation = dd.relations.get(relationIndex);
			int causeIndex = dd.qualifiedNames.indexOf(utils.getCause(relation));
			int effectIndex = dd.qualifiedNames.indexOf(utils.getEffect(relation));
			
			if(causeIndex == -1 || effectIndex == -1) 
				System.err.println("Error generating collapsed document: A relation without a cause or effect has been found.");	
			Relation0 newRelation = null;
			
			if(clusterElementIndexes.contains(effectIndex))
				newRelation = generateClusterRelation(clusterElement, dd.elements.get(causeIndex), relation);
			else if(clusterElementIndexes.contains(causeIndex))
				newRelation = generateClusterRelation(dd.elements.get(effectIndex), clusterElement, relation);
			else
				System.err.println("Error: Relation which should be linked to cluster is not linked in collapseCluster()");

			statements.add(newRelation);
				
		}
		Namespace ns = doc.getNamespace();
		ns.addKnownNamespaces();
		Document newDocument = pFactory.newDocument(ns, statements, new HashSet<NamedBundle>());
		doc = newDocument;
		
		DocumentData newDD = new DocumentData(newDocument);
		List<Statement> newStatements = new ArrayList<Statement>();
		newStatements.addAll(newDD.elements);
		newStatements.addAll(newDD.relations);
		Document newerDocument = pFactory.newDocument(ns, newStatements, new HashSet<NamedBundle>());
		doc = newerDocument;
		return newerDocument;

	}
	
	/**
	 * Generates a relation between the two elements. Rewrites as necessary.
	 * 
	 * @param causeElement New cause element.
	 * @param effectElement New effect element
	 * @param currentRelation Current relation between the two elements.
	 * @return Rewritten version of currentRelation
	 */
	private Relation0 generateClusterRelation(Element causeElement, Element effectElement, Relation0 currentRelation){
		
		QualifiedName newRelationId = pFactory.newQualifiedName(CLUSTER_NS, "generatedRelation"+System.nanoTime(), CLUSTER_PREFIX);
		if(causeElement instanceof Entity){
			if(effectElement instanceof Entity){
				return pFactory.newWasDerivedFrom(newRelationId, causeElement.getId(), effectElement.getId());
			}else if(effectElement instanceof Activity){
				return pFactory.newWasGeneratedBy(newRelationId, causeElement.getId(), effectElement.getId());
			}else if(effectElement instanceof Agent){
				return pFactory.newWasAttributedTo(newRelationId, causeElement.getId(), effectElement.getId());
			}
		}else if(causeElement instanceof Activity){
			if(effectElement instanceof Entity){
				return pFactory.newUsed(newRelationId, causeElement.getId(), effectElement.getId()); 
			}else if(effectElement instanceof Activity){
				return pFactory.newWasInformedBy(newRelationId, causeElement.getId(), effectElement.getId());
			}else if(effectElement instanceof Agent){
				return pFactory.newWasAssociatedWith(newRelationId, causeElement.getId(), effectElement.getId());
			}
		}else if(causeElement instanceof Agent){
			if(effectElement instanceof Entity){
			//	System.err.println("Error generating new cluster relation: Cannot create relation from agent to entity.");
				return pFactory.newWasInfluencedBy(newRelationId, causeElement.getId(), effectElement.getId());
				//return null;
			}else if(effectElement instanceof Activity){
				//System.err.println("Error generating new cluster relation: Cannot create relation from agent to activity - generating wasAssociatedWith in the opposite direction instead.");
				return pFactory.newWasInfluencedBy(newRelationId, causeElement.getId(), effectElement.getId());
			}else if(effectElement instanceof Agent){
				return pFactory.newActedOnBehalfOf(newRelationId, causeElement.getId(), effectElement.getId());
			}
		}
		System.err.println("Error in generateClusterRelation(): Unknown element type");
		return null;
	}

	/**
	 * Gets the cluster id of element e
	 * @param e An element
	 * @return cluster id of element e
	 */
	private String getClusterID(Element e){
		for(Other o: ((HasOther) e).getOther()){
			if(o.getElementName().getPrefix().equals("cluster") && o.getElementName().getLocalPart().equals("id")){
				return o.getConvertedValue().toString();
			}
		}
		return null;
	}
	
	/**
	 * Retrieves element with the specified id
	 * @param dd
	 * @param clusterID
	 * @return
	 */
	private Element getElementByID(DocumentData dd, String clusterID){
		if(clusterID == null){
			return null;
		}
		Other idOther = pFactory.newOther(CLUSTER_NS, "id", CLUSTER_PREFIX, clusterID, name.XSD_STRING);
		for(Element e: dd.elements){
			if(((HasOther) e).getOther().contains(idOther)){
				return e;
			}
		}
		return null;
	}
	
	/**
	 * Get number of elements within a document
	 * @return
	 */
	public String getNumElements(){
		DocumentData dd = new DocumentData(doc);
		return String.valueOf(dd.elements.size());
	}
	
}
