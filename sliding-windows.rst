.. _sliding-windows:

Sliding Windows
============================

Since :ref:`continuous-views` are continuously and incrementally updated over time, PipelineDB has the capability to consider the current time when updating the result of a continuous view. Queries that include a :code:`WHERE` clause with a temporal component relating to the **current time** are called **sliding-window queries**. The set of events that a sliding :code:`WHERE` clause filters or accepts perpetually changes over time.

There are two important components of a sliding :code:`WHERE` clause:

**clock_timestamp ( )**

	A built-in function that always returns the current timestamp.

**arrival_timestamp**

	A special attribute of all incoming events containing the time at which PipelineDB received them, as described in :ref:`arrival-ordering`.

However, it is not necessary to explicitly add a :code:`WHERE` clause referencing these values. PipelineDB does this internally and it is only necessary to specify
the :code:`max_age` storage parameter in a continuous view's definition.

These concepts are probably best illustrated by an example.


Examples
------------

Even though sliding windows are a new concept for a SQL database, PipelineDB does not use any sort of new or proprietary windowing syntax. Instead, PipelineDB uses standard PostgreSQL 9.5 syntax. Here's a simple example:

**What users have I seen in the last minute?**

.. code-block:: pipeline

	CREATE CONTINUOUS VIEW recent_users WITH (max_age = '1 minute') AS
	   SELECT user_id::integer FROM stream;

Internally, PipelineDB will rewrite this query to the following:

.. code-block:: pipeline

  CREATE CONTINUOUS VIEW recent_users AS
     SELECT user_id::integer FROM stream
  WHERE (arrival_timestamp > clock_timestamp() - interval '1 minute');

.. note:: PipelineDB allows users to manually construct a sliding window :code:`WHERE` clause when defining sliding-window continuous views, although it is recommended that :code:`max_age` be used in order to avoid tedium. 

The result of a :code:`SELECT` on this continuous view would only contain the specific users seen within the last minute. That is, repeated :code:`SELECT` s would contain different rows, even if the continuous view wasn't explicitly updated.

Let's break down what's going on with the :code:`(arrival_timestamp > clock_timestamp() - interval '1 minute')` predicate.

Each time :code:`clock_timestamp() - interval '1 minute'` is evaluated, it will return a timestamp corresponding to 1 minute in the past. Adding in :code:`arrival_timestamp` and :code:`>` means that this predicate will evaluate to :code:`true` if the :code:`arrival_timestamp` for a given event is greater than 1 minute in the past. Since the predicate is evaluated every time a new event is read, this effectively gives us a sliding window that is 1 minute width.

.. note:: PipelineDB exposes the :code:`current_date`, :code:`current_time`, and :code:`current_timestamp` values to use within queries, but by design these don't work with sliding-window queries because they remain constant within a transaction and thus don't necessarily represent the current moment in time.


Sliding Aggregates
-------------------

Sliding-window queries also work with aggregate functions. Sliding aggregates work by aggregating their inputs as much as possible, but without losing the granularity needed to know how to remove information from the window as time progresses. This partial aggregatation is all transparent to the user--only fully aggregated results will be visible within sliding-window aggregates.

Let's look at a few examples:

**How many users have I seen in the last minute?**

.. code-block:: pipeline

	CREATE CONTINUOUS VIEW count_recent_users WITH (max_age = '1 minute') AS
	   SELECT COUNT(*) FROM stream;

Each time a :code:`SELECT` is run on this continuous view, the count it returns will be the count of only the events seen within the last minute. For example, if events stopped coming in, the count would decrease each time a :code:`SELECT` was run on the continuous view. This behavior works for all of the :ref:`aggregates` that PipelineDB supports:

**What is the 5-minute moving average temperature of my sensors?**

.. code-block:: pipeline

	CREATE CONTINUOUS VIEW sensor_temps WITH (max_age = '5 minutes') AS
	   SELECT sensor::integer, AVG(temp::numeric) FROM sensor_stream
	GROUP BY sensor;

