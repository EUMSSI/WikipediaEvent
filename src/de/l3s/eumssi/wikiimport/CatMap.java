/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.l3s.eumssi.wikiimport;
import java.io.*;
import java.util.Collections;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.*;
/**
 *
 * @author kanik
 * This file stores the mapped categories of each categorized event into a separate file which would be used later to construct our arff files.
 */
public class CatMap {
    public static void main(String[] args) throws IOException, SQLException {
        //declaring class variables
        Connection con;
        PreparedStatement pstmt = null;
        ResultSet res = null;
        ResultSet result = null;
        Statement stmt;
        
        HashMap<String, String> categories = new HashMap<String, String> ();
        BufferedReader br = new BufferedReader (new FileReader(new File("/home/gupta/Code/cat.txt")));
        String cat;
        while ((cat=br.readLine())!=null) {
                String[] categorymap = cat.split("\t");
                System.out.println("Category : "+categorymap[0]+"  ID : "+categorymap[1]);
                categories.put(categorymap[0],categorymap[1]);
        }
        br.close();
        // This is a variable to establish the connection with the database.
        con = DBHandler.openConnection();
        
        PrintWriter out = new PrintWriter(new FileWriter("/home/gupta/Code/catmap.txt"));
        if(!DBHandler.isClosed(con)) {
            try {
                    pstmt = con.prepareStatement("select EventID, CategoryID from WKEvent where CategoryID>0");
                    res = pstmt.executeQuery();
                    while(res.next()){
                        String category = null;
                        int eventid = res.getInt("EventId");
                        System.out.println("Evenid : "+eventid);
                        int catid = res.getInt("CategoryID");
                        pstmt = con.prepareStatement("select Name from Category where CategoryID=?");
                        pstmt.setInt(1, catid);
                        result = pstmt.executeQuery();
                        while(result.next()){
                            category = result.getString("Name");
                        }
                        out.print(categories.get(category));
                        out.print("\n");
                    }
            
            }catch(SQLException sqle) {
                sqle.printStackTrace();
            }finally {
                DBHandler.closePStatement(pstmt);
                DBHandler.closeResultSet(res);
                DBHandler.closeResultSet(result);
                //DBHandler.closeDBConnection(con);
            }
        }
        out.close();
    }
}
