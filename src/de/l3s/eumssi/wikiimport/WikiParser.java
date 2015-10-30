/*
 * This program is to parse the content of Wikipedia Current WKEvent portals
 * It is developed from Kanik program de.l3s.eumssi.wikiimport.java, which handles the wiki current portal page from June 2010
 * contact
 * giang binh tran
 * gtran@l3s.de
 */
package de.l3s.eumssi.wikiimport;
import java.io.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.sql.*;
import java.io.IOException;
import java.util.*;

import org.jsoup.nodes.Node;

import java.util.Date;

/**
 * This file is used to crawl the wikinews from June 2010 to ongoing.
 * @author SUDHIR SAH
 */


public class WikiParser {
   
    private static boolean debug = true;
    
    
        
    public void parseing(String url, String curdate) throws IOException, InterruptedException {
    	
        CategoryRecognizer CR = new CategoryRecognizer();
        
        // This is a variable to establish the connection with the database.
        ContentHandling astra = new ContentHandling();
            
        if (debug) {
        	System.out.println(url);
        }
        
        //Important Variables
        // Fetching the webpage
        print("Fetching %s...", url);
        Document doc = Jsoup.connect(url).get();
        Elements days = doc.select("div#bodyContent div#mw-content-text.mw-content-ltr table tbody tr td table.vevent");
        for(Element eachday : days){
        	 String newsstory ="";
             String description = "";
        	String actualDate = "";
        	String categoryValue = "";
            print("*****************************************************\n");
            Elements date = eachday.select("tbody tr td table.plainlinks tbody tr td span.summary");
            String modifiedDate = date.text();
            if(modifiedDate.toLowerCase().contains("(".toLowerCase())){
                int firstoccur = modifiedDate.indexOf("(");
                actualDate = modifiedDate.substring(firstoccur+1, firstoccur+11);
            }
            print("______DATE : %s\n",actualDate);
            
            if (actualDate.compareTo(curdate) != 0) 
            	continue;
            
            print("-----Current date: %s\n",curdate);
            
            
            Elements categories = eachday.select("tbody tr td.description");
            print("SIZE: %d",categories.size());
            if (actualDate.contains("2010-06-12")) break;
            for(Element category : categories){    // This will loop just once since size is 1
                for(Element e : category.children()){
                    if (e.tagName().equals("dl") || e.tagName().equals("p")) {
                        categoryValue = e.text();
                        if((actualDate.equals("2011-05-27") && categoryValue.contains("Maddy"))|| (actualDate.equals("2011-01-19") && categoryValue.contains("Joshua")) || (actualDate.equals("2011-10-01") && categoryValue.contains("Sirte")) || (actualDate.equals("2012-01-03") && categoryValue.contains("RadioFreeEurope")) || (actualDate.equals("2011-04-28") && categoryValue.contains("Constellation")) || (actualDate.equals("2011-07-15") && categoryValue.contains("Potter")) || (actualDate.equals("2011-02-27") && categoryValue.contains("Arab")) || (actualDate.equals("2010-07-12") && categoryValue.contains("Horizon"))){
                            categoryValue = "";
                            continue;
                        }
                        
                        // This is how we are doing the stemming part of categories
                        categoryValue = CR.getCategoryName(e.text());
                        
                        if(actualDate.equals("2013-02-27") && categoryValue.contains("enviroment")){
                            categoryValue.replace("enviroment", "environment");
                        }
                        print("_________CATEGORY : %s",categoryValue);  // This will give me the category
                        astra.insertIntoCategory(categoryValue, 0);  // Populating the table Category
                    }
                    else {
                        if (e.tagName().equals("ul")) {
                            Elements stories = e.children();  // This contains different stories including headings of story
                            for (Element li : stories){
                                // We are creating an ArrayList to store the URl of the source so that sourceids can be stored into the WKEvent Table
                                ArrayList<String> storeurl = new ArrayList<String>();
                                
                                // We are creating an ArrayList to store the wikiref to populate the table EventEntityRelation
                                ArrayList<String> wikiurls = new ArrayList<String>();
                                
                                Elements uls = li.children();
                                boolean hasUL = false;    // If li has ul then it implies that it contains a news story
                                for (Element ul: uls) {
                                    if (ul.tagName().equals("ul")) {
                                        hasUL = true; // news story is there
                                        Node st = li.childNode(0);
                                        newsstory = st.attr("title");
                                        int wikiid = astra.insertIntoWikiRef(st.attr("title"), st.attr("href"));   // WKEvent the newsStory has to be populated into WikiRef
                                        astra.insertIntoNewsStory(st.attr("title"),0,"f",wikiid);  // Populating the newsStory table, right now parentid is assumed to be 0 and ongoing to be false.
                                        astra.insertIntoStoryCategoryRelation(st.attr("title"), categoryValue);  // Populating the StoryCategoryRelation table
                                        
                                        Elements innerli = ul.children();       //Now we get inside the li element
                                        for(Element inside : innerli){    // Pick up one li
                                            description = inside.text();  // This part is newly added
                                            System.out.println("NewsStory: "+st.attr("title"));  // Printing the newsStory
                                            System.out.println("URL for NewsStory :"+st.attr("href"));
                                            System.out.println("WKEvent:" + description);   // Even this is newly added
                                            Elements source = inside.children();  // This gives all the a tags
                                            for(Element name : source){
                                                if(name.hasAttr("rel")){   // This implies it is a news source
                                                   int length = name.text().length();
                                                   if(length>2){
                                                        print("_______Source : %s",name.text().substring(1, length-1));
                                                        print("_______SourceURL : %s",name.attr("href"));
                                                        storeurl.add(name.attr("href"));  // To store the urls for WKEvent table
                                                       
                                                        astra.insertIntoSource(name.text().substring(1, length-1),name.attr("href"),"1993-01-01", "Not Published");
                                                        // Populating the source table, right now published date and content are taken some default values as it is quite clear
                                                        
                                                        // This part is added to remove the news source from event
                                                        description = description.replace(name.text(), "");  
                                                   }
                                                }
                                                else{                        // This implies it is just a link
                                                    print("_____For WikiRef Name : %s",name.text());
                                                    print("_____For WikiRef URL : %s",name.attr("href"));
                                                    wikiurls.add(name.attr("href"));  // To store the WikiRef Name for EventEntityRelation table
                                                    astra.insertIntoWikiRef(name.text(), name.attr("href")); //Populating the table Wikiref
                                                }
                                            }
                                            //This is extra added line.
                                            
                                            int eventid = astra.insertIntoEvent(description, newsstory, categoryValue, java.sql.Date.valueOf(actualDate), storeurl, wikiid);  //Populating the WKEvent table
                                            storeurl.clear();
                                            astra.insertIntoEventEntityRelation(eventid, wikiurls);
                                            wikiurls.clear();
                                        }
                                    } 
                                }
                                if (hasUL) {
                                    /*Node st = li.childNode(0);
                                    System.out.println("NewsStory: "+st.attr("title"));  // Printing the newsStory
                                    newsstory = st.attr("title");
                                    System.out.println("URL for NewsStory :"+st.attr("href"));
                                    //int wikiid = astra.insertIntoWikiRef(st.attr("title"), st.attr("href"));   // WKEvent the newsStory has to be populated into WikiRef
                                    //astra.insertIntoNewsStory(st.attr("title"),0,"f",wikiid);  // Populating the newsStory table, right now parentid is assumed to be 0 and ongoing to be false.
                                    //astra.insertIntoStoryCategoryRelation(st.attr("title"), categoryValue);  // Populating the StoryCategoryRelation table
                                    //int eventid = astra.insertIntoEvent(description, newsstory, categoryValue, actualDate, storeurl);  //Populating the WKEvent table
                                    storeurl.clear();
                                    //astra.insertIntoEventEntityRelation(eventid, wikiname);
                                    wikiname.clear();*/
                                }
                                else {// event
                                    System.out.println("WKEvent:" + li.text());  // If it doesn't has ul => no newsStory so we just print WKEvent
                                    description = li.text();
                                    newsstory = null;
                                    Elements sources = li.children();     // This gives all the a tags
                                    for(Element names : sources){
                                        if(names.hasAttr("rel")){    // This implies it is a news source
                                           int length = names.text().length();
                                           if(length>2){
                                                print("_______Source : %s",names.text().substring(1, length-1));
                                                print("_______SourceURL : %s",names.attr("href"));
                                                storeurl.add(names.attr("href"));  // To store the Urls for WKEvent table
                                                astra.insertIntoSource(names.text().substring(1, length-1),names.attr("href"),"1993-01-01", "Not Published");
                                                
                                                // This part is added to remove the news source from event
                                                description = description.replace(names.text(), "");   // This part is added to remove the names of news sources from the event
                                           }
                                           }
                                        else{                       // This implies it is just a link
                                            print("_____For WikiRef Name : %s",names.text());
                                            print("_____For WikiRef URL : %s",names.attr("href"));
                                            wikiurls.add(names.attr("href"));  // To store the WikiRef Name for EventEntityRelation table
                                            astra.insertIntoWikiRef(names.text(), names.attr("href"));  // Populating the Wikiref table
                                        }

                                    }
                                    System.out.println("Edited WKEvent:"+description);
                                    int eventid = astra.insertIntoEvent(description, newsstory, categoryValue, java.sql.Date.valueOf(actualDate), storeurl,0);
                                    storeurl.clear();
                                    astra.insertIntoEventEntityRelation(eventid, wikiurls);
                                    wikiurls.clear();
                                }        
                            }

                        }
                    }
                }
            }
        }
        astra.close();
    }
    
    
       
