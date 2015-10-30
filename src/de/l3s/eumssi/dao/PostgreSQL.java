package de.l3s.eumssi.dao;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;



public class PostgreSQL extends DatabaseInterface
{	
	public PostgreSQL()
	{
		super();
	}
	
	public PostgreSQL (String dataBaseUrl, int port, String dataBaseName, String userName, String password)
	{
		super(dataBaseUrl, port, dataBaseName, userName, password);
	}
	
	public void connect(String dataBaseUrl, int port, String dataBaseName, String userName, String password)
	{
		try
		{
			
			// build the connection string
			String connectionString = "jdbc:postgresql://" + dataBaseUrl + "/" + dataBaseName;
			
			// connect to the database
			System.out.println("Attempting to connect to the database: " + connectionString);
			setConnection(DriverManager.getConnection(connectionString, userName, password));
			System.out.println("Connection established: " + getConnection());
			
			setConnected(true);
		}
		
		catch(Exception e)
		{
			e.printStackTrace();
			setConnected(false);
			return;
		}

	}
	
	public void disconnect()
	{
		try
		{
			getConnection().close();
			setConnected(false);
			System.out.println("Connection closed");
		}
		
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public ResultSet executeQuery(String query)
	{
		// create a Statement object to perform queries and a ResultSet object to store the result
		Statement statement = null;
		ResultSet resultSet = null;
		
		try
		{
			//System.out.println("Performing query: " + query);
			statement = getConnection().createStatement();
			resultSet = statement.executeQuery(query);
			//System.out.println("Query performed");
			
			return resultSet;
		}
		catch(SQLException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public boolean isConnected(){return super.isConnected();}
	
	public static void close(PostgreSQL database) throws SQLException
	{
		if(database != null) {
            database.disconnect();
			database = null;
		}
	}
    public synchronized static void closeResultSet(ResultSet res) {
        if(res != null) {
            try{
                res.close();
                res = null;
            }
            catch (SQLException e) {
                System.out.println("Could not close ResultSet "+e);
            }
        }
    }
	
}
