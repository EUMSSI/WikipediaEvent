package de.l3s.eumssi.importing;

import java.sql.Date;
import java.util.ArrayList;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import de.l3s.eumssi.model.Category;
import de.l3s.eumssi.model.Entity;
import de.l3s.eumssi.model.Event;
import de.l3s.eumssi.model.Reference;
import de.l3s.eumssi.model.Story;

public class WikiParserFromMay2006ToNow extends WikiParser {

	
	@Override
	protected ArrayList<Event> parseMonthPage(Document doc) {
		ArrayList<Event> events = new ArrayList<Event>();

		Elements days = doc.select("div#bodyContent div#mw-content-text.mw-content-ltr table tbody tr td table.vevent");
		System.out.println(" found " + days.size() + " dates!");

		for (Element eachday : days) {
			String actualDate = "";
			String categoryValue = "";

			Elements date = eachday.select("tbody tr td table.plainlinks tbody tr td span.summary");
//			System.out.println("date.text() --> "  + date.text());
			if (!date.text().contains(getYear()+""))
				continue;
			actualDate = getCorrectDate(date.text());
//			System.out.println(date.text() + " -----> " + actualDate);
			
			Elements categories = eachday.select("tbody tr td.description");

			Category cat = null;

			for (Element category : categories) { // This will loop just once
													// since size is 1
				for (Element e : category.children()) {
					if (e.tagName().equals("dl")) {
						for(Element categoryElement: e.children()){
							if(categoryElement.tagName().equals("dt")){
								categoryValue = categoryElement.text();
								cat = new Category();
								cat.setName(getOpenCalaisCategory(categoryValue));
//								System.out.println("Category found: " + categoryValue + " , mapped to ----> " + cat.getName());
								break;
							}
						}
					}else if (e.tagName().equals("p")) {
						for(Element categoryElement: e.children()){
							if(categoryElement.tagName().equals("b")){
								categoryValue = categoryElement.text();
								cat = new Category();
								cat.setName(getOpenCalaisCategory(categoryValue));
//								System.out.println("Category found: " + categoryValue + " , mapped to ----> " + cat.getName());
								break;
							}
						}
						
//					if (e.tagName().equals("dl") || e.tagName().equals("p")) {
//						if (e.child(0).tagName().equals("dl")) {
//							categoryValue = e.text();
//							cat = new Category();
//							cat.setName(getOpenCalaisCategory(categoryValue));
					} else {
						if (e.tagName().equals("ul")) {
							Elements stories = e.children(); // This contains different stories including headings of story
							for (Element li : stories) {
								Elements uls = li.children();
								boolean hasUL = false; // If li has ul then it implies that it contains a news story
								for (Element ul : uls) {
									if (ul.tagName().equals("ul")) {
										hasUL = true; // news story is there
										Node storyNode = li.childNode(0);
										Elements eventsNodes = ul.children(); // Now  we get inside the li element
										for (Element eventNode : eventsNodes) { // Pick up one li
											Event event = extractDescriptionAndLinks(eventNode);
											event.setCategory(cat);
											try{
												event.setDate(Date.valueOf(actualDate));
											}catch(Exception ex){
												ex.printStackTrace();
												System.out.println("Date: " + actualDate);
												continue;
											}

											if (storyNode.attr("title") != null && storyNode.attr("href") != null) {
												if (!storyNode.attr("title").isEmpty() && !storyNode.attr("href").isEmpty()) {
													if (isValidWikiURL(storyNode.attr("href"))){
														Story story = new Story();
														// story.setName(st.attr("title"));
														story.setName(getEntityName(storyNode.attr("href")));
														story.setWikipediaUrl(getEntityURL(storyNode.attr("href")));
														event.setStory(story);
													}
												}
											}
											events.add(event);
										}
									}
								}

								// If it doesn't has ul => no newsStory so we
								if (!hasUL) {
									Event event = extractDescriptionAndLinks(li);
									event.setCategory(cat);
									try{
										event.setDate(Date.valueOf(actualDate));
									}catch(Exception ex){
										ex.printStackTrace();
										System.out.println("Date: " + actualDate);
										continue;
									}
									events.add(event);
								}
							}
						}
					}
				}
			}
		}

		return events;
	}

