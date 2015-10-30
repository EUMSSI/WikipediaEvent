package de.l3s.eumssi.enrichment;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.l3s.eumssi.dao.DatabaseManager;
import de.l3s.eumssi.model.Entity;
import de.l3s.eumssi.model.Story;

public class EnrichStoryRedirection {
	
	DatabaseManager db;
	
	public EnrichStoryRedirection(){
		db = new DatabaseManager();
	}
	
	
	private static String readPage(String url_s) throws Exception {
		URL url = new URL(url_s);
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(url.toURI());
        HttpResponse response = client.execute(request);

        Reader reader = null;
        try {
            reader = new InputStreamReader(response.getEntity().getContent());

            StringBuffer sb = new StringBuffer();
            {
                int read;
                char[] cbuf = new char[1024];
                while ((read = reader.read(cbuf)) != -1)
                    sb.append(cbuf, 0, read);
            }

            return sb.toString();

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
    }
	
	private static HashMap<String, String> parseJson(String jsContent) {
		HashMap<String, String> s = new HashMap<String, String>();
		JsonObject json = (JsonObject) new JsonParser().parse(jsContent);
		Set<Map.Entry<String, JsonElement>> set = json.entrySet();
		
		Iterator<Map.Entry<String, JsonElement>> iterator = set.iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, JsonElement> entry = iterator.next();
			if (entry.getKey().equals("parse")) {
				JsonObject js = (JsonObject) new JsonParser().parse(entry.getValue().toString());
				Set<Map.Entry<String, JsonElement>> elements = js.entrySet();
				
				Iterator<Map.Entry<String, JsonElement>> i = elements.iterator();
				while (i.hasNext()) {
					Map.Entry<String, JsonElement> e = i.next();
					s.put(e.getKey(), e.getValue().toString());
				}
				break;
			}
			
		}
		return s;
	}

	
	public static String decode(String url) {
		try {
			url = java.net.URLDecoder.decode(url, "UTF-8");
		} catch (Exception e) {
			System.err.println("url (" + url + ") is not able to be converted into UTF8 format");
		}
		return url;
	}
	
	public String getEntityName(String url){
		url = decode(url);
		return getEntityURL(url).replace("_", " ");
	}
	
	public static String getEntityURL(String url){
		url = decode(url);
		String[] url_parts = url.split("/");
		return url_parts[url_parts.length-1];
	}
	
	public static String getRedirectURL(String wikipage) {
		//String url = wikipage.replace("http://en.wikipedia.org/wiki/", "");
		String EMPTY = "";
		String url = getEntityURL(wikipage);
		System.out.println("Finding redirected links for " + url);
		String pageContent = null;
		try {
			pageContent = readPage("http://en.wikipedia.org/w/api.php?action=parse&format=json&page=" + url);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Could not read Wikipedia page, check Internet connection");
			return EMPTY;
		}
		String content = parseJson(pageContent).get("text");
		if (content==null) return EMPTY;
		int pos = content.indexOf("redirectText",0);
		//System.out.println(pos);
		if (pos>=0) {//extract redirect url
			pos =content.indexOf("<a href=\\\"",pos+1);
			int to = content.indexOf(" title=\\", pos);
			if (to>0) {
				String redirectedURL = "http://en.wikipedia.org/wiki/" +content.substring(pos + "<a href=\\\"".length() +1, to-2);
				//cleaning
				redirectedURL = redirectedURL.replace("w/index.php?title=", "");
				redirectedURL = redirectedURL.replace("&amp;redirect=no", "");
				redirectedURL = decode(redirectedURL);
				return redirectedURL;
			}
		}
		return EMPTY;
	}
	
	
	
