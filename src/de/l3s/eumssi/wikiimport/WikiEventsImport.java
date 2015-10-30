package de.l3s.eumssi.wikiimport;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import de.l3s.eumssi.dao.DatabaseManager;
import de.l3s.eumssi.model.Event;
import de.l3s.eumssi.model.Reference;

/*
 * This class is used for regular importing of events from WikiPedia current event portal
 * 
 * @Author : SUDHIR SAH
 */

public class WikiEventsImport {

	public static void main(String[] args) throws InterruptedException, IOException {

	//read the configuration file
	Properties prop = new Properties();
	prop.load(new FileInputStream("./configs/wikitimes.properties"));
	
	int startTime = Integer.parseInt(prop.getProperty("startingTime"));
	int interval = Integer.parseInt(prop.getProperty("interval"));
	String eventIndexPath = prop.getProperty("eventIndexPath");
	String newsArticlesIndexPath = prop.getProperty("newsStoryIndexPath");
	
	//System.out.println("Will start at : "+startingTime+ " o'clock");
	//System.out.println("Interval is : "+interval+ " Hours");
	 
	
	startTime = (startTime * 60); // converting into minute
	int interval_ms = interval * 60 * 60 * 1000; // converting into milli second
	
	Date dt = new Date(); //take the current system date
	String str1 = dt.toString();
	String[] str2 = str1.split(" ");
	String[] str3 = str2[3].split(":");
	int curHour = Integer.parseInt(str3[0]);
	int curMin = Integer.parseInt(str3[1]);
	int curTime = (curHour * 60 + curMin ); // in minute
	int sleepingTime = 0;
	
	if(startTime > curTime){
		 
		sleepingTime = (startTime - curTime); // in min
		System.out.println("*****Import will start afte : " +sleepingTime+ " Minutes from now");
		sleepingTime = (sleepingTime * 60 *1000); // in ms
		Thread.sleep(sleepingTime);
	}
	
	else if(startTime < curTime){
		
		sleepingTime = (24 * 60 - curTime) + startTime ;
		System.out.println("******Import will start after: " +sleepingTime+" Minutes from now");
		sleepingTime = (sleepingTime * 60 * 1000); // in ms
		Thread.sleep(sleepingTime);
	}
	else{
		
		System.out.println("StartingTime and currentTime are equal");
	}
	
	
	// All the necessary variables
	WikiPageParser wp = new WikiPageParser();
	ContentHandling ch = new ContentHandling();
	DBOperation dbop = new DBOperation();
	
	WikiIndexer wikiIndexer = new WikiIndexer(eventIndexPath);
	NewsArticlesHandler nah = new NewsArticlesHandler(newsArticlesIndexPath);
	
	DatabaseManager dbManager = new DatabaseManager();
	Event dbEvent = new Event(); //to hold the event object from database
	
	ArrayList<Event> eObjList = new ArrayList<Event>();
	//EventObjects eObj = new EventObjects();
		
	// infinite loop 
	for(;;)
	{
		/*
		for(EventObjects e : new WikiPageParser().parseing(new ContentHandling().returnPastDates(2)[0]))
		{
			if(!new ContentHandling().isExistsInDB(e))
			{
				new DBOperation().insertEventObjIntoDB(e);
			}
		}
		*/
		
		eObjList = wp.parseing(ch.returnPastDates(2)[0], true, prop.getProperty("downloadFolder")); //List of the event_objects from previous date
		
		for(Event eObj : eObjList) 
		{
			if(ch.isExistsInDB(eObj)==null)
			{
				//insert event into DB
				int id = dbop.insertEventObjIntoDB(eObj);
				
				//create lucene index for event
				if(id != 0)
				{
					dbEvent = dbManager.getEventById(Integer.toString(id));
					wikiIndexer.indexEvent(dbEvent);
					
					//populate content field of the Source table
					//create lucene index for newsArticles
					for(Reference ref : dbEvent.getReferences())
					{
						//populate content field of the source table
						nah.storeNewsArticleInDB(ref);
						
						//create lucene index for newsArticles
						nah.indexNewsArticle(ref);
					}
				}
			}
			
		}
		
		//close connection
		
		//wait 
		System.out.println("\nImport finished\n\nwaiting for next import...");
		Thread.sleep(interval_ms);
	}
	
  }
}
