/**
 * code to NERD entities in event
 */
package de.l3s.eumssi.enrichment;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import de.l3s.eumssi.dao.DatabaseManager;
import de.l3s.eumssi.model.Entity;
import de.l3s.eumssi.model.Event;

public class EntityEnrichment {
	DatabaseManager db;
	
	public EntityEnrichment() {
		db = new DatabaseManager();
	}
	
	/**
	 * print event for MPI friend
	 * @param file
	 * @throws IOException 
	 */
	public void printEvent(String file) throws IOException {
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
								new FileOutputStream(file), "utf-8"));
		ArrayList<Event> events = db.getEvents();
		bw.write("[\n");
		for (int i = 0;i < events.size(); i++) {
			Event e = events.get(i);
			JSONObject obj = new JSONObject();
			obj.put("annotation", e.getAnnotatedDescription());
			obj.put("description", e.getDescription());
			obj.put("date", e.getDate().toString());
			ArrayList<Entity> entities = e.getEntities();
			JSONArray jsonEntities = new JSONArray();
			for (Entity ent: entities) {
				JSONObject json_ent = new JSONObject();
				json_ent.put("url", ent.getWikiURL());
				json_ent.put("name", ent.getName());
				jsonEntities.add(json_ent);
			}
			obj.put("entities", jsonEntities);
			bw.write(obj.toJSONString());
			if (i <events.size() -1) bw.write(",\n");
		}
		
		bw.flush();
		bw.write("]");
		bw.close();
	}
	
	public static void main(String[] args) {
		EntityEnrichment ee = new EntityEnrichment();
		String filename = "/workspaces/WikiTimesProject/NERD/data.json";
		try {
			ee.printEvent(filename);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
