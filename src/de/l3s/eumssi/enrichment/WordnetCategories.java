package de.l3s.eumssi.enrichment;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.didion.jwnl.*;
import net.didion.jwnl.data.*;
import net.didion.jwnl.dictionary.Dictionary;
public class WordnetCategories {

	public static Dictionary wordnet = null;

	public static void init() {
		// Initialize the database
		// You must configure the properties file to point to your dictionary files
		WordNetHelper.initialize("src/main/resources/properties.xml");
		wordnet = Dictionary.getInstance();
	}

	public static List<String> listHypernym(Dictionary wordnet, String original, int level) throws JWNLException {
		IndexWord token = wordnet.lookupIndexWord(POS.NOUN, original); //word is a string
		if (token == null) return null;
		Synset[] senses = token.getSenses();
		List<String> hypernyms = Lists.newArrayList();
		for (int i = 0; i < senses.length; i++) {
			try {
				//CATEGORY is the pointer type of the synset containing the domains
				Pointer[] pointerArr = senses[i].getPointers(PointerType.HYPERNYM);

				for (Pointer pointer : pointerArr) {
					Synset syn = pointer.getTargetSynset();
					Word[] words = syn.getWords();
					for (Word word : words) {
						//System.out.println(word.getLemma().trim().toLowerCase());
						Synset wsyn = word.getSynset();
						int j = 0;
						level++;
						for (Word w : wsyn.getWords()) {
							hypernyms.add(w.getLemma());
							j++;
							if (j == 4) break;
							if (level == 4) return hypernyms;
							List<String> hyplevel = listHypernym(wordnet, w.getLemma(), level);
							if (hyplevel != null) hypernyms.addAll(hyplevel);
						}
					}
				}
			}
			catch (NullPointerException e) {
			}
		}

		return hypernyms;
	}

	public static void listSynset(Dictionary wordnet, String original) throws JWNLException {
		IndexWord token = wordnet.lookupIndexWord(POS.NOUN, original); //word is a string
		Synset[] senses = token.getSenses();
		List<String> synset = Lists.newArrayList();
		for (Synset sense : senses) {
			System.out.println(sense.getWord(0).getLemma());
		}
	}


	public static List<String> listCategories(Dictionary wordnet, String original) throws JWNLException {
		IndexWord token = wordnet.lookupIndexWord(POS.NOUN, original); //word is a string
		Synset[] senses = token.getSenses();
		List<String> hypernyms = Lists.newArrayList();
		for (int i = 0; i < senses.length; i++) {
			try {
				//CATEGORY is the pointer type of the synset containing the domains
				Pointer[] pointerArr = senses[i].getPointers(PointerType.CATEGORY);

				for (Pointer pointer : pointerArr) {
					Synset syn = pointer.getTargetSynset();
					Word[] words = syn.getWords();
					for (Word word : words) {
						//System.out.println(word.getLemma().trim().toLowerCase());
						Synset wsyn = word.getSynset();
						int j = 0;
						for (Word w : wsyn.getWords()) {
							j++;
							if (j == 2) break;
							hypernyms.add(w.getLemma());
						}
					}
				}
			}
			catch (NullPointerException e) {
			}
		}

		return hypernyms;
	}
	
	/**
	 * Television_channels_and_stations_established_in_1991
	 * @param category
	 * @return
	 */
	public static String cleanWikiCat(String category) {
		StringBuffer cat = new StringBuffer();
		try {
			Map<String, POS> map = WordNetHelper.getPOS(category);

			Set<String> words = map.keySet();
			
			for (String word : words) {
				if (map.get(word).getLabel().equals("noun"))
					cat.append(word).append(" ");
			}
			
			return cat.toString();
	
		} catch (JWNLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return category;
	}
	
	public static String clean(String category) {
		StringBuffer cleaned = new StringBuffer();
		Set<String> ls = Sets.newHashSet();
		String[] cat = category.split("\\s+");
		for (String c : cat) {
			ls.add(c);
		}
		
		for (String c : ls) {
			cleaned.append(c).append(" ");
		}
		return cleaned.toString();
	}
	
	public static Set<String> _cleanWikiCat(String category) {
		category = clean(category);
		Set<String> ls = Sets.newHashSet();
		try {
			Map<String, POS> map = WordNetHelper.getPOS(category);

			Set<String> words = map.keySet();
			
			for (String word : words) {
				System.out.println(word);
				if (map.get(word).getLabel().equals("noun"))
					ls.add(word);
			}
			
			return ls;
	
		} catch (JWNLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return ls;
	}
	
	public static void main(String[] args) {
		init();
		//cleanWikiCat("squash");
		try {
			List<String> hyps = listHypernym(wordnet, "music", 0);
			for (String hyp : hyps) {
				System.out.println(hyp);
			}
		} catch (JWNLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void listGlosses(IndexWord word) throws JWNLException {
		System.out.println("\n\nDefinitions for " + word.getLemma() + ":");
		// Get an array of Synsets for a word
		Synset[] senses = word.getSenses();
		// Display all definitions
		for (int i = 0; i < senses.length; i++) {
			System.out.println(word + ": " + senses[i].getGloss());
		}    
	}

}
