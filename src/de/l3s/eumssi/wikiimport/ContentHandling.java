package de.l3s.eumssi.wikiimport;

import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.util.ArrayList;
import java.sql.Date;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.Term;

import de.l3s.eumssi.dao.DatabaseManager;
import de.l3s.eumssi.model.*;


/**
 * This class is intended to handle the contents in database.
 * @author SUDHIR KUMAR SAH
 */
public class ContentHandling {
    //declaring class variables
//    public Connection con;
    
    private DatabaseManager db = null;
    /**
     * Constructor
     */
    public ContentHandling()  {
        db = new DatabaseManager();
    }
    
    
    
    /**
     * This method is used to insert the entries in the Category Table
     * @param name  Name of category
     * @param parentid  Id of parent category
     * @return      Insertion successful or unsuccessful
     */
    
    public boolean insertIntoCategory(String name, int parentid){
        boolean done = false;
        PreparedStatement pstmt = null;
        ResultSet result = null;
            try {
                if(name != null && !name.isEmpty()){
                    pstmt = db.openConnection().prepareStatement("select * from Category where Name=?");
                    pstmt.setString(1,name);
                    result = pstmt.executeQuery();
                    if(!result.next()){
                        pstmt = DBHandler.openConnection().prepareStatement("insert ignore into Category(Name,ParentID) values(?,?)");
                        pstmt.setString(1,name);
                        pstmt.setInt(2,parentid);
                        int affectedRow = pstmt.executeUpdate();
                        if(affectedRow == 1) {
                            done = true;
                    }
                    }
            }
            }catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
        return done;
    }
    
    
   
    
    
    
    
    
    public Event getEventById(int eventId) {
    	Event event = null;
		PreparedStatement pstmt = null;
		ResultSet result = null;
		
		try {
			pstmt = db.openConnection().prepareStatement("SELECT * FROM Event where EventID=?");
			pstmt.setInt(1, eventId);
	        result = pstmt.executeQuery();
	        
	        if(result.next()){
	        	event = new Event();
	        	event.setId(result.getString("EventID"));
	        	Story story = new Story();
	        	story.setId(result.getString("NewsStoryID"));
	        	event.setStory(story);
	        }
		} catch (SQLException e) {			
			e.printStackTrace();
		}finally{
			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
		}
		return event;
    }
    
    
    
