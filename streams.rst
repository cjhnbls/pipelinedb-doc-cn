.. _streams:

Streams
=================

Streams are the abstraction that allows clients to push data through :ref:`continuous-views`. A stream row, or simply **event**, looks exactly like a regular table row, and the interface for writing data to streams is identical to the one for writing to tables. However, the semantics of streams are fundamentally different from tables.

Namely, events only "exist" within a stream until they are consumed by all of the :ref:`continuous-views` that are reading from that stream. Even then, it is still not possible for users to :code:`SELECT` from streams. Streams serve exclusively as inputs to :ref:`continuous-views`.

Finally, unlike tables, it is not necessary to create a schema for streams. As long as there is at least one continuous view reading from a stream, you can write to it. The only restriction is that all stream insertions require a column header.

.. _static-streams:

Static Streams
----------------

While PipelineDB does not require that streams be explicitly predeclared, it is possible to do so with statically typed streams. Static streams can yield performance increases due to less internal casting being required, as well as provide more control over how raw inputs are interpreted. Statically typed streams are created with :code:`CREATE STREAM`. 

The syntax for creating a static stream is similar to that of creating a table:


.. code-block:: pipeline

	CREATE STREAM stream_name ( [
		{ column_name data_type [ COLLATE collation ] | LIKE parent_stream } [, ... ]
	] )


**stream_name**

  The name of the stream to be created.

**column_name**

  The name of a column to be created in the new table.

**data_type**

  The data type of the column. This can include array specifiers. For more information on the data types supported by PipelineDB, see :ref:`builtin` and the `PostgreSQL supported types`_ .

.. _PostgreSQL supported types: http://www.postgresql.org/docs/9.4/static/datatype.html

**COLLATE collation**

  The COLLATE clause assigns a collation to the column (which must be of a collatable data type). If not specified, the column data type's default collation is used.

**LIKE parent_table [ like_option ... ]**

	The :code:`LIKE` clause specifies a stream from which the new stream automatically copies all column names and data types.

Static streams can be dropped with the :code:`DROP STREAM` command. Below is an example of creating a continuous view that reads from a static stream. Note that when a continuous view reads from a statically typed stream, it is not necessary to supply type information with :code:`::` syntax:

.. code-block:: pipeline

  pipeline=# CREATE STREAM static_stream (x integer, y integer);
  CREATE STREAM
  pipeline=# CREATE CONTINUOUS VIEW v AS SELECT sum(x + y) FROM static_stream;
  CREATE CONTINUOUS VIEW

Writing To Streams
----------------------

=========
INSERT
=========

Stream writes use a simplified version of a PostgreSQL :code:`INSERT` statement. Here's the syntax:

.. code-block::

	INSERT INTO stream_name ( column_name [, ...] ) VALUES ( expression [, ...] ) [, ...]

.. important:: It is an error to write to a stream that no *active* continuous views are reading from--the write will be rejected. This is to prevent against unknowingly writing data that is being silently ignored. See :ref:`activation-deactivation` for more information about active continuous views.

Let's look at a few examples...

Stream writes can be a single event at a time:

.. code-block:: pipeline

	INSERT INTO stream (x, y, z) VALUES (0, 1, 2);

	INSERT INTO json_stream (payload) VALUES (
	  '{"key": "value", "arr": [92, 12, 100, 200], "obj": { "nested": "value" } }'
	);

Or they can be batched for better performance:

.. code-block:: pipeline

	INSERT INTO stream (x, y, z) VALUES (0, 1, 2), (3, 4, 5), (6, 7, 8)
	(9, 10, 11), (12, 13, 14), (15, 16, 17), (18, 19, 20), (21, 22, 23), (24, 25, 26);

Stream inserts can also contain arbitrary expressions:

.. code-block:: pipeline

	INSERT INTO geo_stream (id, coords) VALUES (42, ST_MakePoint(-72.09, 41.40));

	INSERT INTO udf_stream (result) VALUES (my_user_defined_function('foo'));

	INSERT INTO str_stream (encoded, location) VALUES
	  (encode('encode me', 'base64'), position('needle' in 'haystack'));

	INSERT INTO rad_stream (circle, sphere) VALUES
	  (pi() * pow(11.2, 2), 4 / 3 * pi() * pow(11.2, 3));

	-- Subselects int streams are also supported
	INSERT INTO ss_stream (x) SELECT generate_series(1, 10) AS x;

	INSERT INTO tab_stream (x) SELECT x FROM some_table;

