.. _continuous-views:

Continuous Views
=================

PipelineDB's fundamental abstraction is called a continuous view. A continuous view is much like a regular view, except that it selects from a combination of streams and tables as its inputs and is incrementally updated in realtime as new data is written to those inputs.

As soon as a stream row has been read by the continuous views that must read it, it is discarded. It is not stored anywhere. The only data that is persisted for a continuous view is whatever is returned by running a :code:`SELECT * FROM that_view`. Thus you can think of a continuous view as a very high-throughput, realtime materialized view.

CREATE CONTINUOUS VIEW
---------------------------

Here's the syntax for creating a continuous view:

.. code-block:: pipeline

	CREATE CONTINUOUS VIEW name AS query

where **query** is a subset of a PostgreSQL :code:`SELECT` statement:

.. code-block:: pipeline

  SELECT [ DISTINCT [ ON ( expression [, ...] ) ] ]
      expression [ [ AS ] output_name ] [, ...]
      [ FROM from_item [, ...] ]
      [ WHERE condition ]
      [ GROUP BY expression [, ...] ]
      [ HAVING condition [, ...] ]
      [ WINDOW window_name AS ( window_definition ) [, ...] ]

  where from_item can be one of:

      stream_name [ [ AS ] alias [ ( column_alias [, ...] ) ] ]
      table_name [ [ AS ] alias [ ( column_alias [, ...] ) ] ]
      from_item [ NATURAL ] join_type from_item [ ON join_condition ]

.. note:: This section references streams, which are similar to tables and are what continuous views read from in their :code:`FROM` clause. They're explained in more depth in the :ref:`streams` section, but you can think of them as append-only tables for now.

**expression**

  A PostgreSQL expression_

.. _expression: http://www.postgresql.org/docs/current/static/sql-expressions.html

**output_name**

  An optional identifier to name an expression with

**condition**

  Any expression that evaluates to a result of type :code:`boolean`. Any row that does not satisfy this condition will be eliminated from the output. A row satisfies the condition if it returns :code:`true` when the actual row values are substituted for any variable references.


**window_name**

  A name that can be referenced from :code:`OVER` clauses or subsequent window definitions.

**window_definition**

  .. code-block:: pipeline

    [ existing_window_name ]
    [ PARTITION BY expression [, ...] ]
    [ ORDER BY expression ] [ NULLS { FIRST | LAST } ] [, ...] ]
    [ frame_clause ]

.. note:: PipelineDB's **window_definitions** do not support an :code:`ORDER BY` clause if the input rows come from a stream. In such cases, the stream row's :code:`arrival_timestamp` field is implictly used as the :code:`ORDER BY` clause.

**frame_clause**

  Defines the window frame for window functions that depend on the frame (not all do). The window frame is a set of related rows for each row of the query (called the current row). The **frame_clause** can be one of

  .. code-block:: pipeline

    [ RANGE | ROWS ] frame_start
    [ RANGE | ROWS ] BETWEEN frame_start AND frame_end

**frame_start**, **frame_end**

  Each can be one of the following:

  .. code-block:: pipeline

    UNBOUNDED PRECEDING
    value PRECEDING
    CURRENT ROW
    value FOLLOWING
    UNBOUNDED FOLLOWING

**value**

  An integral value

.. note:: This has mainly covered only the syntax for :code:`CREATE CONTINUOUS VIEW`. To learn more about the semantics of each of these query elements, you should consult the `PostgreSQL SELECT documentation`_.

.. _PostgreSQL SELECT documentation: http://www.postgresql.org/docs/current/static/sql-select.html

DROP CONTINUOUS VIEW
---------------------------

To :code:`DROP` a continuous view from the system, use the :code:`DROP CONTINUOUS VIEW` command. Its syntax is simple:

.. code-block:: pipeline

	DROP CONTINUOUS VIEW name

This will remove the continuous view from the system along with all of its associated resources.

TRUNCATE CONTINUOUS VIEW
---------------------------

To remove all of a continuous view's data without removing the continuous view itself, :code:`TRUNCATE CONTINUOUS VIEW` can be used:

.. code-block:: pipeline

	TRUNCATE CONTINUOUS VIEW name

This command will efficiently remove all of the continuous view's rows, and is therefore analagous to `PostgreSQL's TRUNCATE`_ command.

.. _`PostgreSQL's TRUNCATE`: http://www.postgresql.org/docs/current/static/sql-truncate.html

.. _pipeline-query:

Viewing Continuous Views
---------------------------

To view the continuous views currently in the system, you can run the following query:

.. code-block:: pipeline

	SELECT * FROM pipeline_views();

Don't worry about all of the columns returned--most of them are only for internal use. The important columns are :code:`name`, which contains the name you gave the continuous view when you created it; and :code:`query`, which contains the continuous view's query definition.

Data Retrieval
-------------------

Since continuous views are a lot like regular views, retrieving data from them is simply a matter of performing a :code:`SELECT` on them:

.. code-block:: pipeline

  SELECT * FROM some_continuous_view

========  ===========
  user    event_count
========  ===========
a         10
b         20
c         30
========  ===========

