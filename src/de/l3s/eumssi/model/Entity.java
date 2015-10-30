package de.l3s.eumssi.model;

import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class Entity {
	
	private String name;
	private String id;
	private String wikiURL;
	private int frequency = 1;
	private String type;
	
	public Entity(){
		id = null;
		name= null;		
		wikiURL = null;
		frequency = 0;
		type = null;
	}
	
	public Entity(Entity e) {
		id = e.getId();
		name= e.getName();		
		wikiURL = e.getWikiURL();
		frequency = e.getFrequency();
		type = e.getType();
		
	}
	
	public void increaseFrequency() {
		frequency+=1;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public int getFrequency() {
		return frequency;
	}

	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	public String getWikiURL() {
		return wikiURL;
	}

	public void setWikiURL(String wikiURL) {
		this.wikiURL = wikiURL;
	}
	
	
	
	 public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	    public boolean equals(Object other){
	        if (other == null) return false;
	        if (other == this) return true;
	        Entity otherEntity = (Entity) other;
	        try{        	
	        	return otherEntity.getId().equals(this.getId());
	        }catch(Exception e){
	        	e.printStackTrace();
	        	return false;
	        }
	    }
	
	
}
