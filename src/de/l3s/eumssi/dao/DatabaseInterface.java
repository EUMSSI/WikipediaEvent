package de.l3s.eumssi.dao;

import java.sql.Connection;
import java.sql.ResultSet;

public abstract class DatabaseInterface
{
	private Connection connection;
	private boolean isConnected;

	public DatabaseInterface()
	{
		setConnection(null);
		isConnected = false;
	}
	
	public DatabaseInterface(String dataBaseUrl, int port, String dataBaseName, String userName, String password)
	{
		try
		{
			connect(dataBaseUrl, port, dataBaseName, userName, password);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	abstract void connect(String dataBaseUrl, int port, String dataBaseName, String userName, String password);
	
	abstract void disconnect() throws Exception;
	
	abstract protected ResultSet executeQuery(String query);
	
	public boolean isConnected(){return isConnected;}
	
	public void setConnected(boolean isConnected) {
		this.isConnected = isConnected;
	}

	/**
	 * @return the connection
	 */
	public Connection getConnection() {
		return connection;
	}

	/**
	 * @param connection the connection to set
	 */
	public void setConnection(Connection connection) {
		this.connection = connection;
	}
}