    private static void print(String msg, Object... args) {
        System.out.println(String.format(msg, args));
    }

         
    public static void main(String[] args) throws IOException, InterruptedException {
    	
    	//Reading configuration file
		Properties prop = new Properties();
		FileInputStream input = new FileInputStream("./configs/wikitimes.properties");
		prop.load(input);
		int startingTime = Integer.parseInt(prop.getProperty("startingTime"));
		System.out.println("Will start at : "+startingTime+ " o'clock");
		startingTime = (startingTime * 60); // converting in min
		int interval = Integer.parseInt(prop.getProperty("interval"));
		System.out.println("Interval is : "+interval+ " Hours");
		int interval_ms = interval * 60 * 60 * 1000;
		
	    
			ContentHandling ch = new ContentHandling();
			WikiParser wp = new WikiParser();
			Date dt = new Date();
			String str1 = dt.toString();
			String[] str2 = str1.split(" ");
			String[] str3 = str2[3].split(":");
			int curHour = Integer.parseInt(str3[0]);
			int curMin = Integer.parseInt(str3[1]);
			int curTime = (curHour * 60 + curMin ); // in minute
			int sleepingTime = 0;
			
			if(startingTime > curTime){
				 
				sleepingTime = (startingTime - curTime); // in min
				System.out.println("Sleeping for : " +sleepingTime+ " Minutes from now");
				sleepingTime = (sleepingTime * 60 *1000); // in ms
				Thread.sleep(sleepingTime);
			}
			
			else if(startingTime < curTime){
				
				sleepingTime = (24 * 60 - curTime) + startingTime ;
				System.out.println("Sleeping for: " +sleepingTime+" Minutes from now");
				sleepingTime = (sleepingTime * 60 * 1000); // in ms
				Thread.sleep(sleepingTime);
			}
			else{
				
				System.out.println("StartingTime and currentTime are equal");
			}
			
		
    	for(;;){
    	    		
            // Take current system time,extract month and year, and prepare final url to crowl..
    		
    		Date date=new Date(); //current system date
    		String strdate = date.toString();
    		String[] splitdate = strdate.split(" ");
    		String[] curmonth = ch.ret_month_name_num(splitdate[1]);
    		String curyear = splitdate[5];
    		String month_ = curmonth[0].concat("_");
    		String month_year = month_.concat(curyear);
    		String url_part = "http://en.wikipedia.org/wiki/";
    		String url = url_part.concat(month_year); //final url to fatch from
    		
    		//computing current date in the format : year.mm.dd
    		String a = curyear.concat("-"); //output: yyyy-
    		String b = a.concat(curmonth[1]); // output: yyyy-mm
    		String c = b.concat("-"); //output: yyyy-mm-
    		String curdate = c.concat(splitdate[2]); //output: yyyy-mm-dd
    	
    		wp.parseing(url,curdate); 
    		
    		print("sleeping for %d Millis", interval_ms);
    		
    		Thread.sleep(interval_ms);
    	}
       
    }
    			
  } 

    