    /**
     * This method is used to insert the entries into WikiRef table
     * @param name  Name of the entity
     * @param url   URL of the entity
     * TODO: check the existence of one entity in DB makes it slow, better to normalized before inserting, hence, making checking existence
     * simpler and faster
     * optimized from the slow version
     * @return      WikiRefID
     */
    public int insertIntoWikiRef(String name, String url){
    	 PreparedStatement pstmt = null;
    	 PreparedStatement pstmt2 = null;
         ResultSet result = null;
         ResultSet result2 = null;
         boolean done = false;
         boolean ispresent = false;
         
    	try {
			url = java.net.URLDecoder.decode(url, "UTF-8");
			
			url = "http://en.wikipedia.org"+url;
			
			name = name.replaceAll("[^\\w\\s]","");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
        
        int wikiid = 0;
            try {
                if(url != null && !url.isEmpty()){
                    pstmt = db.openConnection().prepareStatement("select WikiRefID, Name from WikiRef where WikipediaURL=?");
                    pstmt.setString(1,url);
                    result = pstmt.executeQuery();
                    if(!result.next()){// does not exist
                    	//for safe case, check if the name existed but in different URL???
                        pstmt2 = db.openConnection().prepareStatement("select WikiRefID, Name from WikiRef where Name=?");
                        pstmt2.setString(1, name);
                        result2 = pstmt2.executeQuery();
                        if(result2.next()){ //
                        	
                        	pstmt = db.openConnection().prepareStatement("update ignore WikiRef set WikipediaURL=? Where Name=?");
	                        pstmt.setString(1,url);
	                        pstmt.setString(2, name);
	                        int e = pstmt.executeUpdate();
                            ispresent = true;
                        }
                        
                        if(!ispresent){
                            pstmt = db.openConnection().prepareStatement("insert ignore into WikiRef(Name,WikipediaURL) values(?,?)");
                            pstmt.setString(1,name);
                            pstmt.setString(2,url);
                            int affectedRow = pstmt.executeUpdate();
                            if(affectedRow == 1) {
                                done = true;
                            }
                        }
                    }
                    else {//if exists, update that WikiRef entity if the name changed
                    	String oldname = result.getString("Name");
                    	if (!oldname.equals(name)) {
	                    	pstmt = db.openConnection().prepareStatement("update ignore WikiRef set Name=? Where WikipediaURL=? and Name=?");
	                        pstmt.setString(1,name);
	                        pstmt.setString(2,url);
	                        pstmt.setString(3,oldname);
	                        int affectedRow = pstmt.executeUpdate();
	                        if(affectedRow == 1) {
	                            done = true;
	                        }
                    	}
                    }
                    
                    //return the ID
                    //if(!ispresent){
                        pstmt = db.openConnection().prepareStatement("select WikiRefID from WikiRef where Name=?");
                        pstmt.setString(1,name);
                        result2 = pstmt.executeQuery();
                        while(result2.next()) {
                            wikiid = result2.getInt("WikiRefID");
                      //  }
                       }
                } 
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null)  try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (result2 != null) try { result2.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)   try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt2 != null)  try { pstmt2.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
        return wikiid;
    }
    
    /**
     * This method is used to insert the entries into NewsStory table
     * @param label     Label of News Story
     * @param parentid      Id of parent News Story
     * @param isongoing     Is ongoing or not
     * @param wikiid        WikiRefID
     * @return      Insertion successful or unsuccessful
     */
    public boolean insertIntoNewsStory(String label, int parentid, String isongoing, int wikiid){
        boolean done = false;
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
            try { 
                if(label != null && !label.isEmpty()){
                    pstmt = db.openConnection().prepareStatement("select * from NewsStory where WikiRefID=?");
                    pstmt.setInt(1,wikiid);
                    result = pstmt.executeQuery();
                    if(!result.next()){// this story does not exist
                        pstmt = db.openConnection().prepareStatement("insert ignore into NewsStory(Label,WikiRefID,ParentStoryID,IsOngoing) values(?,?,?,?)");
                        pstmt.setString(1,label);
                        pstmt.setInt(2,wikiid);
                        pstmt.setInt(3,parentid);
                        pstmt.setString(4,isongoing);
                        int affectedRow = pstmt.executeUpdate();
                        if(affectedRow == 1) {
                            done = true;
                        }
                    }
                    else { //exists, update if necessary
                    	String oldLabel = result.getString("Label");
                    	if (!oldLabel.equals(label)) {//updating
                    		//updating the new value
                    		pstmt = db.openConnection().prepareStatement("update ignore NewsStory set Label=?, ParentStoryID=?, IsOngoing=? where WikiRefID=?");
                            pstmt.setString(1,label);
                            pstmt.setInt(2,parentid);
                            pstmt.setString(3,isongoing);
                            pstmt.setInt(4,wikiid);
                            
                            int affectedRow = pstmt.executeUpdate();
                            if(affectedRow == 1) {
                            	System.out.println(oldLabel + "->" + label);
                                done = true;
                            }
                    	}
                    }
                } 
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
        return done;
    }
    
    /**
    * This method is used to insert entries into StoryCategoryRelation table
    * @param newsstory     Name of news Story
    * @param category      Name of category
    * @return              Insertion successful or unsuccessful
    * 
    * Optimized from _v1 function to make it faster
    *  
    */
    public boolean insertIntoStoryCategoryRelation(String newsstory, String category){
        boolean done = false;
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
            try {
                int storyid = -1;
                int catid = -1;
                //get the category id
                if(category != null && !category.isEmpty()){
                    pstmt = db.openConnection().prepareStatement("select CategoryID from Category where Name=?");
                    pstmt.setString(1,category);
                    result = pstmt.executeQuery();
                    //once more, res is reused?
                    if(result.next()){
                        catid = result.getInt("CategoryID");
                    }
                }
                
                //get the story id
                pstmt = db.openConnection().prepareStatement("select StoryID from NewsStory where Label=?");
                pstmt.setString(1,newsstory);
                result = pstmt.executeQuery();
                if(result.next()){
                    storyid = result.getInt("StoryID");
                }

                //insert to the table
                pstmt = db.openConnection().prepareStatement("insert ignore into Story_Category_Relation(StoryID,CategoryID) values(?,?)");
                pstmt.setInt(1,storyid);
                pstmt.setInt(2,catid);
                if (storyid>0 && catid >0) {
	                int affectedRow = pstmt.executeUpdate();
	                if(affectedRow == 1) {
	                    done = true;
	                }
                }
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
        return done;
    }
    
    /**
     * This method is used to insert the entries into Source table
     * @param sourcename    Name of source
     * @param url       URL of source
     * @param publisheddate     Published date of article
     * @param content       Content of article
     * @return      Insertion successful or unsuccessful
     */
    
    public boolean insertIntoSource(String sourcename, String url, String publisheddate, String content){
    	 boolean done = false;
         PreparedStatement pstmt = null;
         ResultSet result = null;
            try {
            	//first, remove all duplicated URL in the DB
                if(url != null && !url.isEmpty()){
                pstmt = db.openConnection().prepareStatement("insert ignore into Source(SourceName,URl,PublishedDate,Content) values(?,?,?,?)");
                pstmt.setString(1,sourcename);
                pstmt.setString(2,url);
                pstmt.setString(3,publisheddate);
                pstmt.setString(4,content);
                int affectedRow = pstmt.executeUpdate();
                if(affectedRow == 1) {
                    done = true;
                }   
                }  
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
        return done;
    }
    
    /**
     * This method is used to insert the entries into WKEvent table
     * @param description   Description
     * @param newsstory     News Story
     * @param categoryname  Category
     * @param date      Date
     * @param storeurl      Sources
     * @return      Event ID
     */
    
     public int insertIntoEvent(String description, String newsstory, String categoryname, Date date, ArrayList<String> storeurl, int wikiid){
        int eventid = 0;
        PreparedStatement pstmt = null;
        ResultSet result = null;
            try {
                int storyid = 0;
                int catid = 0;
                String sourceids = "";
                if(newsstory != null && !newsstory.isEmpty()){
                    pstmt = db.openConnection().prepareStatement("select StoryID from NewsStory where WikiRefID=?");  
                    // If I have to change that because of newsStory(same but different) then check for WikiRefID instead of Label.
                    pstmt.setInt(1,wikiid);
                    result = pstmt.executeQuery();
                    if(result.next()){
                        storyid = result.getInt("StoryID");
                    }   
                }
                if(categoryname != null && !categoryname.isEmpty()){
                    pstmt = db.openConnection().prepareStatement("select CategoryID from Category where Name=?");
                    pstmt.setString(1,categoryname);
                    result = pstmt.executeQuery();
                    if(result.next()){
                        catid = result.getInt("CategoryID");
                    } 
                }
                
                for(String s : storeurl){
                    if(s!=null && !s.isEmpty()){
                        Reference ref = db.getReferenceByURL(s); 
                        sourceids = sourceids+" $ "+ ref.getId();
                    }
                }
                if(!date.equals("0000-00-00")){    
                    pstmt = db.openConnection().prepareStatement("insert ignore into Event(Description,NewsStoryID,CategoryID,Date,Sources) values(?,?,?,?,?)");
                    pstmt.setString(1,description);
                    pstmt.setInt(2,storyid);
                    pstmt.setInt(3, catid);
                    pstmt.setDate(4,date);
                    pstmt.setString(5, sourceids);
                    int affectedRow = pstmt.executeUpdate();
                    pstmt = db.openConnection().prepareStatement("select EventID from Event where NewsStoryID=? and CategoryID=? and Date=? and Description=? ");
                    pstmt.setInt(1,storyid);
                    pstmt.setInt(2,catid);
                    pstmt.setDate(3,date);
                    pstmt.setString(4, description);
                    result = pstmt.executeQuery();
                    if(result.next()){
                        eventid = result.getInt("EventID");
                    }
                }
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
        return eventid;
    }
     
     /**
      * This method is used to insert the entries into EventEntityRelation table
      * @param eventid      Event ID
      * @param wikiurls     URL of entities
      * @return 
      */
     
     public boolean insertIntoEventEntityRelation(int eventid, ArrayList<String> wikiurls){
        boolean done = false;
        PreparedStatement pstmt = null;
        ResultSet result = null;
        
        if(eventid == 0)
            return done;
            try {
                int wikiid = 0;
                for(String s : wikiurls){
                	
                    if(s!=null && !s.isEmpty()){
                    	s = "http://en.wikipedia.org"+s;
                    	//System.out.println("====================== "+s);
                    	//System.out.println(s);
                        pstmt = db.openConnection().prepareStatement("select WikiRefID from WikiRef where WikipediaURL=?");
                        pstmt.setString(1,s);
                        result = pstmt.executeQuery();
                        while(result.next()){
                            wikiid = result.getInt("WikiRefID");
                        }
                        pstmt = db.openConnection().prepareStatement("insert ignore into Event_Entity_Relation(EventId,WikiRefID) values(?,?)");
                        pstmt.setInt(1,eventid);
                        pstmt.setInt(2,wikiid);
                        int affectedRow = pstmt.executeUpdate();
                        if(affectedRow == 1) {
                            done = true;
                        } 
                    }
                }
            } catch (SQLException e) {			
    			e.printStackTrace();
    		}finally{
    			if (result != null) try { result.close(); } catch (SQLException e) {e.printStackTrace();}
    			if (pstmt != null)  try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
    		}
        return done;
    }
     
     
     
     /*This function return the past dates from event table
      * @PARAM : number of dates.
      * if it is 5 , then it will return last 5 dates from event tables.
      */
     
     /* This function deletes all the events that have been deleted form the wiki current event portal 
      * @param : List of all the events from wikipedia current event portal for the particular date [date is the second parameter]
      * 
      */
//     public void removeEvent(ArrayList<String> eventsList, String date1){
//    	 
//         boolean isPresent = false;
//         int i=0;
//         if(DBHandler.isClosed(con)) {
//             con = DBHandler.openConnection();
//         }
//         if(!DBHandler.isClosed(con)) {
//             try {
//                 if(date1 != null && !date1.isEmpty()){
//                     pstmt = con.prepareStatement("select * from Event where date = ?");
//                     pstmt.setString(1,date1);
//                     res = pstmt.executeQuery();
//               
//                     while(res.next()){
//                    	 String oldDescription = res.getString("description");
//                    	 int eventID= res.getInt("EventID");
//                    	 isPresent = false;
//                    	 for(i=0;i<eventsList.size();i++){
//                    		 if(oldDescription.equals(eventsList.get(i))){
//                    			 isPresent = true;
//                    			 break;
//                    		 }
//                    			 
//                    	 }
//                    	 
//                    	 if(!isPresent){
//                    		 //delete event with id 'eventID'
//                    		// System.out.println("Enent is deleted whose EventID is :- " +eventID);
//                    		 PreparedStatement stm = con.prepareStatement("delete from Event where EventID=?");
//                    		 stm.setInt(1, eventID);
//                    		 boolean execcode = stm.execute();
//                    		 if (!execcode) {
//                    			 System.out.println("Successfully removed the Event with id : " +eventID);
//                    		 }
//                    	 }
//                     }
//                     
//                     eventsList.clear();
//             }
//             }catch(SQLException sqle) {
//                 sqle.printStackTrace();
//             }finally {
//                 DBHandler.closePStatement(pstmt);
//                 DBHandler.closeResultSet(res);
//                 //DBHandler.closeDBConnection(con);
//             }
//         } else {
//         }
//    	 
//     }
     
     //** This function is used to update event if its newsstory, category or sources is changed
     
//     public int updateEvent(String description, String newsstory, String categoryname, String date, ArrayList<String> storeurl, int wikiid){
//         boolean result = false;
//         int eventid =0;
//         if(DBHandler.isClosed(con)) {
//             con = DBHandler.openConnection();
//         }
//         if(!DBHandler.isClosed(con)) {
//             try {
//                 int storyid = 0;
//                 int catid = 0;
//                 String sourceids = "";
//                 /*if(newsstory != null && !newsstory.isEmpty()){
//                     pstmt = con.prepareStatement("select StoryID from NewsStory where Label=?");  // If I have to change that because of newsStory(same but different) then check for WikiRefID instead of Label.
//                     pstmt.setString(1,newsstory);
//                     res = pstmt.executeQuery();
//                     while(res.next()){
//                         storyid = res.getInt("StoryID");
//                     }   
//                 }*/
//                 if(newsstory != null && !newsstory.isEmpty()){
//                     pstmt = con.prepareStatement("select StoryID from NewsStory where WikiRefID=?");  
//                     // If I have to change that because of newsStory(same but different) then check for WikiRefID instead of Label.
//                     pstmt.setInt(1,wikiid);
//                     res = pstmt.executeQuery();
//                     while(res.next()){
//                         storyid = res.getInt("StoryID");
//                     }   
//                 }
//                 if(categoryname != null && !categoryname.isEmpty()){
//                     pstmt = con.prepareStatement("select CategoryID from Category where Name=?");
//                     pstmt.setString(1,categoryname);
//                     res = pstmt.executeQuery();
//                     while(res.next()){
//                         catid = res.getInt("CategoryID");
//                     } 
//                 }
//                 
//                 for(String s : storeurl){
//                     if(s!=null && !s.isEmpty()){
//                         pstmt = con.prepareStatement("select SourceID from Source where URL=? order by SourceID");
//                         pstmt.setString(1,s);
//                         res = pstmt.executeQuery();
//                         int minid = Integer.MAX_VALUE;
//                         while(res.next()){
//                         	int sid = res.getInt("SourceID");
//                             if (sid <minid) minid = sid;
//                         } 
//                         sourceids = sourceids+" $ "+ minid;
//                     }
//                     DBHandler.closePStatement(pstmt);
//                     DBHandler.closeResultSet(res);
//                 }
//                 if(!date.equals("0000-00-00")){    
//                    
//                     pstmt = con.prepareStatement("update Event set NewsStoryID=?, CategoryID=?, Sources=? where Description=?");
//                     pstmt.setInt(1,storyid);
//                     pstmt.setInt(2, catid);
//                     pstmt.setString(3, sourceids);
//                     pstmt.setString(4, description);
//                     int affectedRow = pstmt.executeUpdate();
//                     if(affectedRow == 1) {
//                         result = true;
//                     }   
//                     pstmt = con.prepareStatement("select EventID from Event where NewsStoryID=? and CategoryID=? and Date=? and Description=?");
//                     pstmt.setInt(1,storyid);
//                     pstmt.setInt(2,catid);
//                     pstmt.setString(3,date);
//                     pstmt.setString(4, description);
//                     res = pstmt.executeQuery();
//                     while(res.next()){
//                         eventid = res.getInt("EventID");
//                     }
//                 }
//             }catch(SQLException sqle) {
//                 sqle.printStackTrace();
//             }finally {
//                 DBHandler.closePStatement(pstmt);
//                 DBHandler.closeResultSet(res);
//                 //DBHandler.closeDBConnection(con);
//             }
//         } else {
//         }
//         return eventid;
//     }
     
  // This is used to update the event_entity relation
    
//     public boolean updateEventEntityRelation(int eventid, ArrayList<String> wikiurls){
//    	 
//         boolean result = false;
//         if(eventid == 0)
//             return result;
//         if(DBHandler.isClosed(con)) {
//             con = DBHandler.openConnection();
//         }
//         if(!DBHandler.isClosed(con)) {
//             try {
//                 int wikiid = 0;
//                 pstmt = con.prepareStatement("delete from Event_Entity_Relation where EventID = ?");
//                 pstmt.setInt(1, eventid);
//                 boolean execode = pstmt.execute();
//                 
//                 DBHandler.closePStatement(pstmt);
//                 
//                 for(String s : wikiurls){
//                     if(s!=null && !s.isEmpty()){
//                    	 s= "http://en.wikipedia.org"+s;
//                         pstmt = con.prepareStatement("select WikiRefID from WikiRef where WikipediaURL=?");
//                         pstmt.setString(1,s);
//                         res = pstmt.executeQuery();
//                         while(res.next()){
//                             wikiid = res.getInt("WikiRefID");
//                         }
//                         pstmt = con.prepareStatement("insert ignore into Event_Entity_Relation(EventId,WikiRefID) values(?,?)");
//                         pstmt.setInt(1,eventid);
//                         pstmt.setInt(2,wikiid);
//                         int affectedRow = pstmt.executeUpdate();
//                         if(affectedRow == 1) {
//                             result = true;
//                         } 
//                     }
//                     DBHandler.closePStatement(pstmt);
//                     DBHandler.closeResultSet(res);
//                 }
//             }catch(SQLException sqle) {
//                 sqle.printStackTrace();
//             }finally {
//                 DBHandler.closePStatement(pstmt);
//                 DBHandler.closeResultSet(res);
//                 //DBHandler.closeDBConnection(con);
//             }
//         } else {
//         }
//         return result;
//     }
     
     /**
      * Remove event duplications
      * @param time: month of removing
      */
//     public void removeDuplication( String time) {
//         if(DBHandler.isClosed(con)) {
//             con = DBHandler.openConnection();
//         }
//         if(!DBHandler.isClosed(con)) {
//        	 ArrayList<String> allevents = new ArrayList<String> ();
//             try {
//                 pstmt = con.prepareStatement("select * from Event where Date like?");  
//                 pstmt.setString(1, time + "%");
//                 
//                 res = pstmt.executeQuery();
//                 while(res.next()){
//                     int eventid = res.getInt("EventID");
//                     String description = res.getString("Description").trim();
//                     String date = res.getString("Date").trim();
//                     //looking for all previously visited events that are duplicated to this event
//                     for (String e: allevents) {
//                    	 String[] tmp = e.split("\\t");
//                    	 int preID = Integer.valueOf(tmp[0]);
//                    	 String predate = tmp[1];
//                    	 String preDes = tmp[2];
//                    	 
//                    	 if (predate.equals(date) && preDes.hashCode()==description.hashCode()) {
//                    		 //remove the event preID
//                    		 PreparedStatement stm = con.prepareStatement("delete from Event where EventID=?");
//                    		 stm.setInt(1, preID);
//                    		 boolean execcode = stm.execute();
//                    		 if (execcode) {
//                    			 System.out.println("Successfully remove duplicated");
//                    		 }
//                    	 }
//                     }
//                     //adding the to the list
//                     String strEvent = String.format("%d\t%s\t%s", eventid, date, description);
//                     allevents.add(strEvent);
//                 }   
//             }catch(Exception e) {
//            	 e.printStackTrace();
//             }
//         }
//                 
//     }
     
     
     /*This function 
      * @param : short name of the month. For example 'Jan', 'Feb'
      * 
      * @ Returns : full name for the month and its corresponding number. For example 'January  01' for 'Jan' 
      */
     public String[] ret_month_name_num(String smonth){
         
     	String[] month = new String[2];
             if      (smonth.compareTo("Jan")==0)  {month[0] = "January"; month[1] = "01"; }
             else if (smonth.compareTo("Feb")==0)  {month[0] = "February" ; month[1] = "02";}
             else if (smonth.compareTo("Mar")==0)  {month[0] = "March" ; month[1] = "03";}
             else if (smonth.compareTo("Apr")==0)  {month[0] = "April" ; month[1] = "04";}
             else if (smonth.compareTo("May")==0)  {month[0] = "May" ; month[1] = "05";}
             else if (smonth.compareTo("Jun")==0)  {month[0] = "June" ; month[1] = "06";}
             else if (smonth.compareTo("Jul")==0)  {month[0] = "July" ; month[1] = "07";}
             else if (smonth.compareTo("Aug")==0)  {month[0] = "August" ; month[1] = "08";}
             else if (smonth.compareTo("Sep")==0)  {month[0] = "September" ; month[1] = "09";}
             else if (smonth.compareTo("Oct")==0)  {month[0] = "October" ; month[1] = "10";}
             else if (smonth.compareTo("Nov")==0)  {month[0] = "November" ; month[1] = "11";}
             else if (smonth.compareTo("Dec")==0)  {month[0] = "December" ; month[1] = "12";}
             else System.out.println("Invalid month name is passed to this function" +smonth);
        
         return month;
     }
      /*This function takes date object as a parameter, extract year,month,day out of that and return the date in the format 'year-month-day'
       * 
       * @param : date object 
       */
     public String returnDate(java.util.Date date){
    	 
         String temp1= date.toString();
         String [] tempArray = temp1.split(" ");
         
         String[] monthName_Num  = ret_month_name_num(tempArray[1]);
         String temp = tempArray[5].concat("-");
         temp = temp.concat(monthName_Num[1]);
         temp = temp.concat("-");
         temp = temp.concat(tempArray[2]);
         return temp;
     }
     
     /* This function accept the number of the past days to be synchronized as parameter and returns all the past dates.
      *This function takes the current system date and build all the dates manually .
      */
     public String[] returnPastDates(int windowSize){
         String[] dateWindow = new String[windowSize];
         int i= (windowSize-1);
         java.util.Date currDate = new java.util.Date();
         dateWindow[i]= returnDate(currDate);
         for(i=(windowSize-2);i>=0;i--){
             
        	 java.util.Date prevDate = new java.util.Date(currDate.getTime()-(24*60*60*1000));
             dateWindow[i] = returnDate(prevDate);
             currDate = prevDate;
         }
         return dateWindow;
     }
     
     /*This method builds all the dates from the given date to the window size in the past
      * 
      * @param : date with format[yyyy-mm-dd] and number of days in the past  
      * 
      * @return : all the past dates starting from given date and back untill window size
      */
     public String[] getPastDates(String date, int windowSize)
     {
    	 String[] dates = new String[windowSize];
    	 
    	 java.util.Date currDate = new java.util.Date();
    	 
    	 int count = 0; //to control the infinite loop
    	 //String temp = null; //temporary variable to store intermediate results
    	 for(;;)
    	 {
    		 count++;
    		 if(returnDate(currDate).equals(date) || count == 360)
    		 {	 
    			 break;
    		 } 
    		 java.util.Date prevDate = new java.util.Date(currDate.getTime()-(24*60*60*1000));
    		 currDate = prevDate;
    	 }
    	 
    	 if(returnDate(currDate).equals(date))
    	 {
    		 
    		 for(int i=windowSize-1; i>=0; i--)
    		 {
    			 dates[i] = returnDate(currDate);
    			 java.util.Date prevDate = new java.util.Date(currDate.getTime()-(24*60*60*1000));
        		 currDate = prevDate;
    		 }
    	 }
    	 else{
    		 System.out.println("*"+date+"* is not found in the past. \nPlease make sure the formate of the date [yyyy-mm-dd]");
    		 System.out.println("The date should not be more than one year back");
    	 }
    	 
    	 return dates;
     }
     
     
     //****************************************************************************************************************
    
     
     public boolean insertIntoNewWikiRef(String name, String wikipediaURL, String dbpediaURI, String yagoURI){
     	try {
 			wikipediaURL = java.net.URLDecoder.decode(wikipediaURL, "UTF-8");
 			name = name.replaceAll("[^\\w\\s]","");
 		} catch (UnsupportedEncodingException e) {
 			e.printStackTrace();
 		}
         boolean done = false;
         boolean ispresent = false;
         PreparedStatement pstmt = null;
         ResultSet result = null;
         ResultSet result2 = null;
         
             try {
                 if(wikipediaURL != null && !wikipediaURL.isEmpty()){
                     pstmt = db.openConnection().prepareStatement("select WikiRefID, Name from WikiRef where WikipediaURL=?");
                     pstmt.setString(1,wikipediaURL);
                     result = pstmt.executeQuery();
                     if(!result.next()){// does not exist
                     	//for safe case, check if the name existed but in different URL???
                         pstmt = db.openConnection().prepareStatement("select WikiRefID, Name from WikiRef where Name=?");
                         pstmt.setString(1, name);
                         result2 = pstmt.executeQuery();
                         if(result2.next()){ // 
                            ispresent = true;
                         }
                         
                         if(!ispresent){
                        	 pstmt = db.openConnection().prepareStatement("insert ignore into WikiRef(Name, WikipediaURL, dbpediaURI, yagoURI) values(?, ?, ?, ?)");
    		            	 pstmt.setString(1,name);
    		            	 pstmt.setString(2,wikipediaURL);
    		            	 pstmt.setString(3, dbpediaURI);
    		            	 pstmt.setString(4, yagoURI);
                             int affectedRow = pstmt.executeUpdate();
                             if(affectedRow == 1) {
                                 done = true;
                                 System.out.println("inserted into WikiRef-> Name :"+name+"----WikiRefURL : "+wikipediaURL);
                             }
                         }
                     }
                     else {//if exists, update that WikiRef entity if the name changed
                     	String oldname = result.getString("Name");
                     	if (!oldname.equals(name)) {
 	                    	pstmt = db.openConnection().prepareStatement("update ignore WikiRef set Name=? Where WikipediaURL=? and Name=?");
 	                        pstmt.setString(1,name);
 	                        pstmt.setString(2,wikipediaURL);
 	                        pstmt.setString(3,oldname);
 	                        int affectedRow = pstmt.executeUpdate();
 	                        if(affectedRow == 1) {
 	                            done = true;
 	                        }
                     	}
                     }
                 } 
             } catch (SQLException e) {			
      			e.printStackTrace();
      		}finally{
      			if (result != null)  try { result.close(); } catch (SQLException e) {e.printStackTrace();}
      			if (result2 != null) try { result2.close(); } catch (SQLException e) {e.printStackTrace();}
      			if (pstmt != null)   try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
      		}
          return done;
        
     }
     
     
     
//     public boolean insertIntoNewsStoryExtraEntityRel(int storyid, int entityid) {
//         boolean result = false;
//         if(DBHandler.isClosed(con)) {
//             con = DBHandler.openConnection();
//         }
//         if(!DBHandler.isClosed(con)) {
//             try {
//		            	 pstmt = con.prepareStatement("insert ignore into NewsStory_ExtraEntity_Relation(NewsStoryID, WikiRefID) values(?, ?)");
//		            	 pstmt.setInt(1,storyid);
//		            	 pstmt.setInt(2,entityid);
//                         int affectedRow = pstmt.executeUpdate();
//                         if(affectedRow == 1) {
//                             result = true;
//                             System.out.println("Inserted into NewsStory-ExtraEntity-Relation-> Srory id : "+storyid+"---WikiRefID : "+entityid);
//                     }
//             
//                 
//             }catch(SQLException sqle) {
//                 sqle.printStackTrace();
//             }finally {
//                 DBHandler.closePStatement(pstmt);
//                 DBHandler.closeResultSet(res);
//                 //DBHandler.closeDBConnection(con);
//             }
//         } else {
//         }
//         return result;
//     }
     
     
     
     
     /*This method accept date as a parameter, formulate event_objects from DataBase and return all the event_objects on the date passed as parameter
      * 
      * @parameter : date [format : yyyy-mm-dd]
      * 
      * @Returns : EventObject
      */
    // @SuppressWarnings("resource")
	@SuppressWarnings("resource")
	/*
	 * @param : EvebtObject 
	 * 
	 * @Return : true if EvebtObject exist, false otherwise
	 */
 public String isExistsInDB(Event event){
		
	 String eventId = null;
	 PreparedStatement pstmt = null;
     ResultSet result = null;	 
         try {
	             if(event.getDescription() != null && !event.getDescription().isEmpty()){
	                 pstmt = db.openConnection().prepareStatement("select EventID from Event where Description = ?");
	                 pstmt.setString(1,event.getDescription());
	                 result = pstmt.executeQuery();
	                 if(result.next()){
	                     eventId = result.getString("EventID");
	                 }
	             }
             
         } catch (SQLException e) {			
   			e.printStackTrace();
   		}finally{
   			if (result != null)  try { result.close(); } catch (SQLException e) {e.printStackTrace();}
   			if (pstmt != null)   try { pstmt.close();  } catch (SQLException e) {e.printStackTrace();}
   		}
      
	 return eventId;
 }



	public void close() {
		db.closeConnection();
	}
 
 
 
 /*
  * @Param : EventObject
  * 
  * @Return : delete event and corresponding entries at the event_entity_relation
  * 
  */
     
// public boolean deleteEvent(Event e)
// {
//	 boolean execcode = true;
//     if(DBHandler.isClosed(con)) {
//         con = DBHandler.openConnection();
//     }
//     if(!DBHandler.isClosed(con)) {
//         try {
//        	 if(e.getEventId() != null && !e.getEventId().isEmpty()){
//     
//                	 pstmt = con.prepareStatement("delete from Event where EventID=?");
//            		 pstmt.setInt(1, Integer.parseInt(e.getEventId()));
//            		 execcode = pstmt.execute();
//            		 if (!execcode) {
//            			 System.out.println("Event wiht eventID : "+e.getEventId()+" is deleted successfully !!" );
//            			 
//            			 //now delete all the respective entries from event_entity relation
//            			 pstmt = con.prepareStatement("delete from Event_Entity_Relation where EventID=?");
//            			 pstmt.setInt(1, Integer.parseInt(e.getEventId()));
//            			 boolean temp = pstmt.execute();
//            			 if(!temp)
//            				 System.out.println("Related entries at event_entity relation have been deleted successfully");
//            			 else
//            				 System.out.println("Related entries at event_entities relation could not be able to deleted");
//            		 }
//            		 else
//            		 {
//            			 System.out.println("Event wiht eventID : "+e.getEventId()+" could not able to be deleted !!" );
//            		 }
//                 }
//             //}
//         }catch(SQLException sqle) {
//             sqle.printStackTrace();
//         }finally {
//             DBHandler.closePStatement(pstmt);
//             DBHandler.closeResultSet(res);
//             //DBHandler.closeDBConnection(con);
//         }
//     } else {
//     }
//     return !execcode;
// }
 
 
 /*
  * This methods update the 
  */
 
// public void updateEvent(Event event)
// {
//	 //boolean result = false;
//	 //ContentHandling ch = new ContentHandling();
//	 DBOperation dbOperation = new DBOperation();
//	 
//	 
//	 //if event exists, perform delete and insert
//	 if(isExistsInDB(event) != null )
//	 {
//		 
//		 //delete
//		 deleteEvent(event);
//		 
//		 //insert
//		 dbOperation.insertEventObjIntoDB(event);
//	 }
//	 
//	 //if event doesn't exist, it seems like new event, just insert it into DataBase 
//	 else
//	 {
//		 dbOperation.insertEventObjIntoDB(event);
//	 }
//	 
//	 //return result;
// }
 
 
}//End of  class