	private String getAnnotatedText(Element eventNode) {
		
		String annotation = eventNode.html();
//		System.out.println("DESCRIPTION: --> " + annotation);
//		System.out.println(" num of tags = " + eventNode.children().size());
		Element tag;
		for(int i=0; i<eventNode.children().size(); i++){
			tag = eventNode.child(i);
//			System.out.println(i + "-tag: " + tag.tagName() + " has html: " + tag.html() + " and text: " + tag.text() + " and outer html:" + tag.outerHtml());
//			tag.ownText();
			if (!tag.tagName().equals("a")){
				annotation = annotation.replace(tag.outerHtml(), tag.text());
			}else if (tag.hasAttr("rel")){
				annotation = annotation.replace(tag.outerHtml(), "");
			}
		}
//		System.out.println("TEST DESCRIPTION: --> " + annotation);
		return annotation;
	}

private String getCorrectDate(String text) {
	String date = "";
	String modifiedDate = text;
	if (modifiedDate.toLowerCase().contains("(".toLowerCase())) {
		int firstoccur = modifiedDate.indexOf("(");
		date = modifiedDate.substring(firstoccur + 1,
				modifiedDate.indexOf(")"));
	}
	try{
		Date.valueOf(date);		
	}catch(Exception ex){
//		ex.printStackTrace();
		System.out.println(date);
		System.out.println(date.split("-").length);
		String day = date.split("-")[2];
		
		date = getYear()+"-"+getMonth()+"-"+day;	
		System.out.println("Date from " + text  + " ---> " + date);
		
//		text = text.replace("Current events of", "");
//		text = text.replace(",", "");
//		text = text.trim();
//		if(text.split(" ").length > 1){
//			String day = text.split(" ")[1];
//			date = getYear()+"-"+getMonth()+"-"+day;	
//			System.out.println("Date from " + text  + " ---> " + date);
//		}else{
//			System.out.println("ERROR: coukdn't extract date !!!! from " + text);
//		}
	}
	
		return date;
	}

//	private String getAnnotatedDescription(Element li) {
////		System.out.println();
////		System.out.println("HTML:" + li.html());
////		System.out.println("TEXT:" + li.text());
//		Elements children = li.children();
//		Iterator<Element> itr = children.iterator();
//		while(itr.hasNext()){
//			Element child = itr.next();
//			System.out.println(child.toString());
////			if (!child.tagName().equals("a")){
////				itr.remove();
////			}else 
//				if(child.hasAttr("rel")){
//				itr.remove();
//			}
//		}
//		System.out.println("UPDATE:" + children.text());
//		
////		ArrayList<Integer> toRemoveList = new ArrayList<Integer>();
////		for(int index=0; index<children.size(); index++){
////			Element child = children.get(index);
////			if (!child.tagName().equals("a")){
////				toRemoveList.add(new Integer(index));
////			}else if(child.hasAttr("rel")){
////				toRemoveList.add(new Integer(index));
////			}
////		}
////		
////		for(Integer index: toRemoveList){
////			children.remove(index);
////		}
//		
//		return children.html();
//	}

	@Override
	protected boolean isValidDate() {
		Date startDate   = Date.valueOf("2006-04-30");
		Date parsingDate = Date.valueOf(getYear()+"-"+getMonth()+"-"+"01");
		if(parsingDate.after(startDate)) return true;
		else return false;
	}

	@Override
	protected ArrayList<String> parseMonthPageForDatesOnly(Document doc) {
		ArrayList<String> dates = new ArrayList<String>();
		Elements days = doc.select("div#bodyContent div#mw-content-text.mw-content-ltr table tbody tr td table.vevent");
		for (Element eachday : days) {
			Elements date = eachday.select("tbody tr td table.plainlinks tbody tr td span.summary");
			if (!date.text().contains(getYear()+""))
				continue;
			dates.add(getCorrectDate(date.text()));
		}
		return dates;
	}
}
