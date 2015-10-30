package de.l3s.eumssi.enrichment;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import de.l3s.eumssi.dao.DatabaseManager;
import de.l3s.eumssi.model.Entity;
import de.l3s.eumssi.model.Event;
import de.l3s.eumssi.model.Story;

public class BelongsToStory {
	DatabaseManager db = new DatabaseManager();
	public static String generaltopics = "resources/generalTopic.txt";
	/**
	 * @param args
	 */
	public static void enrichByAutomaticallyMethod(String[] args) {
		String dataFileName = "D:\\Alrifai\\Dropbox\\WikiTimes\\Backups\\DB\\enrichments\\classified.event\\belongsToStory_relation.txt";
		BelongsToStory importer = new BelongsToStory();
		//importer.moveEventsToRelationTable();
		importer.loadAnnotations(dataFileName, false);
	}

	public ArrayList<Event> moveEventsToRelationTable(){
		DatabaseManager db = new DatabaseManager();
		
		ArrayList<Event> events  = db.getEvents();
		//ArrayList<Event> events  = db.getEvents("2014-07-01", "2014-07-10");
		System.out.println(events.size() + " events ...");
		for(Event event: events){
			if (event.getStory()!=null)
				if(Integer.parseInt(event.getStory().getId()) > 0){
					System.out.println(event.getEventId() + " belongsToStory " + event.getStory().getId());
					float confidence = 1;
					//db.insertEventToStoryRelation(Integer.parseInt(event.getEventId()), Integer.parseInt(event.getStory().getId()), confidence);
				}
		}
		
		db.closeConnection();	
		
		return events;
	}
	
