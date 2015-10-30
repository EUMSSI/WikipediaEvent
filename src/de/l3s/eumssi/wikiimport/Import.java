package de.l3s.eumssi.wikiimport;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;

import de.l3s.eumssi.dao.DatabaseManager;
import de.l3s.eumssi.indexing.Indexer;
import de.l3s.eumssi.model.Entity;
import de.l3s.eumssi.model.Event;
import de.l3s.eumssi.model.Reference;

public class Import {

	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) {
		
		if(args.length < 3){
			System.err.println("ERROR: missing parameters!");
//			System.err.println("formYear fromMonth fromDay toYear toMonth toDay 'command'");
			System.err.println("Syntax: 'command' fromDate toDate");
			System.err.println(" formDate and toDate should be in the format: dd-MM-yyyy");
			System.err.println(" command: test | fetch | import | fetch_import | index");
			System.err.println("         test: test the DB connection and if the dates are set correctly");
			System.err.println("         fetch: only download wiki pages and store them locally");
			System.err.println("         import: import events to DB from fetched wiki pages");
			System.err.println("         fetch_import: fetch and import events to DB from fetched wiki pages");
			System.err.println("         index: get events from DB and index them in Lucene");
			System.exit(0);
		}
			
		Calendar fromDate, toDate;
		
		String command = args[0]; 
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
//		Calendar toDate = new GregorianCalendar();
//		toDate.setTime(new Date());
		
		toDate = new GregorianCalendar(toYear, toMonth-1, toDay);
		toDate.add(Calendar.DAY_OF_MONTH, 1);
		
		Import importer = new Import();
		if(command.equals("test")){
			importer.test(fromDate, toDate);
		}
		else if(command.equals("fetch")){
			importer.download(fromDate, toDate);
		}
		else if(command.equals("fetch_import")){
			importer.run(fromDate, toDate, true);
		}
		else if(command.equals("import")){
			System.out.println("Starting importing events from fetched pages ...");
			importer.run(fromDate, toDate, false);
		}
		else if(command.equals("index")){
			System.out.println("Starting indexing events into Lucene ...");
			importer.indexInSolr(fromDate, toDate);
		}
		else{
			System.err.println(" Unknown command: " + command);
		}
//		
	}
	
	
	
	public void run(Calendar fromDate, Calendar toDate, boolean download){

		Properties prop = new Properties();
		PrintWriter writer = null;
		
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			prop.load(classLoader.getResourceAsStream("wikitimes.properties"));	
			
			writer = new PrintWriter(System.currentTimeMillis()+"_"+prop.getProperty("logFile"), "UTF-8");
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(0);
		}
		
		WikiPageParser wp = new WikiPageParser();
		ContentHandling ch = new ContentHandling();
		DBOperation dbop = new DBOperation();
		DatabaseManager db = new DatabaseManager();

		ArrayList<Event> events = new ArrayList<Event>();
		
		String dateStr = "";
		
		int count = 0;
		int insertedToDB = 0;
		while(fromDate.before(toDate)){
			dateStr = stringFormat(fromDate);
			
			try {
				events = wp.parseing(dateStr, download, prop.getProperty("downloadFolder"));				
				writer.println("------------------------");
				System.out.println(dateStr);
				writer.println(dateStr);
				if(events.isEmpty()){
					System.err.println(" NO events found !!!!");
					writer.println(" NO events found !!!!");
				}
				
				for(Event event : events){
					count++;
					System.out.println(printEvent(event));
					writer.println(printEvent(event));
					if(ch.isExistsInDB(event)==null){
//						printEvent(event);
						dbop.insertEventObjIntoDB(event);
//						db.addNewEvent(event);
						insertedToDB++;											
					}else{
						writer.println(" event found in DB !! -- date: " + event.getDate() + ": " + event.getDescription());
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			fromDate.add(Calendar.DAY_OF_MONTH, 1);	
		}
		
		writer.println("total num of events = " + count);
		writer.println("inserted to DB      = " + insertedToDB);
		writer.close();		
		
		
		db.closeConnection();

	}
	
	
	private String stringFormat(Calendar fromDate) {
		String dateStr = fromDate.get(Calendar.YEAR) + "-" ;
		if ((fromDate.get(Calendar.MONTH)+1) <10) 
			dateStr+= "0";
		dateStr+=(fromDate.get(Calendar.MONTH)+1) + "-";
		if(fromDate.get(Calendar.DAY_OF_MONTH) < 10)
			dateStr+= "0";
		dateStr+=fromDate.get(Calendar.DAY_OF_MONTH);
		return dateStr;
	}



	public void index(Calendar fromDate, Calendar toDate ){
		Properties prop = new Properties();
		PrintWriter writer = null;
		
		WikiIndexer indexer = null;
//		NewsArticlesHandler articlesHandler = null;

		ArrayList<Event> events = null;
		
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			prop.load(classLoader.getResourceAsStream("wikitimes.properties"));	
			
			writer = new PrintWriter(System.currentTimeMillis()+"_"+prop.getProperty("logFile"), "UTF-8");
			indexer = new WikiIndexer(prop.getProperty("eventIndexPath"));
			System.out.println(" index path: " + indexer.getEventIndexpath());
			writer.println(" index path: " + indexer.getEventIndexpath());
//			articlesHandler = new NewsArticlesHandler(prop.getProperty("newsArticleIndexPath"));
			
			DatabaseManager db = new DatabaseManager();
			events = db.getEvents(stringFormat(fromDate), stringFormat(toDate));
			db.closeConnection();
			writer.println(events.size() + " events will be indexed in lucene ...");
			
			for (Event event: events){
				writer.print("eventID " + event.getEventId() + " ---> ");
				System.out.print("eventID " + event.getEventId() + " ---> ");
				indexer.indexEvent(event);
//				for(Reference newsArticle: event.getReferences()){
//					articlesHandler.storeNewsArticleInDB(newsArticle);
//					articlesHandler.indexNewsArticle(newsArticle);
//				}
				writer.println(" done ..");
				System.out.println(" done ..");
			}
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}finally{
			writer.close();	
		}
		
	}
	
	
	public void indexInSolr(Calendar fromDate, Calendar toDate ){
		Properties prop = new Properties();
		PrintWriter writer = null;
		
		Indexer indexer = new Indexer();

		ArrayList<Event> events = null;
		
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			prop.load(classLoader.getResourceAsStream("wikitimes.properties"));	
			
			writer = new PrintWriter(System.currentTimeMillis()+"_"+prop.getProperty("logFile"), "UTF-8");
			
			DatabaseManager db = new DatabaseManager();
			events = db.getEvents(stringFormat(fromDate), stringFormat(toDate));
			db.closeConnection();
			writer.println(events.size() + " events will be indexed in lucene ...");
			System.out.println(events.size() + " events will be indexed in lucene ...");
			
			indexer.indexEvents(events);
			
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (Exception e) {
			System.out.println("ERROR: failed to index events in Solr!!!");
			e.printStackTrace();
		}finally{
			writer.close();	
		}
		
	}
	
	
	public void download(Calendar fromDate, Calendar toDate){

		Properties prop = new Properties();
		PrintWriter writer = null, page = null;
		
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			prop.load(classLoader.getResourceAsStream("wikitimes.properties"));			
			writer = new PrintWriter(System.currentTimeMillis()+"_"+prop.getProperty("logFile"), "UTF-8");
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(0);
		}
		
		WikiPageParser wp = new WikiPageParser();

		String dateStr = "";
		
		String content = null;
		while(fromDate.before(toDate)){
			dateStr = stringFormat(fromDate);
			
			content = null;
			try {								
				writer.println("------------------------");
				writer.println(dateStr);
				System.out.println(dateStr);
				
				content = wp.fetch(dateStr);
				if(content!=null){
					 String[] sdate = dateStr.split("-");
					 String fileName = wp.returnMonth(Integer.parseInt(sdate[1]))+"_"+sdate[0];
					 page = new PrintWriter(prop.getProperty("downloadFolder") + fileName+".html", "UTF-8");
					 page.print(content);
					 page.close();
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			fromDate.add(Calendar.MONTH, 1);	
		}
		
		writer.close();		
		
	}
	

 private void test(Calendar fromDate, Calendar toDate){
	
	DatabaseManager db = new DatabaseManager();
	try {
		db.openConnection();
		db.closeConnection();
	} catch (SQLException e) {
		e.printStackTrace();
	}
	
	String dateStr = "";
	
	while(fromDate.before(toDate)){
		dateStr = stringFormat(fromDate);
		System.out.println(dateStr);
		fromDate.add(Calendar.DAY_OF_MONTH, 1);	
	}
}
	
	
	
	public static String printEvent(Event event){
		String out = "";
		out+= "Date: " + event.getDate() + "||";
		out+= "\t Description: " + event.getDescription() + "||";
		if (event.getStory()!=null){
			out+= "\t Story: {";
			out+= "Name: " + event.getStory().getName();
			out+= "\t URL: " + event.getStory().getWikipediaUrl() + "}";
		}
		if (event.getCategory()!=null){
			out+= "\t Category: " + event.getCategory().getName()+ "||";
		}
		if(event.getEntities()!=null){
			if(!event.getEntities().isEmpty()){
				out+= "\t Entities: {";
				for(Entity entity:event.getEntities()){
					out+= "\t Name: " + entity.getName();
					out+= "\t URL: " + entity.getWikiURL()+ "||";
				}
				out+= "}";
			}
		}
		if(event.getReferences()!=null){
			if(!event.getReferences().isEmpty()){
				out+= "\t Sources: {";
				for(Reference ref: event.getReferences()){
					out+= "\t Source: " + ref.getSource();
					out+= "\t Type: " + ref.getType();
					out+= "\t URL: " + ref.getUrl()+ "||";
				}
				out+= "}";
			}
		}
		return out;
	}

}
