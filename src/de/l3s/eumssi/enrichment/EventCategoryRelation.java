package de.l3s.eumssi.enrichment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import de.l3s.eumssi.dao.DatabaseManager;
import de.l3s.eumssi.model.Category;
import de.l3s.eumssi.model.Event;
import de.l3s.eumssi.model.Story;

/**
 * enrich event categorization from OpenCalais category
 * @author giangbinhtran
 */
public class EventCategoryRelation {
	DatabaseManager db = new DatabaseManager();
	public void enrich() throws UnsupportedEncodingException {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("resources/annotation.tsv"), "utf-8"));
			int count_story=0;
			String line;
			try {
				while ((line=br.readLine())!=null) {
					String l =  line;
					String [] tmp = l.split("\t");
					System.out.println(l);
					String storyURL = "http://en.wikipedia.org/wiki/" + tmp[0].replace(" ", "_");
					storyURL = storyURL.replace("\"", "");
					//update story category
					//correct typos
					if (tmp.length < 2) continue;
					if (tmp[1].trim().equals("Religion")) tmp[1] = "Religion_Belief";
					if (tmp[1].trim().equals("Internaltion_Relation")) tmp[1] = "International_Relation";
					if (tmp[1].trim().equals("Entertaintment_Culture")) tmp[1] = "Entertainment_Culture";
					Category c = db.getCategoryByName(tmp[1].trim());
					Story s = db.getStoryByURL(storyURL);
					if (s!=null) {
						if (c!=null) {
							int storyID = Integer.valueOf(s.getId());
							int categoryID = Integer.valueOf(c.getId()); 
							db.insertStoryToCategoryRelation(storyID, categoryID, db.METHOD_EXPERT_MANUAL_ANNOTATION, (float) 1);
							count_story +=1;
						}
						else {
							System.out.println(" not found category " + tmp[1]);
						}
					}
					else {
						System.err.println(" not found story " + line);
					}
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println(count_story);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * dirty code 
	 * @param outputfile
	 * @throws IOException
	 */
	/*
	public static void get_Unlabeled_stories(String outputfile) throws IOException {
		BufferedWriter bw = new BufferedWriter  (new OutputStreamWriter(new FileOutputStream(outputfile),"UTF-8"));
		DatabaseManager db = new DatabaseManager();
		ArrayList<Story> stories = db.getStories();
		HashMap<Integer, Integer> redirection = db.getRedirectionRelation();
		HashMap<Integer, ArrayList<Integer>> story_cat = db.getStoryCategoryRelation(db.METHOD_WCEP_MANUAL_ANNOTATION);
		HashSet<String> storiesurls = new HashSet<String> ();
		for (int sid: story_cat.keySet()) {storiesurls.add(s.getWikipediaUrl());}
		
		story_cat = db.getStoryCategoryRelation(db.METHOD_EXPERT_MANUAL_ANNOTATION);
		for (Story s: story_cat.keySet()) {storiesurls.add(s.getWikipediaUrl());}
		
		
		for (Story ss: stories) {
			if (!storiesurls.contains(ss.getWikipediaUrl())) {
				//without redirection
				if (!redirection.containsKey(Integer.valueOf(ss.getId()))) {
					System.out.println(ss.getName());
					bw.write(ss.getName() + "\n");
				}
			}
		}
		bw.close();
	}
	*/
	/**
	 * propage event story relation
	 */
	public void propagateEventInStory() {
		String[] methods =  new String[] {db.METHOD_WCEP_MANUAL_ANNOTATION, db.METHOD_EXPERT_MANUAL_ANNOTATION};
//		String[] methods =  new String[] {db.METHOD_WCEP_MANUAL_ANNOTATION};
		for (String method: methods) {
			System.out.println(method);
			HashMap<Integer, ArrayList< Integer>> storycat = db.getStoryCategoryRelation(method);
			for (int storyID: storycat.keySet()) {
				boolean propagate = false;
				int c = -1;
				//get the most popular category of the story
				//ensure that either there is only one category or the most popular one must be >=95% number of events
				int n_category = storycat.get(storyID).size();
				if (n_category==1) {
					propagate = true;
					c = storycat.get(storyID).get(0);
					System.out.println(c);
				}
				else {
					System.out.println(storyID);
					c = getMostPopularCategory(storyID, 0.95);
					propagate = (c !=-1);
				}
				if (propagate) {
				//propagate all events in the story
					ArrayList<Event> events = db.getEventsByStory(storyID, db.METHOD_WCEP_MANUAL_ANNOTATION, 0);
					for (Event e: events) {
						db.insertEventToCategoryRelation(Integer.valueOf(e.getEventId()), c , "PROPAGATE_" + method, (float) 0.95);
					}
				}
			}
		}
	}
	
	/**
	 * return the category of given story that has more than confident fraction events
	 * @param s
	 * @param confident
	 * @return null if not found
	 */
	private int getMostPopularCategory(int storyID, double confident) {
		ArrayList<Event> events = db.getEventsByStory(storyID, db.METHOD_WCEP_MANUAL_ANNOTATION, (float) confident);
		HashMap<Integer, Integer> eventCatCount = new HashMap<Integer, Integer> ();
		for (Event e: events) {
			int c = -1;
			try {
				c= Integer.valueOf(e.getCategory().getId());
			} catch (NullPointerException x) { // no category
				continue;
			}
			if (! eventCatCount.containsKey (c)) eventCatCount.put(c,0);
			eventCatCount.put(c, eventCatCount.get(c) + 1);
		} 
		for (int c: eventCatCount.keySet()) {
			if (1.0 * eventCatCount.get(c) / events.size() >= confident) {
				return c;
			}
		}
		return -1;
	}

	/*
	 * confidence: 0.95
	 */
	public static void categorizeByOpenCalais(double confidence) {
		DatabaseManager db = new DatabaseManager();
		try {
			Scanner sc = new Scanner(new FileInputStream("resources/calais_all_cleaned.text"), "utf-8");
			while (sc.hasNext()) {
				String l = sc.nextLine();
				String [] tmp = l.split("\t");
				String eventid = tmp[0];
				String [] categories = tmp[1].split(",");
				
				//conservatively select category
				if (categories.length >1) continue;
				Event e = db.getEventById(eventid);
				if (e.getCategory() !=null) continue;
				
				
				for (String cat: categories) {
					String[] pred = cat.split(":");
					String categoryName = pred[0].replace(" ", "_");
					double value = Double.valueOf(pred[1]);
					if (value >= confidence) {
						Category c = db.getCategoryByName(categoryName);
						if (c!=null) {
							db.insertEventToCategoryRelation(Integer.valueOf(eventid), Integer.valueOf(c.getId()), db.METHOD_CALAIS, (float) 0.94);
						}
						else {
							System.out.println(categoryName);
						}
					}
				}
			}
			sc.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void main (String[] agrs)  {
		EventCategoryRelation ecat = new EventCategoryRelation();
		/*
		try {
			//get_Unlabeled_stories("resources/un-categorized-stories.txt");
			ecat.enrich();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/
		ecat.propagateEventInStory();
		categorizeByOpenCalais(1.0);
	}
}
