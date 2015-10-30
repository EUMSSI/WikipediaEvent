package de.l3s.eumssi.wikiimport;


import java.util.ArrayList;

import de.l3s.eumssi.model.*;

/*
 * This class is used to insert event object into database
 * @author : SUDHIR KUMAR SAH
 */
public class DBOperation
{
	ContentHandling ch = new ContentHandling();
	/*
	 * This method is used to insert event_object into the database
	 */
	public int insertEventObjIntoDB(Event eventObj)	{
		// Insert into Category table
		if(eventObj.getCategory()!= null)
			ch.insertIntoCategory(eventObj.getCategory().getName(), 0);
		
		/*
		 * Insert into News Story Table.
		 * For each News Story, there should be entry in the WikiRef table
		 * so first insert into WikiRef table
		 * 
		 */
		int wikiid = 0;
		if(eventObj.getStory() != null) //make sure we have News Stories
		{
			wikiid = ch.insertIntoWikiRef(eventObj.getStory().getName(), eventObj.getStory().getWikipediaUrl());
			ch.insertIntoNewsStory(eventObj.getStory().getName(),0,"f",wikiid);
			
			 //insert into StorycCategoryRelation table 
			ch.insertIntoStoryCategoryRelation(eventObj.getStory().getName(), eventObj.getCategory().getName());
			
		}
	
		//insert into source table
		ArrayList<String> srcurl = new ArrayList<String>();
		if(eventObj.getReferences().size() != 0)
		{
			for(Reference src : eventObj.getReferences())
			{
				ch.insertIntoSource(src.getSource(),src.getUrl(),"1993-01-01", "None");
				srcurl.add(src.getUrl());
			}
		}
		
		//insert into WikiRef Table
		ArrayList<String> entityurl = new ArrayList<String>();
		if(eventObj.getEntities().size() != 0)
		{
			for(Entity entity : eventObj.getEntities())
			{
				ch.insertIntoWikiRef(entity.getName(), entity.getWikiURL());
				entityurl.add(entity.getWikiURL());
				//System.out.println("\n"+entity.getName());
			}
		}
		
		//System.out.println("--------------");
		//insert into Event table
		int eventid = 0;
		//News Story exists
		if(eventObj.getStory() != null)
		{
			eventid = ch.insertIntoEvent(eventObj.getDescription(), eventObj.getStory().getName(), eventObj.getCategory().getName(), eventObj.getDate(), srcurl, wikiid);
		}
		//news Story doesn't exists
		else
		{
			eventid = ch.insertIntoEvent(eventObj.getDescription(), null, eventObj.getCategory().getName(), eventObj.getDate(), srcurl, 0);
		}
		//insert into EventEntityRelation table
		ch.insertIntoEventEntityRelation(eventid, entityurl);
		
		//clear all the array lists
		srcurl.clear();
		entityurl.clear();
		
		return eventid;
	}
	
	
	
	
	
	/*
	 * This method is used to insert list of event_objects into database
	 * 
	 * @parameter : ArrayList of event_objeccts
	 * 
	 * @Return : insert list of event_objects into database
	 * 
	 */
	
	public void insertEventObjsListIntoDB(ArrayList<Event> eventList)
	{
		if(eventList.size() != 0)
		{
			for(Event e : eventList)
			{
				this.insertEventObjIntoDB(e);
			}
		}
	}
}
