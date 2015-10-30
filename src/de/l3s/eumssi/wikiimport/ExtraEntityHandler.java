package de.l3s.eumssi.wikiimport;
//package de.l3s.eumssi.wikiimport;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.Reader;
//import java.net.URL;
//import java.nio.charset.Charset;
//import java.util.ArrayList;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import de.l3s.eumssi.dao.DatabaseManager;
//
//
//
//public class ExtraEntityHandler {
//	
//	private String readAll(Reader rd) throws IOException {
//	    StringBuilder sb = new StringBuilder();
//	    int cp;
//	    while ((cp = rd.read()) != -1) {
//	      sb.append((char) cp);
//	    }
//	    return sb.toString();
//	}
//		
//	//This function is used to download the wikipedia page
//	 public String downloadWikiPage(String wikiurl) throws IOException{
//
//		  String wikiPage = null;
//		  InputStream is=null;
//	 
//		    try {
//		    	is = new URL(wikiurl).openStream();
//		    	BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
//		    	wikiPage = readAll(rd);
//		      
//		    }catch(Exception e){
//		    	System.out.println("There is exception in \"downloadWikiPage()\" method");
//		    }
//	   
//		    return wikiPage;
//	}
//	 
//	 
//	public void extraEntityParsing(String date) throws IOException
//	{
//		ContentHandling ch = new ContentHandling();
//		DatabaseManager db = new DatabaseManager();
//		
//		ArrayList<Integer> storiesIDs = ch.returnNewsStoriesIDs(date);
//		
//		for(int storyid : storiesIDs)
//		{
//			if(storyid != 0 )
//			{
//				String wikiURL = db.getStoryById(""+storyid).getWikipediaUrl();
//				System.out.println("***********************" +wikiURL);
//				String wikiPage = downloadWikiPage(wikiURL);
//				
//				 if(wikiPage != null)
//				 {
//					 Pattern pattern = Pattern.compile("href=\"/wiki/[^\\s]*\\stitle=\"[^\"]*");
//					 
//				     Matcher match = pattern.matcher(wikiPage);
//				     while(match.find())
//				     {
//				    	 String maches= match.group();
//				    	// String normURL = java.net.URLDecoder.decode(extraWikiURL, "UTF-8");
//				    	maches =maches.replaceAll("href=","");
//				    	maches =maches.replaceAll("\"","");
//				    	maches =maches.replaceAll("="," ");
//				    	String[] splitintoUrlAndTitle = maches.split("title");
//				    	
//				    	String url = splitintoUrlAndTitle[0];
//				    	String title = splitintoUrlAndTitle[1];
//				    	
//				    	String dbyogo = url.replaceAll("/wiki","");
//				    	String wikipediaURL = "http://en.wikipedia.org"+url;
//				    	String dbpediaURI = "http://dbpedia.org/resource"+dbyogo;
//				    	String yagoURI = "http://yago-knowledge.org/resource"+dbyogo;
//				    	//System.out.println(wikipediaURL+" "+dbpediaURI+" "+yagoURI);
//				    	
//				    	ch.insertIntoNewWikiRef(title, wikipediaURL, dbpediaURI, yagoURI);
////				    	int wikiid = ch.returnWikiRefID(wikipediaURL);
//				    	int wikiid = Integer.parseInt(db.getEntityByURL(wikipediaURL).getId());
//				    	
//				    	//insert into News Story ExtraEntity relation 
//				        ch.insertIntoNewsStoryExtraEntityRel(storyid, wikiid);
//				   	
//				     }
//				 }
//			}
//			
//		}
//		
//	}
//
//}
