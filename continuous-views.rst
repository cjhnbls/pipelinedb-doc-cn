.. _continuous-views:

Continuous views
=================

PipelineDB's fundamental abstraction is called a :code:`CONTINUOUS VIEW`. A :code:`CONTINUOUS VIEW` is much like a regular view, except that it selects from a combination of streams and tables as its inputs and is incrementally updated in realtime as new data is written to those inputs.

As soon as a stream row has been read by the :code:`CONTINUOUS VIEW` s that must read it, it is discarded. It is not stored anywhere. The only data that is persisted for a :code:`CONTINUOUS VIEW` is whatever is returned by running a :code:`SELECT * FROM that_view`. Thus you can think of a :code:`CONTINUOUS VIEW` as a very high-throughput, realtime materialized view.

CREATE CONTINUOUS VIEW
---------------------------

Here's the syntax for creating a :code:`CONTINUOUS VIEW`:

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

.. note:: You may want to visit :ref:`streams` to understand PipelineDB's stream abstraction if you haven't already, although it's not critical.

**expression**
  A PostgreSQL expression_

.. _expression: http://www.postgresql.org/docs/9.4/static/sql-expressions.html

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

.. note:: PipelineDB's **window_definition's** do not support an :code:`ORDER BY` clause if the input rows come from a stream. In such cases, the stream row's :code:`arrival_timestamp` field is implictly used as the :code:`ORDER BY` clause.

**frame_clause**
  Defines the window frame for window functions that depend on the frame (not all do). The window frame is a set of related rows for each row of the query (called the current row). The **frame_clause** can be one of

  .. code-block:: pipeline

    [ RANGE | ROWS ] frame_start
    [ RANGE | ROWS ] BETWEEN frame_start AND frame_end

**frame_start**, **frame_end**

  .. code-block:: pipeline

    UNBOUNDED PRECEDING
    value PRECEDING
    CURRENT ROW
    value FOLLOWING
    UNBOUNDED FOLLOWING

**value**
  An integral value

.. note:: This has mainly covered only the syntax for :code:`CREATE CONTINUOUS VIEW`. To learn more about the semantics of each of these query elements, you should consult the `PostgreSQL SELECT documentation`_.

.. _PostgreSQL SELECT documentation: http://www.postgresql.org/docs/9.4/static/sql-select.html

DROP CONTINUOUS VIEW
---------------------------

To :code:`DROP` a :code:`CONTINUOUS VIEW` from the system, use the :code:`DROP CONTINUOUS VIEW` command. Its syntax is simple:

.. code-block:: pipeline

	DROP CONTINUOUS VIEW name

This will remove the :code:`CONTINUOUS VIEW` from the system along with all of its associated resources.


.. _pipeline-query:

Viewing continuous views
---------------------------

To view the :code:`CONTINUOUS VIEW` s currently in the system, you can run a :code:`SELECT` on the :code:`pipeline_query` catalog table:

.. code-block:: pipeline

	SELECT * FROM pipeline_query;

Don't worry about all of the columns in :code:`pipeline_query` --most of them are only for internal use. The important columns are :code:`name`, which contains the name you gave the :code:`CONTINUOUS VIEW` when you created it; and :code:`query`, which contains the :code:`CONTINUOUS VIEW`'s query definition.

Inferred schemas
--------------------

Since streams and their columns appear in a :code:`CONTINUOUS VIEW` 's :code:`FROM` clause, it seems natural that they would have to have a schema already declared, just like selecting from a table. But with PipelineDB, it is strictly unnecessary to ever explicitly define any sort of schema for a stream. All of the type information necessary for a :code:`CONTINUOUS VIEW` to read from a stream is acquired by what is known as an **inferred schema**. Perhaps this is best illustrated by a simple example.

Consider the following simple :code:`CONTINUOUS VIEW`:

.. code-block:: pipeline

  CREATE CONTINUOUS VIEW inferred AS
  SELECT user_id::integer, COUNT(*), SUM(value::float8), AVG(value) FROM stream
  GROUP BY user_id

PipelineDB uses PostgreSQL's :code:`::` casting syntax to tell the :code:`CONTINUOUS VIEW` what types to convert raw values to. Note that a stream column must only be typed a single time. All other references to it will use the same type.

.. note:: All stream columns must be explicitly appear in the :code:`CONTINUOUS VIEW` 's definition. It is not possible to :code:`SELECT * FROM a_stream`.

Data retrieval
-------------------

Since :code:`CONTINUOUS VIEW` s are a lot like regular views, retrieving data from them is simply a matter of performing a :code:`SELECT` on them:

.. code-block:: pipeline

  SELECT * FROM some_continuous_view

========  ===========
  user    event_count
========  ===========
a         10
b         20
c         30
========  ===========

Any :code:`SELECT` statement is valid on a :code:`CONTINUOUS VIEW`, allowing you to perform further analysis on their perpetually updating contents:

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

Examples
---------------------

Putting this all together, let's go through a few examples of :code:`CONTINUOUS VIEW` s and understand what each one accomplishes.

.. important:: It is important to understand that the only data persisted by PipelineDB for a :code:`CONTINUOUS VIEW` is whatever would be returned by running a :code:`SELECT *` on it (plus a small amount of metadata). This is a relatively new concept, but it is at the core of what makes :code:`CONTINUOUS VIEW` s so powerful!

Emphasizing the above notice, this :code:`CONTINUOUS VIEW` would only ever store a single row in PipelineDB (just a few bytes), even if it read a trillion events over time:

.. code-block:: pipeline

  CREATE CONTINUOUS VIEW avg_of_forever AS SELECT AVG(x::integer) FROM one_trillion_events_stream


**Calculate the number of unique users seen per url referrer each day using only a constant amount of space per day:**

.. code-block:: pipeline

  CREATE CONTINUOUS VIEW uniques AS
  SELECT date_trunc('day', arrival_timestamp) AS day,
    referrer::text, COUNT(DISTINCT user_id::integer)
  FROM users_stream GROUP BY day, referrer;

**Compute the linear regression of a stream of datapoints bucketed by minute:**

.. code-block:: pipeline

  CREATE CONTINUOUS VIEW lreg AS
  SELECT date_trunc('minute', arrival_timestamp) AS minute,
    regr_slope(y::integer, x::integer) AS mx,
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
  SELECT percentile_cont(array[90, 95, 99]) WITHIN GROUP (ORDER BY latency::integer)
  FROM latency_stream;

**How many of my sensors have ever been within 1000 meters of San Francisco?**

.. code-block:: pipeline

  -- PipelineDB ships natively with geospatial support
  CREATE CONTINUOUS VIEW sf_proximity_count AS
  SELECT COUNT(DISTINCT sensor_id::integer)
  FROM geo_stream WHERE ST_DWithin(

    -- Approximate SF coordinates
    ST_GeographyFromText('SRID=4326;POINT(37 -122)')::geometry,

    sensor_coords::geometry, 1000);

----------

We hope you enjoyed learning all about :code:`CONTINUOUS VIEW` s. Next, you should probably check out how :ref:`streams` work.
