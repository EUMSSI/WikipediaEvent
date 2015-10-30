package de.l3s.eumssi.enrichment;

/**
 * this class detect location of events heuristically
 * @author giangbinhtran
 *
 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import de.l3s.eumssi.dao.DatabaseManager;
import de.l3s.eumssi.importing.DBHandler;
import de.l3s.eumssi.model.Entity;
import de.l3s.eumssi.model.Event;
import de.l3s.eumssi.model.Story;

/**
 * 
 * Types GROUP and ARTIFACT are removed since they are not needed.	
 * @author 
 *
 */
public class EnrichLocation
{
	/** Possible entity types */

	private static final String PERSON = "<wordnet_person_100007846>";
	private static final String LOCATION = "<wordnet_location_100027167>";
	
	/** Default values to connect to the database */
	private static final String TABLE_NAME = "yagoFacts";
	private static final String SUBJECT_NAME = "subject";
	private static final String PREDICATE_NAME = "predict";
	private static final String OBJECT_NAME = "object";
	private static final String TYPE_RELATION = "rdf:type";
	/** Database handler (for YAGO2) */
	private DBHandler yago2db;
	private Connection yago2connection;

	DatabaseManager wikitimesdb;
	
	public EnrichLocation()
	{
		yago2db = new DBHandler();
		wikitimesdb = new DatabaseManager();
		try {
			yago2connection = yago2db.openConnection();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
/**
 * 
 * @param entity
 * @return
 * @throws SQLException
 */
	public String extractType(String entity) throws SQLException
	{
		//if (yago2connection.isClosed()) yago2connection = yago2db.openConnection();
		String type = null;
		// format the string to meet the format of the table: everything is unNormalized apart from the underscores.
		entity = entity.replace(' ', '_');
		entity = entity.replace("'", "\\'");
		String query = "select * from " + TABLE_NAME + " where " + SUBJECT_NAME + " = '<" + entity + ">' and " + PREDICATE_NAME + " = '" + TYPE_RELATION + "';";
		//System.out.println(query);
		PreparedStatement pstm = yago2connection.prepareStatement(query);
		
		ResultSet res = pstm.executeQuery();
		
		// check whether the entity belongs to one of the specified types
		while(res.next())
		{
			String currType = res.getString(OBJECT_NAME);
			if(currType.compareTo(PERSON) == 0) return "PERSON";
			else if(currType.compareTo(LOCATION) == 0) return "LOCATION";
		}
		res.close(); pstm.close();
		return type;
	}
	
	
	public ArrayList<String> getHappenIn(String entity) throws SQLException
	{
		ArrayList<String> locationList = new ArrayList<String> ();
		String happenIn= "<happenedIn>";
		//if (yago2connection.isClosed()) yago2connection = yago2db.openConnection();
		String loc = null;
		entity = entity.replace(' ', '_');
		System.out.println(" query yago " + entity);
		entity = entity.replace("'", "\\'");
		String query = "select * from " + TABLE_NAME + " where " + SUBJECT_NAME + " = '<" + entity + ">' and " + PREDICATE_NAME + " = '" +  happenIn  + "';";
		PreparedStatement pstm = yago2connection.prepareStatement(query);
		System.out.println(query);
		ResultSet res = pstm.executeQuery();
		// check whether the entity belongs to one of the specified types
		while(res.next())
		{
			loc = res.getString(OBJECT_NAME);
			locationList.add(loc);
		}
		res.close(); pstm.close();
		return locationList;
	}
	
	/**
	 * get LOCATION/PERSON type of entity
	 * @param entities
	 * @return map {entity url name --> type}
	 * @throws SQLException
	 */
	public HashMap<String, ArrayList<String>> getEntityTypes (ArrayList<String> entities) throws SQLException {
		HashMap<String, ArrayList<String>> types = new HashMap<String, ArrayList<String>> ();
		if (yago2connection.isClosed()) yago2connection = yago2db.openConnection();
		for (String entity: entities) {
			ArrayList<String> type = new ArrayList<String> ();
			// format the string to meet the format of the table: everything is unNormalized apart from the underscores.
			entity = entity.replace(' ', '_');
			entity = entity.replace("'", "\\'");
			String query = "select * from " + TABLE_NAME + " where " + SUBJECT_NAME + " = '<" + entity + ">' and " + PREDICATE_NAME + " = '" + TYPE_RELATION + "';";
			//System.out.println(query);
			PreparedStatement pstm = yago2connection.prepareStatement(query);
			
			ResultSet res = pstm.executeQuery();
			
			// check whether the entity belongs to one of the specified types
			while(res.next())
			{
				String currType = res.getString(OBJECT_NAME);
				type.add(currType);
			}
			if (type.size()>0)
				types.put(entity, type);
			else {
				System.out.println(entity);
			}
			res.close(); pstm.close();
		}
		//yago2connection.close();
		return types;
	}
	
	/**
	 * extract entities type (LOCATION, PERSON) by Yago2 properties
	 * @param args
	 */
	public static void ResolveEntityType() {
		EnrichLocation lc = new EnrichLocation();
		ArrayList<Entity> entities = lc.wikitimesdb.getEntities();
		int count = 0;
		for (Entity e: entities) {
			count ++;
			if (count %1000==0) System.out.println(count + " entities processed...");
			String title = e.getName();
			try {
				String type = lc.extractType(title);
				if (type!=null) {
					lc.wikitimesdb.updateEntityType(e.getId(), type);
				}
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			
		}
	}
	//by default, the confidence = 0.95 as indicated by yago
	private void updateStoryLocation(String storyId, String loc) {
		wikitimesdb.insertStoryToLocationRelation(Integer.valueOf(storyId), loc, wikitimesdb.YAGO_HAPPEN_RELATION, (float) 0.95);
	}
	/**
	 * propagate location for all events 
	 * @param storyId
	 * @param type
	 */
	private void updateEventLocation(String storyId, String loc) {
		ArrayList<Event> events = wikitimesdb.getEventsByStory(Integer.valueOf(storyId).intValue(), wikitimesdb.METHOD_WCEP_MANUAL_ANNOTATION, 1);
		System.out.println(events.size() + " events propagated..");
		for (Event e: events) {
			wikitimesdb.insertEventToLocationRelation(Integer.valueOf(e.getEventId()), loc, wikitimesdb.PROPAGATE_STORY_YAGO_LOCATION, (float) 0.95);
		}
	}
	
	/**
	 * extract location by looking into <happenedIn> of Yago2
	 * then propagate events' location to the location of detected stories
	 */
	public void storyLocationExtraction() {
		ArrayList<Story> stories = wikitimesdb.getStories();
		int count = 0;
		for (Story s: stories) {
			count ++;
			//get yago happenedIn
			if (count %100==0) System.out.println(count + " stories processed...");
			String title = s.getWikipediaUrl().substring(wikitimesdb.conf.getProperty("wiki_url_prefix").length());
			try {
				ArrayList<String> locs = getHappenIn(title);
				System.out.println(locs.size());
				for (String loc: locs) {
					System.out.println(title + "\t" + loc);
					updateStoryLocation(s.getId(), loc.substring(1, loc.length()-2));
					//it is not confident to propagate, dont do it: giang
					//updateEventLocation(s.getId(), loc);
				}
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			
			//propagate location of events to stories in rdf by joining tables
		}
	}
	
	
	/**
	 * Apply heuristic rule to get location of the event
	 * LOC (event) = ENTITY (event) s.t  ENTITY.TYPE = LOCATION and COUNT (ENTITY LOCATION) = 1
	 */
	public void eventLocationExtraction() {
		int found = 0;
		ArrayList<Event> events = wikitimesdb.getEvents();
		System.out.println(events.size() + " events obtained");
		int processed = 0;
		for (Event e: events) {
			processed+=1;
			if (processed %1000 ==0) System.out.println(processed + " events processed.");
			int locationCount = 0;
			String loc = "";
			for (Entity entity: e.getEntities()) {
				if (entity.getType()!=null && entity.getType().equals("LOCATION")) {
					locationCount +=1;
					loc = entity.getName();
				}
			}
			if (locationCount==1) {
				//update location
				found+=1;
				wikitimesdb.insertEventToLocationRelation(Integer.valueOf(e.getEventId()), loc.replace(" ", "_"), wikitimesdb.ONLY_LOCATION, (float) 0.95);
			}
		}
		System.out.println(found + " events having location by heuristics");
	}
	
	//few statistics to check which stories are not in Yago
	public static void getYagoCoverageOfStories() {
		EnrichLocation lc = new EnrichLocation();
		ArrayList<Story> stories = lc.wikitimesdb.getStories();
		ArrayList<String> pages = new ArrayList<String> ();
		for (Story s: stories) pages.add(s.getName());
		try {
			System.out.println("RECALL " + lc.getEntityTypes(pages).size());
		} catch (SQLException e) {
			e.printStackTrace();
		}
		//returns RECALL 253 over wikitimeline data, 20% Wikitimes datas
	}
	public static void main(String[] args) {
//		resolve entity type for every entity in DB
		if (args.length<1) {
			System.out.println("@usage: missing \t '-command' ");
			System.out.println("-command resolveEntityType for resolving entity type ");
			System.out.println("-command enrichLocation for resolving events and stories' locations ");
			System.exit(0);
		}
		
		if (args[1].equals("resolveEntityType"))
			ResolveEntityType();
		
		
		//update location
		EnrichLocation lc = new EnrichLocation(); 
		if (args[1].equals("enrichLocation")) {
			lc.eventLocationExtraction();
			lc.storyLocationExtraction();
		}
		
		
	}
}