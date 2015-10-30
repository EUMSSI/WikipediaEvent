/**
 * this class handles event classification
based on opencalais (12 Sep 2014)
 * 
 */
package de.l3s.eumssi.importing;

import java.util.ArrayList;
import java.util.HashMap;
public class OpenCalaisCategorization {
	private static HashMap<String, Integer> opencalais = new HashMap<String, Integer> ();
	
	public OpenCalaisCategorization() {
		initialize(opencalais);
	}
	
	private void initialize(HashMap< String, Integer> op) {
		op.put( "Business_Finance", 1);
		op.put( "Disaster_Accident", 2);
		op.put( "Education", 3);
		op.put( "Entertainment_Culture", 4);
		op.put( "Environment", 5);
		op.put( "Health_Medical_Pharma", 6);
		op.put( "Hospitality_Recreation", 7);
		op.put( "Human_Interest", 8);
		op.put( "Labor", 9);
		op.put( "Law_Crime", 10);
		op.put( "Politics", 11);
		op.put( "Religion_Belief", 12);
		op.put( "Social_Issues", 13);
		op.put( "Sports", 14);
		op.put( "Technology_Internet", 15);
		op.put( "Weather", 16);
		op.put( "War_Conflict", 17);
		op.put( "Other", 18);
		op.put( "International_Relation", 19);
	}
	/**
	 * convert category from WCEP category to openCalais category
	 * @param WCEPcategory
	 * @return list of topics
	 */
	public String covertToOpenCalaisCategory(String WCEPcategory) {
		WCEPcategory = WCEPcategory.toLowerCase();
		ArrayList<String> r = new ArrayList<String> ();
		if (WCEPcategory.contains("disaster") || WCEPcategory.contains("accident")) r.add("Disaster_Accident");
		
		if (WCEPcategory.contains("history") || WCEPcategory.contains("physic") || WCEPcategory.contains("literature")
			||WCEPcategory.contains("education") || WCEPcategory.contains("exploration") ||  
			WCEPcategory.contains("geography")) 
			r.add("Education");
		
		if (WCEPcategory.contains("environment") ||WCEPcategory.contains("enviroment")||WCEPcategory.contains("ecology")
				|| WCEPcategory.contains("weather") )
			r.add("Environment");
		
		if (WCEPcategory.contains("health") || WCEPcategory.contains("medicine") || WCEPcategory.contains("wellnes") ) 
			r.add("Health_Medical_Pharma");
		
		if (WCEPcategory.contains("science") || WCEPcategory.contains("technology") || WCEPcategory.contains("innovation")  ) 
			r.add("Technology_Internet");
		
		if (WCEPcategory.contains("sport") && ! WCEPcategory.contains("transport")) r.add("Sports");
		
		if (WCEPcategory.contains("human") ||
				WCEPcategory.contains("art") || WCEPcategory.contains("entertainment") || 
				WCEPcategory.contains("culture") || WCEPcategory.contains("medium")) 
			r.add("Entertainment_Culture");
		
		if (WCEPcategory.contains("society") || WCEPcategory.contains("death") || WCEPcategory.contains("life") 
				|| WCEPcategory.contains("transport"))
			r.add("Social_Issues");
		if (WCEPcategory.contains("religio")) r.add("Religion_Belief");
		
		if (WCEPcategory.contains("attack") || WCEPcategory.contains("conflict") || WCEPcategory.contains("war")) 
			r.add("War_Conflict");
		
		if (WCEPcategory.contains("politic") || WCEPcategory.contains("protest")
				|| WCEPcategory.contains("election") || WCEPcategory.contains("government"))
			r.add("Politics");
		
		if (WCEPcategory.contains("legal") || WCEPcategory.contains("law") || WCEPcategory.contains("crime")
				|| WCEPcategory.contains("arrest") || WCEPcategory.contains("detention")) 
			r.add("Law_Crime");
		
		if (WCEPcategory.contains("economy") || WCEPcategory.contains("finance")
				|| WCEPcategory.contains("business") || WCEPcategory.contains("economic")) 
			r.add("Business_Finance");
		
		if (WCEPcategory.contains("international") && WCEPcategory.contains("relation"))
			r.add("International_Relation");
		
		if (WCEPcategory.contains("event")||WCEPcategory.contains("general")) r.add("Other");
		
		
		if (r.size()==0 && !WCEPcategory.equals("None")) r.add("Other");
		return r.get(0);
	}
 }
