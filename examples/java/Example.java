  import java.util.Properties;
  import java.sql.*;
  
  public class Example {
  	
	static final String HOST = "52.10.201.42";
    static final String DATABASE = "leo";
    static final String USER = "ubuntu";
    
    public static void main(String[] args) throws Exception {
  
      // Connect to "test" database on port 6543
      String url = "jdbc:postgresql://" + HOST + ":6543/" + DATABASE;
      ResultSet  rs;
      Properties props = new Properties();
  
      props.setProperty("user", USER);
      Connection conn = DriverManager.getConnection(url, props);
  
      Statement stmt = conn.createStatement();
      stmt.executeUpdate(
        "CREATE CONTINUOUS VIEW v AS SELECT x::integer, COUNT(*) FROM stream GROUP BY x");
  
      // ACTIVATE our CONTINUOUS VIEW so that it starts reading events from stream
      stmt.executeUpdate("ACTIVATE");
  
      for (int i=0; i<100000; i++)
      {
        // 10 unique groupings
        int x = i % 10;
  
        // INSERT INTO stream (x) VALUES (x)
        stmt.addBatch("INSERT INTO stream (x) VALUES (" + Integer.toString(x) + ")");
      }
      
      stmt.executeBatch();
  
      // DEACTIVATE our CONTINUOUS VIEW
      stmt.executeUpdate("DEACTIVATE");
  
      rs = stmt.executeQuery("SELECT * FROM v");
      while (rs.next())
      {
        int id = rs.getInt("x");
        int count = rs.getInt("count");
  
        System.out.println(id + " = " + count);
      }
  
      // Clean up our CONTINUOUS VIEW
      stmt.executeUpdate("DROP CONTINUOUS VIEW v");
    }
  }
