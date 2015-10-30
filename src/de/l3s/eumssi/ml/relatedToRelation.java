/**
 * WWW2015: finding related stories
 * for feature extraction
 */
package de.l3s.eumssi.ml;

import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import de.l3s.eumssi.dao.DatabaseManager;
import de.l3s.eumssi.ml.EntityDistribution;
import de.l3s.eumssi.ml.EventDistribution;
import de.l3s.eumssi.ml.MyUtil;
import de.l3s.eumssi.ml.StoryDistribution;
import de.l3s.eumssi.model.Entity;
import de.l3s.eumssi.model.Event;
import de.l3s.eumssi.model.Story;

public class relatedToRelation {
	DatabaseManager db = new DatabaseManager();
	static String annotationPair = "resources/story-relation-manual-annotation.txt";
	static String wikipediaPageFolder = "resources/story-wikipedia/";
	StoryDistribution globalDistribution = null;
	
	public relatedToRelation() {
		File wikipageFolder = new File(wikipediaPageFolder);
		if (!wikipageFolder.exists()) wikipageFolder.mkdir();
	}
	
	/**
	 * retrievel related pairs from database
	 * generate trainign data from annotation and redirection in Wikipedia
	 * if output_file is null, not write to file
	 * @throws IOException 
	 */
	public HashMap<Integer, ArrayList< Integer>> generateRelatedPair(String output_file) throws IOException {
		HashMap<Integer, ArrayList<Integer>> relatedPairs = db.getStorytoStoryRelation("relatedTo",(float) 0.5);
		if (output_file !=null) {
			PrintWriter pw = new PrintWriter (new File(output_file));
			//now , print the ground truth: id1 id2 1/0
			
			for (int id1: relatedPairs.keySet()) {
				for (int id2: relatedPairs.get(id1)) {
					pw.write(String.format("%d\t%d\t1\n", id1, id2));
				}
				//negative pairs
				/*
				ArrayList<Integer> non_related = new ArrayList<Integer> ();
				for (int id3: relatedPairs.keySet()) {
					if (relatedPairs.get(id1).indexOf(id3) <0) {
						non_related.add(id3);
					}
				}
				Collections.shuffle(non_related);
				for (int i =0; i<10; i++) //under sampling
					if (i <non_related.size())
						pw.write(String.format("%d\t%d\t0\n", id1, non_related.get(i)));
				*/
			}
			pw.close();
		}
		return relatedPairs;
	}
	
	//consider the story that happened in 1 year only
	/*
	private ArrayList<String> getClosebyStories(String eventDate) {
		int daterange = 12 * 7; // 6 months
		ArrayList<String> storyNames = new ArrayList<String> ();
		for (String s: wkstories.keySet()) {
			StoryDistribution story = wkstories.get(s);
			String [] minmaxdate = story.getMinMaxDate();
			int distanceMin = MyUtil.dateDiff(minmaxdate[0], eventDate);
			int distanceMax = MyUtil.dateDiff(minmaxdate[1], eventDate);
			if (eventDate.compareTo(minmaxdate[0])>=0 && eventDate.compareTo(minmaxdate[1]) <=0) storyNames.add(s);
			else 
				if (distanceMin < daterange || distanceMax <daterange) storyNames.add(s);
		}
		return storyNames;
	}
	*/
	private int getTimeOverlap(ArrayList<Event> eventlist1, StoryDistribution s1, ArrayList<Event> eventlist2, StoryDistribution s2) {
		int daterange = 6 * 7; // 3moonths
		String [] minmaxdate1 = s1.getMinMaxDate();
		String [] minmaxdate2 = s2.getMinMaxDate();
		if (minmaxdate1[0].compareTo(minmaxdate2[0])>=0 && minmaxdate1[0].compareTo(minmaxdate2[1]) <=0) return 1;
		if (minmaxdate1[1].compareTo(minmaxdate2[0])>=0 && minmaxdate1[1].compareTo(minmaxdate2[1]) <=0) return 1;
		
		if (minmaxdate2[0].compareTo(minmaxdate1[0])>=0 && minmaxdate2[0].compareTo(minmaxdate1[1]) <=0) return 1;
		if (minmaxdate2[1].compareTo(minmaxdate1[0])>=0 && minmaxdate2[1].compareTo(minmaxdate1[1]) <=0) return 1;
		
		int distanceMin = Math.min(MyUtil.dateDiff(minmaxdate1[0],minmaxdate2[0]), MyUtil.dateDiff(minmaxdate1[0],minmaxdate2[1]));
		int distanceMax = Math.min(MyUtil.dateDiff(minmaxdate1[1],minmaxdate2[0]), MyUtil.dateDiff(minmaxdate1[1],minmaxdate2[1]));
		
		if (distanceMin < daterange || distanceMax <daterange) return 1;
		
		//no -close event
		return 0;
	}
	
