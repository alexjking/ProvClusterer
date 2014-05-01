package uk.co.alexjking.prov.ProvClusterer;

import java.io.PrintStream;
import java.util.HashMap;

import org.openprovenance.prov.dot.ProvToDot;
import org.openprovenance.prov.model.Activity;
import org.openprovenance.prov.model.Agent;
import org.openprovenance.prov.model.Element;
import org.openprovenance.prov.model.Entity;
import org.openprovenance.prov.model.HasOther;
import org.openprovenance.prov.model.Other;
import org.openprovenance.prov.model.QualifiedName;
import org.openprovenance.prov.model.Type;


/**
 * Class which extends ProvToDot to provide custom URLS for the ProvClusterer
 * 
 * @author Alexander King
 *
 */
public class ProvClustererToDot extends ProvToDot {

	public ProvClustererToDot() {
		super();
	}
	public ProvClustererToDot(boolean withRoleFlag) {
		super(withRoleFlag);
	}
	public ProvClustererToDot(Config config) {
		super(config);
	}

	public ProvClustererToDot(String configurationFile) {
		super(configurationFile);
	}

	public ProvClustererToDot(String configurationFile, String other) {
		super(configurationFile, other);
	}

	@Override
	public void emitActivity(Activity p, PrintStream out) {
		HashMap<String,String> properties=new HashMap<String, String>();

		emitElement(p.getId(),
				addClusterURL(p, addActivityShape(p,addActivityLabel(p, addActivityColor(p,properties)))),
				out);

		emitAnnotations("", p,out);
	}

	@Override
	public void emitEntity(Entity a, PrintStream out) {
		HashMap<String,String> properties=new HashMap<String, String>();

		emitElement(a.getId(),
				addClusterURL(a, addEntityShape(a,addEntityLabel(a, addEntityColor(a,properties)))),
				out);

		emitAnnotations("", a,out);
	}

	@Override
	public void emitAgent(Agent ag, PrintStream out) {
		HashMap<String,String> properties=new HashMap<String, String>();

		emitElement(ag.getId(),
				addClusterURL(ag, addAgentShape(ag,addAgentLabel(ag, addAgentColor(ag,properties)))),
				out);

		emitAnnotations("", ag,out);

	}


	public HashMap<String, String> addClusterURL(Element element,
			HashMap<String, String> properties) {
		if(element!=null){
			String id = "";
			String functionName = "";
			/* Find the cluster type to decide the collapse/expand function name */
			for(Type type : element.getType()){
				Object typeValueObj = type.getConvertedValue();
				if(typeValueObj instanceof QualifiedName){
					QualifiedName typeValue = (QualifiedName) typeValueObj;
					if(typeValue.getPrefix().equals("cluster")){
						if(typeValue.getLocalPart().equals("element")){
							functionName = "collapse";
							break;
						}else if(typeValue.getLocalPart().equals("cluster")){
							functionName = "expand";
							break;
						}
					}
				}
			}

			/* Find the cluster id from the attributes */
			for(Other otherAtt: ((HasOther)element).getOther()){
				if(otherAtt.getElementName().getPrefix().equals("cluster") && otherAtt.getElementName().getLocalPart().equals("id")){
					id = otherAtt.getConvertedValue().toString();
					break;  
				}
			}

			/* If we've found an id and a function name, create the JS function URL */
			if(!id.isEmpty() && !functionName.isEmpty()){
				properties.put("URL", "javascript:" + functionName +"(" + id + ")");
			}else{
				properties.put("URL", element.getId().getNamespaceURI()+element.getId().getLocalPart());
			}
		}

		return properties;
	}

}
