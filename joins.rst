.. _joins:

..  Continuous JOINs

流关联
============================

..	:ref:`continuous-views` are not limited to selecting exclusively from :ref:`streams`. Often it can be useful to augment or combine incoming time-series data with static data stored in PipelineDB tables. This can be easily accomplished using what are called stream-table joins.

:ref:`流视图<continuous-views>` 的并非只靠select :ref:`流（foreign table）<streams>` 产生，也可以通过将输入的时序数据和静态的PipelineDB数据表组合后得到，这可以用称作流-表关联的方式实现。

..	Stream-table JOINs

流-表关联
----------------------

..	Stream-table joins work by joining an incoming event with matching rows that exist in the joining table **when the event arrives**. That is, if rows are inserted into the table that would have matched with previously read events, the result of the continuous view containing the stream-table join will not be updated to reflect that. New joined rows are only produced at **event-read time**. Even if all rows in the joining table were deleted, the result of the continuous view would not change.

流-表关联在 **event到达时** 将其与表中匹配的行数据组合。换言之，如果在表数据在 **event** 被读取后才被插入，即使是匹配的，也不会对流视图产生影响。新数据只会在 **event被读取时** 产生。即使流-表关联中的表被清空了，流视图中的数据也不会变化。

..	Supported Join Types

支持的关联类型
--------------------

..	Streams only support a subset of :code:`JOIN` types. :code:`CROSS JOIN` and :code:`FULL JOIN` are **not** supported. :code:`LEFT JOIN` and :code:`RIGHT JOIN` are only supported when the stream is on the side of the :code:`JOIN` whose unmatched rows are returned. :code:`ANTI JOIN` and :code:`SEMI JOIN` require an index on the column of the relation that is being join on.

流只支持 :code:`JOIN` 类型，:code:`CROSS JOIN` 和 :code:`FULL JOIN` 是 **不** 支持的，:code:`LEFT JOIN` 和 :code:`RIGHT JOIN` 只有在 :code:`JOIN` 中的不匹配数据在流这边（即流在left join的右边或right join的左边）时才支持。:code:`ANTI JOIN` 和 :code:`SEMI JOIN` 需要在关联的条件列上创建索引。

..	Examples

示例
-----------

..	**Count the number of events whose id was in the "whitelist" table at some point in time:**

**计算id在whitelist中的event数**

.. code-block:: sql

	CREATE VIEW count_whitelisted AS SELECT COUNT(*) FROM
	 stream JOIN whitelist ON stream.id = whitelist.id;

..	**Augment incoming user data with richer user information stored in the "users" table:**

**计算流中的id在users中每个full_name下的命中数**

.. code-block:: sql

	CREATE VIEW augmented AS SELECT user_data.full_name, COUNT(*)
	 FROM stream JOIN user_data on stream.id::integer = user_data.id
	GROUP BY user_data.full_name;

..	**Spatially join incoming coordinates to their nearest city, and summarize by city name:**

**Spatially将流中的坐标关联到最近的城市中，并计算每个城市命中的坐标数**

.. code-block:: sql

	CREATE VIEW spatial AS SELECT cities.name, COUNT(*) FROM
	geo_stream, cities WHERE st_within(geo_stream.coords::geometry, cities.borders)
	GROUP BY cities.name;


.. note::
	..	As you may have guessed, stream-table joins involving large tables can incur a significant performance cost. For the best performance, tables used by stream-table joins should be relatively small, ideally small enough to fit in memory. It is also advisable to create an index on the table's columns being joined on.

	您可以已经推测到，流-表关联中的表如果很大的话，产生的性能开销会极大。为了达到最高的性能，关联中的表应该是比较小的，小到内存足以承受，同时建议您在关联的列上创建索引。

..	Stream-stream JOINs

流-流关联
-----------------------

..	Joining a stream with another stream is currently not supported, but may be available in future releases of PipelineDB.

目前不支持流之间的关联，这个功能可能会出现在将来的版本中。

.. note::
	译者注：
	2019年5月1日，PipelineDB公司宣布加入Confluent（Kafka的公司），官方版将会停留在1.0.0，详情见 `官方报道`_。

.. _`官方报道`: https://www.pipelinedb.com/blog/pipelinedb-is-joining-confluent
