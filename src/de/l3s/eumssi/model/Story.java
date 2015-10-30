/**
 * light-weighted story object for event
 */
package de.l3s.eumssi.model;

import java.sql.Date;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;



@XmlRootElement
public class Story implements Comparable<Story>{
	private String id, name, wikipediaUrl;
	
	private ArrayList<Event> events;
	
	private Date startDate;
	private Date endDate;

	
	public Story(){
		this.id = null;
		this.name = null;
		this.wikipediaUrl = null;
		this.startDate = Date.valueOf("9999-12-31");
		this.endDate = Date.valueOf("1000-12-31");
		events = new ArrayList<Event>();
	}
	
	public Story(String sid, String sname, String url) {
		id = sid;
		name = sname;
		wikipediaUrl = url;
		this.startDate = Date.valueOf("9999-12-31");
		this.endDate = Date.valueOf("1000-12-31");
		events = new ArrayList<Event>();
	}
	
	public String getWikipediaUrl() {
		return wikipediaUrl;
	}
	public void setWikipediaUrl(String url) {
		wikipediaUrl = url;
	}
	public String getId() {
		return id;
	}
	public int getIdAsNum() {
		return Integer.valueOf(id);
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	
	
	 @Override
	    public boolean equals(Object other){
	        if (other == null) return false;
	        if (other == this) return true;
	        Story otherStory = (Story) other;
	        try{        	
	        	return otherStory.getId().equals(this.getId());
	        }catch(Exception e){
	        	e.printStackTrace();
	        	return false;
	        }
	    }

	 
    public int compareTo(Story e)
    {       	
       return this.name.compareTo(e.name);

    }

    
    public void addEvent(Event event){
		if (!events.contains(event)){
			events.add(event);
			if (event.getDate().before(startDate))
				startDate = event.getDate();
			if (event.getDate().after(endDate))
				endDate = event.getDate();
		}
	}
    
    
	public ArrayList<Event> getEvents() {
		return events;
	}

	public void setEvents(ArrayList<Event> events) {
		this.events.clear();
		this.events.addAll(events);
	}

	public Date getStartDate() {
		return startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	
	

}
