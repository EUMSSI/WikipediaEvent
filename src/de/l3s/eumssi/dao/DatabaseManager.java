package de.l3s.eumssi.dao;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import de.l3s.eumssi.model.*;




public class DatabaseManager{
	
	public static final String YAGO_HAPPEN_RELATION = "YAGO_HAPPEN_IN_RELATION";
	public static final String PROPAGATE_STORY_YAGO_LOCATION = "PROPAGATE_STORY_YAGO_LOCATION";
	public static final String ONLY_LOCATION = "HEURISTIC_ONLY_LOCATION_ENTITY";
	public static final String METHOD_STORY_FIRST = "STORY_ENTITY_BEGIN_DESCRIPTION";
	public static final String METHOD_EXPERT_MANUAL_ANNOTATION = "EXPERT_MANUAL_ANNOTATION";
	public static final String METHOD_CALAIS = "OPEN_CALAIS_ANNOTATION";
	public static final String METHOD_STORY_SECOND = "STORY_ENTITY_IN_DESCRIPTION";
	public String METHOD_WCEP_MANUAL_ANNOTATION = "WCEP_MANUAL_ANNOTATION";
	public String METHOD_ENRICHMENT_1 = "METHOD_ENRICHMENT_1";
	public static final String STORY_IN_WIKITEXT = "STORY_LINK_IN_WIKITEXT";
	public static final String INFORBOX = "INFOBOX";
	
	
	public Properties conf;
	private Connection connection;
	
	
	public DatabaseManager(){
		connection = null;
		try {
			loadConfiguration();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	public void loadConfiguration(String path) throws FileNotFoundException, IOException{
		conf = new Properties();
		conf.load(new FileInputStream(path));		
	}
	
	public void loadConfiguration() throws FileNotFoundException, IOException{
		conf = new Properties();
//		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
//		conf.load(classLoader.getResourceAsStream("wikitimes.properties"));		
		loadConfiguration("./wikitimes.properties");
	}
	
	
	public Connection openConnection() throws SQLException{
		if (connection != null)
			if (!connection.isClosed())
				return connection;
		try {			
			String driverName = conf.getProperty("driverName");
			Class.forName(driverName);
			String serverName = conf.getProperty("serverName");
			String database = conf.getProperty("database");
			String url = "jdbc:mysql://" + serverName + "/" + database;
			url+= "?useUnicode=true&characterEncoding=utf-8";
			String username = conf.getProperty("dbusername");
			String password = conf.getProperty("dbpassword");
			System.out.print(" connecting to server: " + serverName + "/" + database + " ... ");
			connection = DriverManager.getConnection(url, username, password);
			System.out.println(" connection is successfully established!");			
		} catch (ClassNotFoundException e) {			
			System.out.println(" faild to establish connection to DB!");
			e.printStackTrace();
		} catch (SQLException e) {
			System.out.println(" faild to establish connection to DB!");
			e.printStackTrace();
		}		
		return connection;
	}
	
	public void closeConnection() {
		try {
			if (connection != null) {
				if (!connection.isClosed()){
					connection.close();
					System.out.println(" connection to DB was successfuly closed!");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
    
	 
	public ArrayList<Event> searchEventsByKeyword(String query, String from, String to) {
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
        ArrayList<Event> events = new ArrayList<Event>();
        try{
        	query = "%"+query+"%";
            pstmt = openConnection().prepareStatement("select EventID, Date from Event where Description like ? AND Date>=? and Date<=?");
            pstmt.setString(1,query);
            pstmt.setString(2,from);
            pstmt.setString(3,to);
            result = pstmt.executeQuery();
            while(result.next()){
            	events.add(getEventById(result.getString("EventID")));
            }
        }catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
			closeConnection();
		}
        
        return events;
    }
    
	
	
    /**
     * get the list of events from DB 
     * @param connection
     * @param from
     * @param to
     * @return
     * @throws Exception
     */
    public ArrayList<Event> getEvents(String from, String to)  {
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
        ArrayList<Event> events = new ArrayList<Event>();
        try{
            pstmt = openConnection().prepareStatement("select EventID, Date from Event where Date>=? and Date<=?");
            pstmt.setString(1,from);
            pstmt.setString(2,to);
            result = pstmt.executeQuery();
            while(result.next()){
            	events.add(getEventById(result.getString("EventID")));
            }
        }catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
			closeConnection();
		}
        
        return events;
    }
    
    
    public ArrayList<Event> getEvents()  {
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
        ArrayList<Event> events = new ArrayList<Event>();
        try{
            pstmt = openConnection().prepareStatement("select EventID, Date from Event");
            result = pstmt.executeQuery();
            while(result.next()){
            	events.add(getEventById(result.getString("EventID")));
            }
        }catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
			closeConnection();
		}
        return events;
    }
    
    /**
     * get the list of events that contains entity from DB 
     * @param connection
     * @param from
     * @param to
     * @param entityId
     * @return
     * @throws Exception
     */
    public ArrayList<Event> getEventsByEntity(String entityId, String from, String to)    {
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
        ArrayList<Event> events = new ArrayList<Event>();
        try{
            pstmt = openConnection().prepareStatement("select e.EventID, e.Date from Event e " +
            		" join Event_Entity_Relation r on e.EventID=r.EventID " +
            		" where e.Date>=? and e.Date<=? and r.WikiRefID=?");
            pstmt.setString(1,from);
            pstmt.setString(2,to);
            pstmt.setString(3, entityId);
            result = pstmt.executeQuery();
            while(result.next()){
            	events.add(getEventById(result.getString("EventID")));
            }
        }catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
           
        return events;
    }
    
    
    public ArrayList<Event> getEventsByEntityName(String entityName, String from, String to)    {
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
        ArrayList<Event> events = new ArrayList<Event>();
        try{
        	String query = "%"+entityName+"%";
            pstmt = openConnection().prepareStatement("select e.EventID, e.Date from Event e " +
            		" join Event_Entity_Relation r on e.EventID=r.EventID " +
            		" where e.Date>=? and e.Date<=? and r.Name like ?");
            pstmt.setString(1,from);
            pstmt.setString(2,to);
            pstmt.setString(3, query);
            result = pstmt.executeQuery();
            while(result.next()){
            	events.add(getEventById(result.getString("EventID")));
            }
        }catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
           
        return events;
    }
    
    public HashMap<Integer, ArrayList<Integer>> getStorytoStoryRelation(String relation, float confidence) {
		HashMap<Integer, ArrayList<Integer>> relations = new HashMap<Integer, ArrayList<Integer>> ();
		
		PreparedStatement pstmt = null;
		ResultSet result = null;
		
		try {
			pstmt = openConnection().prepareStatement("select from_StoryID, to_StoryID " +
					"from Story_Story_Relation where relation=? and confidence>=?");
			pstmt.setString(1, relation);
			pstmt.setFloat(2, confidence);
			result = pstmt.executeQuery();
			
			while (result.next()) {
				int fromId = result.getInt("from_StoryID");
				int toId = result.getInt("to_StoryID");
				if (! relations.containsKey(fromId)) relations.put(fromId, new ArrayList<Integer> ());
				relations.get(fromId).add(toId);
			}
			
		}catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
		return relations;
	}
	
    /** get all relations regardless relation type */
    public HashMap<Integer, ArrayList<Integer>> getStorytoStoryRelation( float confidence) {
		HashMap<Integer, ArrayList<Integer>> relations = new HashMap<Integer, ArrayList<Integer>> ();
		PreparedStatement pstmt = null;
		ResultSet result = null;
		try {
			pstmt = openConnection().prepareStatement("select from_StoryID, to_StoryID " +
					"from Story_Story_Relation where confidence>=?");
			pstmt.setFloat(1, confidence);
			result = pstmt.executeQuery();
			
			while (result.next()) {
				int fromId = result.getInt("from_StoryID");
				int toId = result.getInt("to_StoryID");
				if (! relations.containsKey(fromId)) relations.put(fromId, new ArrayList<Integer> ());
				relations.get(fromId).add(toId);
			}
			
		}catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
		return relations;
	}
    
    /**
     * get story story relation of a story
     * @param storyid, confidence
     */
    public ArrayList<Integer> getStorytoStoryRelation(int storyid, float confidence) {
		 ArrayList<Integer> relations = new  ArrayList<Integer> ();
		PreparedStatement pstmt = null;
		ResultSet result = null;
		try {
			pstmt = openConnection().prepareStatement("select to_StoryID " +
					"from Story_Story_Relation where from_StoryID=? and confidence>=?");
			pstmt.setInt(1, storyid);
			pstmt.setFloat(2, confidence);
			result = pstmt.executeQuery();
			
			while (result.next()) {
				int toId = result.getInt("to_StoryID");
				relations.add(toId);
			}
			
		}catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
		return relations;
	}
    /**
     * event list as-is in WCEP before redirection
     * @param storyId
     * @param method
     * @param confidence
     * @return
     */
    public ArrayList<Event> getOriginalEventsByStory(int storyId ) {
    	ArrayList<Event> events = new ArrayList<Event>();
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
        try{
            pstmt = openConnection().prepareStatement("select EventID from Event e join NewsStory s on e.NewsStoryID = s.StoryID " +
            		" where s.StoryID = ?");
            pstmt.setInt(1, storyId);
            result = pstmt.executeQuery();
            while(result.next()){
            	events.add(getEventById(result.getString("EventID")));
            }
        }catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
           
        return events;
    }
    
    public ArrayList<Event> getEventsByEntity(String entityId) {
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
        ArrayList<Event> events = new ArrayList<Event>();
        try{
            pstmt = openConnection().prepareStatement("select e.EventID, e.Date from Event e " +
            		" join Event_Entity_Relation r on e.EventID=r.EventID " +
            		" where r.WikiRefID=?");
            pstmt.setString(1, entityId);
            result = pstmt.executeQuery();
            while(result.next()){
            	events.add(getEventById(result.getString("EventID")));
            }
        }catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
           
        return events;
    }
    
    
    public ArrayList<Event> getEventsByDate(String date){
    	ArrayList<Event> events = new ArrayList<Event>();
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
        try{
            pstmt = openConnection().prepareStatement("select EventID, Date from Event  where Date=? ");
            pstmt.setString(1,date);
            result = pstmt.executeQuery();
            while(result.next()){
            	events.add(getEventById(result.getString("EventID")));
            }
        }catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
           
        return events;
    }
    
    
//    public Story getStoryById(String storyId){
//		Story story = null;
//		PreparedStatement pstmt = null;
//		ResultSet result = null;
//		try {
//			pstmt = openConnection().prepareStatement("SELECT n.Label, n.WikipediaURL FROM NewsStory n join WikiRef w on n.WikiRefID = w.WikiRefID where StoryID=?");
//			pstmt.setString(1,storyId);
//	        result = pstmt.executeQuery();
//	        
//	        if(result.next()){
//	        	story = new Story();
//	        	story.setId(storyId);
//	        	story.setName(result.getString("Label"));
//	        	story.setWikipediaUrl(result.getString("WikipediaURL"));
//	        }
//		} catch (SQLException e) {			
//			e.printStackTrace();
//		}finally{
//			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
//			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
//		}
//		
//		return story;
//	}
    
    public Story getStoryById(String storyId){
		Story story = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;
		try {
			pstmt = openConnection().prepareStatement("SELECT n.Label, n.WikipediaURL FROM NewsStory n where StoryID=?");
			pstmt.setString(1,storyId);
	        result = pstmt.executeQuery();
	        
	        if(result.next()){
	        	story = new Story();
	        	story.setId(storyId);
	        	story.setName(result.getString("Label"));
	        	story.setWikipediaUrl(result.getString("WikipediaURL"));
	        }
		} catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
		
		return story;
	}
    
    
//    public Story getStoryByWikiRefId(String wikiRefId){
//		Story story = null;
//		PreparedStatement pstmt = null;
//		ResultSet result = null;
//		try {
//			pstmt = openConnection().prepareStatement("SELECT n.StoryID, n.Label, w.WikipediaURL FROM NewsStory n join WikiRef w on n.WikiRefID = w.WikiRefID where n.WikiRefID=?");
//			pstmt.setString(1,wikiRefId);
//	        result = pstmt.executeQuery();
//	        
//	        if(result.next()){
//	        	story = new Story();
//	        	story.setId(result.getString("StoryID"));
//	        	story.setName(result.getString("Label"));
//	        	story.setWikipediaUrl(result.getString("WikipediaURL"));
//	        }
//		} catch (SQLException e) {			
//			e.printStackTrace();
//		}finally{
//			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
//			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
//		}
//		
//		return story;
//	}
    
    
    public Category getCategoryById(String categoryId){
    	Category category = null;
    	PreparedStatement pstmt = null;
		ResultSet result = null;
		try {
			pstmt = openConnection().prepareStatement("SELECT Name FROM Category where CategoryID=?");
	        pstmt.setString(1,categoryId);
	        result = pstmt.executeQuery();
	        
	        if(result.next()){
	        	category = new Category();
	        	category.setId(categoryId);
	        	category.setName(result.getString("Name"));
	        }
		}catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
    	
        return category;
    }
    
    
    public Category getCategoryByName(String categoryName){
    	Category category = null;
    	PreparedStatement pstmt = null;
		ResultSet result = null;
		try {
			pstmt = openConnection().prepareStatement("SELECT CategoryID FROM Category where Name=?");
	        pstmt.setString(1,categoryName);
	        result = pstmt.executeQuery();
	        
	        if(result.next()){
	        	category = new Category();
	        	category.setId(result.getString("CategoryID"));
	        	category.setName(categoryName);
	        }
		}catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
    	
        return category;
    }
    
    
    public ArrayList<Entity> getEntitiesOfEvent(String eventId){
    	ArrayList<Entity> entities = new ArrayList<Entity>();
    	PreparedStatement pstmt = null;
		ResultSet result = null;
		try {
			pstmt = openConnection().prepareStatement(
					"select w.WikiRefID, w.Name, w.WikipediaURL, w.type from Event_Entity_Relation r " +
					"join WikiRef w on r.WikiRefID=w.WikiRefID where r.EventID=?");
			pstmt.setString(1,eventId);
			result = pstmt.executeQuery();
			Entity entity = null;
	        while(result.next()){
	        	entity = new Entity();
	        	entity.setId(result.getString("WikiRefID"));
	        	entity.setName(result.getString("Name"));
	        	entity.setWikiURL(result.getString("WikipediaURL"));
	        	entity.setType(result.getString("type"));
	        	entities.add(entity);
	        }
		}catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
        
        return entities;
    }
    
    
    public Entity getEntityById(String entityId){
    	Entity entity = null;
    	PreparedStatement pstmt = null;
		ResultSet result = null;
		try {
			pstmt = openConnection().prepareStatement("select Name, WikipediaURL, type from WikiRef where WikiRefID=?");
	        pstmt.setString(1,entityId);
	        result = pstmt.executeQuery();
	        
	        if(result.next()){
	        	entity = new Entity();
	        	entity.setId(entityId);
	        	entity.setName(result.getString("Name"));
	        	entity.setWikiURL(result.getString("WikipediaURL"));
	        	entity.setType(result.getString("type"));
	        }
		}catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
    	
        return entity;
    }
    
    
    public Entity getEntityByURL(String url){
    	Entity entity = null;
    	PreparedStatement pstmt = null;
		ResultSet result = null;
		try {
			pstmt = openConnection().prepareStatement("select Name, WikiRefID, type from WikiRef where WikipediaURL=?");
	        pstmt.setString(1, url);
	        result = pstmt.executeQuery();
	        
	        if(result.next()){
	        	entity = new Entity();
	        	entity.setId(result.getString("WikiRefID"));
	        	entity.setName(result.getString("Name"));
	        	entity.setWikiURL(url);
	        	entity.setType(result.getString("type"));
	        }
		}catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
    	
        return entity;
    }
    
    
    
    public Reference getReferenceById(String referenceId){
    	Reference reference = null;
    	PreparedStatement pstmt = null;
		ResultSet result = null;
		try {
			pstmt = openConnection().prepareStatement("SELECT URL, SourceName FROM Source where SourceID=?");
	        pstmt.setString(1,referenceId);
	        result = pstmt.executeQuery();
	        
	        if(result.next()){
	        	reference = new Reference();
	        	reference.setId(referenceId);
	        	reference.setUrl(result.getString("URL"));
	        	reference.setSource(result.getString("SourceName"));
	        }
		}catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
    	
        return reference;
    }
    
    
    
    public Reference getReferenceByURL(String url){
    	Reference reference = null;
    	PreparedStatement pstmt = null;
		ResultSet result = null;
		try {
			pstmt = openConnection().prepareStatement("SELECT SourceID, SourceName FROM Source where URL=?");
	        pstmt.setString(1,url);
	        result = pstmt.executeQuery();
	        
	        if(result.next()){
	        	reference = new Reference();
	        	reference.setId(result.getString("SourceID"));
	        	reference.setUrl(url);
	        	reference.setSource(result.getString("SourceName"));
	        }
		}catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
    	
        return reference;
    }
    
    
    public Event getEventById(String eventid) {
    	Event event = null;
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
       try{
    	    pstmt = openConnection().prepareStatement("select EventID, Date, Description, AnnotatedDescription from Event where EventID=?");
            pstmt.setString(1,eventid);
            result = pstmt.executeQuery();
            if(result.next()){
                event = new Event();
                event.setId(eventid);
                event.setDate(result.getDate("Date"));
                event.setDescription(result.getString("Description"));
                event.setAnnotatedDescription(result.getString("AnnotatedDescription"));
//                String storyid = result.getString("NewsStoryID");
//                String catid = result.getString("CategoryID");                
//                String[] source_ids = result.getString("Sources").split("\\$");
                
                //get story information
//                if(!storyid.equals("0")){
//                	Story story = getStoryById(storyid);
//                	if (story != null)  event.setStory(story);
//                }
                event = getStoryOfEvent(event, this.METHOD_WCEP_MANUAL_ANNOTATION, 1);
                
                
                //get category information
               	Category category = getCategoryOfEvent(Integer.valueOf(eventid), this.METHOD_WCEP_MANUAL_ANNOTATION, 1);
                if (category != null) event.setCategory(category);
                
                //get entities                
                ArrayList<Entity> entities = getEntitiesOfEvent(eventid);
                if (!entities.isEmpty()){
                	for (Entity entity: entities){
                		 event.addEntity(entity);
                	}
                }
                //get references
                for(Reference ref: getReferencesOfEvent(Integer.valueOf(eventid))){
                	if (ref!=null) 
                		event.addReference(ref);
                }
                
                //get location
                
            }
        }catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
           
        return event;
    }
	
    
    public Event getStoryOfEvent(Event event, String method, float confidence) {
		Story story = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;
		try {
			pstmt = openConnection().prepareStatement("SELECT StoryID, confidence FROM Event_Story_Relation WHERE EventID=? AND Method=? AND confidence>= ?");
			pstmt.setInt(1, Integer.valueOf(event.getEventId()));
			pstmt.setString(2, method);
			pstmt.setFloat(3, confidence);
	        result = pstmt.executeQuery();
		        
	        if(result.next()){
	        	story = getStoryById(Integer.valueOf(result.getInt("StoryID")).toString());
	        	event.setStory(story);
	        	event.setStoryRelationConfidence(result.getFloat("confidence"));
	        }
		} catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
		
		return event;
	}
    
    
    public Category getCategoryOfEvent(int eventId, String method, float confidence){
    	Category category = null;
    	PreparedStatement pstmt = null;
		ResultSet result = null;
		try {
			pstmt = openConnection().prepareStatement(
					"select CategoryID from Event_Category_Relation where EventID=? AND Method=? AND confidence >=? ");
			pstmt.setInt(1, eventId);
			pstmt.setString(2, method);
			pstmt.setFloat(3, confidence);
			result = pstmt.executeQuery();
			
	        if(result.next()){
	        	category = getCategoryById((result.getString("CategoryID")));
	        }
		}catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
        
        return category;
    }
    
    
    public ArrayList<Reference> getReferencesOfEvent(int eventId){
    	ArrayList<Reference> references = new ArrayList<Reference>();
    	 PreparedStatement pstmt = null;
         ResultSet result = null;
         try{
             pstmt = openConnection().prepareStatement("select SourceID from Event_Source_Relation where EventID=? ");
             pstmt.setInt(1, eventId);
             result = pstmt.executeQuery();
             while(result.next()){
            	 references.add(getReferenceById((result.getString("SourceID"))));
             }
         }catch (SQLException e) {			
 			e.printStackTrace();
 		}finally{
 			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
 			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
 		}
         return references;
    }
    
 // this method returns an updated Event object if a relation is found
    // the method then sets the story object AND the confidence score
    public Event getBelongsToRelation(Event event) {
		Story story = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;
		float minConfidence =  Float.valueOf(conf.getProperty("event_story_confidence_min"));
		try {
			pstmt = openConnection().prepareStatement("SELECT StoryID, confidence FROM Event_Story_Relation WHERE EventID=?");
			pstmt.setString(1, event.getEventId());
	        result = pstmt.executeQuery();
		        
	        if(result.next()){
	        	if (result.getFloat("confidence") >= minConfidence){
		        	story = getStoryById(Integer.valueOf(result.getInt("StoryID")).toString());
		        	event.setStory(story);
		        	event.setStoryRelationConfidence(result.getFloat("confidence"));
	        	}
	        }
		} catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
		
		return event;
	}
    
    
    public Event getEventByDateAndDescription(Date date, String description) {
    	Event event = null;
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
       try{
    	    pstmt = openConnection().prepareStatement("select EventID from Event where date=? and description=?");
            pstmt.setDate(1, date);
            pstmt.setString(2, description);
            result = pstmt.executeQuery();
            if(result.next()){
                event = getEventById(result.getString("EventID"));
            }
        }catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
           
        return event;
    }
    
    
    
    
    /* update to use the new func which covers case when method is null when 'method' is regardless 
    public ArrayList<Event> getEventsByStory(int storyId, String method, float confidence) {
   	 	
    	ArrayList<Event> events = new ArrayList<Event>();
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
        try{
            pstmt = openConnection().prepareStatement("select EventID from Event_Story_Relation where StoryID=? AND Method=? AND confidence >=? ");
            pstmt.setInt(1, storyId);
            pstmt.setString(2, method);
            pstmt.setFloat(3, confidence);
            result = pstmt.executeQuery();
            while(result.next()){
            	events.add(getEventById(result.getString("EventID")));
            }
        }catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
           
        return events;
    }*/
	
    /**
     * @param storyId
     * @param method: when method =null, returns all events without method check
     * @param confidence
     * @return
     */
    public ArrayList<Event> getEventsByStory(int storyId, String method, float confidence) {
   	 	
    	ArrayList<Event> events = new ArrayList<Event>();
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
        try{
            pstmt = openConnection().prepareStatement("select EventID from Event_Story_Relation where StoryID=? AND Method=? AND confidence >=? ");
            if (method==null)
            	pstmt = openConnection().prepareStatement("select EventID from Event_Story_Relation where StoryID=? AND confidence >=? ");
            pstmt.setInt(1, storyId);
            if (method!=null) pstmt.setString(2, method);
            if (method!=null) pstmt.setFloat(3, confidence);
            else pstmt.setFloat(2, confidence);
            result = pstmt.executeQuery();
            while(result.next()){
            	events.add(getEventById(result.getString("EventID")));
            }
        }catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
           
        return events;
    }
	
	
    
    
    public Category storeCategory(String name){
        Category category = null;
        PreparedStatement pstmt = null;
        ResultSet result = null;
            try {
                if(name != null && !name.isEmpty()){
                     pstmt = openConnection().prepareStatement("insert into Category(Name) values(?)");
                     pstmt.setString(1,name);
                     int affectedRow = pstmt.executeUpdate();
                     if(affectedRow == 1) {
                         category = getCategoryByName(name);
                     }
                }
            }catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
        return category;
    }
    
    
    
    
    public Entity storeEntity(String name, String url){
		Entity entity = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;


		try {
			if (url != null && !url.isEmpty()) {
				if (!url.startsWith("http"))
					url = conf.getProperty("wiki_url_prefix") + url;
				name = name.replaceAll("[^\\w\\s]", "");
				
				pstmt = openConnection()
						.prepareStatement(
								"insert into WikiRef(Name,WikipediaURL) values(?,?)");
				pstmt.setString(1, name);
				pstmt.setString(2, url);
				int affectedRow = pstmt.executeUpdate();
				if (affectedRow == 1) {
					entity = getEntityByURL(url);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("Entity name: " +  name + ", URL: " + url);
		} finally {
			if (result != null) try { result.close(); } catch (SQLException e) { e.printStackTrace(); }
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) { e.printStackTrace(); }
		}
		return entity;
    }
    
    
    
    
//    public Story storeNewsStory(String label, int wikiRefId){
//        Story story = null;
//        PreparedStatement pstmt = null, pstmt2 = null;
//        ResultSet result = null;
//        
//            try { 
//                if(label != null && !label.isEmpty()){
//                	pstmt = openConnection().prepareStatement("insert into NewsStory(Label,WikiRefID) values(?,?)");
//                    pstmt.setString(1,label);
//                    pstmt.setInt(2,wikiRefId);
//                    int affectedRow = pstmt.executeUpdate();
//                    if(affectedRow == 1) {
//                        story = getStoryByWikiRefId(String.valueOf(wikiRefId));
//                    }else{
//                    	System.out.println(" ERROR: failed to insert news story !!!" );
//                    }
//                } 
//            } catch (SQLException e) {			
//    			e.printStackTrace();
//    		}finally{
//    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
//    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
//    			if (pstmt2 != null)  try { pstmt2.close();  } catch (SQLException e) {e.printStackTrace();}
//    		}
//        return story;
//    }
    
    public Story storeNewsStory(String label, String url){
        Story story = null;
        PreparedStatement pstmt = null, pstmt2 = null;
        ResultSet result = null;
        
            try { 
                if(label != null && !label.isEmpty()){
                	pstmt = openConnection().prepareStatement("insert into NewsStory(Label, WikipediaURL) values(?,?)");
                    pstmt.setString(1, label);
                    pstmt.setString(2, url);
                    int affectedRow = pstmt.executeUpdate();
                    if(affectedRow == 1) {
                        story = getStoryByURL(url);
                    }else{
                    	System.out.println(" ERROR: failed to insert news story !!!" );
                    }
                } 
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt2 != null)  try { pstmt2.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
        return story;
    }
    
    
    public boolean insertStoryToCategoryRelation(int storyId, int categoryId, String method, float confidence){
        boolean done = false;
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
            try {
                pstmt = openConnection().prepareStatement(
                		"insert ignore into Story_Category_Relation(StoryID,CategoryID,Method,confidence) " +
                		"values(?,?,?,?)");
                pstmt.setInt(1, storyId);
                pstmt.setInt(2, categoryId);
                pstmt.setString(3, method);
                pstmt.setFloat(4, confidence);
	            int affectedRow = pstmt.executeUpdate();
	            if(affectedRow == 1) {
	            	done = true;
	            }
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
        return done;
    }
    
    
    public Reference storeReference(String sourcename, String url, String publisheddate, String content){
   	 	Reference reference = null;
        PreparedStatement pstmt = null;
        ResultSet result = null;
           try {
           	//first, remove all duplicated URL in the DB
               if(url != null && !url.isEmpty()){
	               pstmt = openConnection().prepareStatement("insert into Source(SourceName,URl,PublishedDate,Content) values(?,?,?,?)");
	               pstmt.setString(1,sourcename);
	               pstmt.setString(2,url);
	               pstmt.setString(3,publisheddate);
	               pstmt.setString(4,content);
	               int affectedRow = pstmt.executeUpdate();
	               if(affectedRow == 1) {
	                   reference = getReferenceByURL(url);
	               }   
               }  
           } catch (SQLException e) {			
   			e.printStackTrace();
   		}finally{
   			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
   			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
   		}
       return reference;
   }
    
    
//    public int insertEvent(String description, String newsstory, String categoryname, Date date, ArrayList<String> storeurl, int wikiid){
//        int eventid = 0;
//        PreparedStatement pstmt = null;
//        ResultSet result = null;
//            try {
//                int storyid = 0;
//                int catid = 0;
//                String sourceids = "";
//                if(newsstory != null && !newsstory.isEmpty()){
//                    pstmt = openConnection().prepareStatement("select StoryID from NewsStory where WikiRefID=?");  
//                    // If I have to change that because of newsStory(same but different) then check for WikiRefID instead of Label.
//                    pstmt.setInt(1,wikiid);
//                    result = pstmt.executeQuery();
//                    if(result.next()){
//                        storyid = result.getInt("StoryID");
//                    }   
//                }
//                if(categoryname != null && !categoryname.isEmpty()){
//                    pstmt = openConnection().prepareStatement("select CategoryID from Category where Name=?");
//                    pstmt.setString(1,categoryname);
//                    result = pstmt.executeQuery();
//                    if(result.next()){
//                        catid = result.getInt("CategoryID");
//                    } 
//                }
//                
//                for(String s : storeurl){
//                    if(s!=null && !s.isEmpty()){
//                        Reference ref = getReferenceByURL(s); 
//                        sourceids = sourceids+" $ "+ ref.getId();
//                    }
//                }
//                if(!date.equals("0000-00-00")){    
//                    pstmt = openConnection().prepareStatement("insert ignore into Event(Description,NewsStoryID,CategoryID,Date,Sources) values(?,?,?,?,?)");
//                    pstmt.setString(1,description);
//                    pstmt.setInt(2,storyid);
//                    pstmt.setInt(3, catid);
//                    pstmt.setDate(4,date);
//                    pstmt.setString(5, sourceids);
//                    
//                    pstmt.executeUpdate();
//                    pstmt = openConnection().prepareStatement("select EventID from Event where NewsStoryID=? and CategoryID=? and Date=? and Description=? ");
//                    pstmt.setInt(1,storyid);
//                    pstmt.setInt(2,catid);
//                    pstmt.setDate(3,date);
//                    pstmt.setString(4, description);
//                    result = pstmt.executeQuery();
//                    if(result.next()){
//                        eventid = result.getInt("EventID");
//                    }
//                }
//            } catch (SQLException e) {			
//    			e.printStackTrace();
//    		}finally{
//    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
//    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
//    		}
//        return eventid;
//    }
    
//    private void storeEvent(Event event){
//        PreparedStatement pstmt = null;
//        ResultSet result = null;
//            try {
//                    pstmt = openConnection().prepareStatement("insert ignore into Event(Date,Description, AnnotatedDescription, NewsStoryID,CategoryID,Sources) values(?,?,?,?,?,?)");
//                    pstmt.setDate(1, event.getDate());
//                    pstmt.setString(2, event.getDescription());
//                    pstmt.setString(3, event.getAnnotatedDescription());
//                    
//                    if (event.getStory() == null)
//                    	pstmt.setInt(4, 0);
//                    else
//                    	pstmt.setInt(4, Integer.valueOf(event.getStory().getId()));
//                    
//                    if (event.getCategory() == null)
//                    	pstmt.setInt(5, 0);
//                    else
//                    	pstmt.setInt(5, Integer.valueOf(event.getCategory().getId()));
//                    
//                    pstmt.setString(6, getSourcesIdList(event.getReferences()));
//                    
//                    int affectedRaws = pstmt.executeUpdate();
//                    
//                    if (affectedRaws > 0){
//                    	
//                    	Event newEvent = getEventByDateAndDescription(event.getDate(), event.getDescription());
//                    	
//                    	for (Entity entity : event.getEntities()){
//                    		insertEventToEntityRelation(newEvent.getEventId(), entity.getId());
//                    	}
//                    	
//                    	if (newEvent.getStory() != null){
//                    		insertEventToStoryRelation(Integer.valueOf(newEvent.getEventId()), 
//                    									Integer.valueOf(newEvent.getStory().getId()), 
//                    										Float.valueOf(1));
//                    	}
//                    }
//	            } catch (SQLException e) {			
//	    			e.printStackTrace();
//	    		}finally{
//	    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
//	    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
//	    		}
//    }
    
    
    private void storeEvent(Event event){
        PreparedStatement pstmt = null;
        ResultSet result = null;
            try {
                    pstmt = openConnection().prepareStatement("insert ignore into Event(Date,Description, AnnotatedDescription, NewsStoryID) values(?,?,?,?)");
                    pstmt.setDate(1, event.getDate());
                    pstmt.setString(2, event.getDescription());
                    pstmt.setString(3, event.getAnnotatedDescription());
                    if (event.getStory() == null)
                    	pstmt.setInt(4, 0);
                    else
                    	pstmt.setInt(4, Integer.valueOf(event.getStory().getId()));
                    
                    int affectedRaws = pstmt.executeUpdate();
                    
                    if (affectedRaws > 0){
                    	String eventId = getEventByDateAndDescription(event.getDate(), event.getDescription()).getEventId();
                    	if (event.getStory() != null){
                    		insertEventToStoryRelation(Integer.valueOf(eventId), 
                    									Integer.valueOf(event.getStory().getId()),
                    									this.METHOD_WCEP_MANUAL_ANNOTATION,
                    									Float.valueOf(1));
                    	}
                    	
                    	for (Entity entity : event.getEntities()){
                    		insertEventToEntityRelation(Integer.valueOf(eventId), 
                    									Integer.valueOf(entity.getId()),
														this.METHOD_WCEP_MANUAL_ANNOTATION,
														Float.valueOf(1));
                    	}
                    	
                    	 if (event.getCategory() != null){
                    		 insertEventToCategoryRelation(Integer.valueOf(eventId), 
                    				 					   Integer.valueOf(event.getCategory().getId()),
														   this.METHOD_WCEP_MANUAL_ANNOTATION,
														   Float.valueOf(1));
                    	 }
                    	
                    	 //sources
                    	 if(event.getReferences()!=null){
                    		 if(!event.getReferences().isEmpty()){
                    			 for (Reference ref: event.getReferences()){
                    				 insertEventToSourceRelation(Integer.valueOf(eventId), 
                    						 					 Integer.valueOf(ref.getId()),
																 this.METHOD_WCEP_MANUAL_ANNOTATION,
																 Float.valueOf(1));
                    			 }
                    		 }
                    	 }
                    	 
                    }
                    
	            } catch (SQLException e) {			
	    			e.printStackTrace();
	    		}finally{
	    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
	    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
	    		}
    }
    
    
    public boolean insertEventToEntityRelation(int eventId, int entityId, String method, float confidence){
        boolean done = false;
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
        try {
                pstmt = openConnection().prepareStatement(
                		"insert ignore into Event_Entity_Relation(EventID, WikiRefID, Method, confidence) " +
                		"values(?,?,?,?)");
                pstmt.setInt(1, eventId);
                pstmt.setInt(2, entityId);
                pstmt.setString(3, method);
                pstmt.setFloat(4, confidence);
                int affectedRow = pstmt.executeUpdate();
                if(affectedRow == 1) {
                	done = true;
                } 
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
        return done;
    }
    
    public boolean insertEventToCategoryRelation(int eventId, int categoryId, String method, float confidence){
        boolean done = false;
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
        try {
                pstmt = openConnection().prepareStatement(
                		"insert ignore into Event_Category_Relation(EventID, CategoryID, Method, confidence) " +
                		"values(?,?,?,?)");
                pstmt.setInt(1, eventId);
                pstmt.setInt(2, categoryId);
                pstmt.setString(3, method);
                pstmt.setFloat(4, confidence);
                int affectedRow = pstmt.executeUpdate();
                if(affectedRow == 1) {
                	done = true;
                } 
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
        return done;
    }
    
    public boolean insertEventToSourceRelation(int eventId, int sourceId, String method, float confidence){
        boolean done = false;
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
        try {
                pstmt = openConnection().prepareStatement(
                		"insert ignore into Event_Source_Relation(EventID, SourceID, Method, confidence) " +
                		"values(?,?,?,?)");
                pstmt.setInt(1, eventId);
                pstmt.setInt(2, sourceId);
                pstmt.setString(3, method);
                pstmt.setFloat(4, confidence);
                int affectedRow = pstmt.executeUpdate();
                if(affectedRow == 1) {
                	done = true;
                } 
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
        return done;
    }
    
    
    public boolean deleteEventToEntityRelation(String eventId){
        boolean done = false;
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
        try {
                pstmt = openConnection().prepareStatement("delete from Event_Entity_Relation where EventId=? ");
                pstmt.setInt(1, Integer.valueOf(eventId));
                int affectedRow = pstmt.executeUpdate();
                if(affectedRow > 0) {
                	done = true;
                } 
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
        return done;
    }
    
    public boolean deleteEventToCategoryRelation(String eventId){
        boolean done = false;
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
        try {
                pstmt = openConnection().prepareStatement("delete from Event_Category_Relation where EventId=? ");
                pstmt.setInt(1, Integer.valueOf(eventId));
                int affectedRow = pstmt.executeUpdate();
                if(affectedRow > 0) {
                	done = true;
                } 
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
        return done;
    }
    
    
    public boolean deleteEventToSourceRelation(String eventId){
        boolean done = false;
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
        try {
                pstmt = openConnection().prepareStatement("delete from Event_Source_Relation where EventId=? ");
                pstmt.setInt(1, Integer.valueOf(eventId));
                int affectedRow = pstmt.executeUpdate();
                if(affectedRow > 0) {
                	done = true;
                } 
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
        return done;
    }
    
    
    public boolean deleteEventToLocationRelation(String eventId){
        boolean done = false;
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
        try {
                pstmt = openConnection().prepareStatement("delete from Event_Location_Relation where eventId=? ");
                pstmt.setInt(1, Integer.valueOf(eventId));
                int affectedRow = pstmt.executeUpdate();
                if(affectedRow > 0) {
                	done = true;
                } 
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
        return done;
    }
    
    public String getSourcesIdList(ArrayList<Reference> references){
    	String string = "";
    	for (Reference ref: references){
    		string+= " $ "+ ref.getId();
    	}
    	return string;
    }
    
    
    
    
    // This method inserts a new event into DB.. 
    // it firsts inserts news story, entities, category and/or references if not exists before creating the event record 
    public void addNewEvent(Event event)	{
		
    	if(event.getCategory() != null){
    		// Insert into Category table
    		Category category = getCategoryByName(event.getCategory().getName());
    		if (category == null){
    			category = storeCategory(event.getCategory().getName());
    			System.out.println(" a new category was addedd: " + category.getName());
    		}
	    	event.setCategory(category);
    	}
		
		if(event.getStory() != null) {
			// Add News Story to DB
			// For each News Story, there should be entry in the WikiRef table
			// so first insert into WikiRef table
//			Entity storyEntity = getEntityByURL(conf.getProperty("wiki_url_prefix") + event.getStory().getWikipediaUrl()); 
//			if (storyEntity == null){// this means story doesn't exist in db yet
//				System.out.println(" didn't find entity with URL: " + conf.getProperty("wiki_url_prefix") + event.getStory().getWikipediaUrl());
//				storyEntity = storeEntity(event.getStory().getName(), event.getStory().getWikipediaUrl());
//				System.out.println(" a new story entity was added: " + storyEntity.getWikiURL());
//			}
				
			// then add news story into NewsStroy table
			Story story = getStoryByURL(conf.getProperty("wiki_url_prefix") + event.getStory().getWikipediaUrl()); 
			if (story == null){
				System.out.println(" didn't find story with URL: " + conf.getProperty("wiki_url_prefix") + event.getStory().getWikipediaUrl());
				story = storeNewsStory(event.getStory().getName(), conf.getProperty("wiki_url_prefix") + event.getStory().getWikipediaUrl());
				if (story != null){
					System.out.println(" a new news story object was added: " + story.getName());
				}
			}
			event.setStory(story);
		}
	
		
		//insert into StorycCategoryRelation table 
		if (event.getCategory() != null && event.getStory() != null){
			insertStoryToCategoryRelation(Integer.valueOf(event.getStory().getId()), 
					Integer.valueOf(event.getCategory().getId()), this.METHOD_WCEP_MANUAL_ANNOTATION, 1);
		}
		
		
		//insert into source table
		ArrayList<Reference> references;
		if(event.getReferences().size() != 0) {
			references = new ArrayList<Reference>();
			for(int i=0; i<event.getReferences().size(); i++) {
				Reference ref = getReferenceByURL(event.getReferences().get(i).getUrl());
				if (ref == null){
					ref = storeReference(event.getReferences().get(i).getSource(), event.getReferences().get(i).getUrl(), "1993-01-01", "None");
					System.out.println(" a new reference was added: " + ref.getUrl());
				}
				references.add(ref);
			}
			event.setReferences(references);
		}
		
		//insert entities into WikiRef Table
		ArrayList<Entity> entities;
		if(event.getEntities().size() != 0) {
			entities = new ArrayList<Entity>();
			for (int i=0; i< event.getEntities().size(); i++){
				Entity entity = getEntityByURL(conf.getProperty("wiki_url_prefix") + event.getEntities().get(i).getWikiURL());
				if(entity == null){
					entity = storeEntity(event.getEntities().get(i).getName(), event.getEntities().get(i).getWikiURL());
					if (entity!=null) System.out.println(" a new entity was added: " + entity.getWikiURL());
				}
				if (entity!=null) entities.add(entity);
			}
			event.setEntities(entities);
		}
		
		// finally, persist the event object and its links to entities, story, and sources
		System.out.println(" is about to store event: \n" + event.toString());
		Event foundEvent = getEventByDateAndDescription(event.getDate(), event.getDescription());
		if (foundEvent!=null){
			System.out.println(" found event in DB with same date and description (EventID=" + foundEvent.getEventId() + ")... will skip storing this one!!!");
		}else{
			storeEvent(event);
			System.out.println(" a new event was added!" );
		}
		
	}
    
    /**
     * delete a story, note that all foreign key to the story should be set on delete cascade
     * UPDATE: set delete cascade is risky, better control deletation here
     * @param s
     * @return true/false to indicate success of that procedure
     */
    public void deleteStory(Story s) {
    	PreparedStatement pstmt = null;
		try {
			pstmt = openConnection().prepareStatement(
					"DELETE FROM NewsStory WHERE StoryID=?");
			pstmt.setInt(1, s.getIdAsNum());
			pstmt.execute();
			
			//delete all relations to this story
			deleteEventToStoryRelationByStoryID(null, s.getId());
			deleteStoryCategoryRelationByStoryID(s.getId());
			deleteStoryStoryRelationByStoryID(s.getId());
			deleteStoryLocationRelationByStoryID(s.getId());
			
		}catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
    }
    
    
    
    public boolean deleteEvent(Event event){
    	boolean deleted = false;
    	
    	//1. entities
    	//1.1 remove event_entity relations
    	if (event.getEntities().size() > 0){
	    	if (!deleteEventToEntityRelation(event.getEventId())){
	    		System.out.println("ERROR: failed to delete event_entity relations!!");
	    		return false;
	    	}
	    	
    	}
    	
    	//2. stories
    	//2.1 remove event_story relations     	
    	if (event.getStory()!=null){
    		if (!deleteEventToStoryRelationByStoryID(event.getEventId(), event.getStory().getId())){
        		System.out.println("ERROR: failed to delete event_story relations!!");
        		return false;
        	}
        	//if story has no event, remove it
        	if (getEventsByStory(Integer.valueOf(event.getStory().getId()), null, 0).size() == 0){
        		deleteStory(event.getStory());
        	}	
    	}
    	
    	
    	//3. categories
    	//3.1 remove event_category relations
    	if (event.getCategory()!=null){
    		if (!deleteEventToCategoryRelation(event.getEventId())){
	    		System.out.println("ERROR: failed to delete event_category relations!!");
	    		return false;
	    	}
    	}
    	
    	
    	//4. sources
    	//4.1 remove event_source relations
    	if (event.getReferences().size() > 0){
    		if (!deleteEventToSourceRelation(event.getEventId())){
	    		System.out.println("ERROR: failed to delete event_reference relations!!");
	    		return false;
	    	}
    	}
    	
    	//5. locations
    	//5.1 remove event_location relations
		if (!deleteEventToLocationRelation(event.getEventId())){
    		System.out.println("No event_location relations deleted!!");
    	}
    	//6. remove event
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
        try {
                pstmt = openConnection().prepareStatement("delete from Event where EventId=? ");
                pstmt.setInt(1, Integer.valueOf(event.getEventId()));
                int affectedRow = pstmt.executeUpdate();
                if(affectedRow == 1) {
                	deleted = true;
                } 
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
    	
    	return deleted;
    }
    
    
    
	private void deleteEntity(Entity entity) {
		// TODO Auto-generated method stub
	}

	
	
	public void insertEventToStoryRelation(int eventId, int storyId, String method, float confidence){
    	PreparedStatement pstmt = null;
		ResultSet result = null;
            try {
                    pstmt = openConnection().prepareStatement(
                    		"Insert ignore into Event_Story_Relation(EventID, StoryID, Method, confidence) " +
                    		" values (?,?,?,?) ");
                    pstmt.setInt(1, eventId);
                    pstmt.setInt(2, storyId);
                    pstmt.setString(3, method);
                    pstmt.setFloat(4, confidence);
                    pstmt.executeUpdate();
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
    }
    
	
    
    
    public boolean deleteEventToStoryRelationByEventID(String eventId){
    	boolean done = false;
    	PreparedStatement pstmt = null;
		ResultSet result = null;
            try {
                    pstmt = openConnection().prepareStatement("delete from Event_Story_Relation where EventID=? ");
                    pstmt.setInt(1, Integer.valueOf(eventId));
                    int affectedRow = pstmt.executeUpdate();
                    if(affectedRow > 0) {
                    	done = true;
                    } 
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
            return done;
    }
    
    
   /**
    * if EventID = null-> remove all relation of that storyID 
    * @param EventID
    * @param StoryID
    * @return
    */
    public boolean deleteEventToStoryRelationByStoryID(String EventID, String StoryID){
    	boolean done = false;
    	PreparedStatement pstmt = null;
		ResultSet result = null;
            try {
            		if (EventID!=null) {
            			pstmt = openConnection().prepareStatement("delete from Event_Story_Relation where EventID=? and StoryID=? ");
            			pstmt.setInt(1, Integer.valueOf(EventID));
            			pstmt.setInt(2, Integer.valueOf(StoryID));
            		}
            		else {
            			pstmt = openConnection().prepareStatement("delete from Event_Story_Relation where StoryID=? ");
            			pstmt.setInt(1, Integer.valueOf(StoryID));
            		}
                    int affectedRow = pstmt.executeUpdate();
                    if(affectedRow > 0) {
                    	done = true;
                    } 
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
            return done;
    }
    
    public boolean deleteStoryCategoryRelationByStoryID(String StoryID){
    	boolean done = false;
    	PreparedStatement pstmt = null;
		ResultSet result = null;
            try {
                    pstmt = openConnection().prepareStatement("delete from Story_Category_Relation where StoryID=? ");
                    pstmt.setInt(1, Integer.valueOf(StoryID));
                    int affectedRow = pstmt.executeUpdate();
                    if(affectedRow > 0) {
                    	done = true;
                    } 
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
            return done;
    }
    
    public boolean deleteStoryStoryRelationByStoryID(String StoryID){
    	boolean done = false;
    	PreparedStatement pstmt = null;
		ResultSet result = null;
            try {
                    pstmt = openConnection().prepareStatement("delete from Story_Story_Relation where from_StoryID=? OR to_StoryID=?");
                    pstmt.setInt(1, Integer.valueOf(StoryID));
                    pstmt.setInt(2, Integer.valueOf(StoryID));
                    int affectedRow = pstmt.executeUpdate();
                    if(affectedRow > 0) {
                    	done = true;
                    } 
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
            return done;
    }
    
    
    public boolean deleteStoryLocationRelationByStoryID(String StoryID){
    	boolean done = false;
    	PreparedStatement pstmt = null;
		ResultSet result = null;
            try {
                    pstmt = openConnection().prepareStatement("delete from Story_Location_Relation where storyID=?");
                    pstmt.setInt(1, Integer.valueOf(StoryID));
                    int affectedRow = pstmt.executeUpdate();
                    if(affectedRow > 0) {
                    	done = true;
                    } 
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
            return done;
    }
    
//    public Story getStoryByURL(String wikiURL) {
//    	Story story = null;
//		PreparedStatement pstmt = null;
//		ResultSet result = null;
//		
//		try {
//			pstmt = openConnection().prepareStatement("SELECT n.StoryID, n.Label, w.WikipediaURL FROM NewsStory n join WikiRef w on n.WikiRefID = w.WikiRefID where w.WikipediaURL=?");
//			pstmt.setString(1, wikiURL);
//	        result = pstmt.executeQuery();
//	        
//	        if(result.next()){
//	        	story = new Story();
//	        	story.setId(result.getString("StoryID"));
//	        	story.setName(result.getString("Label"));
//	        	story.setWikipediaUrl(result.getString("WikipediaURL"));
//	        }
//		} catch (SQLException e) {			
//			e.printStackTrace();
//		}finally{
//			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
//			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
//		}
//		return story;
//    }
    
    
    public Story getStoryByURL(String wikiURL) {
    	Story story = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;
		
		try {
			pstmt = openConnection().prepareStatement("SELECT StoryID, Label FROM NewsStory where WikipediaURL=?");
			pstmt.setString(1, wikiURL);
	        result = pstmt.executeQuery();
	        
	        if(result.next()){
	        	story = new Story();
	        	story.setId(result.getString("StoryID"));
	        	story.setName(result.getString("Label"));
	        	story.setWikipediaUrl(wikiURL);
	        }
		} catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
		return story;
    }
    
    public void insertStoryToStoryRelation(int storyId1, int storyId2, String relation, float confidence){
    	PreparedStatement pstmt = null;
		ResultSet result = null;
            try {
                    pstmt = openConnection().prepareStatement("Insert ignore into Story_Story_Relation(from_StoryID, to_StoryID, relation, confidence) " +
                    		" values (?,?,?,?) ");
                    pstmt.setInt(1, storyId1);
                    pstmt.setInt(2, storyId2);
                    pstmt.setString(3, relation);
                    pstmt.setFloat(4, confidence);
                    pstmt.executeUpdate();
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
    }
    
    
    public boolean existsInDB(int eventId) {
    	boolean exists = false;
    	
    	PreparedStatement pstmt = null;
		ResultSet result = null;
   	 
        try {
           	 pstmt = openConnection().prepareStatement("select * from Event where EventID = ?");
             pstmt.setInt(1, eventId);
             result = pstmt.executeQuery();
             if(result.next())  exists = true;
        } catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
   	 return exists;
    }
    
    
    
    /*
     * get story redirection by their ids
     */
	public HashMap<Integer, Integer> getRedirectionRelation() {
		HashMap<Integer, Integer> redirections = new HashMap<Integer, Integer> ();
		
		PreparedStatement pstmt = null;
		ResultSet result = null;
		
		try {
			pstmt = openConnection().prepareStatement("select from_StoryID, to_StoryID " +
					"from Story_Story_Relation where relation=?");
			pstmt.setString(1, "isRedirectedTo");
			result = pstmt.executeQuery();
			
			while (result.next()) {
				redirections.put(result.getInt("from_StoryID"), result.getInt("to_StoryID"));
			}
			
		}catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
		return redirections;
	}


	/*
	 * update relation of event to story whenever the story has redirection
	 */
	public void updateEventToStoryRelation(int eventID, int fromID, int toID) {
		PreparedStatement pstmt = null;
		
		try {
			pstmt = openConnection().prepareStatement("update Event_Story_Relation " +
					"set StoryID = ? where EventID = ? and StoryID= ?");
			pstmt.setInt(1, toID);
			pstmt.setInt(2, eventID);
			pstmt.setInt(3, fromID);
			pstmt.executeUpdate();
		} catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
	}

	
	
	
	/*
	 * get the list of events which have relation to one story in storySet
	 */
	public HashMap<Integer, ArrayList<Integer>> getEventStoryRelation(Set<Integer> storySet) {
		HashMap<Integer, ArrayList<Integer>> relations = new HashMap<Integer, ArrayList<Integer>> (); 
		PreparedStatement pstmt = null;
		ResultSet result = null;
		
		try {
			pstmt = openConnection().prepareStatement("select EventID, StoryID " +
					"from Event_Story_Relation");
			result = pstmt.executeQuery();
			int eventId, storyId;
			while (result.next()) {
				eventId = result.getInt("EventID");
				storyId = result.getInt("StoryID");
				if (!storySet.contains(storyId)) continue;
				if (!relations.containsKey(storyId)) relations.put(storyId, new ArrayList<Integer>());
				relations.get(storyId).add(eventId);
			}
		}catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
		return relations;
	}

	
//	
//	public ArrayList<Story> getStories(){
//    	
//    	ArrayList<Story> stories = new ArrayList<Story>();
//        PreparedStatement pstmt = null;
//        ResultSet result = null;
//        try{
//        	pstmt = openConnection().prepareStatement("select StoryID, Label, WikipediaURL from NewsStory n join WikiRef r on n.WikiRefID = r.WikiRefID");
//            result = pstmt.executeQuery();
//            Story story;
//            while(result.next()){
//                story = new Story();
//                story.setId(result.getString("StoryID"));
//                story.setName(result.getString("Label"));
//                story.setWikipediaUrl(result.getString("WikipediaURL"));
//            	stories.add(story);
//            }
//        }catch (SQLException e) {			
//			e.printStackTrace();
//		}finally{
//			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
//			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
//		}
//           
//        return stories;
//    }


	public ArrayList<Story> getStories(){
    	
    	ArrayList<Story> stories = new ArrayList<Story>();
        PreparedStatement pstmt = null;
        ResultSet result = null;
        try{
        	pstmt = openConnection().prepareStatement("select StoryID, Label, WikipediaURL from NewsStory ");
            result = pstmt.executeQuery();
            Story story;
            while(result.next()){
                story = new Story();
                story.setId(result.getString("StoryID"));
                story.setName(result.getString("Label"));
                story.setWikipediaUrl(result.getString("WikipediaURL"));
            	stories.add(story);
            }
        }catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
           
        return stories;
    }
	
	/**
     * get all entities in DB
     * @return array list of entities
     */
    public ArrayList<Entity> getEntities() {
    	ArrayList<Entity> entities = new ArrayList<Entity> ();
    	 PreparedStatement pstmt = null;
         ResultSet result = null;
		try {
			pstmt = openConnection().prepareStatement("select WikiRefID, Name, WikipediaURL from WikiRef");
			result = pstmt.executeQuery();
			Entity entity;
	        while (result.next()){
	        	entity = new Entity();
	        	entity.setId(result.getString("WikiRefID"));
	        	entity.setName(result.getString("Name"));
	        	entity.setWikiURL(result.getString("WikipediaURL"));
	        	entities.add(entity);
	        }
		}catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
    	return entities;
    }

    /**
     * update type of an entity
     * @param id
     * @param type (either LOCATION or PERSON)
     */
	public void updateEntityType(String id, String type) {
		PreparedStatement pstmt = null;
		try {
			pstmt = openConnection().prepareStatement("update WikiRef set type = ? where WikiRefID = ?");
			System.out.println(id + "\t" + type);
			pstmt.setString(2, id);
			pstmt.setString(1, type);
			pstmt.executeUpdate();
		}catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
	}

	
	/**
     */
	public void updateStoryYagoURI(String id, String yagouri) {
		PreparedStatement pstmt = null;
		try {
			pstmt = openConnection().prepareStatement("update NewsStory set yagoURI = ? where StoryID = ?");
			pstmt.setString(2, id);
			pstmt.setString(1, yagouri);
			pstmt.executeUpdate();
		}catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
	}
	public void updateEntityYagoURI(String id, String yagouri) {
		PreparedStatement pstmt = null;
		try {
			pstmt = openConnection().prepareStatement("update WikiRef set yagoURI = ? where WikiRefID = ?");
			pstmt.setString(2, id);
			pstmt.setString(1, yagouri);
			pstmt.executeUpdate();
		}catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
	}
	/**
	 * @param storyId
	 * @param method
	 * @param locationName
	 */
	public void insertStoryToLocationRelation(int storyId, String locationName, String method, float confidence) {
		PreparedStatement pstmt = null;
		try {
			pstmt = openConnection().prepareStatement(
					"insert ignore into Story_Location_Relation(storyID, locationName, Method, confidence) " +
					" values (?, ?, ?, ?)");
			pstmt.setInt(1, storyId);
			pstmt.setString(2, locationName);
			pstmt.setString(3, method);
			pstmt.setFloat(4, confidence);
			pstmt.executeUpdate();
		}catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
		
	}

	public void insertEventToLocationRelation(int eventId, String locationName, String method, float confidence) {
		PreparedStatement pstmt = null;
		try {
			pstmt = openConnection().prepareStatement(
					"insert ignore into Event_Location_Relation(eventID, locationName, Method, confidence) " +
					" values (?, ?, ?, ?)");
			pstmt.setInt(1, eventId);
			pstmt.setString(2, locationName);
			pstmt.setString(3, method);
			pstmt.setFloat(4, confidence);
			pstmt.executeUpdate();
		}catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
		
	}

	/**
	 * get relation of story category given method
	 * @param method
	 * @return
	 */
	public HashMap<Integer, ArrayList<Integer>> getStoryCategoryRelation(String method) {
		HashMap<Integer, ArrayList<Integer>> rel = new HashMap<Integer, ArrayList<Integer>> ();
		PreparedStatement pstmt = null;
		try {
			pstmt = openConnection().prepareStatement(
					"select * from Story_Category_Relation " +
					" where Method =?");
			pstmt.setString(1, method);
			ResultSet r = pstmt.executeQuery();
			while (r.next()) {
				int storyId = r.getInt("StoryID");
				int categoryId = r.getInt("CategoryID");
				if (! rel.containsKey(storyId)) rel.put(storyId, new ArrayList<Integer>());
				rel.get(storyId).add(categoryId);
			}
			
		}catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
		return rel;
	}
	
	public void insertStoryToStoryRelation(int storyId1, int storyId2, String relation, String method, float confidence){
    	PreparedStatement pstmt = null;
		ResultSet result = null;
            try {
                    pstmt = openConnection().prepareStatement("Insert ignore into Story_Story_Relation(from_StoryID, to_StoryID, relation, Method, confidence) " +
                    		" values (?,?,?,?,?) ");
                    pstmt.setInt(1, storyId1);
                    pstmt.setInt(2, storyId2);
                    pstmt.setString(3, relation);
                    pstmt.setString(4, method);
                    pstmt.setFloat(5, confidence);
                    pstmt.executeUpdate();
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
    }

	public String getLatestDateinDB() {
		HashMap<Integer, ArrayList<Integer>> rel = new HashMap<Integer, ArrayList<Integer>> ();
		PreparedStatement pstmt = null;
		try {
			pstmt = openConnection().prepareStatement("select Max(date) as date from Event");
			ResultSet r = pstmt.executeQuery();
			while (r.next()) {
				return r.getString("date");
			}
			
		}catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
		return null;
	}
	
	
//	public void insertEventLocationRelation(String id, String happenRelation, String loc) {
//		PreparedStatement pstmt = null;
//		try {
//			pstmt = openConnection().prepareStatement("insert ignore into Event_Location_Relation(eventID, relation, locationName) " +
//					" values (?, ?, ?)");
//			pstmt.setString(1, id);
//			pstmt.setString(2, happenRelation);
//			pstmt.setString(3, loc);
//			pstmt.executeUpdate();
//		}catch (SQLException e) {			
//			e.printStackTrace();
//		}finally{
//			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
//		}
//	}

}