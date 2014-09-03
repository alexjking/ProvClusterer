ProvClusterer
=============

ProvClusterer is an interactive tool to create a simplified view of a W3C PROV document.
 
This tool identifies clusters of elements within a document using the Girvan-Newman community clustering algorithm.
Each group of elements can be collapsed into a single element resulting in a smaller and less complex document.

This was created by Alexander King as part of a third year project submitted for MEng Computer Science at the
University of Southampton and is built using the ProvToolbox which provides an implementation of the PROV specification.

# Technologies

This application has been seperated into two main packages: 
 * The backend clustering implementation (uk.co.alexjking.prov.ProvClusterer) 
 * The Tomcat WebApp (uk.co.alexjking.prov.ProvClustererWebApp)

## ProvClusterer

This package contains the main logic for clustering and collapsing W3C PROV documents. It requires documents formatted in PROV-JSON and returns the clustered document in the same format. The ProvToDot class can be used to convert a PROV document to a DOT document to display the provenance data as a network graph.

This was implemented in Java using the [ProvToolbox](https://github.com/lucmoreau/ProvToolbox/) to read and manipulate W3C PROV data model representations.

This package can be used alone through command line arguments or can be instantiated to integrate it into another service.

## ProvClustererWebApp

This package integrates the ProvClusterer into a web service and provides an interactive web application (src/main/webapp) using TomCat7, jQuery and Twitter Bootstrap.
This service lets users view and cluster documents stored on the [ProvStore](https://provenance.ecs.soton.ac.uk/store/), as well as letting them save their results here.

# Further Information

For further information on provenance or the W3C PROV specification, visit [W3C: PROV-Overview](http://www.w3.org/TR/prov-overview/) or [W3C: PROV-Primer](http://www.w3.org/TR/prov-primer/).

 



 