	/**
     * update redirect column in WikiRef table
     */
    public void updateRedirect(String fromURL, String redirectedURL) throws Exception{
    	redirectedURL = java.net.URLDecoder.decode(redirectedURL, "UTF-8");
    	Story fromStory = db.getStoryByURL(fromURL);
    	
    	int from_storyID = fromStory==null? -1: Integer.valueOf(fromStory.getId());
    	
    	if (from_storyID <0) {
    		throw new Exception ("From URL does not exists!" + fromURL);
    	}
    	Story toStory = db.getStoryByURL(redirectedURL);
    	int to_storyID = toStory==null?-1: Integer.valueOf(toStory.getId());
    			
    	if (to_storyID <0) {
    		//check if the entity exist
//    		Entity e = db.getEntityByURL(redirectedURL);
//    		int wikiRefId = e==null? -1: Integer.valueOf(e.getId());
//    		
//    		if (wikiRefId <0) {
//    			
//    			String name = redirectedURL.replace(db.conf.getProperty("wiki_url_prefix"), "");
//    	    	name = name.replace("_", " ");
//    			e = db.storeEntity(name, redirectedURL.replace(db.conf.getProperty("wiki_url_prefix"), ""));
//    		}
    		
//    		wikiRefId = e==null? -1: Integer.valueOf(e.getId());
//    		String label = e==null? null: e.getName();

    		String label = getEntityName(redirectedURL);
    		toStory = db.storeNewsStory(label, redirectedURL);
    		to_storyID = toStory==null?-1: Integer.valueOf(toStory.getId());
    		if (to_storyID <0) {
    			throw new Exception ("Not able to create new story, probably there conflicts with constraints in DB@ NewsStory table");
    		}
    	} 
    	if (from_storyID!= to_storyID) {
    		db.insertStoryToStoryRelation(from_storyID, to_storyID, "isRedirectedTo", (float) 1.0);
    	}
    }

	
    /**
     * looking for redirection of all stories in database
     * it just can run once for a month, for example
     */
	public void resolveStoryRedirection() {
		ArrayList<Story> stories = db.getStories();
		int ii = 0;
		for (Story story: stories) {
			String url = story.getWikipediaUrl();
			System.out.println(ii++);
			String redirected = "";
			try {
				redirected = getRedirectURL(url);
				if (redirected.length() >0) {
					System.out.println(String.format("%d\t%s\t%s\n", ii,  url, redirected));
					//update database
					updateRedirect(url, redirected);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		//finally update the Event Story Relation table
		HashMap<Integer, Integer> storyRedirections = db.getRedirectionRelation();
		updateEventStoryRelation(storyRedirections);
		// remember to close the connection to DB only once at the end
		db.closeConnection();
	}
	
	/**
	 * get the redirection of story given its wikipedia URL
	 * it instantly update Event Story Relation table
	 * @param wikipediaURL
	 */
	public void updateStoryRedirection(String wikipediaURL) {
		String redirected = "";
		try {
			redirected = getRedirectURL(wikipediaURL);
			if (redirected.length() >0) {
				//update Redirection table
				updateRedirect(wikipediaURL, redirected);
				//update Event Story Relation table
				updateEventStoryRelation(wikipediaURL, redirected);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void updateEventStoryRelation(String wikipediaURL, String redirected) {
		HashMap<Integer, Integer> storyRedirection = new HashMap<Integer, Integer> ();
		Story fromStory = db.getStoryByURL(wikipediaURL);
		Story toStory = db.getStoryByURL(redirected);
		storyRedirection.put(Integer.valueOf(fromStory.getId()), Integer.valueOf(toStory.getId()));
		updateEventStoryRelation(storyRedirection);
	}
	
	/**
	 * update the Event Story Relation table after redirections
	 */
	public void updateEventStoryRelation(HashMap<Integer, Integer> storyRedirections) {
		//read redirection relation
		HashMap<Integer, ArrayList<Integer>> eventStoryRelation = db.getEventStoryRelation(storyRedirections.keySet());
		
		for (int fromID: storyRedirections.keySet()) {
			int toID = storyRedirections.get(fromID);
			if (!eventStoryRelation.containsKey(fromID)) continue;
			for (int eventID: eventStoryRelation.get(fromID)) {
				System.out.println(eventID + "-" + fromID + "-" + toID);
				db.updateEventToStoryRelation(eventID, fromID, toID);
			}
		}
	}
	
}
