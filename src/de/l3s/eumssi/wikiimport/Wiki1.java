/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.l3s.eumssi.wikiimport;
import java.sql.Date;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
/**
 * This file is used to crawl the wikinews from Jan 2000 to Sep 2000.
 * @author kanik
 */
public class Wiki1 {
	 public static void main(String[] args) {
	    	Wiki1 importer = new Wiki1();
	    	importer.run();
	    }
	    
	    public void run(){
        
        // This is a variable to establish the connection with the database.
        ContentHandling astra = new ContentHandling();
        
        for(int i=1;i<=9;i++){
            
            /*If we have to parse a html file
            File input = new File("/home/kanik/pr.html");
            Document doc = Jsoup.parse(input, "UTF-8", "");*/
            
            // URL of the webpage to parse
            String url = "http://en.wikipedia.org/wiki/"+returnMonth(i)+"_2000";
 
            //Important Variables
            String categoryValue = null;
            String newsstory = null;
            String description = null;
            String actualDate = "";
            
            try{
            // Fetching the webpage
            print("Fetching %s...", url);            
            Document doc = Jsoup.connect(url).get();
            
            Elements days = doc.select("div#bodyContent div#mw-content-text.mw-content-ltr table tbody tr td table.vevent");
            for(Element eachday : days){
                print("*****************************************************\n");
                Elements date = eachday.select("tbody tr td table.plainlinks tbody tr td span.summary");
                String modifiedDate = date.text();
                if(modifiedDate.toLowerCase().contains("(".toLowerCase())){
                    int firstoccur = modifiedDate.indexOf("(");
                    actualDate = modifiedDate.substring(firstoccur+1, firstoccur+11);
                }
                print("______DATE : %s\n",actualDate);
                Elements categories = eachday.select("tbody tr td.description");
                print("SIZE: %d",categories.size());
                for(Element category : categories){    // This will loop just once since size is 1
                    for(Element e : category.children()){
                            if (e.tagName().equals("ul")) {   // In fact ul is the only child of this big td.description
                                Elements stories = e.children();  // This corresponds to li's that means different events under a particular date.
                                for (Element li : stories){
                                        // We are creating an ArrayList to store the wikiref to populate the table EventEntityRelation
                                        ArrayList<String> wikiurls = new ArrayList<String>();
                                        
                                        // We are creating an ArrayList to store the URl of the source so that sourceids can be stored into the WKEvent Table
                                        ArrayList<String> storeurl = new ArrayList<String>();
                                        
                                        System.out.println("WKEvent:" + li.text());  // This are all the events
                                        description = li.text();
                                        Elements sources = li.children();     // This gives all the a tags
                                                for(Element names : sources){
                                                    if(names.tagName().equals("i")){
                                                        Node st = names.childNode(0);
                                                        print("_____For WikiRef Name : %s",st.attr("title"));
                                                        print("_____For WikiRef URL : %s",st.attr("href"));
                                                        wikiurls.add(st.attr("href"));
                                                        astra.insertIntoWikiRef(st.attr("title"), st.attr("href"));
                                                    }
                                                    if(names.tagName().equals("a")){
                                                        print("_____For WikiRef Name : %s",names.text());
                                                        print("_____For WikiRef URL : %s",names.attr("href"));
                                                        wikiurls.add(names.attr("href"));
                                                        astra.insertIntoWikiRef(names.text(), names.attr("href"));
                                                    }
                                            }
                                              int eventid = astra.insertIntoEvent(description, newsstory, categoryValue, Date.valueOf(actualDate), storeurl,0);
                                              astra.insertIntoEventEntityRelation(eventid, wikiurls);
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
