package de.l3s.eumssi.importing;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;

import de.l3s.eumssi.dao.DatabaseManager;
import de.l3s.eumssi.enrichment.EnrichStoryRedirection;
import de.l3s.eumssi.model.Event;

public class ImportAll {

	public static void main(String[] args) {
		
		if(args.length < 1){
			System.err.println("ERROR: missing parameters!");
			System.err.println("  command: test | removeLatest | dates | import | redirections");
			
			System.err.println("Syntax: 'command' fromDate toDate [json] [db]");
			System.err.println("  formDate and toDate should be in the format: dd-MM-yyyy");
			System.err.println("  command: test | removeLatest| dates | import | redirections");
			System.err.println("     test        			: test the DB connection and if the dates are set correctly");
			System.err.println("     removeLatest        	: remove events of the latest date in the DB");
			System.err.println("     dates       			: extract only Dates from fetched wiki pages");
			System.err.println("     import      			: import events to DB from fetched wiki pages");
			System.err.println("     redirections			: imports and exploits story redirection information to marge duplicated stories, it affects DB");
			System.err.println("  json   : if specified then output events in JSON format into a text file (path is defined in config file)");
			System.err.println("  db     : if specified then store events in DB (db properties are defined in config file)");
			System.exit(0);
		}
		
		ImportAll importer = new ImportAll();
		String command = args[0]; 
		System.out.println("Preparing excecute command: " + command);
		if(args.length == 1 && command.equals("redirections")){
			System.out.println("extracting story redirection information to marge duplicated stories, this affects DB ...");
			importer.updateRedirections();
		} else if (command.equals("removeLatest")) {
			RemoveEvents.run();
		} else{
			if (args.length <3) {
				System.err.println("Syntax: 'command' fromDate toDate [json] [db]");
				System.err.println("  formDate and toDate should be in the format: dd-MM-yyyy");
				System.err.println("  command: test | removeLatest| dates | import | redirections");
				System.err.println("     test        			: test the DB connection and if the dates are set correctly");
				System.err.println("     removeLatest        	: remove events of the latest date in the DB");
				System.err.println("     dates       			: extract only Dates from fetched wiki pages");
				System.err.println("     import      			: import events to DB from fetched wiki pages");
				System.err.println("     redirections			: imports and exploits story redirection information to marge duplicated stories, it affects DB");
				System.err.println("  json   : if specified then output events in JSON format into a text file (path is defined in config file)");
				System.err.println("  db     : if specified then store events in DB (db properties are defined in config file)");
				System.exit(0);
			}
			
			Calendar fromDate, toDate;
			String fromDateStr = args[1];
			String toDateStr   = args[2];
			
			int fromDay   = Integer.parseInt(fromDateStr.split("-")[0]);
			int fromMonth = Integer.parseInt(fromDateStr.split("-")[1]);
			int fromYear  = Integer.parseInt(fromDateStr.split("-")[2]);
			
			int toDay   = Integer.parseInt(toDateStr.split("-")[0]);
			int toMonth = Integer.parseInt(toDateStr.split("-")[1]);
			int toYear  = Integer.parseInt(toDateStr.split("-")[2]);
			
			System.out.println();
			fromDate = new GregorianCalendar(fromYear, fromMonth-1, fromDay);
			
			toDate = new GregorianCalendar(toYear, toMonth-1, toDay);
			
			boolean outputJSON = false;
			boolean storeInDB = false; 
			
			if (args.length == 4){
				if (args[3].equals("json"))
					outputJSON = true;	
				if (args[3].equals("db"))
					storeInDB = true;
			}
			
			if (args.length == 5){
				if (args[3].equals("json") || args[4].equals("json"))
					outputJSON = true;	
				if (args[3].equals("db") || args[4].equals("db"))
					storeInDB = true;
			}
			
			System.out.println(" output JSON: " + outputJSON);
			System.out.println(" stote in DB: " + storeInDB);
			System.out.println(" from       : " + stringFormat(fromDate));
			System.out.println(" to         : " + stringFormat(toDate));
			
			if(command.equals("test")){
				importer.test(fromDate, toDate);
			}
			else if(command.equals("import")){
				System.out.println("Starting importing events from Wikipedia ...");
				importer.run(fromDate, toDate, outputJSON, storeInDB);
			}else if(command.equals("dates")){
				System.out.println("Starting extracting dates from Wikipedia ...");
				importer.testExtractingDates(fromDate, toDate);
			}else{
				System.err.println(" Unknown command: " + command);
			}
			
		}
		
		
		
		
			
		
	}
		
	
	private void updateRedirections() {
		EnrichStoryRedirection extractor = new EnrichStoryRedirection();
		extractor.resolveStoryRedirection();
	}


