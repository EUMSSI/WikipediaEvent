package de.l3s.eumssi.wikiimport;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class ImportAll {

	public static void main(String[] args) {
		ImportAll.run();
	}
	
	public static void run(){
			
//		This class is used to crawl the current events from January 2000 to September 2000
		Wiki1 importer1 = new Wiki1();
		
//		This class is used to crawl the current events from October 2000 to December 2004
		Wiki3 importer2 = new Wiki3();
		
//		This class is used to crawl the current events from January 2005 to February 2006 but not May 2005
		Wiki4 importer3 = new Wiki4();
		
//		This class is used to crawl the current events from May 2005
		Wiki6 importer5 = new Wiki6();
		
//		This class is used to crawl the current events from March 2006 to April 2006
		Wiki5 importer4 = new Wiki5();

		
//		This class is used to crawl the current events from May 2006 to NOW
		Import importer6 = new Import();
	
		Calendar fromDate = new GregorianCalendar(2006, 5, 1);
		
		Calendar toDate = new GregorianCalendar();
		toDate.setTime(new Date());
		
		importer6.run(fromDate, toDate, false);  
				
		importer5.run(); 
		
		importer4.run(); 	 
		
		importer3.run();
		
		importer2.run(); 	 
		
		importer1.run();
	}

}
