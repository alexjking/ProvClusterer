package uk.co.alexjking.prov.ProvClustererWebApp;

import java.io.IOException;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import uk.co.alexjking.prov.ProvClustererWebApp.ClusteringAPI;

@Path("/expand")
public class ExpandHandler {
	
	@POST
	@Path("/{cluster-id}")
	public Response expandDocument(@PathParam("cluster-id") String clusterID, @FormParam("clustered-document") String clusteredDocument,  @FormParam("original-clustered-document") String originalClusteredDocument){
		try{
			/* Collapse document if not empty */
			if(clusteredDocument != null && !clusteredDocument.equals("")){
				ClusteringAPI api = new ClusteringAPI();
				return Response.status(200).entity(api.expandClusteredProvJSON(clusteredDocument, originalClusteredDocument, clusterID)).build();
			}else{
				return Response.status(400).entity("Empty PROV-JSON object").build();
			}
		} catch(com.google.gson.JsonSyntaxException mje){
			/* invalid json */
			return Response.status(400).entity(mje.getMessage()).build();
		} catch (IOException e) {
			return Response.status(400).entity(e.getMessage()).build();
		}
	}

}
