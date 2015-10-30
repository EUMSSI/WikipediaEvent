package de.l3s.eumssi.enrichment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import de.l3s.eumssi.dao.DatabaseManager;
import de.l3s.eumssi.importing.DBHandler;
import de.l3s.eumssi.model.Entity;
import de.l3s.eumssi.model.Event;
import de.l3s.eumssi.model.Story;

public class EnrichYagoURI {
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
	
	public EnrichYagoURI() {
		yago2db = new DBHandler();
		wikitimesdb = new DatabaseManager();
		try {
			yago2connection = yago2db.openConnection();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public boolean existsInYago(String entity) throws SQLException
	{
		if (yago2connection.isClosed()) yago2connection = yago2db.openConnection();
		String type = null;
		// format the string to meet the format of the table: everything is unNormalized apart from the underscores.
		entity = entity.replace(' ', '_');
		entity = entity.replace("'", "\\'");
		String query = "select * from " + TABLE_NAME + " where " + SUBJECT_NAME + " = '<" + entity + ">' limit 2;";
		//System.out.println(query);
		PreparedStatement pstm = yago2connection.prepareStatement(query);
		
		ResultSet res = pstm.executeQuery();
		
		// check whether the entity belongs to one of the specified types
		while(res.next())
		{
			res.close(); pstm.close();
			return true;
		}
		return false;
	}
	
	public void updateYagoURIOfStory() throws SQLException {
		int found = 0;
		ArrayList<Story> stories = wikitimesdb.getStories();
		for (Story s: stories) {
			String wikipediaUrl = s.getWikipediaUrl();
			String yagouri = wikipediaUrl.replace("http://en.wikipedia.org/wiki/", "http://yago-knowledge.org/resource/");
			String entityName = wikipediaUrl.replace("http://en.wikipedia.org/wiki/", "");
			if (existsInYago(entityName)) {
				//update the column
				wikitimesdb.updateStoryYagoURI(s.getId(), yagouri);
				System.out.println("Infor: " + entityName);
				found+=1;
			}
		}
		System.out.println("Infor: " + found + " stories in Yago2s");
	}
	
	
	public void updateYagoURIOfEntity() throws SQLException {
		int found = 0;
		ArrayList<Entity> entities = wikitimesdb.getEntities();
		for (Entity s: entities) {
			String wikipediaUrl = s.getWikiURL();
			String yagouri = wikipediaUrl.replace("http://en.wikipedia.org/wiki/", "http://yago-knowledge.org/resource/");
			String entityName = wikipediaUrl.replace("http://en.wikipedia.org/wiki/", "");
			if (existsInYago(entityName)) {
				//update the column
				wikitimesdb.updateEntityYagoURI(s.getId(), yagouri);
				System.out.println("Infor: " + entityName);
				found+=1;
			}
		}
		System.out.println("Infor: " + found + " entities in Yago2s");
	}
	public static void main(String[] args) {
		EnrichYagoURI EYU = new EnrichYagoURI();
		try {
			EYU.updateYagoURIOfStory();
			EYU.updateYagoURIOfEntity();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
