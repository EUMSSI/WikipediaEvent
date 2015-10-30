package de.l3s.eumssi.wikiimport;
import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import de.l3s.eumssi.model.*;

public class WikiPageParser {
	
	private ArrayList<Event> eventObjArray = new ArrayList<Event>();
	
		String currentURL = "";
		Document currentDocument = null;
		CategoryRecognizer CR = null;
		
		public WikiPageParser(){
			CR = new CategoryRecognizer();
		}
	
	  /* This method formulate event objects from WikiPedia current event portal and returns list of all the event_objects on the date passed as a parameter
	   * 
	   * @param : date [format : yyyy-mm-dd]
	   * 
	   * @Returns : all the events [object] on this date
	   * 
	   */
	public ArrayList<Event> parseing(String curdate, boolean downlaod, String downloadFolder) throws IOException, InterruptedException {
		  
			if(eventObjArray.size()!=0)
				eventObjArray.clear();
		
		  //Build URL
		  String cdate = curdate;
		  String[] sdate = cdate.split("-");
		   if(downlaod){
			   String url = "http://en.wikipedia.org/wiki/"+returnMonth(Integer.parseInt(sdate[1]))+"_"+sdate[0];
			   // Fetching the web page
			   if (!url.equals(currentURL)){
				   System.out.println(" fetching page: " + url);
				   currentURL = url;
				   currentDocument = Jsoup.connect(url).get();
			   }			   
		   }else{
			   String fileName = downloadFolder + returnMonth(Integer.parseInt(sdate[1]))+"_"+sdate[0]+".html";
			   System.out.println(" loading events from fetched page " + fileName  + " ...");
			   currentDocument = Jsoup.parse(new File(fileName), "UTF-8");
		   }
		  
	        Document doc = currentDocument;
	        Elements days = doc.select("div#bodyContent div#mw-content-text.mw-content-ltr table tbody tr td table.vevent");
	        System.out.println(" found " + days.size() + " dates!");
	        for(Element eachday : days){
	        	 
	        	String description = "";
	        	String actualDate = "";
	        	String categoryValue = "";
	           
	            Elements date = eachday.select("tbody tr td table.plainlinks tbody tr td span.summary");
	            String modifiedDate = date.text();
	            if(modifiedDate.toLowerCase().contains("(".toLowerCase())){
	                int firstoccur = modifiedDate.indexOf("(");
	                actualDate = modifiedDate.substring(firstoccur+1, firstoccur+11);
	            }
	        
	            //check if the current date(passed parameter to this method) matches to the date on the WikiPedia current event portal
	            if (actualDate.compareTo(curdate) != 0) 
	            	continue;
	            
	            Elements categories = eachday.select("tbody tr td.description");
	           
	            if (actualDate.contains("2010-06-12")) break;
	           
	            Category cat = null;
                
	            for(Element category : categories){    // This will loop just once since size is 1	            	
	            	for(Element e : category.children()){
	                    if (e.tagName().equals("dl") || e.tagName().equals("p")) {
	                        categoryValue = e.text();

//	                        Mohammad: I commented out the following lines - 29.08.2014
//	                        if((actualDate.equals("2011-05-27") && categoryValue.contains("Maddy"))|| (actualDate.equals("2011-01-19") && categoryValue.contains("Joshua")) || (actualDate.equals("2011-10-01") && categoryValue.contains("Sirte")) || (actualDate.equals("2012-01-03") && categoryValue.contains("RadioFreeEurope")) || (actualDate.equals("2011-04-28") && categoryValue.contains("Constellation")) || (actualDate.equals("2011-07-15") && categoryValue.contains("Potter")) || (actualDate.equals("2011-02-27") && categoryValue.contains("Arab")) || (actualDate.equals("2010-07-12") && categoryValue.contains("Horizon"))){
//	                            categoryValue = "";
//	                            continue;
//	                        }
//	                        
//	                        // This is how we are doing the stemming part of categories
//	                        categoryValue = CR.getCategoryName(e.text());
//	                        
//	                        if(actualDate.equals("2013-02-27") && categoryValue.contains("enviroment")){
//	                            categoryValue.replace("enviroment", "environment");
//	                        }
//	                        categoryValue = e.text();
	                        
	                        cat = new Category();
	                        cat.setName(categoryValue);
	                        
	                    }
	                    else {
	                        if (e.tagName().equals("ul")) {
	                            Elements stories = e.children();  // This contains different stories including headings of story
	                            for (Element li : stories){
	                                
	                            	//for storing all the wkReference name and url, this helps in populating the event object
	                                ArrayList<Reference> wkReferenceArray = new ArrayList<Reference>();
	                               
	                                //for storing all the entities information which helps in populating the event object 
	                                ArrayList<Entity> entityArray = new ArrayList<Entity>();
	                                
	                                Elements uls = li.children();
	                                boolean hasUL = false;    // If li has ul then it implies that it contains a news story
	                                for (Element ul: uls) {
	                                    if (ul.tagName().equals("ul")) {
	                                        hasUL = true; // news story is there
	                                        Node st = li.childNode(0);
	                                        Elements innerli = ul.children();       //Now we get inside the li element
	                                        for(Element inside : innerli){    // Pick up one li
	                                            description = inside.text();  // This part is newly added
	                                           
	                                            Elements wkReference = inside.children();  // This gives all the a tags
//	                                            System.out.println(" num of links = " + wkReference.size());
	                                            for(Element name : wkReference){
//	                                            	System.out.println(name.text());
	                                                if(name.hasAttr("rel")){   // This implies it is a news wkReference
	                                                   int length = name.text().length();
	                                                   if(length>2){
	                                                        //push all the wkReference to wkReferenceArray
	                                                        Reference srcobj = new Reference("", name.attr("href"), name.text().substring(1, length-1));
//	                                                        System.out.println(srcobj.getUrl());
	                                                        wkReferenceArray.add(srcobj);
	                                                        description = description.replace(name.text(), "");  
	                                                   }
	                                                }
	                                                else{  // This implies it is just a link
	                                                  
	                                                    //push all the entities in the entity array
	                                                    Entity entityobj = new Entity();
	                                                    entityobj.setName(name.text());
	                                                    entityobj.setWikiURL(getEntityName(name.attr("href")));
	                                                    entityArray.add(entityobj);  
	                                                }
	                                            }
	                                            //push everything into the event object   
	                                            Event eventObj = new Event();
	                                            eventObj.setDescription(description);
	                                            eventObj.setDate(Date.valueOf(actualDate));
	                                            eventObj.setCategory(cat);
	                                            Story story = new Story();
	                                            story.setName(st.attr("title"));
	                                            story.setWikipediaUrl(getEntityName(st.attr("href")));
	                                            eventObj.setStory(story);
	                                            eventObj.setEntities(entityArray);
	                                            eventObj.setReferences(wkReferenceArray);
	                                            
	                                            eventObjArray.add(eventObj);
	                                            
	                                            wkReferenceArray.clear();
	                                            entityArray.clear();
	                                        }
	                                    } 
	                                }
	                                if (hasUL) 
	                                {
	                                    
	                                }
	                                else {
	                                	// event
	                                    // If it doesn't has ul => no newsStory so we just print WKEvent
	                                    description = li.text();
	                                    Elements wkReferences = li.children();     // This gives all the a tags
	                                    for(Element names : wkReferences){
	                                        if(names.hasAttr("rel")){    // This implies it is a news wkReference
	                                           int length = names.text().length();
	                                           if(length>2){
	                                               
	                                        	   Reference srcobj = new Reference("", names.attr("href"), names.text().substring(1, length-1));        
                                                   wkReferenceArray.add(srcobj);
	                                               description = description.replace(names.text(), "");   // This part is added to remove the names of news wkReferences from the event
	                                           }
	                                           }
	                                        else{                       // This implies it is just a link
	                                           
	                                        	Entity entityobj = new Entity();
                                                entityobj.setName(names.text());
                                                entityobj.setWikiURL(getEntityName(names.attr("href")));
                                                entityArray.add(entityobj);  
	                                        	
	                                        }
	                                    }
	                                    
	                                    Event eventObj = new Event();
                                        eventObj.setDescription(description);
                                        eventObj.setDate(Date.valueOf(actualDate));
                                        eventObj.setCategory(cat);
                                        eventObj.setEntities(entityArray);
                                        eventObj.setReferences(wkReferenceArray);                                         
                                        eventObjArray.add(eventObj);
	                                   
	                                    wkReferenceArray.clear();
	                                    entityArray.clear();
	                                }        
	                            }
	                        }
	                    }
	                }
	            }
	        }
	        return eventObjArray;
	    }
	    

