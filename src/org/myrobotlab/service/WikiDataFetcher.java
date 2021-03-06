package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.repo.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;
import org.wikidata.wdtk.datamodel.json.jackson.JacksonValueSnak;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

public class WikiDataFetcher extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(WikiDataFetcher.class);
	
	String language = "en";
	String website = "enwiki";
	

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			WikiDataFetcher wdf = (WikiDataFetcher) Runtime.start("wikiDataFetcher", "WikiDataFetcher");
			Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public WikiDataFetcher(String n) {
		super(n);
	}


	public String[] getCategories() {
		return new String[] { "general" };
	}

	@Override
	public String getDescription() {
		return "Used to collect datas from wikidata";
	}
	
	public void setLanguage(String lang){
		language = lang;
	}
	
	public void setWebSite(String site){
		website = site;
	}
	
	private EntityDocument getWiki(String query) throws MediaWikiApiErrorException{
		WikibaseDataFetcher wbdf =  WikibaseDataFetcher.getWikidataDataFetcher();
		EntityDocument wiki = wbdf.getEntityDocumentByTitle(website,upperCaseAllFirst(query));
		if (wiki == null) {
			System.out.println("ERROR ! Can't get the document : " + query);
		 	} 	
		return wiki;
	}
	
	private EntityDocument getWikiById(String query) throws MediaWikiApiErrorException{
		WikibaseDataFetcher wbdf =  WikibaseDataFetcher.getWikidataDataFetcher();
		EntityDocument wiki = wbdf.getEntityDocument(upperCaseAllFirst(query));
		//System.out.println( (String) wiki.getEntityId().getId());
		if (wiki == null) {
			System.out.println("ERROR ! Can't get the document : " + query);
		 	} 	
		return wiki;
	}
	
	
	public String getDescription(String query) throws MediaWikiApiErrorException{
		EntityDocument document = getWiki(query);
		if (document instanceof ValueSnak) {
			System.out.println("MainSnak Value : ");
		 	} 	
		
		try {
			String answer = ((ItemDocument) document).getDescriptions().get(language).getText();
			return answer;
		} 
		catch (Exception e){return  " Description not found";}
		
	}
	
	public String getLabel(String query) throws MediaWikiApiErrorException{
		EntityDocument document = getWiki(query);
		try {
			String answer =  ((ItemDocument) document).getLabels().get(language).getText();
			return answer;
		} 
		catch (Exception e){return  "Label not found";}
	}
	
	public String getId(String query) throws MediaWikiApiErrorException{
		EntityDocument document = getWiki(query);
		try {
			String answer =  document.getEntityId().getId();
			return answer;
		}
		catch (Exception e){return  "ID not found";}
	}
	
	public String getDescriptionById(String query) throws MediaWikiApiErrorException{
		EntityDocument document = getWikiById(query);
		try {
			String answer =  ((ItemDocument) document).getDescriptions().get(language).getText();
			return answer;
		}
	 catch (Exception e){return  "Description by ID  not found";}
	}
	
	public String getLabelById(String query) throws MediaWikiApiErrorException{
		EntityDocument document = getWikiById(query);
		try {
			String answer =  ((ItemDocument) document).getLabels().get(language).getText();
			return answer;
		}
		catch (Exception e){return  "Label by ID not found";}
	}
	
	public String cutStart(String sentence) throws MediaWikiApiErrorException{// keep only the first word (The cat -> The)
		try {
			String answer =  sentence.substring(sentence.indexOf(" ")+1);
			return answer;
		}
		catch (Exception e){return  sentence;}
	}
	public String grabStart(String sentence) throws MediaWikiApiErrorException{// Remove the first word (The cat -> cat)
		try {
			String answer =  sentence.substring(0,sentence.indexOf(" "));
			return answer;
		}
		catch (Exception e){return  sentence;}
	}
	
	public static String upperCaseAllFirst(String value) {

		char[] array = value.toCharArray();
		// Uppercase first letter.
		array[0] = Character.toUpperCase(array[0]);

		// Uppercase all letters that follow a whitespace character.
		for (int i = 1; i < array.length; i++) {
		    if (Character.isWhitespace(array[i - 1])) {
			array[i] = Character.toUpperCase(array[i]);
		    }
		}

		// Result.
		return new String(array);
	    }
	
	private List<StatementGroup> getStatementGroup(String query, String ID) throws MediaWikiApiErrorException{
		EntityDocument document = getWiki(query);
		return  ((ItemDocument) document).getStatementGroups();
	}
	
	private ArrayList getSnak(String query, String ID) throws MediaWikiApiErrorException{;
		List<StatementGroup> document = getStatementGroup(query,ID);
		String dataType = "error";
		Value data = document.get(0).getProperty();
		ArrayList al = new ArrayList();
		for (StatementGroup sg : document) {
			if (ID.equals(sg.getProperty().getId())) { // Check if this ID exist for this document
				
				for (Statement s : sg.getStatements()) {
				if (s.getClaim().getMainSnak() instanceof ValueSnak) {	
					//System.out.println("DataType : " + ((JacksonValueSnak) s.getClaim().getMainSnak()).getDatatype().toString());
					dataType = ((JacksonValueSnak) s.getClaim().getMainSnak()).getDatatype().toString();
					al.add(dataType);
					al.add((JacksonValueSnak) s.getClaim().getMainSnak());
					 
				} 
				
				}
			}
		
		}
		return al;
	
		
	}
	
	public String getData(String query, String ID)throws MediaWikiApiErrorException{
		try {
			ArrayList al = getSnak(query,ID);
			Value data = ((JacksonValueSnak) al.get(1)).getDatavalue();
			String dataType = (String) al.get(0);
			String answer = "";
			switch (dataType) {
         	case "wikibase-item"://
         		String info = (String) data.toString();
         		int beginIndex = info.indexOf('Q');
                int endIndex = info.indexOf("(") ;
                info = info.substring(beginIndex , endIndex-1);
        		answer = getLabelById(info);
         		break;
         	case "time"://
         		data = (TimeValue) data;
         		answer =  String.valueOf(((TimeValue) data).getDay()) +"/" + String.valueOf(((TimeValue) data).getMonth()) +"/" + String.valueOf(((TimeValue) data).getYear()) +" - " + String.valueOf(((TimeValue) data).getHour()) +"H" + String.valueOf(((TimeValue) data).getMinute()) +"Mn";
         		break;
         	case "globe-coordinates":
         		answer = ((GlobeCoordinatesValue)data).toString();
         		break;
         	case "monolingualtext"://
         		data = (MonolingualTextValue)data;
         		answer = data.toString();
         		break;
         	case "quantity"://
         		data = (QuantityValue)data;
         		String quantity = 	String.valueOf(((QuantityValue) data).getNumericValue());
    			String unit = data.toString();
    			int beginIndex2 = unit.indexOf('Q');
    			if (beginIndex2 != -1){
    				unit = unit.substring(beginIndex2);
    				if (Long.parseLong(quantity, 16) <2){quantity += " " + getLabelById(unit);}
    				else {quantity += " " + getLabelById(unit) + "s";}
    			}
    			answer = quantity;
         		break;
         	case "propertyId":
         		answer = ((PropertyIdValue)data).toString();
         		break;
         	case "url"://
         		answer = data.toString();
         		break;
         	case "commonsMedia":
         		data = ((JacksonValueSnak) data).getDatavalue();
         		break;
         	default:
         		answer = "Not Found !";
         		break;
         }
			return answer;
		}
		catch (Exception e){return  "Not Found !";}
	}

	
	public String getProperty(String query, String ID)throws MediaWikiApiErrorException{
		try {
			ArrayList al = getSnak(query,ID);
			String info = (((JacksonValueSnak) al.get(1)).getDatavalue()).toString();
			int beginIndex = info.indexOf('Q');
			int endIndex = info.indexOf("(") ;
			info = info.substring(beginIndex , endIndex-1);
			return getLabelById(info);
		}
		catch (Exception e){return  "Not Found !";}
	}
	
	public String getTime(String query, String ID, String what)throws MediaWikiApiErrorException{
		try {
			ArrayList al = getSnak(query,ID);
			TimeValue date = (TimeValue) ((JacksonValueSnak) al.get(1)).getDatavalue();
			String data ="";
			switch (what) {
        		case "year":
        			data = String.valueOf(date.getYear());
        			break;
        		case "month":
        			data = String.valueOf(date.getMonth());
        			break;
        		case "day":
        			data = String.valueOf(date.getDay());
        			break;
        		case "hour":
        			data = String.valueOf(date.getHour());
        			break;
        		case "minute":
        			data = String.valueOf(date.getMinute());
        			break;
        		case "second":
        			data = String.valueOf(date.getSecond());
        			break;
        		case "before":
        			data = String.valueOf(date.getBeforeTolerance());
        			break;
        		case "after":
        			data = String.valueOf(date.getAfterTolerance());
        			break;
        		default:
        			data = "ERROR";
     
			}
		return data;
		}
		
		catch (Exception e){return  "Not a TimeValue !";}
	}
	
	public String getUrl(String query, String ID)throws MediaWikiApiErrorException{
		try {
			return (((JacksonValueSnak) getSnak(query,ID).get(1)).getDatavalue()).toString();
		}
		catch (Exception e){return  "Not Found !";}
	}
		
	public String getQuantity(String query, String ID)throws MediaWikiApiErrorException{
		try {
			ArrayList al = getSnak(query,ID);
			QuantityValue data = (QuantityValue) (((JacksonValueSnak) al.get(1)).getDatavalue());
			String info = 	String.valueOf(data.getNumericValue());
			String unit = data.toString();
			int beginIndex = unit.indexOf('Q');
			if (beginIndex != -1){
				unit = unit.substring(beginIndex);
				info+= " " + getLabelById(unit);
			}
		return info;
		}	
		catch (Exception e){return  "Not Found !";}
	}
	
	public String getMonolingualValue(String query, String ID)throws MediaWikiApiErrorException{
		try {
			return (((JacksonValueSnak) getSnak(query,ID).get(1)).getDatavalue()).toString();
		}
		catch (Exception e){return  "Not Found !";}
	}
	
	/**
	 * This static method returns all the details of the class without
	 * it having to be constructed.  It has description, categories,
	 * dependencies, and peer definitions.
	 * 
	 * @return ServiceType - returns all the data
	 * 
	 */
	static public ServiceType getMetaData(){
		
		ServiceType meta = new ServiceType(WikiDataFetcher.class.getCanonicalName());
		meta.addDescription("service interface for Wikipedia");
		meta.addCategory("intelligence");		
		meta.addDependency("org.wikidata.wdtk", "0.5.0");
		return meta;		
	}
	
	
}
