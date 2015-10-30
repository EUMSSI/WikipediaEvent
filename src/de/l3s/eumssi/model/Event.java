/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.l3s.eumssi.model;

import java.sql.Date;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class is used as a data structure to store the attributes related to an event
 * @author gtran, gtran@l3s.de
 */

// This class is used to store the fields of the table EventObjects
@XmlRootElement
public class Event implements Comparable<Event> {
	private String eventId;
	private String description;
	private String annotatedDescription;
	private Date date;
	private Category category = null;
	private Story story = null;
	private ArrayList<Entity> entities = new ArrayList<Entity> ();
	private ArrayList<Reference> references = new ArrayList<Reference> ();
	 private float storyRelationConfidence = 1;
    
    
    public Event(){
    	this.eventId = null;
    	this.description = null;
    	this.annotatedDescription = null;
    	this.date = null;
    	this.category = null;
    	this.story = null;
    	this.entities = new ArrayList<Entity> ();
    	this.references = new ArrayList<Reference> ();
    }
    
    public Event(Event e){
    	this.eventId = e.eventId;
    	this.description = e.description;
    	this.annotatedDescription = e.annotatedDescription;
    	this.date = e.date;
    	this.category = e.category;
    	this.story = e.story;
    	this.entities = e.entities;
    	this.references = e.references;
    }
 
    
    public Story getStory() {
		return story;
	}

	public void setStory(Story story) {
		this.story = story;
	}

	public ArrayList<Reference> getReferences() {
		return references;
	}

	public void setReferences(ArrayList<Reference> references) {
		this.references.clear();
		this.references.addAll(references);
	}

    public Category getCategory(){
        return category;
    }
    
    
	public void setCategory(Category category) {
		this.category = category;
	}

	public ArrayList<Entity> getEntities() {
		return entities;
	}



	public void setEntities(ArrayList<Entity> entities) {
		this.entities.clear();
		this.entities.addAll(entities);
	}



	public void setId(String id) {
    	eventId = id;
    }

	

	public String getEventId() {
		return eventId;
	}
	
	public int getEventIdAsNum() {
		return Integer.valueOf(eventId);
	}


	public void setEventId(String eventId) {
		this.eventId = eventId;
	}



	
   
    
    /**
     * This function returns the date
     * @return  Date
     */
    public Date getDate() {
      return date;
    }
    
    /**
     * This function sets the date
     * @param dates     Date
     */
    public void setDate(Date date) {
      this.date = date;
    }
    
    /**
     * This function returns the description
     * @return  Description
     */
    public String getDescription() {
      return description;
    }
    
    /**
     * This function sets the description
     * @param description   Description
     */
    public void setDescription(String description) {
      this.description = description;
    }
    
    public void addEntity(Entity e) {
    	if(!entities.contains(e))
    		entities.add(e);
    }
    
    public void addReference(Reference r) {
    	if(!references.contains(r))
    		references.add(r);
    }
    
    @Override
    public boolean equals(Object other){
        if (other == null) return false;
        if (other == this) return true;
        Event otherEvent = (Event) other;
        try{        	
        	return otherEvent.getEventId().equals(this.getEventId());
        }catch(Exception e){
        	e.printStackTrace();
        	return false;
        }
    }

    public int compareTo(Event e)
    {       	
       return this.date.compareTo(e.date);

    }

	public String getAnnotatedDescription() {
		return annotatedDescription;
	}

	public void setAnnotatedDescription(String annotatedDescription) {
		this.annotatedDescription = annotatedDescription;
	}
    
	
	public String toString(){
		String qoutationMark = "\"";
		String out = "{";
		out+= "Date: " + qoutationMark + getDate() + qoutationMark;
		out+= ", Description: " + qoutationMark + getDescription().replace('"', '\'') + qoutationMark;
		if (getAnnotatedDescription() != null) 
			out+= ", AnnotatedDescription: " + qoutationMark + getAnnotatedDescription().replace('"', '\'') + qoutationMark;
		if (getStory()!=null){
			out+= ", Story: {";
			out+= "Name: " +  qoutationMark + getStory().getName() + qoutationMark;
			out+= ", URL: " + qoutationMark + getStory().getWikipediaUrl() + qoutationMark + "}";
		}
		if (getCategory()!=null){
			out+= ", Category: " + qoutationMark + getCategory().getName() + qoutationMark;
		}
		if(getEntities()!=null){
			if(!getEntities().isEmpty()){
				out+= ", Entities: [";
				int entityCount = 0;
				for(Entity entity:getEntities()){
					entityCount++;
					if (entityCount > 1)
						out+=",";
					out+= "{ Name: " + qoutationMark + entity.getName() + qoutationMark;
					out+= ", URL: " + qoutationMark + entity.getWikiURL() + qoutationMark;
					out+= "}";
				}
				out+= "]";
			}
		}
		if(getReferences()!=null){
			if(!getReferences().isEmpty()){
				out+= ", Sources: [";
				int refCount = 0;
				for(Reference ref: getReferences()){
					refCount++;
					if(refCount>1)
						out+=",";
					out+= "{ Source: " + qoutationMark + ref.getSource() + qoutationMark;
					out+= ", Type: " + qoutationMark + ref.getType() + qoutationMark;
					out+= ", URL: " + qoutationMark + ref.getUrl() + qoutationMark;
					out+= "}";
				}
				out+= "]";
			}
		}
		out+= "}";
		return out;
	}

	public float getStoryRelationConfidence() {
		return storyRelationConfidence;
	}

	public void setStoryRelationConfidence(float storyRelationConfidence) {
		this.storyRelationConfidence = storyRelationConfidence;
	}
	
}
