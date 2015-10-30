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
 * This file is used to extract the wikinews from October 2000 to December 2004
 */
public class WikiParserFromOct2000ToDec2004 extends WikiParser{
	
	
	protected ArrayList<Event> parseMonthPage(Document doc) {
		
		ArrayList<Event> events = new ArrayList<Event>();

		Elements days = doc.select("div#content.mw-body div#bodyContent div#mw-content-text.mw-content-ltr");
		for (Element eachday : days) { // This will loop only once because it is the whole text
			Elements individual = eachday.children();
			String actualDate = null;
			for (Element dateplustext : individual) { // This consists of alternate date and events (with or withour newsStory)
				if (dateplustext.tagName().equals("h3")) { // Upto December 2004
					actualDate = getDate(dateplustext.child(0).text());
					try{
						Date.valueOf(actualDate);
					}catch(Exception ex){
						ex.printStackTrace();
						System.err.println("ERROR: date format is wrong!!!! date = " + actualDate);
						continue;
					}
				}
				
				if(dateplustext.tagName().equals("ul")){
				
					// Complete news under a given date
	                Elements stories = dateplustext.children();  // This contains different stories (newsStory may be present or not)
	                for (Element li : stories){
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
	protected ArrayList<String> parseMonthPageForDatesOnly(Document doc) {
		ArrayList<String> dates = new ArrayList<String>();
		Elements days = doc.select("div#content.mw-body div#bodyContent div#mw-content-text.mw-content-ltr");
		for (Element eachday : days) { // This will loop only once because it is the whole text
			Elements individual = eachday.children();
			String actualDate = null;
			for (Element dateplustext : individual) { // This consists of alternate date and events (with or withour newsStory)
				if (dateplustext.tagName().equals("h3")) { // Upto December 2004
					actualDate = getDate(dateplustext.child(0).text());
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
	
	
	@Override
	protected boolean isValidDate() {
		Date startDate   = Date.valueOf("2000-09-30");
		Date endDate   = Date.valueOf("2005-01-01");
		Date parsingDate = Date.valueOf(getYear()+"-"+getMonth()+"-"+"01");
		if(parsingDate.after(startDate) && parsingDate.before(endDate)) return true;
		else return false;
	}

}
