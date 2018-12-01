.. _continuous-views:

Continuous Views
=================

PipelineDB's fundamental abstraction is called a continuous view. A continuous view is much like a regular view, except that it selects from a combination of streams and tables as its inputs and is incrementally updated in realtime as new data is written to those inputs.

As soon as a stream row has been read by the continuous views that must read it, it is discarded. Raw, granular data is not stored anywhere. The only data that is persisted for a continuous view is whatever is returned by running a :code:`SELECT * FROM that_view`. Thus you can think of a continuous view as a very high-throughput, realtime materialized view.

Creating Continuous Views
---------------------------

Continuous views are defined as PostgreSQL views with the :code:`action` parameter set to :code:`materialize`. Here's the syntax for creating a continuous view:

.. code-block:: sql

	CREATE VIEW name [WITH (action=materialize [, ...])]  AS query

.. note:: The default :code:`action` is :code:`materialize`, and thus :code:`action` may be ommitted for creating continuous views. As long as a stream is being selected from, PipelineDB will interpret the :code:`CREATE VIEW` statement with an :code:`action` of :code:`materialize`.

where **query** is a subset of a PostgreSQL :code:`SELECT` statement:

.. code-block:: sql

  SELECT [ DISTINCT [ ON ( expression [, ...] ) ] ]
      expression [ [ AS ] output_name ] [, ...]
      [ FROM from_item [, ...] ]
      [ WHERE condition ]
      [ GROUP BY expression [, ...] ]

  where from_item can be one of:

      stream_name [ [ AS ] alias [ ( column_alias [, ...] ) ] ]
      table_name [ [ AS ] alias [ ( column_alias [, ...] ) ] ]
      from_item [ NATURAL ] join_type from_item [ ON join_condition ]

.. note:: This section references streams, which are similar to tables and are what continuous views and transforms read from in their :code:`FROM` clause. They're explained in more depth in the :ref:`streams` section, but you can think of them as append-only tables for now.

**expression**

  A PostgreSQL expression_ or `grouping sets specification`_.

.. _expression: https://www.postgresql.org/docs/current/static/sql-expressions.html
.. _grouping sets specification: https://www.postgresql.org/docs/current/static/queries-table-expressions.html#QUERIES-GROUPING-SETS

**output_name**

  An optional identifier to name an expression with

**condition**

  Any expression that evaluates to a result of type :code:`boolean`. Any row that does not satisfy this condition will be eliminated from the output. A row satisfies the condition if it returns :code:`true` when the actual row values are substituted for any variable references.

.. note:: This has mainly covered only the syntax for creating continuous views. To learn more about the semantics of each of these query elements, you should consult the `PostgreSQL SELECT documentation`_.

.. _PostgreSQL SELECT documentation: https://www.postgresql.org/docs/current/static/sql-select.html

Dropping Continuous Views
---------------------------

To :code:`DROP` a continuous view from the system, use the :code:`DROP VIEW` command. Its syntax is simple:

.. code-block:: sql

	DROP VIEW name

This will remove the continuous view from the system along with all of its associated resources.

Truncating Continuous Views
-----------------------------

To remove all of a continuous view's data without removing the continuous view itself, the :code:`truncate_continuous_view` function can be used:

.. code-block:: sql

    SELECT truncate_continuous_view('name');

This command will efficiently remove all of the continuous view's rows, and is therefore analagous to `PostgreSQL's TRUNCATE`_ command.

.. _`PostgreSQL's TRUNCATE`: https://www.postgresql.org/docs/current/static/sql-truncate.html

.. _pipeline-query:

Viewing Continuous Views
---------------------------

To view the continuous views and their definitions currently in the system, you can run the following query:

.. code-block:: sql

	SELECT * FROM pipelinedb.views;

Data Retrieval
-------------------

Since continuous views are a lot like regular views, retrieving data from them is simply a matter of performing a :code:`SELECT` on them:

.. code-block:: sql

  SELECT * FROM some_continuous_view

========  ===========
  user    event_count
========  ===========
a         10
b         20
c         30
========  ===========

Any :code:`SELECT` statement is valid on a continuous view, allowing you to perform further analysis on their perpetually updating contents:

.. code-block:: sql

  SELECT t.name, sum(v.value) + sum(t.table_value) AS total
  FROM some_continuous_view v JOIN some_table t ON v.id = t.id GROUP BY t.name

========  ===========
  name      total
========  ===========
usman     10
jeff      20
derek     30
========  ===========

.. _ttl-expiration:

Time-to-Live (TTL) Expiration
---------------------------------

A common PipelineDB pattern is to include a time-based column in aggregate groupings and removing old rows that are no longer needed, as determined by that column. While there are a number of ways to achieve this behavior, PipelineDB provides native support for row expiration via time-to-live (TTL) critera specified at the continuous view level.
 
