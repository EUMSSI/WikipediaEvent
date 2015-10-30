/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.l3s.eumssi.wikiimport;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.util.ArrayList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

/**
 * This file is used to extract the wikinews from January 2005 to February 2006 but not May 2005. It has the most clumsy and weird structure without any similarity with the rest.
 * @author kanik
 */
public class Wiki4 {
    // These are some of the variables to automate the process of extraction of webpages of different months and years.
    private static int monthoffset = 0;
    private static int yearoffset = 0;
    
    public static void main(String[] args){
    	Wiki4 importer = new Wiki4();
    	importer.run();
    }
    
    public void run(){
        // This is a variable to establish the connection with the database.
        ContentHandling astra = new ContentHandling();
        for(int i=1;i<=14;i++){
            if(i%13 == 0){
                monthoffset += 12;
                yearoffset +=1;
            }
            if(i==5)
                continue;
            
            /*If we have to parse a html file
            File input = new File("/home/kanik/pr.html");
            Document doc = Jsoup.parse(input, "UTF-8", "");*/
            
            // URL of the webpage to parse
            String url = "http://en.wikipedia.org/wiki/"+returnMonth(i-monthoffset)+"_"+(2005+yearoffset);
     
            //Important Variables
            String categoryValue = null;
            String newsstory ="";
            String description = "";
            String actualDate = "";
            
            try{
            // Fetching the webpage
            print("Fetching %s...", url);
            Document doc = Jsoup.connect(url).get();
            Elements days = doc.select("div#content.mw-body div#bodyContent div#mw-content-text.mw-content-ltr");
            for(Element eachday : days){  // This will loop only once because it is the whole text
                Elements individual = eachday.children();
                // This part is added to automate the process of dates
                int da = 1;
                String mont = monthvalue(returnMonth(i-monthoffset));
                // The above part ends here
                for(Element dateplustext : individual){    // This consists of alternate date and events (with or withour newsStory)
                    if(dateplustext.tagName().equals("h2") || dateplustext.tagName().equals("h3")){  // January 2005 to February 2006 
                        print("*****************************************************\n");
                        String modifiedDate = dateplustext.text();      // This is essential to do because wikipedia present dates in weird manner and if we want to faciliate search using dates in our database then they should be present in this format YYYY-MM-DD
                        
                        if(!modifiedDate.toLowerCase().contains("200".toLowerCase())){
                            continue;
                        }
                        if(da<=9)
                            actualDate = (2005+yearoffset)+"-"+mont+"-0"+da;
                        else
                            actualDate = (2005+yearoffset)+"-"+mont+"-"+da;
                        if((mont.equals("07") && da==4) || (mont.equals("03") && da==15)||(mont.equals("01") && da==13 && (2005+yearoffset)==2006) )
                            da+=2;
                        else
                            da++;
                        /*if(modifiedDate.toLowerCase().contains("(".toLowerCase()) && i!=6){
                            int firstoccur = modifiedDate.indexOf("(");
                            String year = modifiedDate.substring(firstoccur-5, firstoccur-1);
                            String day = modifiedDate.substring(0, 2).trim();
                            if(day.length() == 1){
                                day = "0"+day;
                            }
                            String month = monthvalue(modifiedDate.substring(2, firstoccur-6).trim());
                            actualDate = year+"-"+month+"-"+day;
                        }
                        if(modifiedDate.toLowerCase().contains(",".toLowerCase())){
                            int firstoccur = modifiedDate.indexOf(",");
                            String year = modifiedDate.substring(firstoccur+2, firstoccur+6);
                            String day = modifiedDate.substring(firstoccur-2, firstoccur).replaceAll(" ", "0");
                            String month = monthvalue(modifiedDate.substring(0, firstoccur-2).trim());
                            actualDate = year+"-"+month+"-"+day;
                        }
                        if(i==6){
                            int firstoccur = modifiedDate.indexOf("(");
                            String day = modifiedDate.substring(5, 7).trim();
                            if(day.length() == 1){
                                day = "0"+day;
                            }
                            actualDate = "2005-06-"+day;
                        }*/
                        print("______DATE : %s\n",actualDate);
                    }
                    if(dateplustext.tagName().equals("ul")){   // Complete news under a given date
                        Elements stories = dateplustext.children();  // This contains different stories (newsStory may be present or not)
                                for (Element li : stories){
                                    // We are creating an ArrayList to store the URl of the source so that sourceids can be stored into the WKEvent Table
                                    ArrayList<String> storeurl = new ArrayList<String>();
                                    
                                    // We are creating an ArrayList to store the wikiref to populate the table EventEntityRelation
                                    ArrayList<String> wikiurls = new ArrayList<String>();
                                    
                                    Elements uls = li.children();  // These are either <a> tags if it doesn't have a newsStory or it is <a> and <ul> tag if it contains a newsStory
                                    boolean hasUL = false;    // If li has ul then it implies that it contains a news story
                                    for (Element ul: uls) {
                                        if (ul.tagName().equals("ul")) {
                                            hasUL = true; // news story is there
                                            Node st = li.childNode(0);
                                            //System.out.println("NewsStory: "+st.attr("title"));  // Printing the newsStory
                                            newsstory = st.attr("title");
                                            //System.out.println("URL for NewsStory :"+st.attr("href"));
                                            int wikiid = astra.insertIntoWikiRef(st.attr("title"), st.attr("href"));   // WKEvent the newsStory has to be populated into WikiRef
                                            astra.insertIntoNewsStory(st.attr("title"),0,"f",wikiid);  // Populating the newsStory table, right now parentid is assumed to be 0 and ongoing to be false.
                                            astra.insertIntoStoryCategoryRelation(st.attr("title"), categoryValue);  // Populating the StoryCategoryRelation table
                                            /*description = ul.text();
                                            System.out.println("WKEvent:" + description);  // Gives the event*/ // This block has been commented out by me
                                            Elements innerli = ul.children();       //Now we get inside the ul element which containd different li elements
                                            for(Element inside : innerli){    // Here we are picking one li
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
                                                           astra.insertIntoSource(name.text().substring(1, length-1),name.attr("href"),"1993-01-01", "Not Published"); // Populating the source table, right now published date and content are taken some default values as it is quite clear
                                                           
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
                                                System.out.println("Edited WKEvent:"+description);
                                                int eventid = astra.insertIntoEvent(description, newsstory, categoryValue, Date.valueOf(actualDate), storeurl,wikiid);  //Populating the WKEvent table
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
                                       // int eventid = astra.insertIntoEvent(description, newsstory, categoryValue, actualDate, storeurl);  //Populating the WKEvent table
                                        storeurl.clear();
                                        //astra.insertIntoEventEntityRelation(eventid, wikiname);  //Populating the EventEntityRelation table
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
                                                           description = description.replace(names.text(), "");
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
                                                int eventid = astra.insertIntoEvent(description, newsstory, categoryValue, Date.valueOf(actualDate), storeurl,0); //Populating the WKEvent table
                                                storeurl.clear();
                                                astra.insertIntoEventEntityRelation(eventid, wikiurls); //Populating the EventEntityRelation Table
                                                wikiurls.clear();
                                    }        
                                }

                            }
                    }
                }
            }catch(Exception e){
            	e.printStackTrace();
            	System.out.println(" Failed to fetch url: " + url );
            }
        }
    }
    
    private static String monthvalue(String month){
        String mon = month.toLowerCase();
        if(mon.equals("january"))
            mon = "01";
        if(mon.equals("february"))
            mon = "02";
        if(mon.equals("march"))
            mon = "03";
        if(mon.equals("april"))
            mon = "04";
        if(mon.equals("may"))
            mon = "05";
        if(mon.equals("june"))
            mon = "06";
        if(mon.equals("july"))
            mon = "07";
        if(mon.equals("august"))
            mon = "08";
        if(mon.equals("september"))
            mon = "09";
        if(mon.equals("october"))
            mon = "10";
        if(mon.equals("november"))
            mon = "11";
        if(mon.equals("december"))
            mon = "12";
        return mon;
        
    }        
    
    private static String returnMonth(int value){
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
    
    private static void print(String msg, Object... args) {
        System.out.println(String.format(msg, args));
    }

    private static String trim(String s, int width) {
        if (s.length() > width)
            return s.substring(0, width-1) + ".";
        else
            return s;
    }
    
}
