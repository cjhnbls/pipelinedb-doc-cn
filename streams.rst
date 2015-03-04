.. _streams:

Streams
=================

Streams are the abstraction that allows clients to push data through :ref:`continuous-views`. A stream row, or simply **event**, looks exactly like a regular table row, and the interface for writing data to streams is identical to the one for writing to tables. However, the semantics of streams are fundamentally different from tables.

Namely, events only "exist" within a stream until they are consumed by all of the :ref:`continuous-views` that are reading from that stream. Even then, it is still not possible for users to :code:`SELECT` from streams. Streams serve exclusively as inputs to :ref:`continuous-views`.

Finally, unlike tables, it is not necessary to create a schema for streams. As long as there is at least one :code:`CONTINUOUS VIEW` reading from a stream, you can write to it. The only restriction is that all stream insertions require a column header.

Writing to streams
----------------------

Stream writes use a simplified version of a PostgreSQL :code:`INSERT` statement. Here's the syntax:

.. code-block:: pipeline

	INSERT INTO stream_name ( column_name [, ...] ) VALUES ( expression [, ...] ) [, ...]

.. important:: It is an error to write to a stream that no *active* :code:`CONTINUOUS VIEW` s are reading from, and the write will be rejected. This is to prevent against unknowingly writing data that is being silently ignored. See :ref:`activation-deactivation` for more information about active :code:`CONTINUOUS VIEW` s.

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
	(9, 10, 11), (12, 13, 14), (15, 16, 17), (18, 19, 20), (21, 22, 23), (24, 25, 26) (27, 28, 29);

Stream inserts can also contain arbitrary expressions:

.. code-block:: pipeline

	INSERT INTO geo_stream (id, coords) VALUES (42, ST_MakePoint(-72.09, 41.40));

	INSERT INTO udf_stream (result) VALUES (my_user_defined_function('foo'));

	INSERT INTO str_stream (encoded, location) VALUES
	  (encode('encode me', 'base64'), position('needle' in 'haystack'));

	INSERT INTO rad_stream (circle, sphere) VALUES
	  (pi() * pow(11.2, 2), 4 / 3 * pi() * pow(11.2, 3));

Since PipelineDB is compatible with PostgreSQL, writing to streams is possible from any client that works with PostgreSQL (and probably most clients that work with any SQL database for that matter), so it's not necessary to manually construct stream inserts. To get an idea of what that looks like, you should check out the :ref:`clients` section.

.. _arrival-ordering:

Arrival ordering
------------------

By design, PipelineDB uses **arrival ordering** for event ordering. What this means is that events are timestamped when they arrive at the PipelineDB server, and are given an additional attribute called :code:`arrival_timestamp` containing that timestamp. The :code:`arrival_timestamp` can then be used in :ref:`continuous-views` with a temporal component, such as :ref:`sliding-windows` .

.. note:: :code:`arrival_timestamp` is also implicitly used as the :code:`ORDER BY` clause in :ref:`continuous-views` involving :code:`PARTITION BY` and :code:`OVER`, as it is the only field that can be reasonably used for applying order to an infinite stream.

Event expiration
------------------

After each event arrives at the PipelineDB server, it is given a small bitmap representing all of the :ref:`continuous-views` that still need to read the event. When a :code:`CONTINUOUS VIEW` is done reading an event, it flips a single bit in the bitmap. When all of the bits in the bitmap are set to :code:`1`, the event is discarded and can never be accessed again.

----------

Now that you know what :ref:`continuous-views` are and how to write to streams, it's time to learn about :ref:`activation-deactivation` !
