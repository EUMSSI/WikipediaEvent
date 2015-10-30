/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.l3s.eumssi.importing;

import java.sql.Date;
import java.util.ArrayList;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import de.l3s.eumssi.model.*;

/**
 * This file is used to extract the events from January 2005 to April 2006, but without May 2005
 */
public class WikiParserFromJan2005ToApr2006 extends WikiParser{
	
	
	protected ArrayList<Event> parseMonthPage(Document doc) {
		
		ArrayList<Event> events = new ArrayList<Event>();

		Elements days = doc.select("div#content.mw-body div#bodyContent div#mw-content-text.mw-content-ltr");
		for (Element eachday : days) { // This will loop only once because it is the whole text
			Elements individual = eachday.children();
			String actualDate = null;
			for (Element dateplustext : individual) { // This consists of alternate date and events (with or withour newsStory)
				if(dateplustext.tagName().equals("h2") || dateplustext.tagName().equals("h3")){
					if(!dateplustext.text().toLowerCase().contains(getYear()+"")){
						continue;
					}else{
						actualDate = getDate(dateplustext.text());
					}
					
					try{
						Date.valueOf(actualDate);
					}catch(Exception ex){
						ex.printStackTrace();
						System.err.println("ERROR: date format is wrong!!!! date = " + actualDate);
						continue;
					}
				}
				
				if(dateplustext.tagName().equals("ul")){
					String description = "";
					String annotatedDescription = "";
				
					// Complete news under a given date
	                Elements stories = dateplustext.children();  // This contains different stories (newsStory may be present or not)
	                for (Element li : stories){
	                	// for storing all the wkReference name and url,
						// this helps in populating the event object
						ArrayList<Reference> referenceArray = new ArrayList<Reference>();
	
						// for storing all the entities information
						// which helps in populating the event object
						ArrayList<Entity> entityArray = new ArrayList<Entity>();
	                            
	                    Elements uls = li.children();  // These are either <a> tags if it doesn't have a newsStory or it is <a> and <ul> tag if it contains a newsStory
	                    boolean hasUL = false; 
	                    for (Element ul: uls) {
	                        if (ul.tagName().equals("ul")) {// If li has ul then it implies that it contains a news story
	                        	hasUL = true; // news story is there
	                        	Node storyNode = li.childNode(0); // this the story .. it is used later at the end for each event
	                            Elements eventsNodes = ul.children();       //Now we get inside the ul element which containd different li elements
	                            for(Element eventNode : eventsNodes){    // Here we are picking one li
									Event event = extractDescriptionAndLinks(eventNode);
									try{
										event.setDate(Date.valueOf(actualDate));
									}catch(Exception ex){
										ex.printStackTrace();
										System.err.println("ERROR: date format is wrong!!!! date = " + actualDate);
										continue;
									}
	                                
									// News story
	                                if (!storyNode.attr("title").isEmpty() && !storyNode.attr("href").isEmpty()) {
	                                	if (isValidWikiURL(storyNode.attr("href"))){
		                                	Story story = new Story();
		    								// story.setName(st.attr("title"));
		    								story.setName(getEntityName(storyNode.attr("href")));
		    								story.setWikipediaUrl(getEntityURL(storyNode.attr("href")));
		    								event.setStory(story);
	                                	}
	    							}
	                                events.add(event);
	                            }
	                        }
	                    }
	                    if(!hasUL){// event does not have a story
							Event event = extractDescriptionAndLinks(li);
							try{
								event.setDate(Date.valueOf(actualDate));
							}catch(Exception ex){
								ex.printStackTrace();
								System.err.println("ERROR: date format is wrong!!!! date = " + actualDate);
								continue;
							}
							events.add(event);
	                    }        
	                }
	            }
			}
		}
		return events;
	}
    
	
	
	@Override
	protected boolean isValidDate() {
		Date startDate   = Date.valueOf("2004-12-31");
		Date endDate   = Date.valueOf("2006-05-01");
		Date parsingDate = Date.valueOf(getYear()+"-"+getMonth()+"-"+"01");
		if(parsingDate.after(startDate) && parsingDate.before(endDate)) return true;
		else return false;
	}



	@Override
	protected ArrayList<String> parseMonthPageForDatesOnly(Document doc) {
		ArrayList<String> dates = new ArrayList<String>();
		Elements days = doc.select("div#content.mw-body div#bodyContent div#mw-content-text.mw-content-ltr");
		for (Element eachday : days) { // This will loop only once because it is the whole text
			Elements individual = eachday.children();
			String actualDate = null;
			for (Element dateplustext : individual) { // This consists of alternate date and events (with or withour newsStory)
				if(dateplustext.tagName().equals("h2") || dateplustext.tagName().equals("h3")){
					if(!dateplustext.text().toLowerCase().contains(getYear()+"")){
						continue;
					}else{
						actualDate = getDate(dateplustext.text());
					}
					try{
						Date.valueOf(actualDate);
						dates.add(actualDate);
					}catch(Exception ex){
						ex.printStackTrace();
						System.err.println("ERROR: date format is wrong!!!! date = " + actualDate);
						continue;
					}
				}
			}
		}
		return dates;
	}

}
