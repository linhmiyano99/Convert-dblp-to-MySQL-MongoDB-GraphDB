package xml;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.xml.parsers.*;

import org.neo4j.driver.Session;
import org.neo4j.driver.Driver;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import database.DBConnection;
import com.mongodb.*;




public class Parser {
	private Connection conn;
	MongoClient mongoClient;
	DB db;
	DBCollection dbPaper;
    DBCollection dbAuthor;
    DBCollection dbCitation;
    DBCollection dbConference; 
    Driver driver;
	private int curElement = -1;
	private int ancestor = -1;
	private Paper paper;
	private Conference conf;
	int line = 0;
	PreparedStatement stmt_inproc, stmt_conf, stmt_author, stmt_cite;
	int errors = 0;
	StringBuffer author;
	public static final int MONGODB = 0;
	public static final int NEO4J = 1;
	public static final int MYSQL = 2;
	
	private class ConfigMySQLHandler extends DefaultHandler {
		public void startElement(String namespaceURI, String localName,
				String rawName, Attributes atts) throws SAXException {
			if (rawName.equals("inproceedings")) {
				ancestor = Element.INPROCEEDING;
				curElement = Paper.INPROCEEDING;
				paper = new Paper();
				paper.key = atts.getValue("key");
			} else if (rawName.equals("proceedings")) {
				ancestor = Element.PROCEEDING;
				curElement = Conference.PROCEEDING;
				conf = new Conference();
				conf.key = atts.getValue("key");
			} else if (rawName.equals("author") && ancestor == Element.INPROCEEDING) {
				author = new StringBuffer();
			}

			if (ancestor == Element.INPROCEEDING) {
				curElement = Paper.getElement(rawName);
			} else if (ancestor == Element.PROCEEDING) {
				curElement = Conference.getElement(rawName);
			} else if (ancestor == -1) {
				ancestor = Element.OTHER;
				curElement = Element.OTHER;
			} else {
				curElement = Element.OTHER;
			}

			line++;
		}

		public void characters(char[] ch, int start, int length)
				throws SAXException {
			if (ancestor == Element.INPROCEEDING) {
				String str = new String(ch, start, length).trim();
				if (curElement == Paper.AUTHOR) {
					author.append(str);
					System.out.println("AUTHOR:" + line);
				} else if (curElement == Paper.CITE) {
					paper.citations.add(str);
					System.out.println("CITE:" + line);
				} else if (curElement == Paper.CONFERENCE) {
					paper.conference = str;
					System.out.println("CONFERENCE:" + line);
				} else if (curElement == Paper.TITLE) {
					paper.title += str;
					System.out.println("TITLE:" + line);
				} else if (curElement == Paper.YEAR) {
					paper.year = Integer.parseInt(str);
					System.out.println("YEAR:" + line);
				}
			} else if (ancestor == Element.PROCEEDING) {
				String str = new String(ch, start, length).trim();
				if (curElement == Conference.CONFNAME) {
					conf.name = str;
				} else if (curElement == Conference.CONFDETAIL) {
					conf.detail = str;
				}
			}
		}

