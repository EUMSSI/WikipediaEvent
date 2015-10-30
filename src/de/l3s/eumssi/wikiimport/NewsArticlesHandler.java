package de.l3s.eumssi.wikiimport;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import de.l3s.eumssi.model.*;

/*
 * This class is used to fetch and index NewsArticles and store them into the DataBase, Source Table 
 */

public class NewsArticlesHandler {
	
	 private Connection con;
	 private PreparedStatement pstmt;
	 private ResultSet res;
	 static private boolean linkDead = false;
	 
	 //used for indexing
	 	private IndexWriter indexWriter = null;
	 	
	    //String indexpath = "/home/gtran/WikiTimesLucene/NewsArticlesIndexes";
	 	private String newsStoryPath = null;  
	 	
	    //Directory dir;
	 	Directory NSDir = null;
	 	
	 	StandardAnalyzer analyzer = null;
	    IndexWriterConfig config = null;
	 	
	  /*  @SuppressWarnings("deprecation")
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
	    @SuppressWarnings("deprecation")
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40, analyzer); */
	  
	 
	 /**
	   * Constructor
	 * @throws IOException 
	 */
	    public NewsArticlesHandler(String newsStoryPath ) throws IOException
	    {
	    	this.newsStoryPath = newsStoryPath;
	        this.NSDir = FSDirectory.open(new File(newsStoryPath));
	        System.out.println(NSDir.toString());
	        
	        analyzer = new StandardAnalyzer(Version.LUCENE_43);
	        config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
	    }
	 
	    /**
	     * Constructor 
	     * 
	     */
	    
	    
	private static String readAll(Reader rd) throws IOException {
	    StringBuilder sb = new StringBuilder();
	    int cp;
	    while ((cp = rd.read()) != -1) {
	      sb.append((char) cp);
	    }
	    return sb.toString();
	  }
	
	  public static String readFromUrl(String url) throws IOException{
		  linkDead = false;  
		  String htmlString=null;
		  URLConnection conn;
		    try {
		    	conn = new URL(url).openConnection();
		    	conn.setConnectTimeout(3000);
		    	BufferedReader  rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), Charset.forName("UTF-8")));
		    	htmlString = readAll(rd);
		    }catch(Exception e){
		    	System.out.println("***link is not available : " +url);
		    	htmlString="Link Not Available";
		    	linkDead = true;
		    }
	   
		    return htmlString;
		  }
	  
	  
	  public boolean storeNewsArticleInDB(Reference newsArticle) throws NumberFormatException, IOException{
		  return insertIntoCotentFieldOfSource(newsArticle.getUrl(), Integer.parseInt(newsArticle.getId()) );
	  }
	  
	  public boolean insertIntoCotentFieldOfSource(String url, int sourceid) throws IOException{
		  
	        boolean result = false;
		  	int affectedRow;
	            try {
	                if(url!= null && !url.isEmpty()){
	                	String htmlSourceCode = readFromUrl(url);
	                	
	                	//System.out.println(htmlSourceCode); //--------
	                	
	                    pstmt = DBHandler.openConnection().prepareStatement("update Source set Content = ? where sourceID = ? and URL = ?");
	                    pstmt.setString(1,htmlSourceCode);
	                    pstmt.setInt(2,sourceid);
	                    pstmt.setString(3,url);
	                    affectedRow = pstmt.executeUpdate();
	                        if(affectedRow == 1) {
	                            result = true;
	                        }
	                }
	            
	            }catch(SQLException sqle) {
	                sqle.printStackTrace();
	            }finally {
	                DBHandler.closePStatement(pstmt);
	                DBHandler.closeResultSet(res);
	               // DBHandler.closeDBConnection(con);
	            }
	        
	        return result;
	    }
	  
	  /**
	     * This function gets the index writer.
	     * @param create        Index writer created or not
	     * @return      IndexWriter object
	     * @throws IOException 
	     */
	    
	    public IndexWriter getIndexWriter(boolean create) throws IOException {
	    	  
	    	
	        if (create) {
	            // Create a new index in the directory, removing any
	            // previously indexed documents:
	            config.setOpenMode(OpenMode.CREATE);
	        } else {
	            // Add new documents to an existing index:
	            config.setOpenMode(OpenMode.CREATE_OR_APPEND);
	        }
	        if (indexWriter == null) {
	            indexWriter = new IndexWriter(NSDir, config);
	        }
	        return indexWriter;
	   }    
	   
	    /**
	     * Closes the index writer
	     * @throws IOException 
	     */
	    
	    
	    public void closeIndexWriter() throws IOException {
	        if (indexWriter != null) {
	            indexWriter.close();
	        }
	   }
	  
	    
	   
	  public void newsArticlesIndexing(String newsUrl, String pdate, int srcid ) throws SAXException, TikaException{
		  
		  try{
			  	URL url = new URL(newsUrl);
				URLConnection connection = url.openConnection();
				connection.setConnectTimeout(3000);
				InputStream input = connection.getInputStream();
				String sourceid = srcid+"";
				//sourceid = sourceid.trim();
				
				
				
				//InputStream input = new FileInputStream("myfile.html");
		        ContentHandler handler = new BodyContentHandler(1000000);
		        Metadata metadata = new Metadata();
		        new HtmlParser().parse(input, handler, metadata, new ParseContext());
		        String plainText = handler.toString();
		        
		        //System.out.println("****"+sourceid);
			  
		        indexWriter = null;
		        getIndexWriter(false);
		        
		        Document doc = new Document();
		        doc.add(new StringField("PublishedDate", pdate, Field.Store.YES));
		        doc.add(new StringField("sourceID", sourceid.trim() , Field.Store.YES));
	            doc.add(new TextField("ArticleContent", plainText, Field.Store.YES));  
	            indexWriter.addDocument(doc);
	            indexWriter.updateDocument(new Term("sourceID", sourceid.trim()), doc); 
	            
	            closeIndexWriter();
		  }catch(Exception ex)
		  {
			  System.out.println("***Exception at 'newsArticleHandling method'");
		  }
		    
	  }
	  
