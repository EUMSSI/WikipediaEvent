package de.l3s.eumssi.ml;

import java.util.ArrayList;
import java.util.Random;

/**
 * modelling an event
 * @author giangbinhtran
 *
 */
public class EventDistribution {
	String beginingDate ;
	String endDate ;
	String description;
	public ArrayList<String> descriptionTerm;
	String date;
	ArrayList<EntityDistribution> entities;
	String timeline;
	String category;
	ArrayList<Integer> source_ids;
	double pred_timeline_score;
	String id;
	
	
	int storyid = 0;
	public EventDistribution(String desc, String d) {
		timeline = "";
		description = desc;
		date = d;
		entities = new ArrayList<EntityDistribution> ();
		pred_timeline_score = 1.0; 
		category = "";
		source_ids = new ArrayList<Integer> ();
		beginingDate = date;
		endDate = date;
		
		descriptionTerm = new ArrayList<String> ();
		
		for (String term: desc.split("\\s+")) {
			if (term.endsWith(":")) term = term.replace(":","");
			if (!Stopwords.isStopword(term) && !term.equals("cite"))
				descriptionTerm.add(term);
		}
	}
	
	public void setStoryID (int sid)  {
		storyid = sid;
	}
	
	
	public void updateBeginingDate(String _d) {
		if (_d.compareTo(beginingDate)<0) beginingDate = _d;
	}
	public void updateEndDate(String _d) {
		if (_d.compareTo(endDate)>0) endDate = _d;
	}
	
	public String getBeginingDate() {
		return beginingDate;
	}
	
	public String getEndDate() {
		return endDate;
	}
	
//	public void addDescription(String newdesc) {
//		description += " " + newdesc;
//	}
	public String getRandomEntity () {
		Random r = new Random();
		int n_entity = entities.size();
		String random_entity = "";
		do {
			random_entity = entities.get(r.nextInt(n_entity)).getName();
		} while (random_entity.contains("http://"));
		return random_entity;
	}
	
	public int getRandomSource() {
		Random r = new Random();
		int n_sources =source_ids.size();
		if (n_sources==0) return -1;
		int random_source = source_ids.get(r.nextInt(n_sources));
		return random_source;
	}

	public void setSources(String sourceIdStr) {
		String[] tmp = sourceIdStr.split("\\$");
		for (String id: tmp) {
			if (id.trim().length()==0) continue;
			source_ids.add(Integer.valueOf(id.trim()));
		}
	}
	public void addSource(String sourceid) {
		source_ids.add(Integer.valueOf(sourceid));
	}
	public ArrayList<Integer> getSource_IDs() {
		return source_ids;
	}
	
	public int getStoryID() {return storyid;}
	public void setDate(String d) {
		date =d;
	}
	
	public String getCategory() {
		return category;
	}
	
	public void setCategory(String cat) {
		category = cat;
	}
	public String getTimeline() {
		return timeline;
	}
	
	public void setTimeline(String s) {
		timeline = s;
	}
	
	public void setPredTimelineScore(double pred) {
		pred_timeline_score = pred;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String getDate() {
		return date;
	}
	
	public ArrayList<EntityDistribution> getEntities() {
		return entities;
	}
	
	public void addEntity(EntityDistribution e) {
		entities.add(e);
	}
	
	public int n_entities()
	{
		return entities.size()-1;
	}
	/**
	 * returns description, date and list of entities
	 */
	public String toString() {
		String r = String.valueOf(this.id) + "\t";
		r += description + "\t" + date;
		for (EntityDistribution en: entities) {
			r += "\t" + en.getName();
		}
		
		//add source
		for (int sid: source_ids) {
			r += "\ts:" + String.valueOf(sid);
		}
		
		//add id
		//r += "\tid:" + this.id;
		return r;
	}
	
	
	public void setID (String _id) {id = _id;}
	public String getID() {return id;}
	
	/**
	 * return all metadata of the events
	 * @return
	 */
	public String toFullPrint() {
		//adding timeline information
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("%.5f\t%s\t%s\t%s", pred_timeline_score, timeline, date, description));
		for (EntityDistribution en: entities) {
			sb.append("\t" + en.getName());
		}
		return sb.toString();
	}
	
	public static void main(String[] args) {
		String s = "$ 1232";
		for (String t: s.split("\\$")) System.out.println(t);;
		EventDistribution e = new EventDistribution(" ", " ");
		e.setSources("$ 133261");
		
	}
}


