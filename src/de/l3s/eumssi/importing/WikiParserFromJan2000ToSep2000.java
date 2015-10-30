package de.l3s.eumssi.importing;

import java.sql.Date;
import java.util.ArrayList;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.l3s.eumssi.model.*;

/**
 * This file is used to extract the events from January 2000 to September 2000
 */
public class WikiParserFromJan2000ToSep2000 extends WikiParser{
	
	
	protected ArrayList<Event> parseMonthPage(Document doc) {
		
		ArrayList<Event> events = new ArrayList<Event>();

		Elements days = doc.select("div#bodyContent div#mw-content-text.mw-content-ltr table tbody tr td table.vevent");
        for(Element eachday : days){
            Elements date = eachday.select("tbody tr td table.plainlinks tbody tr td span.summary");
            String modifiedDate = date.text();
            String actualDate = null;
            if(modifiedDate.toLowerCase().contains("(".toLowerCase())){
                int firstoccur = modifiedDate.indexOf("(");
                actualDate = modifiedDate.substring(firstoccur+1, firstoccur+11);
                try{
					Date.valueOf(actualDate);
				}catch(Exception ex){
					ex.printStackTrace();
					System.err.println("ERROR: date format is wrong!!!! date = " + actualDate);
					continue;
				}
            }
            Elements categories = eachday.select("tbody tr td.description");
            for(Element category : categories){    // This will loop just once since size is 1
            	for(Element e : category.children()){
            		if (e.tagName().equals("ul")) {   // In fact ul is the only child of this big td.description
            			Elements eventElements = e.children();  // This corresponds to li's that means different events under a particular date.
                        for (Element eventNode : eventElements){
                            Event event = extractDescriptionAndLinks(eventNode);
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
		Elements days = doc.select("div#bodyContent div#mw-content-text.mw-content-ltr table tbody tr td table.vevent");
        for(Element eachday : days){
            Elements date = eachday.select("tbody tr td table.plainlinks tbody tr td span.summary");
            String modifiedDate = date.text();
            String actualDate = null;
            if(modifiedDate.toLowerCase().contains("(".toLowerCase())){
                int firstoccur = modifiedDate.indexOf("(");
                actualDate = modifiedDate.substring(firstoccur+1, firstoccur+11);
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

		return dates;
	}
	
	
	@Override
	protected boolean isValidDate() {
		Date startDate   = Date.valueOf("1999-12-31");
		Date endDate   = Date.valueOf("2000-10-01");
		Date parsingDate = Date.valueOf(getYear()+"-"+getMonth()+"-"+"01");
		if(parsingDate.after(startDate) && parsingDate.before(endDate)) return true;
		else return false;
	}

}
