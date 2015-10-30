package de.l3s.eumssi.wikiimport;
///*
// *Synchonizign the data to Wikipedia Current portal
// */
//package de.l3s.eumssi.wikiimport;
//import java.io.*;
//import org.jsoup.Jsoup;
//import org.jsoup.helper.Validate;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;
//import java.sql.*;
//import java.io.IOException;
//import java.util.*;
//import org.jsoup.nodes.Node;
//
///**
// * This file is used to syn the wikinews from June 2010 to ongoing.
// * @author gtran
// */
//public class WikiSynchonizer {
//    // These are some of the variables to automate the process of extraction of webpages of different months and years.
//    private static int monthoffset = 0;
//    private static int yearoffset = 0;
//    private static boolean check = false;
//    private static boolean cleaningDuplicationMode = false;
//    
//    public static void main(String[] args) throws IOException {
//        
//        // Below variables are used to get the current month and year.
//        int currentyear = 2013;
//        int currentmonth = 11;
//        
//        int limit = 12 + ((currentyear-2011)*12)+currentmonth;
//        
//        CategoryRecognizer CR = new CategoryRecognizer();
//        
//        // This is a variable to establish the connection with the database.
//        ContentHandling astra = new ContentHandling();
//        
//        for(int i=6;i<=limit;i++){
//            if(check){                          // Switching to next year
//                monthoffset += 12;
//                yearoffset +=1;
//                check = false;
//            }
//            ArrayList<Map.Entry<String, String>> newEvents = new ArrayList<Map.Entry<String, String>>();
//            // URL of the webpage to parse
//            String url = "http://en.wikipedia.org/wiki/"+returnMonth(i-monthoffset)+"_"+(2010+yearoffset);
//            String smonth = i-monthoffset <10? "0" + String.valueOf(i-monthoffset):String.valueOf(i-monthoffset);
//            String syear = String.valueOf(2010+ yearoffset);
//            
//            if(i%12 == 0)
//            {
//                check = true;
//            }
//            if (cleaningDuplicationMode) {
//            	System.out.println(i + "\t" + url);
//            	astra.removeDuplication(syear+ "-" + smonth);
//            	continue;
//            }
//            
//            //Important Variables
//            
//            
//            // Fetching the webpage
//            print("Fetching %s...", url);
//            Document doc = Jsoup.connect(url).get();
//            Elements days = doc.select("div#bodyContent div#mw-content-text.mw-content-ltr table tbody tr td table.vevent");
//            for(Element eachday : days){
//            	String categoryValue = "";
//                String newsstory ="";
//                String description = "";
//                String actualDate = "";
//                print("*****************************************************\n");
//                Elements date = eachday.select("tbody tr td table.plainlinks tbody tr td span.summary");
//                String modifiedDate = date.text();
//                if(modifiedDate.toLowerCase().contains("(".toLowerCase())){
//                    int firstoccur = modifiedDate.indexOf("(");
//                    actualDate = modifiedDate.substring(firstoccur+1, firstoccur+11);
//                }
//                System.out.println(actualDate);
//                Elements categories = eachday.select("tbody tr td.description");
//                for(Element category : categories){    // This will loop just once since size is 1
//                    for(Element e : category.children()){
//                        if (e.tagName().equals("dl") || e.tagName().equals("p")) {
//                            categoryValue = e.text();
//                            if((actualDate.equals("2011-05-27") && categoryValue.contains("Maddy"))|| (actualDate.equals("2011-01-19") && categoryValue.contains("Joshua")) || (actualDate.equals("2011-10-01") && categoryValue.contains("Sirte")) || (actualDate.equals("2012-01-03") && categoryValue.contains("RadioFreeEurope")) || (actualDate.equals("2011-04-28") && categoryValue.contains("Constellation")) || (actualDate.equals("2011-07-15") && categoryValue.contains("Potter")) || (actualDate.equals("2011-02-27") && categoryValue.contains("Arab")) || (actualDate.equals("2010-07-12") && categoryValue.contains("Horizon"))){
//                                categoryValue = "";
//                                continue;
//                            }
//                            
//                            // This is how we are doing the stemming part of categories
//                            categoryValue = CR.getCategoryName(e.text());
//                            
//                            if(actualDate.equals("2013-02-27") && categoryValue.contains("enviroment")){
//                                categoryValue.replace("enviroment", "environment");
//                            }
//                            astra.insertIntoCategory(categoryValue, 0);  // Populating the table Category
//                        }
//                        else {
//                            if (e.tagName().equals("ul")) {
//                                Elements stories = e.children();  // This contains different stories including headings of story
//                                for (Element li : stories){
//                                    // We are creating an ArrayList to store the URl of the source so that sourceids can be stored into the WKEvent Table
//                                    ArrayList<String> storeurl = new ArrayList<String>();
//                                    
//                                    // We are creating an ArrayList to store the wikiref urls to populate the table EventEntityRelation
//                                    ArrayList<String> wikiurls = new ArrayList<String>();
//                                    
//                                    Elements uls = li.children();
//                                    boolean hasUL = false;    // If li has ul then it implies that it contains a news story
//                                    for (Element ul: uls) {
//                                        if (ul.tagName().equals("ul")) {
//                                            hasUL = true; // news story is there
//                                            Node st = li.childNode(0);
//                                            newsstory = st.attr("title");
//                                            int wikiid = astra.insertIntoWikiRef(st.attr("title"), st.attr("href"));   // WKEvent the newsStory has to be populated into WikiRef
//                                            astra.insertIntoNewsStory(st.attr("title"),0,"f",wikiid);  // Populating the newsStory table, right now parentid is assumed to be 0 and ongoing to be false.
//                                            astra.insertIntoStoryCategoryRelation(st.attr("title"), categoryValue);  // Populating the StoryCategoryRelation table
//
//                                            Elements innerli = ul.children();       //Now we get inside the li element
//                                            for(Element inside : innerli){    // Pick up one li
//                                                description = inside.text();  // This part is newly added
//                                               
//                                                Elements source = inside.children();  // This gives all the a tags
//                                                for(Element name : source){
//                                                        if(name.hasAttr("rel")){   // This implies it is a news source
//                                                           int length = name.text().length();
//                                                           if(length>2){
//                                                                
//                                                                storeurl.add(name.attr("href"));  // To store the urls for WKEvent table
//                                                                
//                                                                astra.insertIntoSource(name.text().substring(1, length-1),name.attr("href"),"1993-01-01", "Not Published"); // Populating the source table, right now published date and content are taken some default values as it is quite clear
//                                                                
//                                                                // This part is added to remove the news source from event
//                                                                description = description.replace(name.text(), "");  
//                                                           }
//                                                        }
//                                                        else{                        // This implies it is just a link
//                                                            astra.insertIntoWikiRef(name.text(), name.attr("href")); //Populating the table Wikiref
//                                                            wikiurls.add(name.attr("href"));  // To store the WikiRef Name for EventEntityRelation table
//                                                        }
//                                                }
//                                                //This is extra added line.
//                                                newEvents.add( new AbstractMap.SimpleEntry<String, String> (description.trim(), actualDate));
//                                                int eventid = astra.insertIntoEvent(description, newsstory, categoryValue, java.sql.Date.valueOf(actualDate), storeurl, wikiid);  //Populating the WKEvent table
//                                                storeurl.clear();
//                                                astra.insertIntoEventEntityRelation(eventid, wikiurls);
//                                                wikiurls.clear();
//                                            }
//                                            
//                                        } 
//                                    }
//                                    if (hasUL) {
//                                        /*Node st = li.childNode(0);
//                                        System.out.println("NewsStory: "+st.attr("title"));  // Printing the newsStory
//                                        newsstory = st.attr("title");
//                                        System.out.println("URL for NewsStory :"+st.attr("href"));
//                                        //int wikiid = astra.insertIntoWikiRef(st.attr("title"), st.attr("href"));   // WKEvent the newsStory has to be populated into WikiRef
//                                        //astra.insertIntoNewsStory(st.attr("title"),0,"f",wikiid);  // Populating the newsStory table, right now parentid is assumed to be 0 and ongoing to be false.
//                                        //astra.insertIntoStoryCategoryRelation(st.attr("title"), categoryValue);  // Populating the StoryCategoryRelation table
//                                        //int eventid = astra.insertIntoEvent(description, newsstory, categoryValue, actualDate, storeurl);  //Populating the WKEvent table
//                                        storeurl.clear();
//                                        //astra.insertIntoEventEntityRelation(eventid, wikiname);
//                                        wikiname.clear();*/
//                                    }
//                                    else {// event
//                                        description = li.text();
//                                        newsstory = null;
//                                        Elements sources = li.children();     // This gives all the a tags
//                                        for(Element names : sources){
//                                            if(names.hasAttr("rel")){    // This implies it is a news source
//                                               int length = names.text().length();
//                                               if(length>2){
//                                                    storeurl.add(names.attr("href"));  // To store the Urls for WKEvent table
//                                                    astra.insertIntoSource(names.text().substring(1, length-1),names.attr("href"),"1993-01-01", "Not Published");
//                                                    
//                                                    // This part is added to remove the news source from event
//                                                    description = description.replace(names.text(), "");   // This part is added to remove the names of news sources from the event
//                                               	}
//                                              }
//                                              else{                       // This implies it is just a link
//                                                    astra.insertIntoWikiRef(names.text(), names.attr("href"));  // Populating the Wikiref table
//                                                    wikiurls.add(names.attr("href"));  // To store the WikiRef URL for EventEntityRelation table
//                                              }
//                                        }
//                                        newEvents.add( new AbstractMap.SimpleEntry<String, String> (description.trim(), actualDate));
//                                        int eventid = astra.insertIntoEvent(description, newsstory, categoryValue, java.sql.Date.valueOf(actualDate), storeurl,0);
//                                        storeurl.clear();
//                                        astra.insertIntoEventEntityRelation(eventid, wikiurls);
//                                        wikiurls.clear();
//                                    }        
//                                }
//
//                            }
//                        }
//                    }
//                }
//            }
//            
//            //end of a month
//            //check if an event of this month does not exist anymore in Wikipedia Current portal (newsEvents)
//            //simply remove it from DB
////            astra.syncEvents(newEvents, syear+"-" + smonth);
//        }
//    }
//    
//    private static String returnMonth(int value){
//        String month = "";
//        switch(value){
//            case 1: month = "January"; break;
//            case 2: month = "February"; break;
//            case 3: month = "March"; break;
//            case 4: month = "April"; break;
//            case 5: month = "May"; break;
//            case 6: month = "June"; break;
//            case 7: month = "July"; break;
//            case 8: month = "August"; break;
//            case 9: month = "September"; break;
//            case 10: month = "October"; break;
//            case 11: month = "November"; break;
//            case 12: month = "December"; break;
//        }
//        return month;
//    }
//    
//    
//    
//    private static void print(String msg, Object... args) {
//        System.out.println(String.format(msg, args));
//    }
//
//    private static String trim(String s, int width) {
//        if (s.length() > width)
//            return s.substring(0, width-1) + ".";
//        else
//            return s;
//    }
//}
