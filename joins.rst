.. _joins:

Continuous JOINs
============================

:ref:`continuous-views` are not limited to selecting exclusively from :ref:`streams`. Often it can be useful to augment or combine incoming streaming data with static data stored in PipelineDB tables. This can be easily accomplished using what are called stream-table joins.

Stream-table JOINs
----------------------

Stream-table joins work by joining an incoming event with matching rows that exist in the joining table **when the event arrives**. That is, if rows are inserted into the table that would have matched with previously read events, the result of the continuous view containing the stream-table join will not be updated to reflect that. New joined rows are only produced at **event-read time**. Even if all rows in the joining table were deleted, the result of the continuous view would not change.

Let's look at some examples.


Examples
-----------

**Count the number of events whose id was in the "whitelist" table at some point in time:**

.. code-block:: pipeline

	CREATE CONTINUOUS VIEW count_whitelisted AS SELECT COUNT(*) FROM
	stream JOIN whitelist ON stream.id = whitelist.id;

**Augment incoming user data with richer user information stored in the "users" table:**

.. code-block:: pipeline

	CREATE CONTINUOUS VIEW augmented AS SELECT user_data.full_name, COUNT(*)
	FROM stream JOIN user_data on stream.id::integer = user_data.id
	GROUP BY user_data.full_name;

**Spatially join incoming coordinates to their nearest city, and summarize by city name:**

.. code-block:: pipeline

	CREATE CONTINUOUS VIEW spatial AS SELECT cities.name, COUNT(*) FROM
	geo_stream, cities WHERE st_within(geo_stream.coords::geometry, cities.borders)
	GROUP BY cities.name;


.. note:: As you may have guessed, stream-table joins involving large tables can incur a significant performance cost. For the best performance, tables used by stream-table joins should be relatively small, ideally small enough to fit in memory. It is also advisable to create an index on the table's columns being joined on. 


Stream-stream JOINs
-----------------------

Joining a stream with another stream is currently not supported, but may be available in future releases of PipelineDB.
