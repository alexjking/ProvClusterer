package uk.co.alexjking.prov.ProvClustererWebApp;

import java.io.IOException;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import uk.co.alexjking.prov.ProvClustererWebApp.ClusteringAPI;

@Path("/convert")
public class ConvertHandler {
	
	@POST
	@Path("/svg")
	public Response convertDocumentToSVG( @FormParam("document") String document){
		try{
			/* Collapse document if not empty */
			if(document != null && !document.equals("")){
				ClusteringAPI api = new ClusteringAPI();
				return Response.status(200).entity(api.convertProvJSONToSVG(document)).build();
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
