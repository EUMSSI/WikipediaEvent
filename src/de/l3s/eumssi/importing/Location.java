package de.l3s.eumssi.importing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import de.l3s.eumssi.dao.PostgreSQL;
import de.l3s.eumssi.model.Story;


public class Location {

	public static Connection con;
	private static final String WIKIREFID = "WikiRefID";
	private static final String STORYID = "StoryID";
	private static final String WIKIURL = "WikipediaURL";
	private static final String WIKIPEDIA = "http://en.wikipedia.org/wiki/";
		
    private PreparedStatement selectwikiStmt;
    private PreparedStatement selectwikiidStmt;
    private PreparedStatement selectStmt;
    private ResultSet resWikiID;
    private ResultSet resWikiURL;
    private ResultSet resID;
	    
	/** Possible entity types */
//		private static final String City = "wikicategory_Cities";
//		private static final String Country = "wordnet_country";
//		private static final String Continent = "wordnet_continent";

	/** Default values to connect to the database */
	private static final String SERVER_URL = "pharos.l3s.uni-hannover.de";
	private static final int SERVER_PORT = 3306;
	private static final String DATABASE_NAME = "YAGO2";
	private static final String USER_NAME = "postgres";
	private static final String PASSWORD = "";
		
	/** Data for making queries */
	private static final String TABLE_NAME = "public.yagoFacts";
	private static final String SUBJECT_NAME = "subject";
	private static final String PREDICATE_NAME = "predicate";
	private static final String OBJECT_NAME = "object";
	private static final String HASWIKIURL = "<hasWikipediaUrl>";
	private static final String HAPPENDIN = "<happenedIn>";
	    
	/** Database handler (for YAGO2) */
	private PostgreSQL database;
		
		
		
	public static void main(String[] args) throws SQLException {
		Location loc = new Location();
		loc.openConnection();
		
		Story story = new Story();
		story.setWikipediaUrl("Syrian_civil_war");
		ArrayList<String> locations = loc.getLocationInfo(story);
		loc.closeConnection();
		
		System.out.println(" ------------------------- ");
		for (String location: locations){
			System.out.println(location);
		}
		
	}

	
	
		
	public Location(){
		
	}
		
	public PostgreSQL openConnection(){
		if (database != null)
			if (database.isConnected())
				return database;
		
		try	{
			database = new PostgreSQL(SERVER_URL, SERVER_PORT, DATABASE_NAME, USER_NAME, PASSWORD);
		}catch(Exception e)	{
			e.printStackTrace();
		}
		
		return database;
	}
	
	
	public void closeConnection(){
		database.disconnect();
	}
	
	
	private ArrayList<String> getLocationInfo(Story story) throws SQLException {
		ArrayList<String> locations = new ArrayList<String>();
		
		String WikiURL = story.getWikipediaUrl();
		try{
			System.out.println("**** Method starts with wikipedia entry: " + WikiURL);
			if (WikiURL.contains("'")){
				WikiURL = WikiURL.replace("'", "''");
				System.out.println("!!!!Correction in name: " +WikiURL);
			} 
			String query = "select object from " + TABLE_NAME + " where " + SUBJECT_NAME + " = '<" + WikiURL + ">' and " + PREDICATE_NAME + " = '" + HAPPENDIN + "';";
			ResultSet res = database.executeQuery(query);
			if (res.next()){
				String location = null;
				while (res.next()) {
					location = res.getString(OBJECT_NAME);					
					location = location.replace ("<","");
					location = location.replace (">","");
					location = WIKIPEDIA.concat(location);
					System.out.println("@@@@ It happened in:" +location);
					locations.add(location);
				} 
			}
		}catch(SQLException sqle) {
	        sqle.printStackTrace();
		}finally{
			database.disconnect();
		}
		return locations;
	}
}
