package de.l3s.eumssi.enrichment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.didion.jwnl.JWNLException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import mx.bigdata.jcalais.CalaisClient;
import mx.bigdata.jcalais.CalaisObject;
import mx.bigdata.jcalais.CalaisResponse;
import mx.bigdata.jcalais.rest.CalaisRestClient;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import org.json.*;

public class Utils {
	static Connection conn = Utils.connectRemoteWikipDB();
	static Connection conn2 = Utils.connectRemoteYAGO2DB();
	public static Connection connectWikipDB() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		Connection conn = null;
		try {
			String userName = "root";
			String password = "";
			String serverName = "localhost";
			String mydatabase = "WikipEvent";
			String url = "jdbc:mysql://" + serverName + "/" + mydatabase; 
			conn = DriverManager.getConnection(url, userName, password);
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println ("Cannot connect to database server");
		}

		return conn;
	}

	public static Connection connectRemoteWikipDB() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		Connection conn = null;
		try {
			String userName = "gtran";
			String password = "dvDp3KCGJfSv7tXs";
			String url = "jdbc:mysql://db.l3s.uni-hannover.de:3306/wikitimeline"; 
			conn = DriverManager.getConnection(url, userName, password);
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println ("Cannot connect to database server");
		}

		return conn;
	}

	public static Connection connectRemoteYAGO2DB() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		Connection conn = null;
		try {
			String userName = "gtran";
			String password = "dvDp3KCGJfSv7tXs";
			String url = "jdbc:mysql://db.l3s.uni-hannover.de:3306/yago2"; 
			conn = DriverManager.getConnection(url, userName, password);
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println ("Cannot connect to database server");
		}

		return conn;
	}

	public static void main(String[] args) throws SQLException, IOException {

		//Utils.buildBOWArff();
		//		List<String> ids = Utils.getTopicEvents("147855.0");
		//		for (String id : ids) {
		//			System.out.println(id);
		//		}

		//		Utils.getKBCategories();

		//		Utils u = new Utils();
		//		u.getCalaisStats();


		//		Utils.init();
		//Utils.buildBOEArff();

        //Utils.buildBOWStoryCategoriesFullDescription();
		//Utils.crawlDBPediaCategoriesFromStory("Sinking_of_the_MV_Sewol");
		
		//Utils.predictData(args[0]);
		
		//Utils.getKBCategories();
		Utils.loadtsv("calais2.txt");

	}



	public static Multimap<String, String> mapProbTopicEvents() {
		try {
			List<String> lines = Files.readLines(new File("/home/gtran/WikiTimes/story_categorical_events/story_labelled_categorical_event.txt"), Charsets.UTF_8);

			Multimap<String, String> etmap = ArrayListMultimap.create();

			for (String line : lines) {
				String[] dat = line.split("\t");
				//confident score (frm Giang)
				if (Double.parseDouble(dat[1]) >= 0.3) 
					etmap.put(dat[2], dat[0]);	       
			}
			System.out.println(etmap.size());
			return etmap;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static  Collection<String> getSameStoryEvents(String eventID, Multimap<String, String> etmap) {
		for (Entry<String, String> entry : etmap.entries()) {
			if (entry.getValue().equals(eventID)) {
				return etmap.get(entry.getKey());
			}
		}
		ArrayList<String> e = new ArrayList<String>();
		e.add(eventID);
		return e;
	}

	public static List<String> getTopicEvents(String eventID) throws SQLException {
		//list of events in the same story
		List<String> events = Lists.newArrayList();

		String _sql = "SELECT f.NewsStoryID FROM Event f WHERE f.EventID = ?" ;

		PreparedStatement _stm = conn.prepareStatement(_sql, PreparedStatement.RETURN_GENERATED_KEYS);
		_stm.setString(1, eventID);
		ResultSet _rs = _stm.executeQuery();
		_rs.next();
		String storyID = _rs.getString("f.NewsStoryID");
		_stm.close();
		_rs.close();
		if (storyID.trim().equals("0")) {
			return events;
		}


		String sql = "SELECT e.EventID FROM Event e WHERE e.NewsStoryID = ?" ;
		PreparedStatement stm = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
		stm.setString(1, storyID);
		ResultSet rs = stm.executeQuery();
		while (rs.next()) {
			events.add(rs.getString("e.EventID"));
		}
		rs.close();
		stm.close();
		//tweak 1
		//if (events.size() == 0) events.add(eventID);
		return events;
	}

	public static String getTopic(String eventID) throws SQLException {

		String sql = "SELECT f.NewsStoryID FROM Event f WHERE f.`EventID` = ?" ;
		PreparedStatement stm = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
		stm.setString(1, eventID);
		ResultSet rs = stm.executeQuery();
		rs.next();
		String story = rs.getString("f.NewsStoryID");

		rs.close();
		stm.close();
		return story;
	}




	public static List<Instance> getTopicEventInstances(Instances data, Collection<String> _eventIDs) {		
		List<String> eventIDs = new ArrayList<String>(_eventIDs);
		List<Instance> te = Lists.newArrayList();
		for (int j = 0; j < data.numInstances(); j++) {
			for (int i = 0; i < eventIDs.size(); i++) {
				// attribute 1 is EventID
				if (data.instance(j).stringValue(1).equals(eventIDs.get(i))) {
					te.add(data.instance(j));
				}

			}
		}
		return te;
	}

	public static Multimap<String,String> cacheEntityTopic() throws SQLException {
		Multimap<String, String> cache = ArrayListMultimap.create();
		// WHERE e.`NewsStoryID` != 0
		String sql = "SELECT e.EventID, e.NewsStoryID, t.Name FROM Event e join Event_Topic_Relation r on e.EventID = r.EventID join Topic t on r.TopicID = t.ID";
		PreparedStatement stm = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
		ResultSet rs = stm.executeQuery();

		while (rs.next()) {
			String topic = rs.getString("t.Name");
			if (topic.trim().equals("") || topic.equals("Environment") 
					|| topic.equals("Education") || topic.equals("Other") || topic.equals("Religion_Belief")
					|| topic.equals("Health_Medical_Pharma")) continue;
			cache.put(rs.getString("e.NewsStoryID"), rs.getString("e.EventID"));
		}
		return cache;
	}


	public static Instance getInstance(Instances data, Collection<String> eventIDs) {
		for (int j = 0; j < data.numInstances(); j++) {
			String eventID = data.instance(j).stringValue(1);
			if (eventIDs.contains(eventID)) {
				return data.instance(j);
			}
		}
		return null;
	}


	public static double[] sumDoubleArray(double[] a, double[] b) {
		if (a.length != b.length) return null;
		for (int i = 0; i < a.length; i++) {
			b[i] = a[i] + b[i];
		}
		return b;
	}

	public static double[] divideDoubleArray(double[] a, double n) {
		for (int i = 0; i < a.length; i++) {
			a[i] = (double) a[i] / n;
		}

		return a;
	}

	public static void getKBCategories() throws SQLException {
		CalaisClient client = new CalaisRestClient("mnvv3qxfxu8vmdbmjdcncrty");
		String sql = "SELECT distinct e.EventID, e.Description from Event e left join Event_Category_Relation r  on  e.`EventID`=r.`EventID` where r.`EventID` is null";
		//String sql = "SELECT distinct e.EventID, e.Description FROM Event e join Category c on e.CategoryID = 0 and e.NewsStoryID = 0;";
		PreparedStatement stm = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
		ResultSet rs = stm.executeQuery();
		StringBuffer sb = new StringBuffer();
		while (rs.next()) {
			String eventID = rs.getString("e.EventID");


			try {
				System.out.println(rs.getString("e.Description"));
				CalaisResponse response = client.analyze(rs.getString("e.Description"));
				sb.append(eventID).append("\t");
				for (CalaisObject t: response.getTopics()) {
					sb.append(t.getField("categoryName")).append(":").append(t.getField("score")).append(",");
				}
				sb.append("\n");
				
				
//				String sql2 = "UPDATE Event SET CalaisCategories = ? WHERE EventID = ?";
//				PreparedStatement stm2 = conn.prepareStatement(sql2, PreparedStatement.RETURN_GENERATED_KEYS);
//				stm2.setString(1, sb.toString());
//				stm2.setString(2, eventID);
//				try {
//					stm2.executeUpdate();
//					System.out.println("updated: " + eventID + " " + sb.toString());
//				} catch (Exception e) {
//					System.out.println("Error updating event " + eventID);
//					e.printStackTrace();
//					continue;
//				} finally {
//					try {
//						stm2.close(); 
//					} catch(SQLException se) {
//						se.printStackTrace();
//					}
//				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			Files.write(sb, new File ("calais2.txt"), Charsets.UTF_8);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void buildBOWArff() throws SQLException, IOException {
		//list of labels
		Set<String> labels = Sets.newHashSet();
		String sql = "SELECT e.EventID, e.Description, e.NewsStoryID, c.Name FROM Event e join Category c on e.CategoryID = c.CategoryID and c.CategoryID != 0;";
		PreparedStatement stm = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
		ResultSet rs = stm.executeQuery();

		StringBuilder sb = new StringBuilder();
		while (rs.next()) {
			String topic = rs.getString("c.Name");
			if (topic.trim().equals("") || topic.equals("Environment") 
					|| topic.equals("Education") || topic.equals("Other") || topic.equals("Religion_Belief")
					|| topic.equals("Health_Medical_Pharma")
					|| topic.equals("Human Interest")
					|| topic.equals("Social Issues")) continue;
			topic = topic.replaceAll(" ", "_");
			labels.add(topic);
			sb.append(topic).append(",").append(weka.core.Utils.quote(rs.getString("e.EventID"))).
			append(",").append(weka.core.Utils.quote(rs.getString("e.Description"))).append("\n");
		}

		StringBuilder _sb = new StringBuilder();
		_sb.append("@relation wiki_event");
		_sb.append("\n");
		_sb.append("@attribute categories");
		_sb.append("{");
		for (String l : labels) {
			_sb.append(l);
			_sb.append(",");
		}
		_sb.append("}");
		_sb.append("\n");
		_sb.append("@attribute eid String");
		_sb.append("\n");
		_sb.append("@attribute text String");
		_sb.append("\n");

		_sb.append("@data");
		_sb.append("\n");
		sb.insert(0, _sb.toString());

		Files.write(sb, new File ("wk.arff"), Charsets.UTF_8);
	}




	public static void _buildBOWArff() throws SQLException, IOException {
		//list of labels
		Set<String> labels = Sets.newHashSet();
		String sql = " SELECT e.EventID, e.Description, e.NewsStoryID, e.Sources, t.Name FROM Event e join Event_Topic_Relation r on e.EventID = r.EventID join Topic t on r.TopicID = t.ID";
		PreparedStatement stm = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
		ResultSet rs = stm.executeQuery();

		StringBuilder sb = new StringBuilder();
		int i = 0;
		while (rs.next()) {		
			String sourceID = rs.getString("e.Sources");
			String[] sIDs = sourceID.split("\\$");
			StringBuilder sb_articles = new StringBuilder();
			sb_articles.append("");
			boolean isContent = false;
			for (String sID : sIDs) {
				if (sID.trim().equals("")) continue;
				String c = getNewsContent(sID.trim());
				if (c == null) {
					continue;
				}
				isContent = true;
				sb_articles.append(c);
				sb_articles.append("\n");
			}
			if (!isContent) {
				i++;
				//sb_articles.append(rs.getString("e.Description"));
			}
			String topic = rs.getString("t.Name");
			if (topic.trim().equals("") || topic.equals("Environment") 
					|| topic.equals("Education") || topic.equals("Other") || topic.equals("Religion_Belief")
					|| topic.equals("Health_Medical_Pharma")) continue;
			labels.add(topic);
			sb.append(weka.core.Utils.quote(sb_articles.toString())).append("\n");
		}
		System.out.println("number of event with no content: " + i);

		StringBuilder _sb = new StringBuilder();
		_sb.append("@relation wiki_event");
		_sb.append("\n");
		//		_sb.append("@attribute categories");
		//		_sb.append("{");
		//		for (String l : labels) {
		//			_sb.append(l);
		//			_sb.append(",");
		//		}
		//		_sb.append("}");
		//		_sb.append("\n");
		_sb.append("@attribute long_text String");
		_sb.append("\n");

		_sb.append("@data");
		_sb.append("\n");
		sb.insert(0, _sb.toString());

		Files.write(sb, new File ("wk_articles.arff"), Charsets.UTF_8);
	}



	public static void buildBOEArff() throws SQLException, IOException {
		//list of labels
		Set<String> labels = Sets.newHashSet();

		String sql = "SELECT e.EventID, e.Description, e.NewsStoryID, c.Name FROM Event e join Category c on e.CategoryID = c.CategoryID and c.CategoryID != 0;";
		PreparedStatement stm = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
		ResultSet rs = stm.executeQuery();

		StringBuilder sb = new StringBuilder();
		while (rs.next()) {
			String topic = rs.getString("c.Name");
			if (topic.trim().equals("") || topic.equals("Environment") 
					|| topic.equals("Education") || topic.equals("Other") || topic.equals("Religion_Belief")
					|| topic.equals("Health_Medical_Pharma")
					|| topic.equals("Human Interest")
					|| topic.equals("Social Issues")) continue;
			topic = topic.replaceAll(" ", "_");
			labels.add(topic);
			List<String> entities = Utils.getEntitiesFromEvent(rs.getString("e.EventID"));
			//sb.append(topic).append(",");
			int c = 0;

			StringBuilder sbe = new StringBuilder();
			if (entities.size() > 0) {
				for (String e : entities) {
					sbe.append(e);
					if (c++ == entities.size() -1) break;
					sbe.append(" ");
				}
			}	
			sb.append(weka.core.Utils.quote(sbe.toString()));
			sb.append("\n");
		}

		StringBuilder _sb = new StringBuilder();
		_sb.append("@relation wiki_event");
		_sb.append("\n");
		//		_sb.append("@attribute categories");
		//		_sb.append("{");
		//		for (String l : labels) {
		//			_sb.append(l);
		//			_sb.append(",");
		//		}
		//		_sb.append("}");
		//		_sb.append("\n");
		_sb.append("@attribute entities String");
		_sb.append("\n");

		_sb.append("@data");
		_sb.append("\n");
		sb.insert(0, _sb.toString());

		Files.write(sb, new File ("wk2.arff"), Charsets.UTF_8);
	}


	public static List<String> getEntitiesFromEvent(String EventID) throws SQLException {
		List<String> entities = Lists.newArrayList();
		String sql = "SELECT w.`Name` FROM Event e join Event_Entity_Relation r on e.`EventID` = r.`EventID` join `WikiRef` w on r.`WikiRefID` = w.`WikiRefID` WHERE e.`EventID` = ?";
		PreparedStatement stm = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
		stm.setString(1, EventID);
		ResultSet rs = stm.executeQuery();
		while (rs.next()) {
			entities.add(rs.getString("w.Name"));
		}
		rs.close();
		stm.close();
		return entities;

	}
	/**
	 * Mapping 
	 * Law_Crime = Law_Crime
	 * Technology_Science = Technology_Internet
	 * Disaster_Accident = Disaster_Accident
	 * Politics = Politics
	 * Business_Finance = Business_Finance
	 * Sports = Sports
	 * Entertainment_Culture_Society = Entertainment_Culture
	 * War_Conflict = War_Conflict
	 * @throws SQLException
	 */
	enum Calais {Law_Crime, Technology_Internet, Disaster_Accident, Politics, Business_Finance, Sports, Entertainment_Culture, War_Conflict, Education, Environment, Health_Medical_Pharma,
		Hospitality_Recreation, Human_Interest, Labor, Religion_Belief, Social_Issues, Weather, Other};
		public String convertCalaisWikiCat(String calais) {
			switch(Calais.valueOf(calais)){
			case Law_Crime:
				return calais;
			case Technology_Internet:
				return "Technology_Science";
			case Disaster_Accident:
				return calais;
			case Politics:
				return calais;
			case Business_Finance:
				return calais;
			case Sports:
				return calais;
			case Entertainment_Culture:
				return "Entertainment_Culture_Society";
			case War_Conflict:
				return calais;
			default: return calais;
			}
		}
		public void getCalaisStats() throws SQLException {
			HashMap<String, Pair> map = Maps.newHashMap();
			String sql = "SELECT e.EventID, e.Description, e.CalaisCategories, t.Name FROM Event e join Event_Topic_Relation r on e.EventID = r.EventID join Topic t on r.TopicID = t.ID";
			PreparedStatement stm = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
			ResultSet rs = stm.executeQuery();
			int noIns = 0; //number of instances
			int noCIns = 0; //correct classified
			int i = 0; //number of no classification due to api bug
			while (rs.next()) {
				String topic = rs.getString("t.Name");
				if (topic.trim().equals("") || topic.equals("Environment") 
						|| topic.equals("Education") || topic.equals("Other") || topic.equals("Religion_Belief")
						|| topic.equals("Health_Medical_Pharma")) continue;
				String ct = rs.getString("e.CalaisCategories");
				if (ct == null) continue; // fail to classify -> skip
				String[] cc = ct.split(",");
				boolean isIn = false;
				for (String _cc : cc) {
					if(_cc.trim().equals("")) {
						i++;
						continue;
					}
					_cc = _cc.split(":")[0];

					/* special cases */
					_cc = _cc.replaceAll(" ", "_"); //Social Issues;

					_cc = convertCalaisWikiCat(_cc);
					if(topic.equals(_cc)) {
						isIn = true;
						noCIns++;
						break;
					}
				}
				Pair p;
				if (isIn) {
					p = map.containsKey(topic) ? map.get(topic) : new Pair(0,0,0);
					//correctly classified
					p.t = p.t + 1;
				} else {
					p = map.containsKey(topic) ? map.get(topic) : new Pair(0,0, 0);
					//incorrectly classified
					p.k = p.k+ 1;
				}
				p.l = p.l +1 ;
				map.put(topic, p);
				noIns++;
			}

			System.out.println("bug: " + i);

			Set<String> topics = map.keySet();
			for (String t : topics) {
				Pair _p = map.get(t);
				//System.out.println(_p.k + " " + _p.l + " " + _p.t);
				double fp = (double) _p.k / (noIns - _p.l);
				double tp = (double) _p.t / _p.l;
				double precision = (double) tp / (fp + tp);
				double f_m = (double) 2 * precision * tp / (precision + tp);

				System.out.println(t + " precision: " + precision + " recall: " + tp + " F-Measure: " + f_m);

				System.out.println("Correctly classified: " + noCIns);
				System.out.println("Incorrectly classified: " + (noIns - noCIns));

			}

			System.out.println("Accuracy: " + (double) noCIns / noIns);


		}

		static IndexReader indexReader;
		static IndexSearcher indexSearcher;
		public static void init() {
			try {
				Directory dir = FSDirectory.open(new File("/home/gtran/WikiTimesLucene/NewsArticlesIndexes"));
				indexReader = IndexReader.open(dir);
				indexSearcher = new IndexSearcher(indexReader);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		public static String getNewsContent(String id) throws IOException{
			System.out.println("source id: "  + id);
			TermQuery tq= new TermQuery(new Term("sourceID", id));
			TopScoreDocCollector collector = TopScoreDocCollector.create(1, true);
			indexSearcher.search(tq, collector);
			ScoreDoc[] hits = collector.topDocs().scoreDocs;
			if (hits.length == 0) return null;
			Document entry = indexSearcher.doc(hits[0].doc);
			//System.out.println(entry.getField("ArticleContent").stringValue());
			IndexableField content;
			String _content;
			if ((content = entry.getField("ArticleContent"))!=null) {
				return content.stringValue();
			}
			else return "";

		}



		public class Pair{
			public int t; // classified as topic and is true
			public int k; // classified as topic
			public int l; // actual topic
			public Pair(int t, int k, int l) {
				this.t = t;
				this.k = k;
				this.l = l;
			}
		}
		private static String readAll(Reader rd) throws IOException {
			StringBuilder sb = new StringBuilder();
			int cp;
			while ((cp = rd.read()) != -1) {
				sb.append((char) cp);
			}
			return sb.toString();
		}

		public static void buildBOWStoryCategories() {
			WordnetCategories.init();
			Set<String> labels = Sets.newHashSet();
			Set<String> stories = Sets.newHashSet();
			Map<String, String> slmap = Maps.newHashMap();
			String sql2 = "select distinct label, Category.`Name` from NewsStory join Event on NewsStory.`StoryID`=Event.`NewsStoryID` join Category on Event.`CategoryID`=Category.`CategoryID`";
			try {
				PreparedStatement stm2 = conn.prepareStatement(sql2, PreparedStatement.RETURN_GENERATED_KEYS);
				ResultSet rs = stm2.executeQuery();

				while (rs.next()) {
					slmap.put(rs.getString("label"), rs.getString("Name"));
					labels.add(rs.getString("Name"));
					stories.add(rs.getString("label"));
				}
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}


			StringBuilder sb = new StringBuilder();

			for (String story : stories) {
				String label = slmap.get(story);
				if (label == null) System.out.println("label is null: " + story);
				story = story.replaceAll(" ", "_");
				StringBuilder[] sbs = crawlDBPediaCategoriesFromStory(story);
				if (sbs[0] == null) continue;
				sb.append(label);
				sb.append(",");
				sb.append(story);
				sb.append(",");


				sb.append(weka.core.Utils.quote(sbs[0].toString()))
				.append(weka.core.Utils.quote(sbs[1].toString())).append("\n");
			}
			StringBuilder _sb = new StringBuilder();
			_sb.append("@relation wiki_event");
			_sb.append("\n");
			_sb.append("@attribute categories");
			_sb.append("{");
			for (String l : labels) {
				_sb.append(l);
				_sb.append(",");
			}
			_sb.append("}");
			_sb.append("\n");
			_sb.append("@attribute text String");
			_sb.append("\n");

			_sb.append("@data");
			_sb.append("\n");
			sb.insert(0, _sb.toString());

			try {
				Files.write(sb, new File ("wk_story4_withstoryName.arff"), Charsets.UTF_8);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


		}

		public static StringBuilder[] crawlDBPediaCategoriesFromStory(String story) {
			StringBuilder[] sbs = new StringBuilder[2];
			System.out.println("Crawling: " + story);
			String s = story.replaceAll(" ","_");
			List<String> general = Arrays.asList(general_topics);
			if (general.contains(s)) return sbs;
			Set<String> cat = Sets.newHashSet();

			InputStream is = null;
			try {
				if (story.equals("Chadian-Sudanese_conflict")) story = "Chadian_Civil_War_(2005-10)";
				if (story.equals("Conflict_in_Iraq")) story = "Iraq_War";
				if (story.equals("2006_Israel-Lebanon_conflict")) story = "2006_Lebanon_War";
				if (story.equals("Iraq_sectarian_violence")) story = "Sectarian_violence_in_Iraq";
				if (story.equals("Waziristan_War")) story = "War_in_North-West_Pakistan";
				if (story.equals("Pope_Benedict_XVI_Islam_controversy")) story = "Regensburg_lecture";
				if (story.equals("Iraq_Insurgency")) story = "Iraqi_insurgency_(2003���11)";
				if (story.equals("Georgia-Russia_spying_row")) story = "2006_Georgian���Russian_espionage_controversy";
				if (story.equals("Mark_Foley_scandal")) story = "Mark_Foley_congressional_page_incident";
				if (story.equals("United_States_general_elections,_2006")) story = "United_States_elections,_2006";
				if (story.equals("Beit_Hanoun_November_2006_incident")) story = "2006_shelling_of_Beit_Hanoun";
				if (story.equals("2006_Tonga_riots")) story = "2006_Nuku���alofa_riots";
				if (story.equals("Palestinian_civil_skirmishes_(December_2006)")) story = "Fatah���Hamas_conflict";
				if (story.equals("Ethiopian_involvement_in_the_Somali_Civil_War")) story = "War_in_Somalia_(2006���09)";
				if (story.equals("The_New_Way_Forward")) story = "Iraq War troop surge of 2007";
				if (story.equals("Western_U.S._Freeze_of_2007")) story = "2007_Western_United_States_freeze";
				if (story.equals("Iraqi_insurgency_(post_U.S._withdrawal)")) story = "Iraqi_insurgency_(2011���present)";
				if (story.equals("Hurricane_Dean_(2007)")) story = "Hurricane_Dean";
				if (story.equals("2007_Greek_fires")) story = "2007_Greek_forest_fires";
				if (story.equals("Hurricane")) story = "Tropical_cyclone";
				if (story.equals("Hurricane_Felix_(2007)")) story = "Hurricane_Felix";
				if (story.equals("APEC")) story = "Asia-Pacific_Economic_Cooperation";
				if (story.equals("Islamic_insurgency_in_Algeria_(2002-present)")) story = "Insurgency_in_the_Maghreb_(2002���present)";
				if (story.equals("2014_pro-Russian_conflict_in_Ukraine")) story = "2014_pro-Russian_unrest_in_Ukraine";
				if (story.equals("2012���13_FA_Cup")) story = "2012-13_FA_Cup";

				is = new URL("http://dbpedia.org/data/"+ story +".json").openStream();
				BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
				String jsonText = null;
				try {
					jsonText = readAll(rd);
				} catch (org.json.JSONException e) {
					return sbs;
				}
				JSONObject json = new JSONObject(jsonText);

				story = story.replaceAll("���", "-");

				JSONObject sson = null;
				try {
					sson = json.getJSONObject("http://dbpedia.org/resource/" + story);
				} catch (org.json.JSONException e) {
					try {
						JSONArray redirects = json.getJSONArray("http://dbpedia.org/ontology/wikiPageRedirects");
						String redirect = redirects.getJSONObject(0).get("value").toString().replace("http://dbpedia.org/resource/", "");
						System.out.println("Redirect: " + redirect);
						return crawlDBPediaCategoriesFromStory(redirect);
					} catch (org.json.JSONException e2) {
					}

				}
				if (sson == null) return sbs;
				JSONArray categories = null;
				try {
					categories = sson.getJSONArray("http://purl.org/dc/terms/subject");
				} catch (org.json.JSONException e) {
					try {
						JSONArray redirects = sson.getJSONArray("http://dbpedia.org/ontology/wikiPageRedirects");
						String redirect = redirects.getJSONObject(0).get("value").toString().replace("http://dbpedia.org/resource/", "");
						return crawlDBPediaCategoriesFromStory(redirect);
					} catch (org.json.JSONException je) {} 
				}
				if (categories == null) return sbs;
				for (int i = 0; i < categories.length(); i++) {
					String dbcat = categories.getJSONObject(i).get("value").toString();
					dbcat = dbcat.replace("http://dbpedia.org/resource/Category:", "");
					cat.add(dbcat);
					int itr = 0;
					crawlDBPediaCategories(dbcat, cat, itr);
				}
				StringBuilder _sb = new StringBuilder();
				int i = 0;
				for (String c : cat) {
					i++;	
					if (c.length() > 40) continue;
					_sb.append(WordnetCategories.cleanWikiCat(c));
					if (i != cat.size())_sb.append(" ");
				}
				sbs[0] = _sb;
				StringBuilder _sb2 = new StringBuilder();

				for (String c : cat) {
					if (c.length() > 40) continue;
					List<String> wnet = WordnetCategories.listHypernym(WordnetCategories.wordnet, c, 0);
					if (wnet == null) continue;
					for (String w : wnet) {
						_sb2.append(w).append(" ");
					}
				}
				sbs[1] = _sb2;
				return sbs;

			} catch (MalformedURLException e) {
				e.printStackTrace();
				return sbs;
			} catch (IOException e) {
				System.out.println("IO Error:" + story);
				return sbs;
			} catch (org.json.JSONException e) {
				System.out.println(story);
				e.printStackTrace();
				return sbs;
			} catch (JWNLException e) {

			} finally {
				try {
					is.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NullPointerException e) {
					System.out.println("NPE:" + story);
				}
			}

			return null;


		}

		public static void crawlDBPediaCategories(String category, Set<String> cat, int itr) {
			if (category == null) return;
			InputStream is = null;
			try {
				is = new URL("http://dbpedia.org/data/Category:" + category + ".json").openStream();
				BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
				String jsonText = readAll(rd);
				JSONObject json = new JSONObject(jsonText);
				JSONObject cson = json.getJSONObject("http://dbpedia.org/resource/Category:" + category);
				JSONArray categories = null;
				try {
					categories = cson.getJSONArray("http://www.w3.org/2004/02/skos/core#broader");
				} catch (org.json.JSONException e) {
					categories = null;
				}
				if (categories == null) return;
				for (int i = 0; i < categories.length(); i++) {
					String dbcat = categories.getJSONObject(i).get("value").toString();
					dbcat = dbcat.replace("http://dbpedia.org/resource/Category:", "");

					if (cat.contains(dbcat)) continue; 
					cat.add(dbcat);
					itr ++;
					if (itr == 3) return;
					crawlDBPediaCategories(dbcat, cat, itr);
				}
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("IO Error (cat):" + category);
				return;
			}  finally {
				try {
					is.close();
				} catch (NullPointerException e) {
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		public static void test2(StringBuffer sb, String category) {
			String sql = "select * from yagoFacts where subject=? and predict=\"<hasWordnetDomain>\"";
			try {
				PreparedStatement stm2 = conn2.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
				stm2.setString(1, category);
				ResultSet rs = stm2.executeQuery();

				while (rs.next()) {
					sb.append(rs.getString("object") + "\t");
				}
				stm2.close();
				rs.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}


		public static void test() {
			int correct = 0;
			int incorrect = 0;
			Set<String> labels = Sets.newHashSet();
			Set<String> stories = Sets.newHashSet();
			Map<String, String> slmap = Maps.newHashMap();
			String sql2 = "select distinct label, Category.`Name` from NewsStory join Event on NewsStory.`StoryID`=Event.`NewsStoryID` join Category on Event.`CategoryID`=Category.`CategoryID`";
			try {
				PreparedStatement stm2 = conn.prepareStatement(sql2, PreparedStatement.RETURN_GENERATED_KEYS);
				ResultSet rs = stm2.executeQuery();

				while (rs.next()) {
					slmap.put(rs.getString("label"), rs.getString("Name"));
					labels.add(rs.getString("Name"));
					stories.add(rs.getString("label"));
				}
				stm2.close();
				rs.close();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			PreparedStatement stm;
			int no = 0;
			for (String story : stories) {
				String label = slmap.get(story);
				List<String> generals = Arrays.asList(general_topics);
				if (!label.equals("War_Conflict")) continue; 
				String s = story.replaceAll(" ", "_");
				if (generals.contains(s)) {
					System.out.println("general topic: " + story); continue;
				}
				no++;
				story = story.replaceAll(" ", "_");
				story = "<" + story + ">";
				String sql = "select * from yagoFacts where subject=? and predict=\"rdf:type\"";

				try {
					stm = conn2.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
					stm.setString(1, story);

					ResultSet rs = stm.executeQuery();
					StringBuffer sb = new StringBuffer();
					if(!rs.isBeforeFirst()){
						continue;
					}
					while (rs.next()) {
						sb.append(rs.getString("object") + "\t");
						test2(sb, rs.getString("object"));
						//System.out.println(rs.getString("subject") + "\t" + rs.getString("object"));
					}

					String content = sb.toString().toLowerCase();
					if (content.contains("war") || content.contains("conflict")
							|| content.contains("dispute")
							|| content.contains("crime")) correct++;
					else {
						incorrect++;
						System.out.println(story + "\t" + sb.toString());
					}

				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			System.out.println("number of stories: " + no);
			System.out.println("correct: " + correct);
			System.out.println("incorrect: " + incorrect);
			System.out.println("accuracy: " + (double)correct / (correct + incorrect));

		}
		
		
		public static void buildBOWStoryCategoriesFullDescription() {
			Set<String> labels = Sets.newHashSet();
			Set<String> stories = Sets.newHashSet();
			Map<String, String> slmap = Maps.newHashMap();
			String sql2 = "select distinct label, Category.`Name` from NewsStory join Event on NewsStory.`StoryID`=Event.`NewsStoryID` join Category on Event.`CategoryID`=Category.`CategoryID`";
			try {
				PreparedStatement stm2 = conn.prepareStatement(sql2, PreparedStatement.RETURN_GENERATED_KEYS);
				ResultSet rs = stm2.executeQuery();

				while (rs.next()) {
					slmap.put(rs.getString("label"), rs.getString("Name"));
					labels.add(rs.getString("Name"));
					stories.add(rs.getString("label"));
				}
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			StringBuilder sb = new StringBuilder();
			for (String story : stories) {
				String label = slmap.get(story);
				if (label == null) System.out.println("label is null: " + story);
				//story = story.replaceAll(" ", "_");
				
				StringBuilder content_sb = new StringBuilder();
				String sql = "select e.Description from Event e join Event_Story_Relation r on e.`EventID`=r.`EventID` join NewsStory s on r.`StoryID`=s.`StoryID` where e.`NewsStoryID` = (select s.`StoryID` from NewsStory s where s.`Label`=?) and r.`confidence`=1";
				PreparedStatement stm;
				try {
					stm = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
					stm.setString(1, story);
					ResultSet rs = stm.executeQuery();
					while (rs.next()) {
						content_sb.append(rs.getString("Description"));
						content_sb.append("\t");
					}
					
					stm.close();
					rs.close();
					
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (!content_sb.toString().trim().equals("")) {
					sb.append(label);
					sb.append(",");
					sb.append(weka.core.Utils.quote(content_sb.toString())).append("\n");
				}		
			}
			
			StringBuilder _sb = new StringBuilder();
			_sb.append("@relation wiki_event");
			_sb.append("\n");
			_sb.append("@attribute categories");
			_sb.append("{");
			for (String l : labels) {
				_sb.append(l);
				_sb.append(",");
			}
			_sb.append("}");
			_sb.append("\n");
			_sb.append("@attribute text String");
			_sb.append("\n");

			_sb.append("@data");
			_sb.append("\n");
			sb.insert(0, _sb.toString());

			try {
				Files.write(sb, new File ("wk_story5.arff"), Charsets.UTF_8);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			
		}
		
		
		public static Set<String> getUnlabelStories() {
			Set<String> labeled_stories = Sets.newHashSet();
			String sql2 = "select distinct label, Category.`Name` from NewsStory join Event on NewsStory.`StoryID`=Event.`NewsStoryID` join Category on Event.`CategoryID`=Category.`CategoryID`";
			try {
				PreparedStatement stm2 = conn.prepareStatement(sql2, PreparedStatement.RETURN_GENERATED_KEYS);
				ResultSet rs = stm2.executeQuery();

				while (rs.next()) {
					labeled_stories.add(rs.getString("label"));
				}
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			Set<String> unlabeled_stories = Sets.newHashSet();
			String sql = "select label from NewsStory";
			
			try {
				PreparedStatement stm = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
				ResultSet rs = stm.executeQuery();

				while (rs.next()) {
					String story = rs.getString("label");
					if (!labeled_stories.contains(story)) unlabeled_stories.add(story);
				}
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			System.out.println("size: " + unlabeled_stories.size());
			return unlabeled_stories;
			
		}
		
		public static String getStoryFullDescription(String story) {
			String sql = "select e.Description from Event e join Event_Story_Relation r on e.`EventID`=r.`EventID` join NewsStory s on r.`StoryID`=s.`StoryID` where e.`NewsStoryID` = (select s.`StoryID` from NewsStory s where s.`Label`=?) and r.`confidence`=1";
			PreparedStatement stm;
			try {
				stm = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
				stm.setString(1, story);
				ResultSet rs = stm.executeQuery();
				StringBuilder content_sb = new StringBuilder();
				while (rs.next()) {
					content_sb.append(rs.getString("Description"));
					content_sb.append("\t");
				}
				stm.close();
				rs.close();

				return content_sb.toString();
				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
		
		public static void predictData(String model) {
			FastVector cls = new FastVector();
			
			cls.addElement("Environment");
			cls.addElement("Law_Crime");
			cls.addElement("Entertaintment_Culture");
			cls.addElement("Politics");
			cls.addElement("Business_Finance");
			cls.addElement("Other");
			cls.addElement("Religion_Belief");
			cls.addElement("Sports");
			cls.addElement("Disaster_Accident");
			cls.addElement("Technology_Internet");
			cls.addElement("Health_Medical_Pharma");
			cls.addElement("War_Conflict");
			cls.addElement("Internaltion_Relation");
			
			FastVector atts = new FastVector();
			atts.addElement(new Attribute("categories", cls));
			atts.addElement(new Attribute("text", (FastVector) null));
			
			//atts.addElement(new Attribute("content",(ArrayList<String>)null));
			
			Instances test = new Instances("test", atts, 0);
			
			
			
			test.setClassIndex(0);
			
			Attribute a = test.classAttribute();
			System.out.println("class:" + a.toString());
			try {
				Classifier wcf = (Classifier) weka.core.SerializationHelper.read(model);
				Set<String> unlabels = getUnlabelStories();
				
				double[] attValues;
				for (String story : unlabels) {
					String content = getStoryFullDescription(story);
					if (content == null) {
						System.out.println("No-event story: " + story); continue;
					}
					
					attValues = new double[2];
					
					attValues[0] = Instance.missingValue();
					attValues[1] = test.attribute(1).addStringValue(content);
					
					test.add(new Instance(1.0, attValues));
					
				    Instance ins = test.lastInstance();
                    
					double value  = wcf.classifyInstance(ins);
					
					String prediction = test.classAttribute().value((int) value);
					
					System.out.println(story+ "\t" +  prediction );	
				}
				
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		}
		
		public static void propogateEventLabel(String tsv) {
			Map<String, String> map = Maps.newHashMap();
			try {
				List<String> lines = Files.readLines(new File(tsv), Charsets.UTF_8);
				for (String line : lines) {
					String[] _line = line.split("\t");
					map.put(_line[0], _line[1]);
				}
				
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			StringBuilder sb = new StringBuilder();
			Set<String> stories = map.keySet();
			
			for (String story : stories) {
				System.out.println("story: " + story);
				String sql = "select e.`EventID` from Event e join Event_Story_Relation r on e.`EventID`=r.`EventID` join NewsStory s on r.`StoryID`=s.`StoryID` where e.`NewsStoryID` = (select s.`StoryID` from NewsStory s where s.`Label`=?) and r.`confidence`=1";
				PreparedStatement stm;
				try {
					stm = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
					stm.setString(1, story);
					ResultSet rs = stm.executeQuery();
					while (rs.next()) {
						sb.append(rs.getString("EventID")).append("\t").append(map.get(story));
						sb.append("\n");
					}
					stm.close();
					rs.close();
					
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			try {
				Files.write(sb, new File ("propagate.tsv"), Charsets.UTF_8);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
		
		public static String getMaxPair(String[] pairs) {
			double max = 0;
			for (String pair : pairs) {
				String[] p = pair.split(":");
				if (Double.parseDouble(p[1]) > max) {
					max = Double.parseDouble(p[1]);
				}
			}
			String s = "";
			for (String pair : pairs) {
				String[] p = pair.split(":");
				if (Double.parseDouble(p[1]) == max) {
					s = s + pair + ",";
				}
			}
			return s;
	
		}
		
		public static List<String> pickNRandom(List<String> lst, int n) {
		    List<String> copy = new LinkedList<String>(lst);
		    Collections.shuffle(copy);
		    return copy.subList(0, n);
		}
		
		public static void loadtsv(String tsv) throws IOException {
			List<String> lines = Files.readLines(new File(tsv), Charsets.UTF_8);
			List<String> newlines = Lists.newArrayList();
			System.out.println("size:" + lines.size());
			Iterator<String> itr = lines.iterator();
			StringBuilder sb = new StringBuilder();
			while (itr.hasNext()) {
				String line = itr.next();
				String[] parts = line.split("\t");
				if (parts.length == 1) itr.remove();		
			}
			System.out.println("size:" + lines.size());
			for (String line : lines) {
				String[] parts = line.split("\t");
				String[] labels = parts[1].split(",");
				newlines.add(parts[0] + "\t" + getMaxPair(labels));
				sb.append(parts[0]).append("\t").append(getMaxPair(labels)).append("\n");
			}
			Files.write(sb, new File ("calais_cleaned.text"), Charsets.UTF_8);
			
			List<String> randomPicks = pickNRandom(newlines, 50);
			StringBuilder sb2 = new StringBuilder();
			for (String p : randomPicks) {
				sb2.append(p).append("\n");
			}
			Files.write(sb2, new File ("calais_50.text"), Charsets.UTF_8);
		}

		static String[] general_topics = {"Protester",
			"Zimbabwe",
			"Abu_Ghraib_prison",
			"Afghanistan",
			"Africa",
			"African_Union",
			"Air_France",
			"Al-Qaeda",
			"Archaeological_Museum_of_Olympia",
			"Asia",
			"Asia-Pacific_Economic_Cooperation",
			"Asif_Ali_Zardari",
			"Aung_San_Suu_Kyi",
			"Australia",
			"Barack_Obama",
			"Bono",
			"BP",
			"Burma",
			"Chad",
			"Chief_of_Army_Staff_(Pakistan)",
			"China",
			"Christopher_Coke",
			"Colombia",
			"Conjoined_twins",
			"Democratic_Republic_of_the_Congo",
			"Dennis_Hopper",
			"Dominique_Strauss-Kahn",
			"East_Timor",
			"Easter_Island",
			"Egypt",
			"England",
			"Europe",
			"European_Union",
			"Federal_government_of_the_United_States",
			"FIFA",
			"France",
			"Gaza",
			"Georgia_(country)",
			"Guinea",
			"Harold_Camping",
			"History_of_Eritrea",
			"Hong_Kong",
			"Hosni_Mubarak",
			"Hurricane",
			"Indonesia",
			"Iran",
			"Iraq",
			"Islamic_Republic_of_Iran",
			"Israel",
			"Ivory_Coast",
			"Japan",
			"Jean-Claude_Duvalier",
			"Joe_Biden",
			"John_Bryson",
			"Jos",
			"Julian_Assange",
			"Korean_peninsula",
			"Kosovo",
			"Kunsthal",
			"Kyrgyzstan",
			"Lance_Armstrong",
			"Libya",
			"Liu_Xiaobo",
			"London",
			"Machu_Picchu",
			"Madagascar",
			"Maldives",
			"Manchester",
			"Mel_Gibson",
			"Melissa_O'Neil",
			"Mexico",
			"Michigan",
			"Middle_East",
			"Muammar_al-Gaddafi",
			"NASA",
			"National_Basketball_Association",
			"National_Football_League",
			"NATO",
			"Nawaz_Sharif",
			"Nepal",
			"New_Orleans",
			"New_South_Wales",
			"News_International",
			"NFL",
			"Nigeria",
			"North_Korea",
			"Pakistan",
			"Park51",
			"People's_Republic_of_China",
			"Philippines",
			"Politics_of_Italy",
			"Pope_Benedict_XVI",
			"Prachanda",
			"President_of_Iran",
			"Republic_of_Ireland",
			"Rudolf_Elmer",
			"Russia",
			"Rwanda",
			"Saudi_Arabia",
			"Shahram_Amiri",
			"Sky_News",
			"Sky_Sports",
			"Somalia",
			"South_Sudan",
			"Spain",
			"Sri_Lanka",
			"Stephen_Gately",
			"Strait_of_Hormuz",
			"Sudan",
			"Supreme_Court_of_the_United_States",
			"Syria",
			"Television_in_the_United_Kingdom",
			"Thailand",
			"Tiger_(zodiac)",
			"Togo",
			"Troy_Davis",
			"Tunisia",
			"TVNZ",
			"UEFA",
			"United_Kingdom",
			"United_Nations_Security_Council",
			"United_States",
			"United_States_Army",
			"United_States_Congress",
			"United_States_Democratic_Party",
			"United_States_Senate",
			"Uzbekistan",
			"Venezuela",
			"West_Bank",
			"Wisconsin",
			"Wisconsin_Sikh_temple_shooting",
			"Xi_Jinping",
			"Xinjiang",
			"Yasukuni_Shrine",
			"Yemen",
			"APEC",
			"Football_(soccer)",
			"Chang'e_3",
			"H5N1",
			"Chang'e-3",
			"The_Ashes",
			"Association_football",
			"Basketball",
			"FIFA_World_Cup",
			"World_Heritage_List",
			"United_States_economy",
			"President_of_the_United_States",
			"British���Irish_Council",
			"New_Year's_Eve",
			"Rugby_league",
			"Earth_Hour",
			"World_AIDS_Day",
			"Nobel_Prize",
			"Supreme_Court_of_the_United_States",
			"United_States_Supreme_Court",
			"Pollution_in_China",
			"Chinese-U.S._relations",
			"India-United_States_relations",
		"United_States-Syria_relations"};


}

