.. _clients:

Clients
============

Since PipelineDB runs as an extension of PostgreSQL 10.1+ and 11.0+, it doesn't have its own client libraries. Instead, any client that works with PostgreSQL (or any SQL database for that matter) will work with PipelineDB.

Here you'll find examples of a simple PipelineDB application written in a few different languages and clients. The application simply creates the continuous view:

.. code-block:: sql

  CREATE VIEW continuous view WITH (action=materialize) AS
	SELECT x::integer, COUNT(*) FROM stream GROUP BY x;

The application then emits :code:`100,000` events resulting in :code:`10` unique groupings for the continuous view, and prints out the results.

Python
----------------

For this example in Python, you'll need to have psycopg2_ installed.

.. _psycopg2: http://initd.org/psycopg/docs/install.html

.. code-block:: python

  import psycopg2

  conn = psycopg2.connect('dbname=test user=user host=localhost port=5432')
  pipeline = conn.cursor()

  create_stream = """
  CREATE FOREIGN TABLE stream (x integer) SERVER pipelinedb
  """
  pipeline.execute(create_stream)

  create_cv = """
  CREATE VIEW continuous_view WITH (action=materialize) AS SELECT x::integer, COUNT(*) FROM stream GROUP BY x
  """
  pipeline.execute(create_cv)
  conn.commit()

  rows = []

  for n in range(100000):
      # 10 unique groupings
      x = n % 10
      rows.append({'x': x})

  # Now write the rows to the stream
  pipeline.executemany('INSERT INTO stream (x) VALUES (%(x)s)', rows)

  # Now read the results
  pipeline.execute('SELECT * FROM continuous_view')
  rows = pipeline.fetchall()

  for row in rows:
      x, count = row

      print x, count

  pipeline.execute('DROP VIEW continuous_view')
  pipeline.close()

----------------------

Ruby
----------------

This example in Ruby uses the pg_ gem.

.. _pg: https://rubygems.org/gems/pg/versions/0.18.2

.. code-block:: ruby

	require 'pg'
	pipeline = PGconn.connect("dbname='test' user='user' host='localhost' port=5432")

	# This continuous view will perform 3 aggregations on page view traffic, grouped by url:
	#
	# total_count - count the number of total page views for each url
	# uniques     - count the number of unique users for each url
	# p99_latency - determine the 99th-percentile latency for each url

	s = "
	CREATE FOREIGN TABLE page_views (
		url text,
		cookie text,
		latency integer
	) SERVER pipelinedb"
	pipeline.exec(s)

	q = "
	CREATE VIEW v WITH (action=materialize) AS
	SELECT
	  url,
	  count(*) AS total_count,
	  count(DISTINCT cookie) AS uniques,
	  percentile_cont(0.99) WITHIN GROUP (ORDER BY latency) AS p99_latency
	FROM page_views GROUP BY url"

	pipeline.exec(q)

	for n in 1..10000 do
	  # 10 unique urls
	  url = '/some/url/%d' % (n % 10)

	  # 1000 unique cookies
	  cookie = '%032d' % (n % 1000)

	  # latency uniformly distributed between 1 and 100
	  latency = rand(101)

	  # NOTE: it would be much faster to batch these into a single INSERT
	  # statement, but for simplicity's sake let's do one at a time
	  pipeline.exec(
	  "INSERT INTO page_views (url, cookie, latency) VALUES ('%s', '%s', %d)"
		% [url, cookie, latency])
	end

	# The output of a continuous view can be queried like any other table or view
	rows = pipeline.exec('SELECT * FROM v ORDER BY url')

	rows.each do |row|
	  puts row
	end

	# Clean up
	pipeline.exec('DROP VIEW v')


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

      // Connect to "test" database on port 5432
      String url = "jdbc:postgresql://" + HOST + ":5432/" + DATABASE;
      ResultSet  rs;
      Properties props = new Properties();

      props.setProperty("user", USER);
      Connection conn = DriverManager.getConnection(url, props);

      Statement stmt = conn.createStatement();
      stmt.executeUpdate(
        "CREATE FOREIGN TABLE stream (x integer) SERVER pipelinedb");
      stmt.executeUpdate(
        "CREATE VIEW v WITH (action=materialize) AS SELECT x::integer, COUNT(*) FROM stream GROUP BY x");

      for (int i=0; i<100000; i++)
      {
        // 10 unique groupings
        int x = i % 10;

        // INSERT INTO stream (x) VALUES (x)
        stmt.addBatch("INSERT INTO stream (x) VALUES (" + Integer.toString(x) + ")");
      }

      stmt.executeBatch();

      rs = stmt.executeQuery("SELECT * FROM v");
      while (rs.next())
      {
        int id = rs.getInt("x");
        int count = rs.getInt("count");

        System.out.println(id + " = " + count);
      }

      // Clean up
      stmt.executeUpdate("DROP VIEW v");
      conn.close();
    }
  }