Any :code:`SELECT` statement is valid on a continuous view, allowing you to perform further analysis on their perpetually updating contents:

.. code-block:: pipeline

  SELECT t.name, sum(v.value) + sum(t.table_value) AS total
  FROM some_continuous_view v JOIN some_table t ON v.id = t.id GROUP BY t.name

========  ===========
  name      total
========  ===========
usman     10
jeff      20
derek     30
========  ===========

Time-to-Live (TTL) Expiration
---------------------------------

A very common PipelineDB pattern is to include a time-based column in aggregate groupings. Another related common pattern is removing old rows that are no longer needed, as determined by this time-based column. PipelineDB includes built-in per-row time-to-live (TTL) support to address these related patterns.

TTL expiration behavior can be assigned to continuous views via the :code:`ttl` and :code:`ttl_column` storage parameters. The autovacuumer will :code:`DELETE` any rows having a :code:`ttl_column` value that is older than the interval specified by :code:`ttl` (relative to wall time). 
Here's a version of the previous example that will tell the autovacuumer to delete any rows whose **minute** column is older than a month:

.. code-block:: pipeline

  CREATE CONTINUOUS VIEW v_ttl WITH (ttl = '1 month', ttl_column = 'minute') AS
    SELECT minute(arrival_timestamp), COUNT(*) FROM some_stream GROUP BY minute;

Note that TTL behavior is a hint to the autovacuumer, and thus will not guarantee that rows will be physically deleted exactly when they are expired. 

If you'd like to guarantee that no TTL-expired rows will be read, you should create a view over the continuous view with a :code:`WHERE` clause that excludes expired rows at read time. 

Activation and Deactivation
----------------------------

Because continuous views are continuously processing input streams, it is useful to have a notion of starting and stopping that processing without having to completely shutdown PipelineDB. For example, if a continuous view incurs an unexpected amount of system load or begins throwing errors, it may be useful to temporarily stop continuous processing until the issue is resolved.

This level of control is provided by the :code:`ACTIVATE` and :code:`DEACTIVATE` commands, which are synonymous with "play" and "pause". When continuous views are *active*, they are actively reading from their input streams and incrementally updating their results accordingly. Conversely, *inactive* continuous views are not reading from their input streams and are not updating their results. PipelineDB remains functional when continuous views are inactive, and continuous views themselves are still readable--they're just not updating.

The syntax for the :code:`ACTIVATE` and :code:`DEACTIVATE` commands is simple and takes no parameters:

.. code-block:: pipeline

	ACTIVATE | DEACTIVATE


.. important:: When continuous views are inactive, any events written to their input streams while they're inactive will never be read by that continuous view, even after they're activated again.

Examples
---------------------

Putting this all together, let's go through a few examples of continuous views and understand what each one accomplishes.

.. important:: It is important to understand that the only data persisted by PipelineDB for a continuous view is whatever would be returned by running a :code:`SELECT *` on it (plus a small amount of metadata). This is a relatively new concept, but it is at the core of what makes continuous views so powerful!

Emphasizing the above notice, this continuous view would only ever store a single row in PipelineDB (just a few bytes), even if it read a trillion events over time:

.. code-block:: pipeline

  CREATE CONTINUOUS VIEW avg_of_forever AS SELECT AVG(x) FROM one_trillion_events_stream


**Calculate the number of unique users seen per url referrer each day using only a constant amount of space per day:**

.. code-block:: pipeline

  CREATE CONTINUOUS VIEW uniques AS
  SELECT date_trunc('day', arrival_timestamp) AS day,
    referrer, COUNT(DISTINCT user_id)
  FROM users_stream GROUP BY day, referrer;

**Compute the linear regression of a stream of datapoints bucketed by minute:**

.. code-block:: pipeline

  CREATE CONTINUOUS VIEW lreg AS
  SELECT date_trunc('minute', arrival_timestamp) AS minute,
    regr_slope(y, x) AS mx,
    regr_intercept(y, x) AS b
  FROM datapoints_stream GROUP BY minute;

**How many ad impressions have we served in the last five minutes?**

.. code-block:: pipeline

  CREATE CONTINUOUS VIEW imps AS
  SELECT COUNT(*) FROM imps_stream
  WHERE (arrival_timestamp > clock_timestamp() - interval '5 minutes');

**What are the 90th, 95th, and 99th percentiles of my server's request latency?**

.. code-block:: pipeline

  CREATE CONTINUOUS VIEW latency AS
  SELECT percentile_cont(array[90, 95, 99]) WITHIN GROUP (ORDER BY latency)
  FROM latency_stream;

**How many of my sensors have ever been within 1000 meters of San Francisco?**

.. code-block:: pipeline

  -- PipelineDB ships natively with geospatial support
  CREATE CONTINUOUS VIEW sf_proximity_count AS
  SELECT COUNT(DISTINCT sensor_id)
  FROM geo_stream WHERE ST_DWithin(

    -- Approximate SF coordinates
    ST_GeographyFromText('SRID=4326;POINT(37 -122)'), sensor_coords, 1000);

----------

We hope you enjoyed learning all about continuous views. Next, you should probably check out how :ref:`streams` work.