		public void endElement(String namespaceURI, String localName,
				String rawName) throws SAXException {
			if (rawName.equals("author") && ancestor == Element.INPROCEEDING) {
				paper.authors.add(author.toString().trim());
			}
			
			if (Element.getElement(rawName) == Element.INPROCEEDING) {
				ancestor = -1;
				try {
					if (paper.title.equals("") || paper.conference.equals("")
							|| paper.year == 0) {
						System.out.println("Error in parsing " + paper);
						errors++;
						return;
					}
					stmt_inproc.setString(1, paper.title);
					stmt_inproc.setInt(2, paper.year);
					stmt_inproc.setString(3, paper.conference);
					stmt_inproc.setString(4, paper.key);
					stmt_inproc.addBatch();

					for (String author: paper.authors) {
						stmt_author.setString(1, author); 
						stmt_author.setString(2, paper.key);
						stmt_author.addBatch();
					}
					for (String cited: paper.citations) {
						if (!cited.equals("...")) {
							stmt_cite.setString(1, paper.key);
							stmt_cite.setString(2, cited);
							stmt_cite.addBatch();
						}
					}
				} catch (SQLException e) {
					e.printStackTrace();
					System.out.println("line:" + line);
					System.exit(0);
				}
			} else if (Element.getElement(rawName) == Element.PROCEEDING) {
				ancestor = -1;
				try {
					if (conf.name.equals(""))
						conf.name = conf.detail;
					if (conf.key.equals("") || conf.name.equals("")
							|| conf.detail.equals("")) {
						System.out.println("Line:" + line);
						System.exit(0);
					}
					stmt_conf.setString(1, conf.key);
					stmt_conf.setString(2, conf.name);
					stmt_conf.setString(3, conf.detail);
			        System.out.print(conf.name);

					stmt_conf.addBatch();
				} catch (SQLException e) {
					e.printStackTrace();
					System.out.println("line:" + line);
					System.exit(0);
				}
			}

			if (line % 10000 == 0) {
				try {
					stmt_inproc.executeBatch();
					stmt_conf.executeBatch();
					stmt_author.executeBatch();
					stmt_cite.executeBatch();
					conn.commit();
				} catch (SQLException e) {
					System.err.println(e.getMessage());
				}
			}
		}
		
	}
		private class ConfigNeo4jHandler extends DefaultHandler {
			public void startElement(String namespaceURI, String localName,
					String rawName, Attributes atts) throws SAXException {
				if (rawName.equals("inproceedings")) {
					ancestor = Element.INPROCEEDING;
					curElement = Paper.INPROCEEDING;
					paper = new Paper();
					paper.key = atts.getValue("key");
				} else if (rawName.equals("proceedings")) {
					ancestor = Element.PROCEEDING;
					curElement = Conference.PROCEEDING;
					conf = new Conference();
					conf.key = atts.getValue("key");
				} else if (rawName.equals("author") && ancestor == Element.INPROCEEDING) {
					author = new StringBuffer();
				}

				if (ancestor == Element.INPROCEEDING) {
					curElement = Paper.getElement(rawName);
				} else if (ancestor == Element.PROCEEDING) {
					curElement = Conference.getElement(rawName);
				} else if (ancestor == -1) {
					ancestor = Element.OTHER;
					curElement = Element.OTHER;
				} else {
					curElement = Element.OTHER;
				}

				line++;
			}

			public void characters(char[] ch, int start, int length)
					throws SAXException {
				if (ancestor == Element.INPROCEEDING) {
					String str = new String(ch, start, length).trim();
					if (curElement == Paper.AUTHOR) {
						author.append(str);
					} else if (curElement == Paper.CITE) {
						paper.citations.add(str);
					} else if (curElement == Paper.CONFERENCE) {
						paper.conference = str;
					} else if (curElement == Paper.TITLE) {
						paper.title += str;
					} else if (curElement == Paper.YEAR) {
						paper.year = Integer.parseInt(str);
					}
				} else if (ancestor == Element.PROCEEDING) {
					String str = new String(ch, start, length).trim();
					if (curElement == Conference.CONFNAME) {
						conf.name = str;
					} else if (curElement == Conference.CONFDETAIL) {
						conf.detail = str;
					}
				}
			}

