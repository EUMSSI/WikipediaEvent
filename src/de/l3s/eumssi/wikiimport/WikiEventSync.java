package de.l3s.eumssi.wikiimport;
//package de.l3s.eumssi.wikiimport;
//
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//import java.util.Properties;
//
//import de.l3s.eumssi.dao.DatabaseManager;
//import de.l3s.eumssi.model.Event;
//
///*
// * Author : Sudhir Kumar sah
// */
//public class WikiEventSync {
//
//	public static void main(String[] args) throws IOException, InterruptedException {
//		
//		System.out.println("Hi this is wiki event synchronization code");
//		
//		//Reading from configuration file
//		Properties prop = new Properties();
//		prop.load(new FileInputStream("./configs/wikieventsync.properties"));
//		
//		String date = prop.getProperty("date");
//		int windowSize = Integer.parseInt(prop.getProperty("windowsize"));
//		int startTime = Integer.parseInt(prop.getProperty("starttime"));
//		int interval = Integer.parseInt(prop.getProperty("interval"));
//		String eventIndexPath = prop.getProperty("eventIndexPath");
//		
//		
//		//convert start time into minute
//		startTime *= 60;
//		
//		//convert intervla into milli sec
//		interval *= (60*60*1000);
//		
//		//wait until starting time
//		Date dt = new Date(); //take the current system date
//		String str1 = dt.toString();
//		String[] str2 = str1.split(" ");
//		String[] str3 = str2[3].split(":");
//		int curHour = Integer.parseInt(str3[0]);
//		int curMin = Integer.parseInt(str3[1]);
//		int curTime = (curHour * 60 + curMin ); // in minute
//		int sleepingTime = 0;
//		
//		if(startTime == 0)
//		{
//			Thread.sleep(0);
//		}
//		
//		else if(startTime > curTime){
//			 
//			sleepingTime = (startTime - curTime); // in min
//			System.out.println("***** Import will start afte : " +sleepingTime+ " Minutes from now");
//			sleepingTime = (sleepingTime * 60 *1000); // in ms
//			Thread.sleep(sleepingTime);
//		}
//		
//		else if(startTime < curTime){
//			
//			sleepingTime = (24 * 60 - curTime) + startTime ;
//			System.out.println("****** Import will start after: " +sleepingTime+" Minutes from now");
//			sleepingTime = (sleepingTime * 60 * 1000); // in ms
//			Thread.sleep(sleepingTime);
//		}
//		else{
//			
//			System.out.println("****** Import will start right after now");
//		}
//		
//		//some important variables
//		ContentHandling ch = new ContentHandling();
//		WikiPageParser wpp = new WikiPageParser();
//		DBOperation dbop = new DBOperation();
//		List<Event> events = new ArrayList<Event>();
//		String[] pastDates;
//		
//		WikiIndexer wikiIndexer = new WikiIndexer(eventIndexPath);
//		
//		Event dbEvent = new Event(); //to store event object fro data base, used for indexing
//		DatabaseManager dbManager = new DatabaseManager();
//		
//		//infinite loop
//		for(;;)
//		{
//			pastDates = ch.getPastDates(date, windowSize);
//			
//			//just over each days
//			for(String tempDate : pastDates)
//			{
//				System.out.println("\n*****Updating events for the date : "+tempDate);
//				events = wpp.parseing(tempDate, true, prop.getProperty("downloadFolder"));
//				for(Event event : events)
//				{
//					if(ch.isExistsInDB(event) != null)
//					{
//						//if event exists then perform deletion and insertion operation
//						//delete this event from DB
//						ch.deleteEvent(event);
//						
//						//insert this event in DB
//						int id = dbop.insertEventObjIntoDB(event);
//						
//						//update lucene index for this event
//						if(id != 0)
//						{
//							dbEvent = dbManager.getEventById(Integer.toString(id));
//							wikiIndexer.updateIndex(dbEvent);
//							System.out.println("Event with id : "+id+" has been updated successfully !!");
//						}
//						
//					}
//					else
//					{
//						//if event doesn't exists, meaning this is a new event
//						//insert event into DB
//						int id = dbop.insertEventObjIntoDB(event);
//						
//						//create lucene index for this event
//						if(id != 0)
//						{
//							dbEvent = dbManager.getEventById(Integer.toString(id));
//							wikiIndexer.indexEvent(dbEvent);
//							System.out.println("Event with id : "+id+" has been inserted successfully !!");
//						}
//					
//					}
//				}
//			}
//			
//			//close connection
//			if(!DBHandler.isClosed(ch.con))
//			{
//				DBHandler.closeDBConnection(ch.con);
//			}
//			
//			//wait
//			System.out.println("Waiting for next run");
//			Thread.sleep(interval);
//		}
//		
//	}
//
//}
