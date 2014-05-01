package uk.co.alexjking.prov.ProvClustererWebApp;

import java.io.IOException;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import uk.co.alexjking.prov.ProvClustererWebApp.ClusteringAPI;



@Path("/cluster")
public class ClusterHandler {

		@POST
		@Path("/{num-communities}")
		public Response clusterDocument(@PathParam("num-communities") int numCommunities, @FormParam("document") String document){
			try{
				/* Cluster document if not empty */
				if(document != null && !document.equals("")){
					ClusteringAPI api = new ClusteringAPI();
					return Response.status(200).entity(api.clusterProvJSON(document, numCommunities)).build();
				}else{
					return Response.status(400).entity("Empty PROV-JSON object").build();
				}
			}catch(com.google.gson.JsonSyntaxException mje){
				/* invalid json */
				return Response.status(400).entity(mje.getMessage()).build();
			} catch (IOException e) {
				return Response.status(400).entity(e.getMessage()).build();
			}
		}
		
		
}