			public void endElement(String namespaceURI, String localName,
					String rawName) throws SAXException {
				Session session = driver.session();
				if (rawName.equals("author") && ancestor == Element.INPROCEEDING) {
					paper.authors.add(author.toString().trim());
				}
			

				if (Element.getElement(rawName) == Element.INPROCEEDING) {
					ancestor = -1;
					if (paper.title.equals("") || paper.conference.equals("")
							|| paper.year == 0) {
						System.out.println("Error in parsing " + paper);
						errors++;
						return;
					}
				

				    session.writeTransaction(
			        		tx -> tx.run("MERGE (n:paper {title: \'"+ getNeoSyntax(paper.title) 
			        				+ "\', year: "+paper.year
			        				+", conference: \'"+getNeoSyntax(paper.conference) 
			        				+"\',paper_key: \'"+getNeoSyntax(paper.key)+"\'})"));
			        



					for (String author: paper.authors) {
					
						  
						session.writeTransaction(
						tx -> tx.run("MERGE (n:author {name: \'"+getNeoSyntax(author)
						+"\'})"));
						session.writeTransaction(
								tx -> tx.run("MATCH (a:author),(b:paper)"
										+ " WHERE a.name = \'" +getNeoSyntax(author) + "\' AND b.paper_key =\'"+getNeoSyntax(paper.key)
										+ "\' MERGE (a)-[r:AUTHOR]->(b)"
										+ "RETURN type(r)"));
						
						
					}
					for (int i = 0; i < paper.authors.size(); i++) {
						for(int j = i + 1; j < paper.authors.size(); j++) {
							int m = j, n = i;
							session.writeTransaction(
									tx -> tx.run("MATCH (a:author),(b:author)"
											+ "WHERE a.name = \'" + getNeoSyntax(paper.authors.get(n)) + "\' AND b.name = \'" + getNeoSyntax(paper.authors.get(m))
											+ "\' MERGE (a)-[r:CO_AUTHOR]->(b)"
											+ "RETURN type(r)"));
						}
					}
					if(paper.citations.size() >0) {
					System.out.println("paper.citations:" + paper.citations);
					}

					for (String cited: paper.citations) {
						if (!cited.equals("...")) {
							 session.writeTransaction(
						        		tx -> tx.run(
						        				"MERGE (n:citation {paper_cite_key: \'"+getNeoSyntax(paper.key)
						        		+"\',paper_cited_key: \'"+getNeoSyntax(cited)+"\'})"));
						}
					}
				} else if (Element.getElement(rawName) == Element.PROCEEDING) {
					ancestor = -1;
					if (conf.name.equals(""))
						conf.name = conf.detail;

					session.writeTransaction(
			        		tx -> tx.run(
			        				"MERGE (n:conference {conf_key: \'"+getNeoSyntax(conf.key)
			        		+"\',name: \'"+getNeoSyntax(conf.name)
			        		+"\',detail: \'"+getNeoSyntax(conf.detail)+"\'})"));

				}
				session.close();
			}


		private void Message(String mode, SAXParseException exception) {
			System.out.println(mode + " Line: " + exception.getLineNumber()
					+ " URI: " + exception.getSystemId() + "\n" + " Message: "
					+ exception.getMessage());
		}

		public void warning(SAXParseException exception) throws SAXException {
			Message("**Parsing Warning**\n", exception);
			throw new SAXException("Warning encountered");
		}

		public void error(SAXParseException exception) throws SAXException {
			Message("**Parsing Error**\n", exception);
			throw new SAXException("Error encountered");
		}

		public void fatalError(SAXParseException exception) throws SAXException {
			Message("**Parsing Fatal Error**\n", exception);
			throw new SAXException("Fatal Error encountered");
		}
	}

	private class ConfigMongoDBHandler extends DefaultHandler {
		public void startElement(String namespaceURI, String localName,
				String rawName, Attributes atts) throws SAXException {
			if (rawName.equals("inproceedings")) {
				ancestor = Element.INPROCEEDING;
				curElement = Paper.INPROCEEDING;
				paper = new Paper();
				paper.key = atts.getValue("key");
			} else if (rawName.equals("proceedings")) {
				ancestor = Element.PROCEEDING;
				curElement = Conference.PROCEEDING;
				conf = new Conference();
				conf.key = atts.getValue("key");
			} else if (rawName.equals("author") && ancestor == Element.INPROCEEDING) {
				author = new StringBuffer();
			}

			if (ancestor == Element.INPROCEEDING) {
				curElement = Paper.getElement(rawName);
			} else if (ancestor == Element.PROCEEDING) {
				curElement = Conference.getElement(rawName);
			} else if (ancestor == -1) {
				ancestor = Element.OTHER;
				curElement = Element.OTHER;
			} else {
				curElement = Element.OTHER;
			}

			line++;
		}

