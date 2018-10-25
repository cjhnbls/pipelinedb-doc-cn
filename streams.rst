.. _streams:

Streams
=================

Streams are the abstraction that allows clients to push time-series data through :ref:`continuous-views`. A stream row, or simply **event**, looks exactly like a regular table row, and the interface for writing data to streams is identical to the one for writing to tables. However, the semantics of streams are fundamentally different from tables.

Namely, events only "exist" within a stream until they are consumed by all of the :ref:`continuous-views` that are reading from that stream. Even then, it is still not possible for users to :code:`SELECT` from streams. Streams serve exclusively as inputs to :ref:`continuous-views`.

Streams are represented in PipelineDB as `foreign tables`_ managed by the :code:`pipelinedb` `foreign server`_. The syntax for creating a foreign table is similar to that of creating a regular PostgreSQL table:

.. _`foreign tables`: https://www.postgresql.org/docs/current/static/sql-createforeigntable.html
.. _`foreign server`: https://www.postgresql.org/docs/current/static/sql-createserver.html

.. code-block:: sql

	CREATE FOREIGN TABLE stream_name ( [
	   { column_name data_type [ COLLATE collation ] } [, ... ]
	] )
	SERVER pipelinedb;


**stream_name**

  The name of the stream to be created.

**column_name**

  The name of a column to be created in the new table.

**data_type**

  The data type of the column. This can include array specifiers. For more information on the data types supported by PipelineDB, see :ref:`builtin` and the `PostgreSQL supported types`_ .

.. _PostgreSQL supported types: https://www.postgresql.org/docs/current/static/datatype.html

**COLLATE collation**

  The :code:`COLLATE` clause assigns a collation to the column (which must be of a collatable data type). If not specified, the column data type's default collation is used.

Columns can be added to streams using :code:`ALTER STREAM`:

.. code-block:: psql

  postgres=# ALTER FOREIGN TABLE stream ADD COLUMN x integer;
  ALTER FOREIGN TABLE

.. note:: Columns cannot be dropped from streams.

Streams can be dropped with the :code:`DROP FOREIGN TABLE` command. Below is an example of creating a simple continuous view that reads from a stream.

.. code-block:: psql

  postgres=# CREATE FOREIGN TABLE stream (x integer, y integer) SERVER pipelinedb;
  CREATE FOREIGN TABLE
  postgres=# CREATE VIEW v AS SELECT sum(x + y) FROM stream;
  CREATE VIEW

Writing To Streams
----------------------

=========
INSERT
=========

Stream writes are just regular PostgreSQL :code:`INSERT` statements. Here's the syntax:

.. code-block:: sql

  INSERT INTO stream_name ( column_name [, ...] )
    { VALUES ( expression [, ...] ) [, ...] | query }

Where **query** is a :code:`SELECT` query.

Let's look at a few examples...

Stream writes can be a single event at a time:

.. code-block:: sql

	INSERT INTO stream (x, y, z) VALUES (0, 1, 2);
	INSERT INTO json_stream (payload) VALUES (
	  '{"key": "value", "arr": [92, 12, 100, 200], "obj": { "nested": "value" } }'
	);

Or they can be batched for better performance:

.. code-block:: sql

	INSERT INTO stream (x, y, z) VALUES (0, 1, 2), (3, 4, 5), (6, 7, 8)
	(9, 10, 11), (12, 13, 14), (15, 16, 17), (18, 19, 20), (21, 22, 23), (24, 25, 26);

Stream inserts can also contain arbitrary expressions:

.. code-block:: sql

	INSERT INTO geo_stream (id, coords) VALUES (42, a_function(-72.09, 41.40));

	INSERT INTO udf_stream (result) VALUES (my_user_defined_function('foo'));

	INSERT INTO str_stream (encoded, location) VALUES
	  (encode('encode me', 'base64'), position('needle' in 'haystack'));

	INSERT INTO rad_stream (circle, sphere) VALUES
	  (pi() * pow(11.2, 2), 4 / 3 * pi() * pow(11.2, 3));

	-- Subselects into streams are also supported
	INSERT INTO ss_stream (x) SELECT generate_series(1, 10) AS x;

	INSERT INTO tab_stream (x) SELECT x FROM some_table;