	public void test(Calendar fromDate, Calendar toDate){
		DatabaseManager db = new DatabaseManager();
		try {
			db.openConnection();
			db.closeConnection();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		int startYear = fromDate.get(Calendar.YEAR);
		int endYear = toDate.get(Calendar.YEAR);
		
		for(int year=startYear; year<=endYear; year++){
			for(int month=1; month<=12; month++){
				System.out.println(month + "-" + year);
			}
		}
		
	}
	
	public void run(Calendar fromDate, Calendar toDate, boolean outputJSON, boolean storeInDB){
		ArrayList<Event> events = null;
		ArrayList<Event> events_tmp = null;
		
		int startYear = fromDate.get(Calendar.YEAR);
		int endYear = toDate.get(Calendar.YEAR);
		int startMonth = 1;
		int endMonth = 12;
		if (startYear == endYear){
			startMonth = fromDate.get(Calendar.MONTH)+1;
			endMonth = toDate.get(Calendar.MONTH)+1;
		}
		
		for(int year= startYear; year<=endYear; year++){
			for(int month=startMonth; month<=endMonth; month++){
				WikiParser parser = getParser(month, year);
				events_tmp = parser.getEvents(year, month);
				if (events_tmp != null){
					if(!events_tmp.isEmpty()){
						events = new ArrayList<Event>();
						for (Event event: events_tmp){
							if (event.getDate().compareTo(Date.valueOf(stringFormat(fromDate))) >= 0 && 
								event.getDate().compareTo(Date.valueOf(stringFormat(toDate))) <= 0	){
								events.add(new Event(event));
							}
						}
						System.out.println(" num of events of " + month + "-" + year + " = " + events.size());
						if (events.size() > 0){
							System.out.println("----------------------------------");
							if (outputJSON)
								this.outputJSON(events);
							if (storeInDB)
								this.storeEventsInDB(events);
							System.out.println("----------------------------------");
						}
					}
				}
			}
		}
		System.out.println("++++++++++++++++++++");
	}
	
	
	private void outputJSON(ArrayList<Event> events){
		Properties prop = new Properties();
		PrintWriter writer = null;
		String fromToStr = "from_" + events.get(0).getDate().toString() + "_To_" +  events.get(events.size()-1).getDate().toString();
		try {
//			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
//			prop.load(classLoader.getResourceAsStream("wikitimes.properties"));	
			prop.load(new FileInputStream("wikitimes.properties"));
			writer = new PrintWriter(prop.getProperty("outputFolderPath") + "events_" + fromToStr + "_v" + System.currentTimeMillis() +".txt", "UTF-8");
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(0);
		}
		
		System.out.print("{events:[");
		writer.println("{events:[");
		int count = 0;
		for (Event event: events){
			count++;
			if (count>1){
				System.out.print(",");
				writer.print(",");
			}
			System.out.println(event.toString());
			writer.println(event.toString());
		}
		System.out.println("]}");
		writer.println("]}");
		System.out.println("----------------------------------");
		System.out.println(" num of events = " + events.size());
		System.out.println();
		writer.close();
	}
			
	private void storeEventsInDB(ArrayList<Event> events){
		DatabaseManager db = new DatabaseManager();
		int count=0;
		int found_counter = 0;
		int stored_counter = 0;
		for(Event event: events){
			count++;
			Event foundEvent = db.getEventByDateAndDescription(event.getDate(), event.getDescription());
			if (foundEvent!= null){
				found_counter++;
				System.out.println(" event found in DB and will be not be added !!");
				System.out.println(foundEvent.getEventId() + " - " + foundEvent.getDate() + " - " + foundEvent.getDescription());
//				db.deleteEvent(foundEvent);					
			}else{
				db.addNewEvent(event);	
				stored_counter++;
			}
			
		}
		System.out.println("#################################");
		System.out.println(count + " events were extracted ");
		System.out.println(found_counter + " events were found in DB!!!");
		System.out.println(stored_counter + " events were added to DB!!!");
		
		db.closeConnection();
	}
	
	
	public void testExtractingDates(Calendar fromDate, Calendar toDate){
		ArrayList<String> dates = null;
		ArrayList<String> dates_tmp = null;
		
		int startYear = fromDate.get(Calendar.YEAR);
		int endYear = toDate.get(Calendar.YEAR);
		
		for(int year= startYear; year<=endYear; year++){
			for(int month=1; month<=12; month++){
				WikiParser parser = getParser(month, year);
				dates_tmp = parser.getDates(year, month);
				if (dates_tmp != null){
					if(!dates_tmp.isEmpty()){
						dates = new ArrayList<String>();
						for (String date: dates_tmp){
							if (Date.valueOf(date).compareTo(Date.valueOf(stringFormat(fromDate))) >= 0 && 
									Date.valueOf(date).compareTo(Date.valueOf(stringFormat(toDate))) <= 0	){
								dates.add(new String(date));
							}
						}
						System.out.println(" num of Dates of " + month + "-" + year + " = " + dates.size());
						if (dates.size() > 0){
							System.out.println("----------------------------------");
							for (String date: dates)
								System.out.println(date);
							System.out.println("----------------------------------");
						}
					}
				}
			}
		}
		System.out.println("++++++++++++++++++++");
	}
	
	
	
	public static WikiParser getParser(int month, int year){
		if (year == 2000 && month <= 9)
			return new WikiParserFromJan2000ToSep2000();
		else if (year == 2000 && month >= 10)
			return new WikiParserFromOct2000ToDec2004();
		else if (year >= 2001 && year <= 2004)
			return new WikiParserFromOct2000ToDec2004();
		else if (year == 2005 && month == 5)
			return new WikiParserForMay2005();
		else if (year == 2005 && month != 5)
			return new WikiParserFromJan2005ToApr2006();
		else if (year == 2006 && month <= 4)
			return new WikiParserFromJan2005ToApr2006();
		else if (year == 2006 && month >= 5) 
			return new WikiParserFromMay2006ToNow();
		else if (year >= 2006 ) 
			return new WikiParserFromMay2006ToNow();
		else
			return null;
	}

	
	public static String stringFormat(Calendar date) {
		String dateStr = date.get(Calendar.YEAR) + "-" ;
		if ((date.get(Calendar.MONTH)+1) <10) 
			dateStr+= "0";
		dateStr+=(date.get(Calendar.MONTH)+1) + "-";
		if(date.get(Calendar.DAY_OF_MONTH) < 10)
			dateStr+= "0";
		dateStr+=date.get(Calendar.DAY_OF_MONTH);
		return dateStr;
	}
}