		public void characters(char[] ch, int start, int length)
				throws SAXException {
			if (ancestor == Element.INPROCEEDING) {
				String str = new String(ch, start, length).trim();
				if (curElement == Paper.AUTHOR) {
					author.append(str);
				} else if (curElement == Paper.CITE) {
					paper.citations.add(str);
				} else if (curElement == Paper.CONFERENCE) {
					paper.conference = str;
				} else if (curElement == Paper.TITLE) {
					paper.title += str;
				} else if (curElement == Paper.YEAR) {
					paper.year = Integer.parseInt(str);
				}
			} else if (ancestor == Element.PROCEEDING) {
				String str = new String(ch, start, length).trim();
				if (curElement == Conference.CONFNAME) {
					conf.name = str;
				} else if (curElement == Conference.CONFDETAIL) {
					conf.detail = str;
				}
			}
		}

		public void endElement(String namespaceURI, String localName,
				String rawName) throws SAXException {
			if (rawName.equals("author") && ancestor == Element.INPROCEEDING) {
				paper.authors.add(author.toString().trim());
			}
		

			if (Element.getElement(rawName) == Element.INPROCEEDING) {
				ancestor = -1;
				if (paper.title.equals("") || paper.conference.equals("")
						|| paper.year == 0) {
					System.out.println("Error in parsing " + paper);
					errors++;
					return;
				}
				DBObject documentPaper = new BasicDBObject("paper", new BasicDBObject("title", paper.title)
				           .append("year", paper.year)
				           .append("conference", paper.conference)
				           .append("paper_key", paper.key));
				dbPaper.insert(documentPaper);



				for (String author: paper.authors) {
					DBObject documentAuthor = new BasicDBObject("author", new BasicDBObject("name", author)
				               .append("paper_key", paper.key));
					dbAuthor.insert(documentAuthor);

					
				}
				for (String cited: paper.citations) {
					if (!cited.equals("...")) {
						DBObject documentCitation = new BasicDBObject("citation", new BasicDBObject("paper_cite_key", paper.key)
				                   .append("paper_cited_key", cited));
						dbCitation.insert(documentCitation);
					}
				}
			} else if (Element.getElement(rawName) == Element.PROCEEDING) {
				ancestor = -1;
				if (conf.name.equals(""))
					conf.name = conf.detail;
				if (conf.key.equals("") || conf.name.equals("")
						|| conf.detail.equals("")) {
					System.out.println("Line:" + line);
					System.exit(0);
				}
				DBObject documentConferences = new BasicDBObject("conference", new BasicDBObject("conf_key", conf.key)
				           .append("name",  conf.name)
				           .append("detail", conf.detail));
				dbConference.insert(documentConferences);

			}
		
		}


	private void Message(String mode, SAXParseException exception) {
		System.out.println(mode + " Line: " + exception.getLineNumber()
				+ " URI: " + exception.getSystemId() + "\n" + " Message: "
				+ exception.getMessage());
	}

	public void warning(SAXParseException exception) throws SAXException {
		Message("**Parsing Warning**\n", exception);
		throw new SAXException("Warning encountered");
	}

	public void error(SAXParseException exception) throws SAXException {
		Message("**Parsing Error**\n", exception);
		throw new SAXException("Error encountered");
	}

	public void fatalError(SAXParseException exception) throws SAXException {
		Message("**Parsing Fatal Error**\n", exception);
		throw new SAXException("Fatal Error encountered");
	}
}

	Parser(String uri, int db) throws SQLException {
		switch(db) {
		case Parser.MONGODB:
			parserMongoDB(uri);
			break;
		case Parser.NEO4J:
			parserGraph(uri);
			break;
		case Parser.MYSQL:
			parserMySQL(uri);
			break;
		}
	}
	