=================
Prepared INSERT
=================

Stream inserts also work with prepared inserts in order to reduce network overhead:

.. code-block:: pipeline

	PREPARE write_to_stream AS INSERT INTO stream (x, y, z) VALUES ($1, $2, $3);

	EXECUTE write_to_stream(0, 1, 2);
	EXECUTE write_to_stream(3, 4, 5);
	EXECUTE write_to_stream(6, 7, 8);

==============
COPY
==============

Finally, it is also possible to use COPY_ to write data from a file into a stream:

.. code-block:: pipeline

	COPY stream (data) FROM '/some/file.csv'

.. _COPY: http://www.postgresql.org/docs/9.4/static/sql-copy.html

:code:`COPY` can be very useful for retroactively populating a continuous view from archival data. Here is how one might stream compressed archival data from S3 into a PipelineDB stream:

.. code-block:: pipeline

	s3cmd get s3://bucket/logfile.gz - | gunzip | pipeline -c "COPY stream (data) FROM STDIN"


==============
Other Clients
==============

Since PipelineDB is compatible with PostgreSQL, writing to streams is possible from any client that works with PostgreSQL (and probably most clients that work with any SQL database for that matter), so it's not necessary to manually construct stream inserts. To get an idea of what that looks like, you should check out the :ref:`clients` section.


stream_targets
----------------------

Sometimes you might want to update only a select set of continuous views when writing to a stream, for instance, when replaying historical data into a newly created continuous view. You can use the :code:`stream_targets` configuration parameter to specify the continuous views that should be updated when writing to streams. Set :code:`stream_targets` to a comma separated list of continuous views you want to be affecting when inserting to streams.

.. code-block:: pipeline

  pipeline=# CREATE CONTINUOUS VIEW v0 AS SELECT COUNT(*) FROM stream;
  CREATE CONTINUOUS VIEW
  pipeline=# CREATE CONTINUOUS VIEW v1 AS SELECT COUNT(*) FROM stream;
  CREATE CONTINUOUS VIEW
  pipeline=# ACTIVATE;
  ACTIVATE 2
  pipeline=# INSERT INTO stream (x) VALUES (1);
  INSERT 0 1
  pipeline=# SET stream_targets TO v0;
  SET
  pipeline=# INSERT INTO stream (x) VALUES (1);
  INSERT 0 1
  pipeline=# SET stream_targets TO DEFAULT;
  SET
  pipeline=# INSERT INTO stream (x) VALUES (1);
  INSERT 0 1
  pipeline=# DEACTIVATE;
  DEACTIVATE 2
  pipeline=# SELECT count FROM v0;
   count
  -------
       3
  (1 row)

  pipeline=# SELECT count FROM v1;
   count
  -------
       2
  (1 row)

  pipeline=#

.. _arrival-ordering:

Arrival Ordering
------------------

By design, PipelineDB uses **arrival ordering** for event ordering. What this means is that events are timestamped when they arrive at the PipelineDB server, and are given an additional attribute called :code:`arrival_timestamp` containing that timestamp. The :code:`arrival_timestamp` can then be used in :ref:`continuous-views` with a temporal component, such as :ref:`sliding-windows` .

.. note:: :code:`arrival_timestamp` is also implicitly used as the :code:`ORDER BY` clause in :ref:`continuous-views` involving :code:`PARTITION BY` and :code:`OVER`, as it is the only field that can be reasonably used for applying order to an infinite stream.

Event Expiration
------------------

After each event arrives at the PipelineDB server, it is given a small bitmap representing all of the :ref:`continuous-views` that still need to read the event. When a :code:`CONTINUOUS VIEW` is done reading an event, it flips a single bit in the bitmap. When all of the bits in the bitmap are set to :code:`1`, the event is discarded and can never be accessed again.

----------

Now that you know what :ref:`continuous-views` are and how to write to streams, it's time to learn about PipelineDB's expansive :ref:`builtin`!