	private double[] getEventToStoryDistance(ArrayList<Event> eventlist1, StoryDistribution storyDistr1,
			ArrayList<Event> eventlist2, StoryDistribution storyDistr2) {
		double[] maxMinAvg = new double [] {0,100,0};
		String [] minmaxdate = storyDistr2.getMinMaxDate();
		for (Event e: eventlist1) {
			String eventDate = e.getDate().toString();
			double cosine = 0;
			double L1 = 0, L2 =0;
			ArrayList<Event> tmp = new ArrayList<Event> ();
			tmp.add(e);
			
			StoryDistribution ed = indexStory(tmp); //distribution of the event 
			HashSet<String> allterm =ed.getTerms();
			allterm.addAll(storyDistr2.getTerms());
			
			for (String term: allterm) {
				double p_term_1 = getDirichletSmoothing(storyDistr1, term);
				double p_term_2 = getDirichletSmoothing(storyDistr2, term);
				
				cosine += p_term_1 * p_term_2;
				L1 += p_term_1 * p_term_1;
				L2 += p_term_2 * p_term_2;
			}
			cosine = cosine / Math.sqrt(L1 * L2);
			
			//distance to the story2 by weeks
			int distanceMin = MyUtil.dateDiff(minmaxdate[0], eventDate)/7;
			int distanceMax = MyUtil.dateDiff(minmaxdate[1], eventDate)/7;
			//normalize by distance to the story
			cosine /= (Math.min(distanceMin, distanceMax) + 1);
			
			if (cosine > maxMinAvg[0]) maxMinAvg[0] = cosine;
			if (cosine < maxMinAvg[1]) maxMinAvg[1] = cosine;
			maxMinAvg[2] += cosine;
		}
		maxMinAvg[2] /= eventlist1.size();
		return maxMinAvg;
	}
	
	private double[] getDistributionSimilarity(ArrayList<Event> eventList1,
			StoryDistribution storyDistr1, ArrayList<Event> eventList2,
			StoryDistribution storyDistr2) {
		int daterange = 6 * 7; //+- daterange = 3 months
		//get closeby events
		String [] minmaxdate2 = storyDistr2.getMinMaxDate();
		String [] minmaxdate1 = storyDistr1.getMinMaxDate();
		
		ArrayList<Event> temporalList1 = new ArrayList<Event>  ();
		ArrayList<Event> temporalList2 = new ArrayList<Event>  ();
		for (Event e: eventList1) {
			String eventDate = e.getDate().toString();
			//distance to the story2 by weeks
			int distanceMin = MyUtil.dateDiff(minmaxdate2[0], eventDate);
			int distanceMax = MyUtil.dateDiff(minmaxdate2[1], eventDate);
			
			if (eventDate.compareTo(minmaxdate2[0])>=0 && eventDate.compareTo(minmaxdate2[1]) <=0) temporalList1.add(e);
			else 
				if (distanceMin < daterange || distanceMax <daterange) temporalList1.add(e);
		}
		
		for (Event e: eventList2) {
			String eventDate = e.getDate().toString();
			//distance to the story2 by weeks
			int distanceMin = MyUtil.dateDiff(minmaxdate1[0], eventDate);
			int distanceMax = MyUtil.dateDiff(minmaxdate1[1], eventDate);
			
			if (eventDate.compareTo(minmaxdate1[0])>=0 && eventDate.compareTo(minmaxdate1[1]) <=0) temporalList2.add(e);
			else 
				if (distanceMin < daterange || distanceMax <daterange) temporalList2.add(e);
		}
		
		StoryDistribution temporalDistr1 = indexStory(temporalList1);
		StoryDistribution temporalDistr2 = indexStory(temporalList2);
		return new double[] {getDistributionCosine(temporalDistr1, temporalDistr2), 
				getDistributionDifference(temporalDistr1, temporalDistr2)};
	}
	//generation of features for learning to rank
	private void generateFeatures(String groundtruth_file, String feature_output) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter (new File(feature_output));
		System.out.println("DEBUG: Get global event distribution...");
		ArrayList<Event> wikitimesData = db.getEvents();
		globalDistribution = indexStory(wikitimesData);
		