//	  public void insertAndIndex() throws IOException, SAXException, TikaException{
//		   NewsArticlesHandler nah = new NewsArticlesHandler();
//		   boolean result = false ;
//		   boolean isPresent = false;
//	        if(DBHandler.isClosed(con)) {
//	            con = DBHandler.getConnection();
//	        }
//	        if(!DBHandler.isClosed(con)) {
//	            try {
//	            	
//	                    pstmt = con.prepareStatement("select * from Source where content = ?");
//	                    pstmt.setString(1,"Not Published");
//	                  //  pstmt.setString(1,"test");
//	                    res = pstmt.executeQuery();
//	                    int count = 0;
//	                    while(res.next()){
//	                    	System.out.println(count++);
//	                    	isPresent=true;
//	                    	String newsArticleUrl = res.getString("URL");
//	                    	String publishedDate = res.getString("PublishedDate");
//	                    	int id = res.getInt("SourceID"); 
//	                    	
//	                    	System.out.println("Url : " +newsArticleUrl);
//	                    	System.out.println("Source ID  : " +id);
//		                    
//	                    	result = nah.insertIntoCotentFieldOfSource(newsArticleUrl,id);
//	                    	if(result & !linkDead)
//	                    		System.out.println("NewsArticle with id: "+id+" is inserted in the Source Table\n");
//	                    	else
//	                    		System.out.println("****NewsArticle with id: "+id+" could not be able to  inserted in the Source Table. Rather status is changed to 'Link Not Available'");
//	                    	
//	                    	//Indexing goes here
//	                    	if(!linkDead){
//	                    		newsArticlesIndexing(newsArticleUrl,publishedDate,id);
//	                    	}
//	                    }
//	                    if(!isPresent)
//	                    	System.out.println("Content could not found in the source Table");
//	                    
//	            }catch(SQLException sqle) {
//	                sqle.printStackTrace();
//	            }finally {
//	                DBHandler.closePStatement(pstmt);
//	                DBHandler.closeResultSet(res);
//	                DBHandler.closeDBConnection(con);
//	            }
//	        } else {
//	        }
//	    }

	  
	  
	  
	  public void indexNewsArticle(Reference newsArticle) throws UnsupportedEncodingException{
      	// get content from DB
	            try {
	                if(newsArticle.getId() != null && !newsArticle.getId().isEmpty()){
	                    pstmt = DBHandler.openConnection().prepareStatement("select Content from Source where SourceID=?");
	                    pstmt.setInt(1,Integer.parseInt(newsArticle.getId()));
	                    res = pstmt.executeQuery();
	                    if(res.next()){
	                    	
	                    	if(res.getString("Content") != "None" && res.getString("Content") != "Link Not Available")	{
	                    		//convert the string into inputstream
	                    		InputStream input = new ByteArrayInputStream(res.getString("Content").getBytes(StandardCharsets.UTF_8));
	                    		//InputStream input = new ByteArrayInputStream(res.getString("Content").getBytes());
	                    		
	                    		//Extract the text from HTML page
	                    		ContentHandler handler = new BodyContentHandler(1000000);
	            		        Metadata metadata = new Metadata();
	            		        new HtmlParser().parse(input, handler, metadata, new ParseContext());
	            		        String plainText = handler.toString();
//	            		        System.out.println(plainText);
	            		        
	            		        //index
	            		        indexWriter = null;
	            		        getIndexWriter(false);
	            		        
	            		        Document doc = new Document();
	            		        doc.add(new StringField("sourceID", newsArticle.getId(), Field.Store.YES));
	            	            doc.add(new TextField("ArticleContent", plainText, Field.Store.YES));  
	            	            indexWriter.addDocument(doc);
	            	            indexWriter.updateDocument(new Term("sourceID", newsArticle.getId()), doc); 
	            	            
	            	            closeIndexWriter(); //don't forget to close indexwriter
	                    	}
	                    	
	                    	else 	{
	                    		System.out.println("Content not available yet");
	                    	}
	                    }
	            }
	            }catch(SQLException sqle) {
	                sqle.printStackTrace();
	            } catch (IOException e) {
					e.printStackTrace();
				} catch (SAXException e) {
					e.printStackTrace();
				} catch (TikaException e) {
					e.printStackTrace();
				}finally {
	                DBHandler.closePStatement(pstmt);
	                DBHandler.closeResultSet(res);
//	                DBHandler.closeDBConnection(con);
	            }
      }
	  
	/*  public static void main(String[] args) throws IOException {
			NewsArticlesHandler nah;
			Reference ref = new Reference();
			ref.setId("103783");
			
			nah = new NewsArticlesHandler();
			nah.indexNewsArticle(ref);
			
    }*/
}
