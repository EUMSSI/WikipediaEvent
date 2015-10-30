package de.l3s.eumssi.indexing;

import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.SolrInputDocument;

import de.l3s.eumssi.model.*;


public class Indexer {

	
	public void indexEvents(ArrayList<Event> events) throws Exception{
		SolrServer server = SolrConnector.getSolrServer();
		int counter = 0;
		
		LinkedList<SolrInputDocument> docs = new LinkedList<SolrInputDocument>();
		SolrInputDocument eventDocument = null;
		
		System.out.println(events.size() + " events to be indexed in Solr ... ");
		
		for(Event event: events){
			eventDocument = new SolrInputDocument();
			
			eventDocument.addField("id", event.getEventId());
			eventDocument.addField("date", event.getDate().toString());
			eventDocument.addField("description", event.getDescription());
		
			if(event.getCategory() != null) {
				eventDocument.addField("categoryId", event.getCategory().getId());
				eventDocument.addField("categoryName", event.getCategory().getName());
			}
			
			if(event.getStory() != null) {
				eventDocument.addField("storyId", event.getStory().getId());
				eventDocument.addField("storyName", event.getStory().getName());
				eventDocument.addField("storyWikiURL", event.getStory().getWikipediaUrl());
			}
			
			if(event.getEntities().size() != 0) {
				ArrayList<String> entities = new ArrayList<String>();
				for(Entity entity: event.getEntities()){
					entities.add(entity.getId()+"###"+entity.getName()+"###"+entity.getWikiURL());
				}
				eventDocument.addField("entities", entities);
			}
			
			if(event.getReferences().size() != 0){
				ArrayList<String> references = new ArrayList<String>();
				for(Reference ref: event.getReferences()){
					references.add(ref.getId()+"###"+ref.getSource()+"###"+ref.getUrl());
				}
				eventDocument.addField("articles", references);
			}
			
			docs.add(eventDocument);
			
			if(docs.size()>100){
				server.add(docs, 100);
				counter+= docs.size();
				System.out.println( counter + " events were indexed so far...");
				docs = new LinkedList<SolrInputDocument>();  
			 }
			
		 }
		 
		 if(docs.size()> 0){
			server.add(docs, 100);
			counter+= docs.size();
			System.out.println( counter + " events were indexed so far...");
		 }
		
	}

	

			 
}


