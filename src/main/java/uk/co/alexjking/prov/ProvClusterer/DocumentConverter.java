package uk.co.alexjking.prov.ProvClusterer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


import org.openprovenance.prov.dot.ProvToDot.Config;
import org.openprovenance.prov.json.Converter;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.ProvUtilities;
import org.openprovenance.prov.xml.ProvFactory;

/**
 * Utility class which uses ProvToolbox to convert between document formats.
 *
 */
public class DocumentConverter {
	
	private ProvClustererToDot provToDot;
	static final ProvUtilities utils = new ProvUtilities();
	public static org.openprovenance.prov.model.ProvFactory pFactory = new ProvFactory();
	public static org.openprovenance.prov.model.Name name = pFactory.getName();
	public static final String CLUSTER_NS = "http://prov.alexjking.co.uk/";
	public static final String CLUSTER_PREFIX = "cluster";
	final Converter pFactoryConverter = new Converter(pFactory);
	
	public DocumentConverter(){
		this.provToDot = new ProvClustererToDot(Config.ROLE_NO_LABEL);
	}
	

	/**
	 * Converts a document to SVG, saving resulting file as "output.svg"
	 * @param clusteredDocument
	 */
	public void convertToSVG(Document doc, String dotFileName, String svgFileName){
		try {
			provToDot.convert(doc, dotFileName, svgFileName, "svg", "Clustered Document - SVG");
		} catch (FileNotFoundException e) {
			System.err.println("File not found: Could not generate SVG.");
		} catch (IOException e) {
			System.err.println("IOException: Could not generate SVG.");
		}
	}
	
	/**
	 * Converts a document to PDF, saving resulting file to "output.pdf"
	 * @param clusteredDocument
	 */
	public void convertToPDF(Document doc, String dotFileName, String pdfFileName){
		try {
			provToDot.convert(doc, dotFileName, pdfFileName, "pdf", "Clustered Document - PDF");
		} catch (FileNotFoundException e) {
			System.err.println("File not found: Could not generate PDF.");
		} catch (IOException e) {
			System.err.println("IOException: Could not generate PDF.");
		}
	}
	
	/**
	 * Converts a clustered document to xdot for use in an external application such as JS visualisation
	 * @param clusteredDocument
	 */
	public void convertToXDOT(Document doc, String dotFileName, String xdotFileName){
		try {
			provToDot.convert(doc, dotFileName, xdotFileName, "xdot", "Clustered Document - XDOT");
		} catch (FileNotFoundException e) {
			System.err.println("File not found: Could not generate XDOT.");
		} catch (IOException e) {
			System.err.println("IOException: Could not generate XDOT.");
		}
	}
	
	/**
	 * Converts String in PROV-JSON to Java document
	 * @param provJSON
	 * @return
	 * @throws IOException
	 */
	public Document convertProvJSONToDocument(String provJSON) throws IOException{
		File file = File.createTempFile("json-", ".json");
		BufferedWriter bwj= new BufferedWriter(new FileWriter(file));
		bwj.append(provJSON);
		bwj.close();
		Document doc = pFactoryConverter.readDocument(file.getAbsolutePath());
		return doc;
	}
	
	/**
	 * Converts Java document to String SVG
	 * @param doc
	 * @return
	 * @throws IOException
	 */
	public String convertDocumentToSVG(Document doc) throws IOException{
		String svg = "";
		File file = File.createTempFile("svg-", ".svg");
		File dotFile = File.createTempFile("dot-", ".dot");
		convertToSVG(doc, dotFile.getAbsolutePath(), file.getAbsolutePath());
		BufferedReader br = new BufferedReader(new FileReader(file));
		while(br.ready()){
			svg += br.readLine();
		}
		br.close();
		return svg;
	}
	
	
	/**
	 * Converts Java document to PROV-JSON String
	 * @param doc
	 * @return
	 * @throws IOException
	 */
	public String convertDocumentToProvJSON(Document doc) throws IOException{
		String provJSON = "";
		File file = File.createTempFile("output-provjson-", ".json");
		pFactoryConverter.writeDocument(doc, file.getAbsolutePath());
		BufferedReader br = new BufferedReader(new FileReader(file));
		while(br.ready()){
			provJSON += br.readLine();
		}
		br.close();
		return provJSON;
	}


}
