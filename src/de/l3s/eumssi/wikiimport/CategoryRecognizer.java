/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.l3s.eumssi.wikiimport;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import de.l3s.lemma.MyLingpipe;
import de.l3s.lemma.lemma;


/**
 * This class is used to stem the categories and remove stopwords
 * @author Giang
 */
public class CategoryRecognizer {
    String catfile = "category.txt";
	
	public CategoryRecognizer() {
		try {
			MyLingpipe.init();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * @param cat
	 * @return category name
	 */
	public String getCategoryName(String cat) {
		String lemmatizedCat = lemma.getLemmatization(cat);
		String [] keywords = lemmatizedCat.split("\\s+");
		ArrayList<String> normalizedCategory = new ArrayList<String>();
		for (String kw: keywords) {
			if (!Stopwords.isStopword(kw)) normalizedCategory.add(kw);
		}
		Collections.sort(normalizedCategory);
		String s= normalizedCategory.toString();
		return s.substring(1, s.length()-1);
	}
        
        public String[] getlem(String s){
            String lemmatizedCat = lemma.getLemmatization(s);
            String [] keywords = lemmatizedCat.split("\\s+");
            return keywords;
        }
	
	/**
	 * performs the category standardization
	 * @param category
	 * @return standardized category
	 * @throws Exception 
	 */
	public void organizeCategory() throws Exception {
		
		HashMap<String, Integer> categories = new HashMap<String, Integer> ();
		BufferedReader br = new BufferedReader (new FileReader(new File(catfile)));
		String cat;
		while ((cat=br.readLine())!=null) {
			cat=cat.trim().toLowerCase();
			String s = getCategoryName(cat);
			int count =0;
			if (categories.containsKey(s)) count = categories.get(s);
			categories.put(s, count+1);
		}
		br.close();
		ArrayList<String> catset = new ArrayList<String> (categories.keySet());
		catset.remove("");
		Collections.sort(catset);
		
		//performing category grouping
		HashMap<String, Boolean> touched = new HashMap<String, Boolean> ();
		HashMap<String, ArrayList<String>> sub_parent = new HashMap<String, ArrayList<String>> ();
		
		for (String key: catset) { touched.put(key, false);}
		for (String key: catset) {
//			System.out.print(String.format("%s %d\n", key, categories.get(key)));
			if (touched.get(key)) continue;
			
			HashSet<String> parent = new HashSet<String> ();
			ArrayList<String> children = new ArrayList<String> ();
			children.add(key);
			for (String keyword: key.split("[\\s+,]")) 
				if (keyword.length()>0) parent.add(keyword);
			
			touched.put(key, true);
			boolean stop = false;
			while (!stop) {
				stop = true;
				//chasing the category
				
				for (String friend:catset) {
					if (!touched.get(friend) && isCommon(friend, parent)) {
						stop = false;
						touched.put(friend, true);
						children.add(friend);
						for (String keyword: friend.split("[\\s+,]"))
							if (keyword.length()>0) parent.add(keyword); 
					}
				}
			}
			String parentStr = parent.toString();
			parentStr = parentStr.substring(1, parentStr.length()-1);
			sub_parent.put(parentStr, children);
		}
		
		//printing
		toString(sub_parent);
	}
	
	
	public String toString(HashMap<String, ArrayList<String>> subparent) {
		StringBuffer sb = new StringBuffer();
		for (String parent: subparent.keySet()) {
			if (parent.length()==0) continue;
			sb.append("<category name=\"" + parent + "\">\n");
			for (String child: subparent.get(parent)) {
				sb.append("\t<sub>");
				sb.append(child);
				sb.append("</sub>\n");
			}
			sb.append("</category>\n");
		}
		System.out.println(sb.toString());
		return sb.toString();
	}
	
	/**
	 * check if a category is in a sub-relation to a parent category
	 * @param cat
	 * @param parent
	 * @return
	 */
	public boolean isCommon(String cat, HashSet<String> parent) {
		for (String keyword: cat.split("[\\s+,]")) {
			if (parent.contains(keyword)) return true;
		}
		return false;
	}
	
	//@usage
	/*public static void main(String[] args) {
		CategoryRecognizer CR = new CategoryRecognizer();
		String s = "Kanik is a very good boy. He loves to do writing.";
                String val = CR.getCategoryName(s);
                String [] keywords = val.split(",");
                for(String k : keywords)
                    System.out.println(k.trim());
	}*/
    
}