	public String getEntityName(String url){
//        String[] url_parts = url.split("/");
//        return url_parts[url_parts.length-1];
		return url;
	}
	
	
	//This function is used to print the event_objects
	  public void print(ArrayList<Event> e)
	  {
		  for(Event eobj : e)
		  {
			  System.out.println("Event : " +eobj.getDescription());
			  System.out.println("Date : "+eobj.getDate());
			  System.out.println("Category : "+eobj.getCategory().getName());
			  if(eobj.getStory() != null)
				  System.out.println("News Story : "+eobj.getStory().getName()+" -> "+eobj.getStory().getWikipediaUrl());
			  System.out.print("Entities("+eobj.getEntities().size()+") : ");
			  for(Entity entity : eobj.getEntities())
			  {
				  System.out.print(entity.getName()+" -> "+entity.getWikiURL()+"   ");
			  }
			  System.out.print("\nSources("+eobj.getReferences().size()+") : ");
			  for(Reference wkReference : eobj.getReferences())
			  {
				  System.out.print(wkReference.getSource()+" -> "+wkReference.getUrl()+"   ");
			  }
			  System.out.println("\n-------------------------------------------------");
		  }
	  }
	  
	    // This method is used for building URL form date
	    public String returnMonth(int value){
	        String month = "";
	        switch(value){
	            case 1: month = "January"; break;
	            case 2: month = "February"; break;
	            case 3: month = "March"; break;
	            case 4: month = "April"; break;
	            case 5: month = "May"; break;
	            case 6: month = "June"; break;
	            case 7: month = "July"; break;
	            case 8: month = "August"; break;
	            case 9: month = "September"; break;
	            case 10: month = "October"; break;
	            case 11: month = "November"; break;
	            case 12: month = "December"; break;
	        }
	        return month;
	    }
	    
	    
	    public String fetch(String curdate) throws IOException, InterruptedException {
			  
		  //Build URL
		  String cdate = curdate;
		  String[] sdate = cdate.split("-");
		  String url = "http://en.wikipedia.org/wiki/"+returnMonth(Integer.parseInt(sdate[1]))+"_"+sdate[0];
		  // Fetching the web page
		   System.out.println(" fetching page: " + url);
		   Document doc = Jsoup.connect(url).get();
		   return doc.toString();
		}
		  
}
