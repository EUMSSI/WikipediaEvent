package de.l3s.eumssi.enrichment;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import de.l3s.eumssi.dao.DatabaseManager;
import de.l3s.eumssi.model.Story;

public class RelatedToStory {
	DatabaseManager db = new DatabaseManager();
//	static String wikipediaPageFolder = "/workspaces/WikiTimesProject/software/branches/giang/myWKE/resources/story-wikipedia/";
	static String wikipediaPageFolder = "resources/story-wikipedia/";
	static String WIKIRAW = ".wikiraw.json";
	static String WIKITEXT = ".page.json";
	static String DBPEDIA = ".dbpedia.json";
	
	
	public RelatedToStory() {
		
	}
	
	public static HashSet<String> getGeneralTopics() {
		HashSet<String> generalTopics = new HashSet<String> ();
		File dir = new File(wikipediaPageFolder);
		if (!dir.exists()) dir.mkdirs();
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
	
	
	public static int getIdAsNum(Story s) {
		return Integer.valueOf(s.getId());
	}
	
	private String readAll(Reader rd) throws IOException {
	    StringBuilder sb = new StringBuilder();
	    int cp;
	    while ((cp = rd.read()) != -1) {
	      sb.append((char) cp);
	    }
	    return sb.toString();
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
	
	public HashMap<String, Double> readConfidenceScore (String regression_file) {
		HashMap<String, Double> scores = new HashMap<String, Double> ();
		Scanner sc;
		try {
			sc = new Scanner(new File(regression_file));
			while (sc.hasNext()) {
				String l = sc.nextLine().trim();
				String [] tmp = l.split("\t");
				String id1_id2 = tmp[0];
				System.out.println(l);
				double score = Double.valueOf(tmp[1]);
				scores.put(id1_id2, score);
			}
			sc.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return scores;
	}
	
	public void extractInforBoxRelatedStories () throws IOException {
		HashMap<String, Double> confidenceScore = readConfidenceScore("resources/regeression-score.txt");
		HashSet<String> notStory = getGeneralTopics();
		HashMap<Integer, Integer> redirection = db.getRedirectionRelation();
		ArrayList<Story> stories = db.getStories();
		BufferedWriter pw = new BufferedWriter (new OutputStreamWriter(
				new FileOutputStream(new File("resources/related_pairs.txt")), "UTF-8"
				));
		for (Story s: stories) {
			int storyid = getIdAsNum(s);
			String storyname = s.getName().replace(" ", "_").toLowerCase();
			if (!redirection.containsKey(storyid) && !notStory.contains(storyname)) {
				//get the redirection
				System.out.println(storyid);
				try {
					//checkFile(storyid);
					checkDBpediaFile(storyid);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				//find related entities
				for (String relationCode: new String[] {"partOf", "isRelatedTo", "refersTo"}) { 
					System.out.println(relationCode);
					HashSet<String> links = getLinks(storyid, relationCode);
					if (links==null) continue;
					for (String name: links) {
						String wikiURL = "http://en.wikipedia.org/wiki/" + name.replace(" ", "_"); 
						Story s2 = db.getStoryByURL(wikiURL);
						String norm_name = name.replace(" ", "_").toLowerCase();
						if (!notStory.contains(norm_name) && s2!=null) {
							int storyId2 = getIdAsNum(s2);
							if (storyId2 != storyid) {
								String db_relation = relationCode;
								double confidence = 0.95; 
								//otherwise, use confidence score
								if (relationCode.equals("isRelatedTo")||relationCode.equals("refersTo")) {
									db_relation  = "relatedStory"; //cast to related to relaton
									confidence = 0.2; //reduce the confidence a bit
									String idpair = String.format("%d_%d", storyid, storyId2);
									if (confidenceScore.containsKey(idpair)) confidence = confidenceScore.get(idpair);
									else {
										idpair = String.format("%d_%d",storyId2, storyid);// reorder
										if (confidenceScore.containsKey(idpair)) confidence = confidenceScore.get(idpair);
									}
									if (confidence ==1) confidence = confidence - 0.02; //make sure <1.0 
								}
								
								//insert to db
								db.insertStoryToStoryRelation(storyid, storyId2, db_relation, db.INFORBOX, (float) confidence);
								//String score = "2";
								//if (relationCode.equals("refersTo")) score = "-1";
								//pw.write(score + "\t" + s.getWikipediaUrl() + "\t " + s2.getWikipediaUrl() + "\n");
							}
						}
					}
				}
			}
		}
		pw.close();
	}

	public static ArrayList<String> getLinkInLine(String line) {
		ArrayList<String> internalLinks = new ArrayList<String> ();
		int start_index = line.indexOf("[[");
		while (start_index >-1) {
			int end_index = line.indexOf("]]", start_index +1);
			if (end_index >-1) {//found an internal link
				String link = line.substring(start_index + 2, end_index);
				//check if there is alias
				int alias_pos = link.indexOf("|");
				if (alias_pos >-1) link = link.substring(0, alias_pos);
				internalLinks.add(link.trim().replace(" ","_"));
			}
			else break;
			start_index = line.indexOf("[[", end_index+2);
		}
		return internalLinks;
	}
	
	
	public static ArrayList<String> getPartOfLinkInLine(String line) {
		ArrayList<String> internalLinks = new ArrayList<String> ();
		int start_index = line.indexOf("[[");
		while (start_index >-1) {
			int end_index = line.indexOf("]]", start_index +1);
			if (end_index >-1) {//found an internal link
				String link = line.substring(start_index + 2, end_index);
				//check if there is alias
				int alias_pos = link.indexOf("|");
				if (alias_pos >-1) link = link.substring(0, alias_pos);
				
				internalLinks.add(link.trim().replace(" ","_"));
			}
			else break;
			start_index = line.indexOf("[[", end_index+2);
		}
		return internalLinks;
	}
	//get part of relation from the text
	public HashSet<String> getPartOf(int storyid) throws IOException{
		HashSet<String> partOfs = new HashSet<String> ();
		BufferedReader br = new BufferedReader (new InputStreamReader 
				(new FileInputStream(wikipediaPageFolder + "/" + String.valueOf(storyid) + WIKIRAW), "utf-8"));
		String l ;
		while ((l=br.readLine())!=null){
			if (l.contains("partof")) { // part_of relation
				//get the list of story
				ArrayList<String> rel = getLinkInLine(l);
				partOfs.addAll(rel);
			}
			if (l.startsWith("==")) break; //get into the main section already,  break
		}
		br.close();
		return partOfs;
	}
	
	/**
	 * get from inforbox, abstract and main article links of each paragraph if exists
	 * @param storyid
	 * @return
	 * @throws IOException
	 */
	public HashSet<String> getRelatedToFromInforbox(int storyid) throws IOException{
		HashSet<String> partOfs = new HashSet<String> ();
		BufferedReader br = new BufferedReader (new InputStreamReader 
				(new FileInputStream(wikipediaPageFolder + "/" + String.valueOf(storyid) + WIKIRAW), "utf-8"));
		String l ;
		boolean mainSection = false;
		while ((l=br.readLine())!=null){
			if (!l.contains("partof") && !mainSection) { // part_of relation
				//get the list of story
				ArrayList<String> rel = getLinkInLine(l);
				partOfs.addAll(rel);
			}
			
			if (mainSection && l.startsWith("{{")) { //get main article of the paragraph
				if (l.toLowerCase().startsWith("{{main")) {
					int start_index = l.indexOf("|") +1;
					int end_index = l.indexOf("}}");
					if (start_index >-1 && end_index >-1) {
						String link = l.substring(start_index, end_index).trim().replace(" ","_");
						partOfs.add(link);
					}
				}
			}
			if (l.startsWith("==")) mainSection= true; //get into the main section already,  break
		}
		br.close();
		return partOfs;
	}
	
	
	public HashSet<String> getReferToFromContent(int storyid) throws IOException{
		HashSet<String> partOfs = new HashSet<String> ();
		BufferedReader br = new BufferedReader (new InputStreamReader 
				(new FileInputStream(wikipediaPageFolder + "/" + String.valueOf(storyid) + WIKIRAW), "utf-8"));
		String l ;
		boolean isTextContent = false;
		while ((l=br.readLine())!=null){
			if (l.startsWith("==")) {
				isTextContent = true; //get into the main section already,  break
				if (l.contains("See also") || l.contains("External links")) break;
			}
			
			if (!isTextContent) continue;
			//get the list of story
			ArrayList<String> rel = getLinkInLine(l);
			partOfs.addAll(rel);
			
		}
		br.close();
		return partOfs;
	}
	public HashSet<String> getLinks(int storyid, String property) throws IOException {
		if (property.equals("partOf")) {
			return getPartOf(storyid);
		}
		else if (property.equals("isRelatedTo")) {
			return getRelatedToFromInforbox(storyid);
		}
		else if (property.equals("refersTo")) {
			return getReferToFromContent(storyid);
		}
		return null;
	}
	
	
	/**
	 * read the manual annotation file contains related pairs with there score of relatedness
	 * @throws IOException 
	 */
	public void writeRelatedStoriesAfterManualAnnotation() throws IOException {
		
		HashSet<String> nonstories = getGeneralTopics();
		HashMap<Integer, Integer> redirections = db.getRedirectionRelation();
		BufferedReader br = new BufferedReader (new InputStreamReader 
				(new FileInputStream("resources/manual_annotation_related_pairs.txt"), "utf-8"));
		PrintWriter pw = new PrintWriter (new File("resources/manual_annotation_related_pairs.id"));
		String l = "";
		HashMap<String, Boolean> printed = new HashMap<String, Boolean> ();
		while ((l=br.readLine())!=null) {
			if (l.startsWith("=====")) break;
			String [] tmp = l.trim().split("\t");
			String score = tmp[0];
			String story1_url = tmp[1].trim();
			String story2_url = tmp[2].trim();
			Story s1 = db.getStoryByURL(story1_url);
			Story s2 = db.getStoryByURL(story2_url);
			if (s1==null) {
				System.err.println(tmp[1]);
				System.exit(0);
			}
			
			if (s2==null) {
				System.err.println(tmp[2]);
				System.exit(0);
			}
			int id1 = getIdAsNum(s1);
			int id2 = getIdAsNum(s2);
			
			//resolve redirectio
			id1 = (redirections.containsKey(id1))? redirections.get(id1):id1;
			id2 = (redirections.containsKey(id2))? redirections.get(id2):id2;
			
			if (!nonstories.contains(id1) && ! nonstories.contains(id2)) {
				String pair_1 = String.format("%d_%d", id1, id2);
				String pair_2 = String.format("%d_%d", id2, id1);
				if (!printed.containsKey(pair_1) && !printed.containsKey(pair_2)) {
					//print
					pw.write(score + "\t" + pair_1 + "\n");
					printed.put(pair_1, true);
				}
			}
		
		}
		br.close();
		pw.close();
	}
	
	public static void main(String[] args) {
		RelatedToStory rts = new RelatedToStory();
		
		/*
		try {
			rts.writeRelatedStoriesAfterManualAnnotation();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.exit(0);
		*/
		
		
		String line = "|partof = the [[2012���13 Egyptian protests]] during the [[Egyptian Crisis (2011���present)|Egyptian Crisis]]";
		for (String l : getLinkInLine(line)) System.out.println(l);
		try {
			rts.extractInforBoxRelatedStories();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
