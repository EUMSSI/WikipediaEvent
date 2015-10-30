package de.l3s.eumssi.importing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.l3s.eumssi.enrichment.EnrichStoryRedirection;
import de.l3s.eumssi.model.Entity;
import de.l3s.eumssi.model.Event;
import de.l3s.eumssi.model.Reference;

public abstract class WikiParser {
	
	
	private int year;
	private int month;
	private OpenCalaisCategorization OCC;
	
	public WikiParser() {
		OCC = new OpenCalaisCategorization();
	}

	public ArrayList<Event> getEvents(int year, int month){
		this.year = year;
		this.month = month;
		if (isValidDate()){
			return parseMonthPage(fetchMonthPage(year, month));
		}else{
			System.out.println("ERROR: this parser (" + this.getClass().getName() + ") is not suitable for paring the month: " + month+"-"+year);
			return null;
		}
	}
	
	public ArrayList<String> getDates(int year, int month){
		this.year = year;
		this.month = month;
		if (isValidDate()){
			return parseMonthPageForDatesOnly(fetchMonthPage(year, month));
		}else{
			System.out.println("ERROR: this parser (" + this.getClass().getName() + ") is not suitable for paring the month: " + month+"-"+year);
			return null;
		}
	}
	
	protected abstract boolean isValidDate() ;

	protected abstract ArrayList<Event> parseMonthPage(Document doc);
	
	protected abstract ArrayList<String> parseMonthPageForDatesOnly(Document doc);
	
