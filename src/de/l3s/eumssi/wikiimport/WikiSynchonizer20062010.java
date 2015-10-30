package de.l3s.eumssi.wikiimport;
///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package de.l3s.eumssi.wikiimport;
//
//import java.io.File;
//import java.io.IOException;
//import java.sql.Connection;
//import java.util.AbstractMap;
//import java.util.ArrayList;
//import java.util.Map;
//
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//import org.jsoup.nodes.Node;
//import org.jsoup.select.Elements;
//
///**
// * This file is used to crawl the wikinews from May 2006 to May 2010.
// * @author kanik
// */
//public class WikiSynchonizer20062010 {
//    // These are some of the variables to automate the process of extraction of webpages of different months and years.
//    private static int monthoffset = 0;
//    private static int yearoffset = 0;
//    private static boolean check = false;
//    private static boolean cleaningDuplicationMode = false;
//    
//    public static void main(String[] args) throws IOException {
//        // This is a variable to establish the connection with the database.
//        ContentHandling astra = new ContentHandling();
//        
//        for(int i=5;i<=53;i++){
//            if(check){                                // Switching to next year
//                monthoffset += 12;
//                yearoffset +=1;
//                check = false;
//            }
//            /*If we have to parse a html file
//            File input = new File("/home/kanik/pr.html");
//            Document doc = Jsoup.parse(input, "UTF-8", "");*/
//            ArrayList<Map.Entry<String, String>> newEvents = new ArrayList<Map.Entry<String, String>>();
//            // URL of the webpage to parse
//            String url = "http://en.wikipedia.org/wiki/"+returnMonth(i-monthoffset)+"_"+(2006+yearoffset);
//            if(i%12 == 0)
//            {
//                check = true;
//            }
//            
//            
//            String smonth = i-monthoffset <10? "0" + String.valueOf(i-monthoffset):String.valueOf(i-monthoffset);
//            String syear = String.valueOf(2006+ yearoffset);
//            
//            if (cleaningDuplicationMode) {
//            	System.out.println(i + "\t" + url);
//            	astra.removeDuplication(syear+ "-" + smonth);
//            	continue;
//            }
//            
//            
//            // Fetching the webpage
//            print("Fetching %s...", url);
//            Document doc = Jsoup.connect(url).get();
//            Elements days = doc.select("div#bodyContent div#mw-content-text.mw-content-ltr table tbody tr td table.vevent");
//            // This part is added to automate the process of dates for January 2009 because de.l3s.eumssi.wikiimport has weird structure for it
//            int da = 1;
//            // The above part ends here
//            for(Element eachday : days){
//            	//Important Variables
//                String categoryValue = null;
//                String newsstory ="";
//                String description = "";
//                String actualDate = "";
//                print("*****************************************************\n");
//                Elements date = eachday.select("tbody tr td table.plainlinks tbody tr td span.summary");
//                String modifiedDate = date.text();
//                if((2006+yearoffset)==2009 && (i==37 || i==48)){
//                    if(da<=9)
//                        if(i==37)
//                            actualDate = "2009-01-0"+da;
//                        if(i==48)
//                            actualDate = "2009-12-0"+da;
//                    else
//                        if(i==37)
//                            actualDate = "2009-01-"+da;
//                        if(i==48)
//                            actualDate = "2009-12-"+da;
//                    da++;
//                }
//                else{
//                    if(modifiedDate.toLowerCase().contains("(".toLowerCase())){
//                        int firstoccur = modifiedDate.indexOf("(");
//                        actualDate = modifiedDate.substring(firstoccur+1, firstoccur+11);
//                    }
//                }
//                print("______DATE : %s\n",actualDate);
//                Elements categories = eachday.select("tbody tr td.description");
//                print("SIZE: %d",categories.size());
//                for(Element category : categories){    // This will loop just once since size is 1
//                    for(Element e : category.children()){
//                        /*if (e.tagName().equals("dl")) {
//                            print("_________CATEGORY : %s",e.text());  // This will give me the category 
//                            astra.insertIntoCategory(e.text(), 0);  // Populating the table Category
//                            categoryValue = e.text();
//                        }*/
//                        //else {
//                            if (e.tagName().equals("ul")) {
//                                Elements stories = e.children();  // This contains different stories including headings of story
//                                for (Element li : stories){
//                                    // We are creating an ArrayList to store the URl of the source so that sourceids can be stored into the WKEvent Table
//                                    ArrayList<String> storeurl = new ArrayList<String>();
//                                    
//                                    // We are creating an ArrayList to store the wikiref to populate the table EventEntityRelation
//                                    ArrayList<String> wikiurls = new ArrayList<String>();
//                                    
//                                    Elements uls = li.children();
//                                    boolean hasUL = false;    // If li has ul then it implies that it contains a news story
//                                    for (Element ul: uls) {
//                                        if (ul.tagName().equals("ul")) {
//                                            hasUL = true; // news story is there
//                                            Node st = li.childNode(0);
//                                            //System.out.println("NewsStory: "+st.attr("title"));  // Printing the newsStory
//                                            newsstory = st.attr("title");
//                                            //System.out.println("URL for NewsStory :"+st.attr("href"));
//                                            int wikiid = astra.insertIntoWikiRef(st.attr("title"), st.attr("href"));   // WKEvent the newsStory has to be populated into WikiRef
//                                            astra.insertIntoNewsStory(st.attr("title"),0,"f",wikiid);  // Populating the newsStory table, right now parentid is assumed to be 0 and ongoing to be false.
//                                            astra.insertIntoStoryCategoryRelation(st.attr("title"), categoryValue);  // Populating the StoryCategoryRelation table
//                                            /*description = ul.text();
//                                            System.out.println("WKEvent:" + description);  // Gives the event*/ // This block has been commented out by me
//                                            Elements innerli = ul.children();       //Now we get inside the li element
//                                            for(Element inside : innerli){
//                                                description = inside.text();  // This part is newly added
//                                                System.out.println("NewsStory: "+st.attr("title"));  // Printing the newsStory
//                                                System.out.println("URL for NewsStory :"+st.attr("href"));
//                                                System.out.println("WKEvent:" + description);   // Even this is newly added
//                                                Elements source = inside.children();  // This gives all the a tags
//                                                for(Element name : source){
//                                                        if(name.hasAttr("rel")){   // This implies it is a news source
//                                                           int length = name.text().length();
//                                                           if(length>2){
//                                                                print("_______Source : %s",name.text().substring(1, length-1));
//                                                                print("_______SourceURL : %s",name.attr("href"));
//                                                                storeurl.add(name.attr("href"));  // To store the urls for WKEvent table
//                                                                astra.insertIntoSource(name.text().substring(1, length-1),name.attr("href"),"1993-01-01", "Not Published"); // Populating the source table, right now published date and content are taken some default values as it is quite clear
//                                                               
//                                                                // This part is added to remove the news source from event
//                                                                description = description.replace(name.text(), "");      
//                                                           }
//                                                           }
//                                                        else{                        // This implies it is just a link
//                                                            print("_____For WikiRef Name : %s",name.text());
//                                                            print("_____For WikiRef URL : %s",name.attr("href"));
//                                                            wikiurls.add(name.attr("href"));  // To store the WikiRef Name for EventEntityRelation table
//                                                            astra.insertIntoWikiRef(name.text(), name.attr("href")); //Populating the table Wikiref
//                                                        }
//
//                                                }
//                                                //This is extra added line.
//                                                newEvents.add( new AbstractMap.SimpleEntry<String, String> (description.trim(), actualDate));
//                                                System.out.println("Edited WKEvent:"+description);
//                                                int eventid = astra.insertIntoEvent(description, newsstory, categoryValue, java.sql.Date.valueOf(actualDate), storeurl,wikiid);  //Populating the WKEvent table
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
//                                        System.out.println("WKEvent:" + li.text());  // If it doesn't has ul => no newsStory so we just print WKEvent
//                                        description = li.text();
//                                        newsstory = null;
//                                        Elements sources = li.children();     // This gives all the a tags
//                                                for(Element names : sources){
//                                                        if(names.hasAttr("rel")){    // This implies it is a news source
//                                                           int length = names.text().length();
//                                                           if(length>2){
//                                                                print("_______Source : %s",names.text().substring(1, length-1));
//                                                                print("_______SourceURL : %s",names.attr("href"));
//                                                                storeurl.add(names.attr("href"));  // To store the Urls for WKEvent table
//                                                                astra.insertIntoSource(names.text().substring(1, length-1),names.attr("href"),"1993-01-01", "Not Published");
//                                                                
//                                                                // This part is added to remove the news source from event
//                                                                description = description.replace(names.text(), "");
//                                                           }
//                                                           }
//                                                        else{                       // This implies it is just a link
//                                                            print("_____For WikiRef Name : %s",names.text());
//                                                            print("_____For WikiRef URL : %s",names.attr("href"));
//                                                            wikiurls.add(names.attr("href"));  // To store the WikiRef Name for EventEntityRelation table
//                                                            astra.insertIntoWikiRef(names.text(), names.attr("href"));  // Populating the Wikiref table
//                                                        }
//
//                                                }
//                                                newEvents.add( new AbstractMap.SimpleEntry<String, String> (description.trim(), actualDate));
//                                                System.out.println("Edited WKEvent:"+description);
//                                                int eventid = astra.insertIntoEvent(description, newsstory, categoryValue, java.sql.Date.valueOf(actualDate), storeurl,0);
//                                                storeurl.clear();
//                                                astra.insertIntoEventEntityRelation(eventid, wikiurls);
//                                                wikiurls.clear();
//                                    }        
//                                }
//
//                            }
//                       // }
//                    }
//                }
//            }
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