	public void loadAnnotations(String dataFileName, boolean manual){
		float confidence;
		DatabaseManager db = new DatabaseManager();
		
        BufferedReader bReader;
		try {
			//bReader = new BufferedReader(new FileReader(dataFileName));
			bReader = new BufferedReader(
					   new InputStreamReader(
			                      new FileInputStream(dataFileName), "UTF-8"));
			String line;
			int count_all = 0, count_found = 0;
	        while ((line = bReader.readLine()) != null) {
	        	count_all ++;
	            String datavalue[] = line.split("\t");
	            String eventId = datavalue[3];
	            String storyURL = datavalue[2].replace("_cluster:", "").trim();
	            
	            if(manual) confidence = 1;
	            else confidence = Float.parseFloat(datavalue[1]);
	            
	            Story story = null; 
	            if(confidence > 0.5){	            	
	            	if(db.existsInDB(Integer.parseInt(eventId))){
	            		story = db.getStoryByURL(storyURL);
			            if(story!=null){
			            	count_found++;	            	
			            	System.out.println(eventId + " ---> belongsToStory ---> " + story.getId() + ", confidence score = " + confidence);
			            	//db.insertEventToStoryRelation(Integer.parseInt(eventId), Integer.parseInt(story.getId()), confidence);
			            	if(count_found >=1000) break;
			            }else{
			            	System.err.println(eventId + ": no story found in DB with wikiURL: " + storyURL);			            	
			            }
	            	}else{
	            		System.err.println("No event found in DB with id: "+ eventId);
	            	}
	            }
	        }	
	        System.out.println(" total number of relations       = " + count_all);
	        System.out.println(" successfully imported relations = " + count_found); 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		db.closeConnection();	
	}
	
	
	public static HashSet<String> getGeneralTopics() {
		HashSet<String> generalTopics = new HashSet<String> ();
		try {
			BufferedReader br = new BufferedReader (new InputStreamReader(
					new FileInputStream(generaltopics), "utf-8"));
			String gentopic = "";
			while ((gentopic=br.readLine())!=null) {
				gentopic = gentopic.trim();
				generalTopics.add(gentopic);
			}
			br.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return generalTopics;
	}
	
	/**
	 * Rule 1: enrich event belongsToStory relation by looking into the
	 * event description to find if there has an entity which is the story URL and begins the description
	 */
	public void enrichEventByEntity() {
		/*
		String test = "<a href=\"/wiki/Jamil_Abdullah_Al-Amin\" title=\"Jamil Abdullah Al-Amin\" class=\"mw-redirect\">Jamil Abdullah Al-Amin</a>, a former";
		Entity ee = getStartingEntity(test);
		System.out.println(ee.getWikiURL());
		String x = ee.getWikiURL().substring("http://en.wikipedia.org/wiki/".length());
		System.out.println(x);
		System.exit(0);
		*/
		HashSet<String> generalTopics = getGeneralTopics();
		ArrayList<Event> events = db.getEvents();
		int found = 0;
		for (Event e: events) {
			if (e.getStory() == null) { // unlabell
				//find in the list of entities
				Entity firstEntity = getStartingEntity(e.getAnnotatedDescription());
				if (firstEntity!=null) {
					String urlLikeName = firstEntity.getWikiURL().substring("http://en.wikipedia.org/wiki/".length());
					//ignore story which is general topic
					if (generalTopics.contains(urlLikeName)) continue;
					
					Story s = db.getStoryByURL(firstEntity.getWikiURL());
					if (s!=null) {
						//assign this event to the story
						db.insertEventToStoryRelation(Integer.valueOf(e.getEventId()), Integer.valueOf(s.getId()), db.METHOD_STORY_FIRST, (float) 1.0 );
						found+=1;
					}

				}
			}
		}
		System.out.println("Found " + found + " events enriched by strategy " + db.METHOD_STORY_FIRST );
		
		//after enrichment, resolve redirection if exists
		/*
		HashMap<Integer, Integer> storyRedirection = db.getRedirectionRelation();
		new EnrichStoryRedirection().updateEventStoryRelation(db, storyRedirection);
		*/
	}
	/**
	 * simply get the entity which starts the event, 
	 * if this entity is a story, the event should belongs to that story
	 * @param annotatedDescription
	 * @return Entity or null
	 */
	private Entity getStartingEntity(String annotatedDescription) {
		String hypertag = "<a href=";
		String wikitag = "/wiki/";
		Entity initEntity = null;
		if (annotatedDescription.startsWith(hypertag)) {
			int endPosition = annotatedDescription.indexOf("title=");
			if (endPosition>0) {
				String url_short = annotatedDescription.substring(hypertag.length() + 1, endPosition-2);
				if (url_short.startsWith(wikitag)) url_short = url_short.substring(wikitag.length());
				System.out.println(url_short);
				initEntity = db.getEntityByURL(db.conf.getProperty("wiki_url_prefix") + url_short);
			}
		}
		return initEntity;
	}

	//rule2: having entity which has same url with one of existing stories, and date is overlapped within +-3 months
	private void enrichByRule2() {
		HashMap<Integer, Integer> redirection = db.getRedirectionRelation();
		
		HashSet<String> generalTopics = getGeneralTopics();
		ArrayList<Event> events = db.getEvents();
		int found = 0;
		for (Event e: events) {
			if (e.getStory() == null) { // unlabell
				//find in the list of entities
				for (Entity entity: e.getEntities()) {
					String urlLikeName = entity.getWikiURL().substring("http://en.wikipedia.org/wiki/".length());
					//ignore story which is general topic
					if (generalTopics.contains(urlLikeName)) continue;
					
					Story s = db.getStoryByURL(entity.getWikiURL());
					
					if (s!=null ) {
						int storyID = Integer.valueOf(s.getId());
						if (redirection.containsKey(storyID)) storyID = redirection.get(storyID);
						if (temporalOverlapped(storyID, e)) { //story is one of entities
							//assign this event to the story
							db.insertEventToStoryRelation(Integer.valueOf(e.getEventId()), storyID, db.METHOD_STORY_SECOND, (float) 1.0 );
							found+=1;
						}
					}
	
				}
			}
		}
		System.out.println("Found " + found + " events enriched by strategy " + db.METHOD_STORY_SECOND );
	}
	
	/**
	 * @param _fromdate
	 * @param _todate
	 * @return the absolute difference in date between 2 dates
	 * format yyyy-MM-dd
	 */
	public static int dateDiff(String _fromdate, String _todate) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		try {
			java.util.Date fdate = formatter.parse(_fromdate);
			java.util.Date tdate = formatter.parse(_todate);
			return (int) Math.abs((tdate.getTime() - fdate.getTime())/1000/60/60/24);
		}
		catch (Exception e) {
			return -1;
		}
	}
	
	
	/**
	 * check if the event happened during the story +-3 months variant
	 * @param s
	 * @param e
	 */
	private boolean temporalOverlapped(int storyID, Event e) {
		ArrayList<Event> events = db.getEventsByStory(storyID, db.METHOD_WCEP_MANUAL_ANNOTATION, (float) 1.0);
		ArrayList<Date> dates = new ArrayList<Date> ();
		for (Event story_event: events) {
			dates.add(story_event.getDate());
		}
		if (dates.size()>0) {
			Collections.sort(dates);
			int distance_early = dateDiff(dates.get(0).toString(), e.getDate().toString());
			int distance_late = dateDiff(dates.get(dates.size()-1).toString(), e.getDate().toString());
	 		return (distance_early <= 30 || distance_late <=30);
		}
		else return false;
	}

	public static void main(String[] args) {
		BelongsToStory bts = new BelongsToStory();
		
		//rule1: story is in the begining of the event
		//bts.enrichEventByEntity();
		
		//rule2: having entity which has same url with one of existing stories, and date is overlapped within +-3 months
		bts.enrichByRule2();
		
		
		//resolve redirection
		DatabaseManager db = new DatabaseManager();
		HashMap<Integer, Integer> storyRedirection = db.getRedirectionRelation();
		new EnrichStoryRedirection().updateEventStoryRelation(storyRedirection);
	}

	
}