	private Document fetchMonthPage(int year, int month){
		String monthPageName = getMonthName(month)+"_"+year;
		String monthPagePath = "./downloads/"+monthPageName+".html";
		Document doc = null;
		try {
			if(!new File(monthPagePath).exists()){
				String url = "http://en.wikipedia.org/wiki/"+monthPageName;
				// Fetching the web page
				System.out.println(" fetching page: " + url);
				doc = Jsoup.connect(url).get();			
			}else{
				System.out.println(" loading events from fetched page " + monthPagePath  + " ...");
				doc = Jsoup.parse(new File(monthPagePath), "UTF-8");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return doc;	
	}


	
	protected Event extractDescriptionAndLinks(Element eventNode){
		
		String description = eventNode.text().trim(); // This part is newly added
		String annotatedDescription = getAnnotations(eventNode);
		ArrayList<Reference> referenceArray = new ArrayList<Reference>();
		ArrayList<Entity> entityArray = new ArrayList<Entity>();
		
		Elements tags = eventNode.children(); // This gives all the a tags
		ArrayList<Element> eventLinks = new ArrayList<Element>();
		for (Element link : tags) {
			if (link.tagName().equals("a")) 
				eventLinks.add(link);
			//check if the description has also children:
			if (link.tagName().equals("ul")) {
				for (Element childElement: link.children()){
					if (childElement.tagName().equals("li")){
//						System.out.println("Child element.html --> " + childElement.html());
//						System.out.println("Child element.outerHtml --> " + childElement.outerHtml());
						annotatedDescription = annotatedDescription.replace(childElement.outerHtml(), childElement.html());
						for(Element childTag: childElement.children()){
							if (childTag.tagName().equals("a")) 
								eventLinks.add(childTag);
						}
					}
				}
				annotatedDescription = annotatedDescription.replace("<ul>", "");
				annotatedDescription = annotatedDescription.replace("</ul>", "");
			}
		}
		
		for(Element tag:eventLinks){
			if (tag.hasAttr("rel")) { // This implies it is a news wkReference
				int length = tag.text().length();
				if (length > 2) {
					Reference ref = new Reference("", tag.attr("href"), getSourceName(tag.text()));
					referenceArray.add(ref);
					description = description.replace(ref.getSource(), ""); // This part is added to remove the names of news wkReferences from the event
					description = description.trim();
					description = description.replace("()", "");
					annotatedDescription = annotatedDescription.replace(tag.outerHtml(), "");
				}
			} else { // This implies it is just a link
				// push all the entities in the entity array
				if (isValidWikiURL(tag.attr("href"))){
					Entity entityobj = new Entity();
					// entityobj.setName(name.text());
					entityobj.setName(getEntityName(tag.attr("href")));
					entityobj.setWikiURL(getEntityURL(tag.attr("href")));
					entityArray.add(entityobj);
					annotatedDescription = annotatedDescription.replace(tag.attr("href"), getEntityURL(tag.attr("href")));
				}
			}
		}
		
		// push everything into the event object
		Event event = new Event();
		event.setDescription(description);
		event.setAnnotatedDescription(annotatedDescription);
		event.setEntities(entityArray);
		event.setReferences(referenceArray);
		referenceArray.clear();
		entityArray.clear();
		
		return event;
		
	}
	
	
	
	
	protected String getDate(String text) {
		try{
			int index = 1;
	    	if (text.contains(",")){
	    		if(text.split(",")[0].substring(0,1).matches("[0-9]"))
	    			index = 0;
				return getYear()+"-"+getMonth()+"-"+text.split(",")[0].split(" ")[index];			
			}else{
				if(text.split(" ")[0].substring(0,1).matches("[0-9]"))
	    			index = 0;
				return getYear()+"-"+getMonth()+"-"+text.split(" ")[index];
			}
		}catch(Exception e){
			return "";
		}
	}
	
	private String getMonthName(int month) {
		String monthName = null;
		switch(month){
			case 1: monthName = "January"; break;
		    case 2: monthName = "February"; break;
		    case 3: monthName = "March"; break;
		    case 4: monthName = "April"; break;
		    case 5: monthName = "May"; break;
		    case 6: monthName = "June"; break;
		    case 7: monthName = "July"; break;
		    case 8: monthName = "August"; break;
		    case 9: monthName = "September"; break;
		    case 10: monthName = "October"; break;
		    case 11: monthName = "November"; break;
		    case 12: monthName = "December"; break;
		}
		return monthName;
	}

	protected String getSourceName(String text) {
		if(text.startsWith("("))
			text = text.substring(1, text.length());
		if(text.endsWith(")"))
			text = text.substring(0, text.length()-1);
		return text;
	}
	
	protected String decode(String url) {
		try {
			url = java.net.URLDecoder.decode(url, "UTF-8");
		} catch (Exception e) {
			System.err.println("url (" + url + ") is not able to be converted into UTF8 format");
		}
		return url;
	}

//	protected String getAnnotations(String html, Event event){
//		String annotations = null;
//		String labeledText = null;
//		int entityPos, pos1, pos2;
//		for (Entity entity: event.getEntities()){
//			entityPos = html.indexOf(entity.getWikiURL());
//			pos1 = html.indexOf(">", entityPos) + entityPos;
//			pos2 = html.indexOf("</a>", pos1) + pos1;
//			labeledText = html.substring(pos1, pos2);
//			pos1 = event.getDescription().indexOf(labeledText);
//			pos2 = pos1+labeledText.length();
//			annotations+=entity.getWikiURL()+"___"+pos1+"___"+pos2+"###";			
//		}
//		return annotations;
//	}
	
	protected String getAnnotations(Element  eventNode){
		String annotation = eventNode.html();
//		System.out.println("DESCRIPTION: --> " + annotation);
//		System.out.println(" num of tags = " + eventNode.children().size());
		Element tag;
		for(int i=0; i<eventNode.children().size(); i++){
			tag = eventNode.child(i);
//			System.out.println(i + "-tag: " + tag.tagName() + " has html: " + tag.html() + " and text: " + tag.text() + " and outer html:" + tag.outerHtml());
//			tag.ownText();
			if (!tag.tagName().equals("a") && !tag.tagName().equals("ul")){
				annotation = annotation.replace(tag.outerHtml(), tag.text());
			}else if (tag.tagName().equals("a") && tag.hasAttr("rel")){
				annotation = annotation.replace(tag.outerHtml(), "");
			}
		}
//		System.out.println("TEST DESCRIPTION: --> " + annotation);
		return annotation;
	}
	
	protected String getEntityName(String url){
		//make sure that it is in unicode font
		url = decode(url);
		return getEntityURL(url).replace("_", " ");
	}
	
	protected String getEntityURL(String url){
		url = decode(url);
		String[] url_parts = url.split("/");
		return url_parts[url_parts.length-1];
	}
	
	/*
	 * Return open calais category 
	 * @param WCEP Category: category mentioned in WCEP  
	 */
	protected String getOpenCalaisCategory(String WCEPCategory) {
		return OCC.covertToOpenCalaisCategory(WCEPCategory);
	}
	
	protected String getStoryRedirectionURL(String WikipediaURL) {
		return EnrichStoryRedirection.getRedirectURL(WikipediaURL);
	}

	protected int getYear() {
		return year;
	}

	protected void setYear(int year) {
		this.year = year;
	}

	protected int getMonth() {
		return month;
	}

	protected void setMonth(int month) {
		this.month = month;
	}
	
	protected boolean isValidWikiURL(String url){
		return url.startsWith("/wiki/");
	}
	
}
