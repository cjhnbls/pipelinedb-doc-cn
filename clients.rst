.. _clients:

Clients
============

Since PipelineDB is compatible with PostgreSQL 9.4, it doesn't have its own client libraries. Instead, any client that works with PostgreSQL (or any SQL database for that matter) will work with PipelineDB.

Here you'll find examples of a simple PipelineDB application written in a few different languages and clients. The application simply creates the :code:`CONTINUOUS VIEW`:

.. code-block:: sql

  CREATE CONTINUOUS VIEW continuous view AS SELECT x::integer, COUNT(*) FROM stream GROUP BY x;

The application then emits :code:`100,000` events resulting in :code:`10` unique groupings for the :code:`CONTINUOUS VIEW`, and prints out the results.

Python
----------------

For this example in Python, you'll need to have psycopg2_ installed.

.. _psycopg2: http://initd.org/psycopg/docs/install.html

.. code-block:: python

  import psycopg2

  conn = psycopg2.connect('dbname=test user=user host=localhost port=6543')
  pipeline = conn.cursor()

  create_cv = """
  CREATE CONTINUOUS VIEW continuous_view AS SELECT x::integer, COUNT(*) FROM stream GROUP BY x
  """
  pipeline.execute(create_cv)
  conn.commit()

  # The CONTINUOUS VIEW is now reading from its input stream
  pipeline.execute('ACTIVATE continuous_view')

  rows = []

  for n in range(100000):
      # 10 unique groupings
      x = n % 10
      rows.append({'x': x})

  # Now write the rows to the stream
  pipeline.executemany('INSERT INTO stream (x) VALUES (%(x)s)', rows)

  # Stop the CONTINUOUS VIEW
  pipeline.execute('DEACTIVATE continuous_view')

  # Now read the results
  pipeline.execute('SELECT * FROM continuous_view')
  rows = pipeline.fetchall()

  for row in rows:
      x, count = row

      print x, count

  pipeline.execute('DROP CONTINUOUS VIEW continuous_view')
  pipeline.close()

----------------------

Java
----------------

For this example you'll need to have JDBC_ installed and on your :code:`CLASSPATH`.

..  _JDBC: http://docs.oracle.com/javase/tutorial/jdbc/basics/gettingstarted.html

.. code-block:: java

  import java.util.Properties;
  import java.sql.*;

  public class Example {

    static final String HOST = "localhost";
    static final String DATABASE = "test";
    static final String USER = "user";

    public static void main(String[] args) throws SQLException {

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

      // Clean up
      stmt.executeUpdate("DROP CONTINUOUS VIEW v");
      conn.close();
    }
  }