=================
Prepared INSERT
=================

Stream inserts also work with prepared inserts in order to reduce network overhead:

.. code-block:: sql

	PREPARE write_to_stream AS INSERT INTO stream (x, y, z) VALUES ($1, $2, $3);
	EXECUTE write_to_stream(0, 1, 2);
	EXECUTE write_to_stream(3, 4, 5);
	EXECUTE write_to_stream(6, 7, 8);

==============
COPY
==============

Finally, it is also possible to use COPY_ to write data from a file into a stream:

.. code-block:: sql

	COPY stream (data) FROM '/some/file.csv'

.. _COPY: http://www.postgresql.org/docs/current/static/sql-copy.html

:code:`COPY` can be very useful for retroactively populating a continuous view from archival data. Here is how one might stream compressed archival data from S3 into PipelineDB:

.. code-block:: sh

	aws s3 cp s3://bucket/logfile.gz - | gunzip | pipeline -c "COPY stream (data) FROM STDIN"


==============
Other Clients
==============

Since PipelineDB is an extension of PostgreSQL, writing to streams is possible from any client that works with PostgreSQL (and probably most clients that work with any SQL database for that matter), so it's not necessary to manually construct stream inserts. To get an idea of what that looks like, you should check out the :ref:`clients` section.

.. _output-streams:

Output Streams
----------------------

Output streams make it possible to read from the stream of incremental changes made to any continuous view, or rows selected by a continuous transform. Output streams are regular PipelineDB streams and as such can be read by other continuous views or transforms. They're accessed via the the :code:`output_of` function invoked on a continuous view or transform.

For continuous views, each row in an output stream always contains an **old** and **new** tuple representing a change made to the underlying continuous view. If the change corresponds to a continuous view insert, the old tuple will be :code:`NULL`. If the change corresponds to a delete (currently this is only possible when a sliding-window tuple goes out of window), the new tuple is :code:`NULL`.

Let's look at a simple example to illustrate some of these concepts in action. Consider a trivial continuous view that simply sums a single column of a stream:

.. code-block:: sql

	CREATE VIEW v_sum AS SELECT sum(x) FROM stream;

Now imagine a scenario in which we'd like to make a record of each time the sum changes by more than 10. We can create another continuous view that reads from :code:`v_sum`'s output stream to easily accomplish this:

.. code-block:: sql

  CREATE VIEW v_deltas AS SELECT abs((new).sum - (old).sum) AS delta
    FROM output_of('v_sum')
    WHERE abs((new).sum - (old).sum) > 10;

.. note:: **old** and **new** tuples must be wrapped in parentheses

Check out :ref:`ct-output-streams` for more information about output streams on continuous transforms.

==================================
Output Streams on Sliding Windows
==================================

For non-sliding-window continuous views, output streams are simply written to whenever a write to a stream yields a change to the continuous view's result. However, since sliding-window continuous views' results are also dependent on time, their output streams are automatically written to as their results change with time. That is, sliding-window continuous views' output streams will receive writes even if their input streams are not being written to.

Delta Streams
---------------------------

In addition to **old** and **new** tuples written to a continuous view's output stream, a **delta** tuple is also emitted for each incremental change made to the continuous view. The **delta** tuple contains the value representing the "difference" between the **old** and **new** tuples. For trivial aggregates such as :code:`sum`, the delta between an **old** and **new** value is simply the scalar value :code:`(new).sum - (old).sum`, much like we did manually in the above example.

Let's see what this actually looks like:

