package de.l3s.eumssi.importing;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Properties;

import org.jsoup.nodes.Document;

import de.l3s.eumssi.dao.DatabaseManager;
import de.l3s.eumssi.model.Event;

public class TestImport {

	public static void main(String[] args) {

		Properties prop = new Properties();
		PrintWriter writer = null;
		
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
//			prop.load(classLoader.getResourceAsStream("wikitimes.properties"));	
			prop.load(new FileInputStream("wikitimes.properties"));
			
			writer = new PrintWriter(System.currentTimeMillis()+"_events.txt", "UTF-8");
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(0);
		}
		
		int month = 5;
		int year  = 2012;
		
//		DatabaseManager db = new DatabaseManager();

		ArrayList<Event> events = null;
		int counter = 0;
		writer.println("{wikitimes_dataset:[");
		for(year= 2000; year<=2014; year++){
//		for(year= 2007; year<=2007; year++){
			for(month=1; month<=12; month++){
				counter++;
				if(counter > 1)
					writer.println(",");
				writer.println("{month:" + "\"" + month+"-"+year + "\"");
//				WikiParser parser = new WikiParserFromJan2000ToSep2000();
//				WikiParser parser = new WikiParserFromOct2000ToDec2004();
//				WikiParser parser = new WikiParserFromJan2005ToApr2006();
//				WikiParser parser = new WikiParserForMay2005();
//				WikiParser parser = new WikiParserFromMay2006ToNow();
				
				WikiParser parser = getParser(month, year);
				
				events = parser.getEvents(year, month);
				if (events != null){
					System.out.println(" num of events of " + month + "-" + year + " = " + events.size());
					System.out.println("----------------------------------");
					
					System.out.println(", events:[");
					writer.println(", events:[");
					int count = 0;
					for (Event event: events){
						count++;
						if (count>1){
							System.out.print(",");
							writer.print(",");
						}
						System.out.println(event.toString());
						writer.println(event.toString());
//						if(event.getEntities().size() < 2)
//							System.out.println("\n ----> " + event.getAnnotatedDescription() + "\n");
					}
					System.out.println("]}");
					writer.println("]}");
					System.out.println("----------------------------------");
					System.out.println(" num of events = " + events.size());
					System.out.println();
				}
			}
			System.out.println("++++++++++++++++++++");
		}
		writer.println("]}");
		writer.close();	
			
			
//			int count=0;
//			for(Event event: events){				
//				Event foundEvent = db.getEventByDateAndDescription(event.getDate(), event.getDescription());
//				if (foundEvent!= null){
//					System.out.println(" event found in DB and will be removed first!!");
//					System.out.println(foundEvent.toString());
//					db.deleteEvent(foundEvent);					
//				}
//				
//				db.addNewEvent(event);
//				count++;
//				
//			}
//			System.out.println("#################################");
//			System.out.println(count + " events were stored successfully!");
			
//			db.closeConnection();
	}
	
	public static WikiParser getParser(int month, int year){
		if (year == 2000 && month <= 9)
			return new WikiParserFromJan2000ToSep2000();
		else if (year == 2000 && month >= 10)
			return new WikiParserFromOct2000ToDec2004();
		else if (year == 2005 && month == 5)
			return new WikiParserForMay2005();
		else if (year == 2005 && month != 5)
			return new WikiParserFromJan2005ToApr2006();
		else if (year == 2006 && month <= 4)
			return new WikiParserFromJan2005ToApr2006();
		else return new WikiParserFromMay2006ToNow();
	}

}
