package de.l3s.eumssi.wikiimport;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;



public class Export {

  private static String readAll(Reader rd) throws IOException {
    StringBuilder sb = new StringBuilder();
    int cp;
    while ((cp = rd.read()) != -1) {
      sb.append((char) cp);
    }
    return sb.toString();
  }
  public String readJsonFromUrl(String url) throws IOException {
	    
	  	InputStream is = new URL(url).openStream();
	    try {
	      BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
	      String jsonText = readAll(rd);
	      return jsonText;
	    } finally {
	      is.close();
	    }
	  }

  /*
   * This function return full name and number for the particular month. Example for "Jan" returns "January 01"
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
  /*
   * This function builds date in the format 'yyyy-mm-dd' out of date object.
   */
  public String returnDate(Date date){
	  String currDate = null;
	  String temp = date.toString();
	  String[] str = temp.split(" ");
	  String[] month = ret_month_name_num(str[1]);
	  String str1 = str[5].concat("-");
	  str1 = str1.concat(month[1]);
	  str1 = str1.concat("-");
	  currDate = str1.concat(str[2]);
	  return currDate;
  }
  
 /* public String returnStartDate(int days){
	  
	  int oneDay = 24*60*60*1000;
	  String endDate = null;
	  Date currDate = new Date();
	  for(int j=1;j<=days;j++){
		  Date prevDate = new Date(currDate.getTime()-oneDay);
		  currDate = prevDate; 
	  }  
	  endDate = returnDate(currDate);
	  return endDate;
  }
   
  }
  */
  
  /*
   * This function will return the first date from event table.
   */
 /* public static String returnFirstDateFromEvent(){
	  
	   Connection con= DBHandler.getConnection();
	   PreparedStatement pstmt =  null ;
	   ResultSet res = null ;
	  
	   String fdate= "0000-00-00";
      if(DBHandler.isClosed(con)) {
          con = DBHandler.getConnection();
      }
      if(!DBHandler.isClosed(con)) {
          try {
                  pstmt = con.prepareStatement("select * from event order by Date");
                  res = pstmt.executeQuery();
                  if(res.next()){
                     fdate = res.getString("Date"); 
                     System.out.println("first date in the event table : "+fdate);
                  }
          
          }catch(SQLException sqle) {
              sqle.printStackTrace();
          }finally {
              DBHandler.closePStatement(pstmt);
              DBHandler.closeResultSet(res);
              DBHandler.closeDBConnection(con);
          }
      } else {
      }
      return fdate;
  }
  */
  
  
  public static void main(String[] args) throws IOException, InterruptedException {
	  

	  Properties prop = new Properties();
  	  FileInputStream input = new FileInputStream("./configs/export.properties");
  	  prop.load(input);
	  
  	  String pathForEvents = prop.getProperty("pathForEvents");
  	  String pathForStories = prop.getProperty("pathForStories");
  	  int interval = Integer.parseInt(prop.getProperty("interval"));
  	  
	  long oneDay = 24*60*60*1000;
	  
	  System.out.println("Path for event : "+pathForEvents);
	  System.out.println("Path for story : "+pathForStories);
	  System.out.println("Interval : "+interval+ " Days");
		
	  Export exp = new Export();
	  
	  for(;;){
		
		String endDate = exp.returnDate(new Date());
		
	//	String startDate = returnFirstDateFromEvent();
		String startDate = "2000-01-01";
		System.out.println("Start Date is : "+startDate);
		System.out.println("End date is  : " +endDate);
		
		String fileName ="From {"+startDate+"} to {"+endDate+"}"+".json"; 
		String eventPath = pathForEvents+"\\"+"Events "+fileName;
		String storyPath = pathForStories+"\\"+"Stories "+fileName;
		
		System.out.println("final event path with file name : " +eventPath);
		System.out.println("final story path with file name : " +storyPath);
		
		String endOfURL = startDate+"/"+endDate;
		String eventURL = "http://pharos.l3s.uni-hannover.de:8080/WikiTimes/webresources/WebService/getEvents/";
		eventURL = eventURL.concat(endOfURL);
		
		String storyURL = "http://pharos.l3s.uni-hannover.de:8080/WikiTimes/webresources/WebService/getStories/";
		storyURL = storyURL.concat(endOfURL);
		
		
		//Export events to the eventpath
		File file = new File(eventPath);
		if(!file.exists())
			file.createNewFile();
		
		try {
			
		  System.out.println("\nReading events from URL...");
		  String str = exp.readJsonFromUrl(eventURL);
		  System.out.println("Writing events to the file...");
          FileWriter jsonFileWriter = new FileWriter(eventPath);
          jsonFileWriter.write(str);
          jsonFileWriter.flush();
          jsonFileWriter.close();
          System.out.println("**Reading and writing events is done !!");

		} catch (IOException e) {

          e.printStackTrace();
         }
		
		//export stories to the storypath
		File file1 = new File(storyPath);
		if(!file1.exists())
			file1.createNewFile();
		
		try {

		  System.out.println("\nReading stories from URL...");
		  String str = exp.readJsonFromUrl(storyURL);
		  System.out.println("Writing stories in the file...");
          FileWriter jsonFileWriter = new FileWriter(storyPath);
          jsonFileWriter.write(str);
          jsonFileWriter.flush();
          jsonFileWriter.close();
          System.out.println("**Reading and writing of stories is done !!");

		} catch (IOException e) {

          e.printStackTrace();
         }
		
		System.out.println("\nNext export will be after "+interval+" days !!" );
		for(int i=1;i<=interval;i++){
        Thread.sleep(oneDay);
		}
	}
	}
	}