.. code-block:: psql

  postgres=# CREATE VIEW v AS SELECT COUNT(*) FROM stream;
  CREATE VIEW
  postgres=# CREATE VIEW v_real_deltas AS SELECT (delta).sum FROM output_of('v');
  CREATE VIEW
  postgres=# INSERT INTO stream (x) VALUES (1);
  INSERT 0 1
  postgres=# SELECT * FROM v_real_deltas;
  sum
  -----
     1
  (1 row)
  postgres=# INSERT INTO stream (x) VALUES (2);
  INSERT 0 1
  postgres=# INSERT INTO stream (x) VALUES (3);
  INSERT 0 1
  postgres=# SELECT * FROM v_real_deltas;
  sum
  -----
     1
     2
     3
  (3 rows)

As you can see, **v_real_deltas** records the incremental changes resulting from each insertion. But :code:`sum` is relatively boring. The real magic of **delta** streams is that they work for all aggregates, and can even be used in conjunction with :ref:`combine` to efficiently aggregate continuous views' output at different granularities/groupings.

Let's look at a more interesting example. Suppose we have a continuous view counting the number of distinct users per minute:

.. code-block:: sql

  CREATE VIEW uniques_1m AS
    SELECT minute(arrival_timestamp) AS ts, COUNT(DISTINCT user_id) AS uniques
  FROM s GROUP BY ts;

For archival and performance purposes we may want to down aggregate this continuous view to an hourly granularity after a certain period of time. With an aggregate such as :code:`COUNT(DISTINCT)`, we obviously can't simply sum the counts over all the minutes in an hour, because there would be duplicated uniques across the original **minute** boundaries. Instead, we can :ref:`combine` the distinct **delta** values produced by the output of the minute-level continuous view:

.. code-block:: sql

  CREATE VIEW uniques_hourly AS
    SELECT hour((new).ts) AS ts, combine((delta).uniques) AS uniques
  FROM output_of('uniques_1m') GROUP BY ts;

The **uniques_hourly** continuous view will now contain hourly uniques rows that contain the *exact same information as if all of the original raw values were aggregated at the hourly level*. But instead of duplicating the work performed by reading the raw events, we only had to further aggregate the output of the minute-level aggregation.

pipelinedb.stream_targets
----------------------------------

Sometimes you might want to update only a select set of continuous queries (views and transforms) when writing to a stream, for instance, when replaying historical data into a newly created continuous view. You can use the :code:`pipelinedb.stream_targets` configuration parameter to specify the continuous queries that should read events written to streams from the current session. Set :code:`pipelinedb.stream_targets` to a comma-separated list of continuous queries you want to consume the events:

.. code-block:: psql

  postgres=# CREATE VIEW v0 AS SELECT COUNT(*) FROM stream;
  CREATE VIEW
  postgres=# CREATE VIEW v1 AS SELECT COUNT(*) FROM stream;
  CREATE VIEW
  postgres=# INSERT INTO stream (x) VALUES (1);
  INSERT 0 1
  postgres=# SET stream_targets TO v0;
  SET
  postgres=# INSERT INTO stream (x) VALUES (1);
  INSERT 0 1
  postgres=# SET stream_targets TO DEFAULT;
  SET
  postgres=# INSERT INTO stream (x) VALUES (1);
  INSERT 0 1
  postgres=# SELECT count FROM v0;
   count
  -------
       3
  (1 row)

  postgres=# SELECT count FROM v1;
   count
  -------
       2
  (1 row)

  postgres=#

.. _arrival-ordering:

Arrival Ordering
------------------

By design, PipelineDB uses **arrival ordering** for event ordering. What this means is that events are timestamped when they arrive at the PipelineDB server, and are given an additional attribute called :code:`arrival_timestamp` containing that timestamp. The :code:`arrival_timestamp` can then be used in :ref:`continuous-views` with a temporal component, such as :ref:`sliding-windows` .

Event Expiration
------------------

After each event arrives at the PipelineDB server, it is given a small bitmap representing all of the :ref:`continuous-views` that still need to read the event. When a continuous view is done reading an event, it flips a single bit in the bitmap. When all of the bits in the bitmap are set to :code:`1`, the event is discarded and can never be accessed again.

----------

Now that you know what :ref:`continuous-views` are and how to write to streams, it's time to learn about PipelineDB's expansive :ref:`builtin`!
