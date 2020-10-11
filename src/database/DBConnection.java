package database;

import java.sql.*;
 
public class DBConnection {
	private static String dbMySQLUrl = "jdbc:mysql://localhost:3306/dblp?characterEncoding=latin1&useConfigs=maxPerformance";
	private static String username = "root";
	private static String password = "123456";
	
	public static Connection getMySQLConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			return DriverManager.getConnection(dbMySQLUrl,username, password);
		}
		catch (Exception e){
			System.out.println("Error while opening a conneciton to database server: "
					+ e.getMessage());
		}
		return null;

	}

	public static Connection getMongoDBConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			
			return DriverManager.getConnection(dbMySQLUrl,username, password);
		}
		catch (Exception e){
			System.out.println("Error while opening a conneciton to database server: "
					+ e.getMessage());
		}
		return null;

	}

	public static Connection getGraphConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			return DriverManager.getConnection(dbMySQLUrl,username, password);
		}
		catch (Exception e){
			System.out.println("Error while opening a conneciton to database server: "
					+ e.getMessage());
		}
		return null;

	}

}
