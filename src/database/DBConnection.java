package database;

import java.sql.*;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Driver;
 
public class DBConnection {
	private static String dbMySQLUrl = "jdbc:mysql://localhost:3306/dblp?characterEncoding=latin1&useConfigs=maxPerformance";
	private static String dbNeo4jLUrl = "bolt://localhost:7687/dblp";
	private static String password = "123456";
	
	public static Connection getMySQLConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			return DriverManager.getConnection(dbMySQLUrl,"root", password);
		}
		catch (Exception e){
			System.out.println("Error while opening a conneciton to database server: "
					+ e.getMessage());
		}
		return null;

	}


	public static Driver getNeo4jConnection() {
		try {
			Class.forName("org.neo4j.jdbc.Driver");
			System.out.println("good");

			return (Driver) GraphDatabase.driver(dbNeo4jLUrl,AuthTokens.basic("neo4j", password));
		}
		catch (Exception e){
			System.out.println("Error while opening a conneciton to database server: "
					+ e.getMessage());
		}
		return null;

	}

}