		HashMap<Integer, Integer> redirection = db.getRedirectionRelation();
		int count =0;
		System.out.println("DEBUG: gen features...");
		int preid = -1;
		
		HashMap<Integer, ArrayList<Integer>> relatedness = db.getStorytoStoryRelation("relatedTo",(float)0.5);
		int qid = 0;
		for (int id1:relatedness.keySet()) {
			qid ++;
			Story s1 = db.getStoryById(String.valueOf(id1));
			if (redirection.containsKey(id1)) continue;
			for (int id2: relatedness.get(id1)) {
				if (redirection.containsKey(id2)) continue;
				if (count++ == 2) break;
				ArrayList<Double> features = new ArrayList<Double>  ();
				
				//generate feature for any 2 pairs in format of svm rank
				String label = String.format("%d_%d", id1, id2);
				Story s2 = db.getStoryById(String.valueOf(id2));
				System.out.println(s1.getName() + "\t" +  s2.getName());
				//event similarity measurement
				ArrayList<Event> eventList1 = db.getOriginalEventsByStory(id1);
				ArrayList<Event> eventList2 = db.getOriginalEventsByStory(id2);
				//if originally a story does not have events, it means it is redirected one and did not appear in WCEP
				//in this case, it makes sense to use enriched events (+ redirection) to approximate that story
				if (eventList1.size()==0) eventList1 = db.getEventsByStory(id1, null, (float) 0.9);
				if (eventList2.size()==0) eventList2 = db.getEventsByStory(id2, null, (float) 0.9);
				
				
				//form word distribution
				StoryDistribution storyDistr1 = indexStory(eventList1);
				StoryDistribution storyDistr2 = indexStory(eventList2);
				
				//compute similarity measurement
				double text_similarity  = getTextDifference(storyDistr1, storyDistr2);
				features.add(text_similarity);
				
				double distribitionalDifference = getDistributionDifference(storyDistr1, storyDistr2);
				features.add(distribitionalDifference);
				
				double cosineSimilarity = getDistributionCosine(storyDistr1, storyDistr2);
				features.add(cosineSimilarity);
				
				double entity_distribitionalSimilarity = getEntityDistributionDifference(storyDistr1, storyDistr2);
				features.add(entity_distribitionalSimilarity);
				
				
				double entity_similarity = getEntityDifference(storyDistr1, storyDistr2);
				features.add(entity_similarity);
				
				double entity_cosine = getEntityDistributionCosine(storyDistr1, storyDistr2);
				features.add(entity_cosine);
				
				int timeOverlap = getTimeOverlap(eventList1, storyDistr1, eventList2, storyDistr2);
				features.add(1.0 * timeOverlap);
				
				double[] eventToStorySimilarity = getEventToStoryDistance(eventList1, storyDistr1, eventList2, storyDistr2);
				for (double x: eventToStorySimilarity)
					features.add(x);
				//get temporal similarity
				double[] temporalSimilarities = getDistributionSimilarity(eventList1, storyDistr1, eventList2, storyDistr2);
				for (double x: temporalSimilarities)
					features.add(x);
				
				
				
				//check redirection
				int wid1 = redirection.containsKey(id1)? redirection.get(id1): id1;
				int wid2 = redirection.containsKey(id2)? redirection.get(id2): id2;
				//wikipages
				try {
					System.out.println("DEBUG: fetching wikipedia pages...");
					checkFile(wid1);
					checkFile(wid2);
				} catch (IOException e) {
					e.printStackTrace();
				}
	
				StoryDistribution wikiDistr1 =null;
				StoryDistribution wikiDistr2 =null;
				if (wid1!=wid2) {
					try {
						wikiDistr1 = getWikiDescription(wid1);
						wikiDistr2 = getWikiDescription(wid2);
					} catch (IOException e) {
						e.printStackTrace();
					}
	
				}
				if (wikiDistr1 == null || wikiDistr2 == null) continue;
				
				double wiki_similarity  = (wid1==wid2)?1:getTextDifference(wikiDistr1, wikiDistr2);
				features.add(wiki_similarity);
				
				double wiki_distribitionalDifference = (wid1==wid2)?1:getDistributionDifference(wikiDistr1, wikiDistr2);
				features.add(wiki_distribitionalDifference);
				
				double wiki_cosineSimilarity = (wid1==wid2)?1:getDistributionCosine(wikiDistr1, wikiDistr2);
				features.add(wiki_cosineSimilarity);
				
				double wiki_entity_distribitionalSimilarity = (wid1==wid2)?1:getEntityDistributionDifference(wikiDistr1, wikiDistr2);
				features.add(wiki_entity_distribitionalSimilarity);
				
				double wiki_entity_similarity = (wid1==wid2)?1:getEntityDifference(wikiDistr1, wikiDistr2);
				features.add(wiki_entity_similarity);
				
				double wiki_entity_cosine = (wid1==wid2)?1:getEntityDistributionCosine(wikiDistr1, wikiDistr2);
				features.add(wiki_entity_cosine);
				
				int hasReferToEachOther = (wid1==wid2)?1:referToEachOther(wid1,s1.getName().toLowerCase(), wid2, s2.getName().toLowerCase());
				features.add(1.0 * hasReferToEachOther);
	
				//print
				pw.write(String.format("%s ", label));
				pw.write(String.format("qid:%d ", qid));
				for (int i = 0; i< features.size(); i++)
					pw.write(String.format("%d:%.5f ", i, features.get(i)));
				pw.write("\n");
			}
		}
		pw.close();
	}
	
	
	/**
	 * get text description of wikipedia page
	 * @param wid1
	 * @return
	 */
	private StoryDistribution getWikiDescription(int wid) throws IOException{
		StoryDistribution storydstr = new StoryDistribution();
		BufferedReader br = new BufferedReader (new InputStreamReader 
				(new FileInputStream(wikipediaPageFolder + "/" + String.valueOf(wid) + ".page.json"), "utf-8"));
		StringBuffer sb = new StringBuffer();
		String l = "";
		while ((l=br.readLine())!=null) sb.append(l + " ");
		br.close();
		try {
			//get plain text
			Object obj=JSONValue.parse(sb.toString());
			JSONObject obj2=(JSONObject)  obj;
			JSONObject parseObj = (JSONObject) obj2.get("parse");
			
			String wikitext = parseObj.get("wikitext").toString();
			WikiModel wikiModel = new WikiModel("http://www.mywiki.com/wiki/${image}", "http://www.mywiki.com/wiki/${title}");
	        String plainStr = wikiModel.render(new PlainTextConverter(), wikitext);
			
			//get entity
			JSONArray entityArray=(JSONArray) parseObj.get("links");
			
			EventDistribution eventd = new EventDistribution(plainStr, "2000-01-01");
			for (int i = 0; i<entityArray.size(); i++) {
				JSONObject entityObj = (JSONObject) entityArray.get(i);
				String entityName = entityObj.get("*").toString();
				String entityURL = "http://en.wikipedia.org/wiki/" + entityName.replace(" ", "_") ;
				EntityDistribution ed = new EntityDistribution(entityURL);
				eventd.addEntity(ed);
			}
			
			//fake the wikipedia as an event and compute distributional semantics
			storydstr.index(eventd);
		} 
		catch (Exception e) {
			return null;
		}
		return storydstr;
	}

	private String readAll(Reader rd) throws IOException {
	    StringBuilder sb = new StringBuilder();
	    int cp;
	    while ((cp = rd.read()) != -1) {
	      sb.append((char) cp);
	    }
	    return sb.toString();
	}
	private void checkFile(int wid) throws IOException {
		// if the parsed file of wikipedia page is not in wikipeidPageFolder, download it under name wid2.page.json
		File f = new File(wikipediaPageFolder + "/" + String.valueOf(wid) + ".page.json");
		Story s = db.getStoryById(String.valueOf(wid));
		System.out.println(wid + "\t" + s.getWikipediaUrl());
		String urlstr = "http://en.wikipedia.org/w/api.php?action=parse&prop=wikitext|links&format=json&page=" + s.getName().replace(" ", "_");
//		System.out.println(urlstr);
		URL u= new URL(urlstr);
		if (!f.exists()) {//fetch this page
			BufferedWriter bw = new BufferedWriter (new OutputStreamWriter(new FileOutputStream(f), "utf-8"));
			String wikiPage = null;
			InputStream is=null;
		    try {
		    	is = new URL(urlstr).openStream();
		    	BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
		    	wikiPage = readAll(rd);
		    }catch(Exception e){
		    	System.out.println("There is exception in \"downloadWikiPage()\" method");
		    }
		   bw.write(wikiPage);
			bw.close();
		}
	}
	
	private void checkDBpediaFile(int wid) throws IOException {
		// if the parsed file of wikipedia page is not in wikipeidPageFolder, download it under name wid2.page.json
		File f = new File(wikipediaPageFolder + "/" + String.valueOf(wid) + ".wikiraw.json");
		Story s = db.getStoryById(String.valueOf(wid));
		System.out.println(wid + "\t" + s.getWikipediaUrl());
		//String urlstr = "http://dbpedia.org/data/" + s.getName().replace(" ", "_") + ".json";
		String urlstr = "http://en.wikipedia.org/w/index.php?action=raw&title=" + s.getName().replace(" ", "_");
//		http://en.wikipedia.org/w/index.php?action=raw&title=June_2013_Egyptian_protests
//		System.out.println(urlstr);
		URL u= new URL(urlstr);
		if (!f.exists() || f.length()<100) {//fetch this page
			BufferedWriter bw = new BufferedWriter (new OutputStreamWriter(new FileOutputStream(f), "utf-8"));
			String wikiPage = null;
			InputStream is=null;
		    try {
		    	is = new URL(urlstr).openStream();
		    	BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
		    	wikiPage = readAll(rd);
		    }catch(Exception e){
		    	System.out.println("There is exception in \" DBPedia \" method");
		    }
		   if (wikiPage!=null)
			   bw.write(wikiPage);
		   bw.close();
		}
	}

	private int referToEachOther(int wid1, String original_name1,
			int wid2, String original_name2) {
		String u1 = db.getStoryById(String.valueOf(wid1)).getName().toLowerCase();
		String u2 = db.getStoryById(String.valueOf(wid2)).getName().toLowerCase();
		
		HashSet<String> wikilinks1 = null;
		HashSet<String> wikilinks2 = null;
		try {
			wikilinks1 = getLinks (wid1);
			wikilinks2 = getLinks (wid2);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (wikilinks1.contains(u2) || wikilinks1.contains(original_name2)  ||
			wikilinks2.contains(u1) || wikilinks2.contains(original_name1)) return 1;
		else return 0;
	}

	
	private HashSet<String> getLinks(int wid) throws IOException {
		HashSet<String> links = new HashSet<String> ();
		StoryDistribution storydstr = null;
		BufferedReader br = new BufferedReader (new InputStreamReader 
				(new FileInputStream(wikipediaPageFolder + "/" + String.valueOf(wid) + ".page.json"), "utf-8"));
		StringBuffer sb = new StringBuffer();
		String l = "";
		while ((l=br.readLine())!=null) sb.append(l + " ");
		br.close();
		
		Object obj=JSONValue.parse(sb.toString());
		JSONObject obj2=(JSONObject)  obj;
		JSONObject parseObj = (JSONObject) obj2.get("parse");
		//get entity 
		if (parseObj==null) return null;
		JSONArray entityArray=(JSONArray) parseObj.get("links");
		
		for (int i = 0; i<entityArray.size(); i++) {
			JSONObject entityObj = (JSONObject) entityArray.get(i);
			String entityName = entityObj.get("*").toString();
			links.add(entityName.replace (" ", "_"));
		}
		return links;
	}

	private double getEntityDistributionCosine(StoryDistribution storyDistr1,
			StoryDistribution storyDistr2) {
		double cosine = 0;
		double L1 = 0, L2 =0;
		HashSet<String> allterm =storyDistr1.getNETerms();
		allterm.addAll(storyDistr2.getNETerms());
		
		for (String term: allterm) {
			double p_term_1 = getDirichletSmoothing(storyDistr1, term);
			double p_term_2 = getDirichletSmoothing(storyDistr2, term);
			
			cosine += p_term_1 * p_term_2;
			L1 += p_term_1 * p_term_1;
			L2 += p_term_2 * p_term_2;
		}
		cosine = cosine / Math.sqrt(L1 * L2);
		return cosine;
	}

	private double getEntityDistributionDifference(
			StoryDistribution storyDistr1, StoryDistribution storyDistr2) {
		double kl = 0;
		for (String term: storyDistr1.getNETerms()) {
			double p_term_1 = getDirichletSmoothing(storyDistr1, term);
			double p_term_2 = getDirichletSmoothing(storyDistr2, term);
			if (p_term_2 * p_term_1 >0 )  kl += p_term_1 * Math.log(p_term_1/ p_term_2);
		}
		if (kl <0) {
			System.err.println("KL entity distance <0");
		}
		return kl;
	}

	private double getEntityDifference(StoryDistribution storyDistr1,
			StoryDistribution storyDistr2) {
		int common = 0;
		for (String term: storyDistr1.getNETerms())
			if (storyDistr2.getNETerms().contains(term)) common +=1;
		int total = storyDistr1.getNETerms().size() + storyDistr2.getNETerms().size() - common;
		if (total ==0) return 1.0;
		return 1.0 * common / total;
	}
	
	public double getDirichletSmoothing(StoryDistribution f, String term) {
		double param = 2000;
		int countw_story = f.getFrequency(term);
		double pw_story = globalDistribution.getLocalProb(term);
		return 1.0 *(countw_story + param * pw_story ) / (f.total + param);
		
	}

	private double getDistributionDifference(StoryDistribution storyDistr1,
			StoryDistribution storyDistr2) {
		double kl = 0;
		for (String term: storyDistr1.getTerms()) {
			double p_term_1 = getDirichletSmoothing(storyDistr1, term);
			double p_term_2 = getDirichletSmoothing(storyDistr2, term);
			
			if (p_term_2 * p_term_1>0 ) kl += p_term_1 * Math.log(p_term_1/ p_term_2);
		}
		if (kl <0) {
			System.err.println("KL distance <0");
		}
		return kl;
	}
	
	private double getDistributionCosine(StoryDistribution storyDistr1,
			StoryDistribution storyDistr2) {
		double cosine = 0;
		double L1 = 0, L2 =0;
		HashSet<String> allterm =storyDistr1.getTerms();
		allterm.addAll(storyDistr2.getTerms());
		
		for (String term: allterm) {
			double p_term_1 = getDirichletSmoothing(storyDistr1, term);
			double p_term_2 = getDirichletSmoothing(storyDistr2, term);
			
			cosine += p_term_1 * p_term_2;
			L1 += p_term_1 * p_term_1;
			L2 += p_term_2 * p_term_2;
		}
		if (L1 * L2 >0) cosine = cosine / Math.sqrt(L1 * L2);
		return cosine;
	}
	
	
	private double getTextDifference(StoryDistribution storyDistr1,
			StoryDistribution storyDistr2) {
			int common = 0;
			for (String term: storyDistr1.getTerms())
				if (storyDistr2.getTerms().contains(term)) common +=1;
			int total = storyDistr1.getTerms().size() + storyDistr2.getTerms().size() - common;
			return 1.0 * common / total;
	}

	/**
	 * index word distribution
	 * @param eventList1
	 * @return
	 */
	private StoryDistribution indexStory(ArrayList<Event> eventList1) {
		StoryDistribution storyDistr1 = new StoryDistribution();
		for (Event e: eventList1) {
			EventDistribution eventdistr = new EventDistribution(e.getDescription(), e.getDate().toString());
			for (Entity entity: e.getEntities()) {
				eventdistr.addEntity(new EntityDistribution(entity.getWikiURL()));
			}
			storyDistr1.index(eventdistr);
		}
		return storyDistr1;
	}

	
	public static HashSet<String> getGeneralTopics() {
		HashSet<String> generalTopics = new HashSet<String> ();
		try {
			BufferedReader br = new BufferedReader (new InputStreamReader(
					new FileInputStream("resources/generalTopic.txt"), "utf-8"));
			String gentopic = "";
			while ((gentopic=br.readLine())!=null) {
				gentopic = gentopic.trim();
				generalTopics.add(gentopic.toLowerCase());
			}
			br.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return generalTopics;
	}
	
	
	public void getPartOfStories(String fromdate, String todate) {
		//todo
	}
	
	public static void main(String[] args) {
		/*
		StringBuffer sb = new StringBuffer();
		sb.append("in Iraq. Multiple [[Suicide attack|suicide bombs]] also went off before the base was stormed.<ref>{{cite web|url=http://www.dailystar.com.lb/News/Middle-East/2014/Aug-08/266471-jihadists-capture-key-base-from-syrian-army.ashx|title=Jihadists capture key base from Syrian army|work=The Daily Star Newspaper - Lebanon|accessdate=2 October 2014}}</ref> On 13 August, ISIL forces took the towns of [[Akhtarin]] and Turkmanbareh from rebels in [[Aleppo Governorate|Aleppo]]. ISIL forces also took a handful of nearby villages. The other towns seized include Masoudiyeh, [[Dabiq]] and Ghouz.\n\nAlso on 13 August, around 31 rebel fighters and 8 ISIL fighters were killed in clashes in the Aleppo Province. On 14 August, the [[Free Syrian Army]] commander Sharif As-Safouri admitted working with [[Israel]] and receiving [[Anti-tank warfare|anti-tank]] weapons from Israel and FSA soldiers also received medical treatment inside Israel.<ref>{{cite web|url=http://www.abna.ir/english/service/middle-east-west-asia/archive/2014/08/14/631067/story.html|title=Syrian militant commander admits collaboration with Israel|date=14 August 2014|publisher=|accessdate=2 October 2014}}</ref> On 14 August, the [[Syrian Army]] as well as [[Hezbollah]] militias retook the town of Mleiha in [[Rif Dimashq Governorate]]. The Supreme Military Council of the FSA denied claims of Mleiha's seizure, rather the rebels have redeployed from recent advances to other defensive lines.<ref>{{cite web|url=http://eaworldview.com/2014/08/syria-daily-insurgents-doomed-aleppo/#mleiha2|title=Syria Daily, August 14: Are Insurgents Doomed in Aleppo?|work=EA WorldView|accessdate=2 October 2014}}</ref> Mleiha has been held by the [[Islamic Front (Syria)|Islamic Front]]. Rebels had used the town to fire mortars on government held areas inside Damascus.<ref>[http://uk.reuters.com/article/2014/08/14/uk-syria-crisis-town-idUKKBN0GE0MN20140814 Syrian army takes town outside Damascus ��� report |");
		WikiModel wikiModel = new WikiModel("http://www.mywiki.com/wiki/${image}", "http://www.mywiki.com/wiki/${title}");
        String plainStr = wikiModel.render(new PlainTextConverter(), sb.toString());
        System.out.print(plainStr);
		System.exit(0);
		*/
		
		
		boolean generateFeatureForML = true;
		
		relatedToRelation R = new relatedToRelation();
		try {
			if (generateFeatureForML) R.generateFeatures("resources/related_pairs.txt", "resources/pair.features");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	
}