	public void parserMySQL(String uri) throws SQLException {
		try {
			System.out.println("Parsing...");
			conn = DBConnection.getMySQLConnection();
			conn.setAutoCommit(false);
			stmt_inproc = conn
					.prepareStatement("insert into paper(title,year,conference,paper_key) values (?,?,?,?)");

			stmt_author = conn
					.prepareStatement("insert into author(name,paper_key) values (?,?)");

			stmt_cite = conn
					.prepareStatement("insert into citation(paper_cite_key,paper_cited_key) values (?,?)");

			stmt_conf = conn
					.prepareStatement("insert into conference(conf_key,name,detail) values (?,?,?)");

			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser parser = parserFactory.newSAXParser();
			ConfigMySQLHandler handler = new ConfigMySQLHandler();
			parser.getXMLReader().setFeature(
					"http://xml.org/sax/features/validation", true);
			parser.parse(new File(uri), handler);
			
			try {
				
			
				  stmt_inproc.executeBatch(); stmt_conf.executeBatch();
				  stmt_author.executeBatch(); stmt_cite.executeBatch();
				
				conn.commit();
				System.out.println("Processed " + line);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			conn.close();
			System.out.println("num of errors: " + errors);
		} catch (IOException e) {
			System.out.println("Error reading URI: " + e.getMessage());
		} catch (SAXException e) {
			System.out.println("Error in parsing: " + e.getMessage());
		} catch (ParserConfigurationException e) {
			System.out.println("Error in XML parser configuration: "
					+ e.getMessage());
		}
	}
	
	@SuppressWarnings("deprecation")
	public void parserMongoDB(String uri) throws SQLException {
		try{

            // To connect to mongodb server
             mongoClient = new MongoClient( "localhost" , 27017);

            // Now connect to your databases
            db = mongoClient.getDB("dblp");
            System.out.println("Connect to database successfully");

            // if collection doesn't exists, MongoDB will create it     
            dbPaper = db.getCollection("paper");
            dbAuthor = db.getCollection("author");
            dbCitation = db.getCollection("citation");
            dbConference = db.getCollection("conference");

            System.out.println("Collection coll selected successfully");

			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser parser = parserFactory.newSAXParser();
			ConfigMongoDBHandler handler = new ConfigMongoDBHandler();
			parser.getXMLReader().setFeature(
					"http://xml.org/sax/features/validation", true);
			parser.parse(new File(uri), handler);
			 
            System.out.println("Document added to Collection coll successfully");
		 }catch(Exception e){
             System.err.println( e.getClass().getName() + ": " + e.getMessage() );
          }
	}
	
	public  void parserGraph(String uri)  throws SQLException {
		try {
		System.out.println("Parsing...");
		driver = (Driver) DBConnection.getNeo4jConnection();
       
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		SAXParser parser = parserFactory.newSAXParser();
		ConfigNeo4jHandler handler = new ConfigNeo4jHandler();
		parser.getXMLReader().setFeature(
				"http://xml.org/sax/features/validation", true);
		parser.parse(new File(uri), handler);
		driver.close();
		System.out.println("num of errors: " + errors);
	} catch (IOException e) {
		System.out.println("Error reading URI: " + e.getMessage());
	} catch (SAXException e) {
		System.out.println("Error in parsing: " + e.getMessage());
	} catch (ParserConfigurationException e) {
		System.out.println("Error in XML parser configuration: "
				+ e.getMessage());
	}
	    }
	public static String getNeoSyntax(String input) {
		String value = input;
		if(input.contains("\"")) {
			value = input.replace("\"", "\\\"");
		}
		
		if(input.contains("\'")) {
			value = input.replace("\'", "\\\' ");
		}
		if(input.contains("\\")) {
			value = input.replace("\\", "\\\\");
		}
		return value;
	}
	
	public static void main(String[] args) throws SQLException, ClassNotFoundException{
		Long start = System.currentTimeMillis();
		System.out.println(start);
		Parser p = new Parser("C:/Users/17520/Documents/GitHub/DBLP-PARSER/dblp.xml", NEO4J);
		Long end = System.currentTimeMillis();
		System.out.println("Used:" + (end - start) / 1000 + "second");
		
	}
}
