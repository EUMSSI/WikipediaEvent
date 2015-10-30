
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.l3s.eumssi.wikiimport;
import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import de.l3s.eumssi.model.Entity;
import de.l3s.eumssi.model.Event;
import de.l3s.eumssi.model.Reference;

//import de.l3s.eumssi.dao.DatabaseManager;
/**
 * This class is used to index the events and News Stories
 * @author SUDHIR KUMAR SAH
 */
public class WikiIndexer {
    
//    private String eventIndexpath = "/home/gtran/WikiTimesLucene/EventsIndexes";
//    private String storyIndexpath ="/home/gtran/WikiTimesLucene/NewsStoriesIndexes";
    
   // String eventIndexpath = "F:\\LuceneIndex";
   // String storyIndexpath ="/home/gtran/WikiTimesLucene/NewsStoriesIndexes";
    
	private IndexWriter indexWriter = null;
	
	private String eventIndexpath = null;
//	private String storyIndexpath = null;
	
	private Directory eventdir = null;
//	private Dierctory storydir = null;
	
	StandardAnalyzer analyzer = null;
    IndexWriterConfig config = null;
    
    /**
     * Creates a new instance of Indexer
     */
    
//    Properties prop = new Properties();
    
    public WikiIndexer(String eventIndexPath) throws IOException { 
    	//Reading from configuration file
//		prop.load(new FileInputStream("./configs/wikieventsync.properties"));
    	
    	this.eventIndexpath = eventIndexPath;
        this.eventdir = FSDirectory.open(new File(eventIndexPath));
        System.out.println(eventIndexPath);
//        this.storydir = FSDirectory.open(new File(storyIndexpath));
        //this.dir = new RAMDirectory();
    	analyzer = new StandardAnalyzer(Version.LUCENE_43);
        config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
        
    }
    
    
    
	
    /**
     * This function gets the index writer
     * @param create    Index writer has been created or not
     * @return      IndexWriter object
     * @throws IOException 
     */
    	public void openIndexWriter(boolean create) throws IOException {
    	
	        if (create) {
	            // Create a new index in the directory, removing any
	            // previously indexed documents:
	            config.setOpenMode(OpenMode.CREATE);
	        } else {
	            // Add new documents to an existing index:
	            config.setOpenMode(OpenMode.CREATE_OR_APPEND);
	        }
	        
	        if (indexWriter == null) {
	       		indexWriter = new IndexWriter(eventdir, config);
	        }
	        
    	}    
   
    
        
        public void indexEvent(Event event) throws IOException  {   
        	
        	openIndexWriter(false);
            Document doc = new Document();
            	
            //Event
            if(event.getDescription() != null)	{
    	        doc.add(new StringField("event.id", event.getEventId(), Field.Store.YES)); 
    	       	doc.add(new StringField("event.date", event.getDate().toString(), Field.Store.YES));
    	       	doc.add(new TextField("event.description", event.getDescription(), Field.Store.YES));
            	
   	        	//category
   	        	if(event.getCategory() != null) {
    		        doc.add(new StringField("event.category.id", event.getCategory().getId(), Field.Store.YES));
    		        doc.add(new StringField("event.category.name", event.getCategory().getName(), Field.Store.YES));
    	        }
    	        	
    	        //News Story
    	        if(event.getStory() != null) {
    		        doc.add(new StringField("event.story.id", event.getStory().getId(), Field.Store.YES));
    		        doc.add(new TextField("event.story.name", event.getStory().getName(), Field.Store.YES));
    		        doc.add(new StringField("event.story.wikiURL", event.getStory().getWikipediaUrl(), Field.Store.YES));
    	        }
    	        	
    	        //Entity
    	        if(event.getEntities().size() != 0) {
    	        	doc.add(new StringField("event.entities.number", Integer.toString(event.getEntities().size()), Field.Store.YES));
    	        	int counter = 0;
    	        	for(Entity entity : event.getEntities())	{
    	        		counter++;
    		        	doc.add(new StringField("event.entity.id."+counter, entity.getId(), Field.Store.YES));    	        		
    		        	doc.add(new StringField("event.entity.name."+counter, entity.getName(), Field.Store.YES));
    		        	doc.add(new StringField("event.entity.wikiURL."+counter, entity.getWikiURL(), Field.Store.YES));
    		        }
    	        }
    	        	
    	        //source
    	        if(event.getReferences().size() != 0){
    	        	doc.add(new StringField("event.sources.number", Integer.toString(event.getReferences().size()), Field.Store.YES));
    	        	int counter = 0;
    	        	for(Reference src : event.getReferences()){
    	        		counter++;
    	        		doc.add(new StringField("event.source.id."+counter, src.getId(), Field.Store.YES));
    	        		doc.add(new StringField("event.source.name."+counter, src.getSource(), Field.Store.YES));
    	        		doc.add(new StringField("event.source.URL."+counter, src.getUrl(), Field.Store.YES));
    	        	}
    	        }
    	        	
    	        //now index
    	        indexWriter.addDocument(doc);
            }
        
        // Don't forget to close the index writer when done*********
        closeIndexWriter(); 
     }
        
        
        
    //This method removes document from the lucene index
    public void removeIndex(Event event) throws IOException {
    	openIndexWriter(false);
       	indexWriter.deleteDocuments(new Term("event.id", event.getEventId()));
       	closeIndexWriter();  //don't forget to close index writer
    }
     
    
    
    //This method updates the document in the lucene index
    public void updateIndex(Event event) throws IOException, InterruptedException  {
        //first remove
        removeIndex(event);
        	
        //now insert
        indexEvent(event);
    }
    
    
    
    public void closeIndexWriter() throws IOException {
        if (indexWriter != null) {
            indexWriter.close();
            indexWriter = null;
        }
    }
    
    
    public String getEventIndexpath() {
		return eventIndexpath;
	}

	public void setEventIndexpath(String eventIndexpath) throws IOException {
		this.eventIndexpath = eventIndexpath;
		this.eventdir = FSDirectory.open(new File(this.eventIndexpath));
	}
	

	public Directory getEventdir() {
		return eventdir;
	}

	public void setEventdir(Directory eventdir) {
		this.eventdir = eventdir;
	}
        
}