**How many unique users have we seen over the last 30 days?**

.. code-block:: pipeline

	CREATE CONTINUOUS VIEW uniques WITH (max_age = '30 days') AS
	   SELECT COUNT(DISTINCT user::integer) FROM user_stream;

**What is my server's 99th precentile response latency over the last 5 minutes?**

.. code-block:: pipeline

	CREATE CONTINUOUS VIEW latency WITH (max_age = '5 minutes') AS
	   SELECT server_id::integer, percentile_cont(0.99)
	   WITHIN GROUP (ORDER BY latency::numeric) FROM server_stream
	GROUP BY server_id;

Temporal Invalidation
-----------------------

Obviously, sliding-window rows in continuous views become invalid after a certain amount of time because they've become too old to ever be included in a continuous view's result. Such rows must thus be **garbage collected**, which can happen in two ways:


**Background invalidation**

	A background process similar to PostgreSQL's autovacuumer_ periodically runs and physically removes any expired rows from sliding-window continuous views.

.. _autovacuumer: http://www.postgresql.org/docs/current/static/runtime-config-autovacuum.html

**Read-time invalidation**

	When a continuous view is read with a :code:`SELECT`, any data that are too old to be included in the result are discarded on the fly while generating the result. This ensures that even if invalid rows still exist, they aren't actually included in any query results.

-----------------------


Multiple Windows
-------------------------------

It is relatively common to have a need for multiple sliding-windows for the same query. For example, keeping track of user event counts for the last
5 minutes, 10 minutes, one day, etc. For this reason, PipelineDB supports the creation of regular views over a single sliding window continuous view,
which ultimately saves resources because only a single continuous view is actually being updated internally.

For example, to maintain counts over three different window sizes:

.. code-block:: pipeline

  CREATE CONTINUOUS VIEW sw0 WITH (max_age = '1 hour') AS SELECT COUNT(*) FROM event_stream;
  CREATE VIEW sw1 WITH (max_age = '5 minutes') AS SELECT * FROM sw0;
  CREATE VIEW sw2 WITH (max_age = '10 minutes') AS SELECT * FROM sw0;

Note that :code:`sw1` and :code:`sw2` are not defined using the :code:`CONTINUOUS` keyword. However, querying them will only return rows that are
within their own respective windows.

step_factor
-------------------------

Internally, the materialization tables backing sliding-window queries are aggregated as much as possible. However, rows can't be aggregated down to the same level of granularity as the query's final output because data must be removed from aggregate results when it goes out of window.

For example, a sliding-window query that aggregates by hour may actually have minute-level aggregate data on disk so that only the last 60 minutes are included in the final aggregate result returned to readers. These internal, more granular aggregate levels for sliding-window queries are called "steps". An "overlay" view is placed over these step aggregates in order to perform the final aggregation at read time.

You have probably noticed at this point that step aggregates can be a significant factor in determining sliding-window query read performance, because each final sliding-window aggregate group will internally be composed of a number of steps. The number of steps that each sliding-window aggregate group will have is tunable via the **step_factor** parameter:

**step_factor**

  An integer between 1 and 50 that specifices the size of a sliding-window step as a percentage of window size given by **max_age**. A smaller **step_factor** will provide more granularity in terms of when data goes out of window, at the cost of larger on-disk materialization table size. A larger **step_factor** will reduce on-disk materialization table size at the expense of less out-of-window granularity.

Here's an example of using **step_factor** in conjunction with **max_age** to aggregate over an hour with a step size of 30 minutes:


.. code-block:: pipeline

  CREATE CONTINUOUS VIEW hourly (WITH max_age = '1 hour', step_factor = 50)
    AS SELECT COUNT(*) FROM stream;

-----------------------------

Now that you know how sliding-window queries work, it's probably a good time to learn about :ref:`joins`.
