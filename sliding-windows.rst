.. _sliding-windows:

Sliding Windows
============================

Since :ref:`continuous-views` are continuously and incrementally updated over time, PipelineDB has the capability to consider the current time when updating the result of a continuous view. Queries that include a :code:`WHERE` clause with a temporal component relating to the **current time** are called **sliding-window queries**. The set of events that a sliding :code:`WHERE` clause filters or accepts perpetually changes over time.

There are two important components of a sliding :code:`WHERE` clause:

**clock_timestamp ( )**

	A built-in function that always returns the current timestamp.

**arrival_timestamp**

	A special attribute of all incoming events containing the time at which PipelineDB received them, as described in :ref:`arrival-ordering`.

These concepts are probably best illustrated by an example.


Examples
------------

Even though sliding windows are a new concept for a SQL database, PipelineDB does not use any sort of new or proprietary windowing syntax. Instead, PipelineDB uses standard PostgreSQL 9.4 syntax. Here's a simple example:

**What users have I seen in the last minute?**

.. code-block:: sql

	CREATE CONTINUOUS VIEW recent_users AS SELECT user_id::integer FROM stream
	WHERE (arrival_timestamp > clock_timestamp() - interval '1 minute');

The result of a :code:`SELECT` on this continuous view would only contain the specific users seen within the last minute. That is, repeated :code:`SELECT` s would contain different rows, even if the continuous view wasn't explicitly updated.

Let's break down what's going on with the :code:`(arrival_timestamp > clock_timestamp() - interval '1 minute')` predicate.

Each time :code:`clock_timestamp() - interval '1 minute'` is evaluated, it will return a timestamp corresponding to 1 minute in the past. Adding in :code:`arrival_timestamp` and :code:`>` means that this predicate will evaluate to :code:`true` if the :code:`arrival_timestamp` for a given event is greater than 1 minute in the past. Since the predicate is evaluated every time a new event is read, this effectively gives us a sliding window that is 1 minute width.

.. note:: PipelineDB exposes the :code:`current_date`, :code:`current_time`, and :code:`current_timestamp` values to use within queries, but by design these don't work with sliding-window queries because they remain constant within a transaction and thus don't necessarily represent the current moment in time.


Sliding Aggregates
-------------------

Sliding-window queries also work with aggregate functions. Sliding aggregates work by aggregating their inputs as much as possible, but without losing the granularity needed to know how to remove information from the window as time progresses. This partial aggregatation is all transparent to the user--only fully aggregated results will be visible within sliding-window aggregates.

Let's look at a few examples:

**How many users have I seen in the last minute?**

.. code-block:: sql

	CREATE CONTINUOUS VIEW count_recent_users AS SELECT COUNT(*) FROM stream
	WHERE (arrival_timestamp > clock_timestamp() - interval '1 minute');


Each time a :code:`SELECT` is run on this continuous view, the count it returns will be the count of only the events seen within the last minute. For example, if events stopped coming in, the count would decrease each time a :code:`SELECT` was run on the continuous view. This behavior works for all of the :ref:`aggregates` that PipelineDB supports:

**What is the 5-minute moving average tempurature of my sensors?**

.. code-block:: sql

	CREATE CONTINUOUS VIEW sensor_temps AS SELECT sensor::integer, AVG(temp::numeric)
	FROM sensor_stream
	WHERE (arrival_timestamp > clock_timestamp() - interval '5 minutes')
	GROUP BY sensor;

**How many unique users have we seen over the last 30 days?**

.. code-block:: sql

	CREATE CONTINUOUS VIEW uniques AS SELECT COUNT(DISTINCT user::integer)
	FROM user_stream
	WHERE (arrival_timestamp > clock_timestamp() - interval '30 days');

**What is my server's 99th precentile response latency over the last 5 minutes?**

.. code-block:: sql

	CREATE CONTINUOUS VIEW latency AS SELECT server_id::integer, percentile_cont(0.99)
	WITHIN GROUP (ORDER BY latency::numeric) FROM server_stream
	WHERE (arrival_timestamp > clock_timestamp() - interval '5 minutes')
	GROUP BY server_id;

Temporal Invalidation
-----------------------

Obviously, sliding-window rows in continuous views become invalid after a certain amount of time because they've become too old to ever be included in a continuous view's result. Such rows must thus be **garbage collected**, which can happen in two ways:


**Background invalidation**

	A background process similar to PostgreSQL's autovacuumer_ periodically runs and physically removes any expired rows from sliding-window continuous views.

.. _autovacuumer: http://www.postgresql.org/docs/9.4/static/runtime-config-autovacuum.html

**Read-time invalidation**

	When a continuous view is read with a :code:`SELECT`, any data that are too old to be included in the result are discarded on the fly while generating the result. This ensures that even if invalid rows still exist, they aren't actually included in any query results.

-----------------------

Now that you know how sliding-window queries work, it's probably a good time to learn about :ref:`joins`.
