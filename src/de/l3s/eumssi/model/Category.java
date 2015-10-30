package de.l3s.eumssi.model;

import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class Category implements Comparable<Category>{
	private String id;
	private String name;
	private int count;
	

	public Category(){
		id = null;
		name = null;
	}
	
	public Category(String cid, String cname) {
		id = cid;
		name = cname;
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

	public void setId(String id) {
		this.id = id;
	}


	
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	 @Override
	    public boolean equals(Object other){
	        if (other == null) return false;
	        if (other == this) return true;
	        Category otherCategory = (Category) other;
	        try{        	
	        	return otherCategory.getId().equals(this.getId());
	        }catch(Exception e){
	        	e.printStackTrace();
	        	return false;
	        }
	    }
	
	 
		public int compareTo(Category c) {
			if(this.count == c.count) return 0;
			else if (this.count > c.count) return 1;
			else return -1;
		}
	
}
