/**
 * Remove events of given day in DB
 */
package de.l3s.eumssi.importing;

import java.sql.Date;
import java.util.ArrayList;

import de.l3s.eumssi.dao.DatabaseManager;
import de.l3s.eumssi.model.Event;

public class RemoveEvents {
	static DatabaseManager db = new DatabaseManager();
	
	public static void run() {
		String latestDate = db.getLatestDateinDB();
		System.out.println("Latest date in DB " + latestDate);
		ArrayList<Event> events = db.getEventsByDate(latestDate);
		for (Event e: events) {
			//remove that event
			System.out.println("Delete event " + e.getEventId());
			db.deleteEvent(e);
		}
	}
	
	public static void main(String[] args) {
		RemoveEvents.run();
	}
}