TTL expiration behavior can be assigned to continuous views via the :code:`ttl` and :code:`ttl_column` storage parameters. Expiration is handled by one or more **"reaper"** processes that will :code:`DELETE` any rows having a :code:`ttl_column` value that is older than the interval specified by :code:`ttl` (relative to wall time). Here's an example of a continuous view definition that will tell the reaper to delete any rows whose **minute** column is older than one month:

.. code-block:: sql

  CREATE VIEW v_ttl WITH (ttl = '1 month', ttl_column = 'minute') AS
    SELECT minute(arrival_timestamp), COUNT(*) FROM some_stream GROUP BY minute;

Note that TTL behavior is a hint to the **reaper**, and thus will not guarantee that rows will be physically deleted exactly when they are expired. 

If you'd like to guarantee that no TTL-expired rows will be read, you should create a view over the continuous view with a :code:`WHERE` clause that excludes expired rows at read time. 

Modifying TTLs
----------------------------

TTLs can be added, modified, and removed from continuous views via the **pipelinedb.set_ttl** function:

**pipelinedb.set_ttl ( cv_name, ttl, ttl_column )**

	Update the given continuous view's TTL with the given paramters. **ttl** is an interval expressed as a string (e.g. :code:`'1 day'`), and **ttl_column** is the name of a timestamp-based column. 

	Passing :code:`NULL` for both the **ttl** and **ttl_column** parameters will effectively remove a TTL from the given continuous view. Note that a TTL cannot be modified on or removed from a sliding-window continuous view.


.. _activation-deactivation:

Activation and Deactivation
----------------------------

Because continuous views are continuously processing input streams, it can be useful to have a notion of starting and stopping that processing without having to completely shutdown PipelineDB. For example, if a continuous view incurs an unexpected amount of system load or begins throwing errors, it may be useful to temporarily stop continuous processing for that view (or all of them) until the issue is resolved.

This level of control is provided by the :code:`activate` and :code:`deactivate` functions, which are synonymous with "play" and "pause". When continuous views are *active*, they are actively reading from their input streams and incrementally updating their results accordingly. Conversely, *inactive* continuous views are not reading from their input streams and are not updating their results. PipelineDB remains functional when continuous views are inactive, and continuous views themselves are still readable--they're just not updating.

The function signatures take only a continuous view or transform name:

.. code-block:: sql

	SELECT pipelinedb.activate('continuous_view_or_transform');
	SELECT pipelinedb.deactivate('continuous_view_or_transform');

:ref:`continuous-transforms` can also be activated and deactivated.

.. important:: When continuous queries (views or transforms) are inactive, any events written to their input streams while they're inactive will never be read by that continuous query, even after they're activated again.

See :ref:`operations` for more information.

Examples
---------------------

Putting this all together, let's go through a few examples of continuous views and understand what each one accomplishes.

.. important:: It is important to understand that the only data persisted by PipelineDB for a continuous view is whatever would be returned by running a :code:`SELECT * FROM my_cv` on it (plus a small amount of metadata). This is a relatively new concept, but it is at the core of what makes continuous views so powerful!

Emphasizing the above notice, this continuous view would only ever store a single row in PipelineDB (just a few bytes), even if it read a trillion events over time:

.. code-block:: sql

  CREATE VIEW avg_of_forever AS SELECT AVG(x) FROM one_trillion_events_stream;


**Calculate the number of unique users seen per url referrer each day using only a constant amount of space per day:**

.. code-block:: sql

  CREATE VIEW uniques AS
  SELECT date_trunc('day', arrival_timestamp) AS day,
    referrer, COUNT(DISTINCT user_id)
  FROM users_stream GROUP BY day, referrer;

**Compute the linear regression of a stream of datapoints bucketed by minute:**

.. code-block:: sql

  CREATE VIEW lreg AS
  SELECT date_trunc('minute', arrival_timestamp) AS minute,
    regr_slope(y, x) AS mx,
    regr_intercept(y, x) AS b
  FROM datapoints_stream GROUP BY minute;

**How many ad impressions have we served in the last five minutes?**

.. code-block:: sql

  CREATE VIEW imps AS
    SELECT COUNT(*) FROM imps_stream
  WHERE (arrival_timestamp > clock_timestamp() - interval '5 minutes');

**What are the 90th, 95th, and 99th percentiles of my server's request latency?**

.. code-block:: sql

  CREATE VIEW latency AS
    SELECT percentile_cont(array[90, 95, 99]) WITHIN GROUP (ORDER BY latency)
  FROM latency_stream;

----------

We hope you enjoyed learning all about continuous views! Next, you should probably check out how :ref:`streams` work.
