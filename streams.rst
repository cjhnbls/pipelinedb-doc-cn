.. _streams:

..  Streams

流（foreign table）
==================

..	Streams are the abstraction that allows clients to push time-series data through :ref:`continuous-views`. A stream row, or simply **event**, looks exactly like a regular table row, and the interface for writing data to streams is identical to the one for writing to tables. However, the semantics of streams are fundamentally different from tables.

流是一种允许客户端将时序数据写入 :ref:`流视图<continuous-views>` 的抽象管道。流里面的一行数据（或者简单称作 **event**），与数据表中的行数据是很相似的，并且二者的写入也是完全一致的。然而，流和数据表的语义是完全不同的。

..	Namely, events only "exist" within a stream until they are consumed by all of the :ref:`continuous-views` that are reading from that stream. Even then, it is still not possible for users to :code:`SELECT` from streams. Streams serve exclusively as inputs to :ref:`continuous-views`.

换言之，**event** 只会在被所有的 :ref:`流视图<continuous-views>` 消费完之前“存在”于流中。即使这样，用户仍然不能直接从流中 :code:`SELECT` 数据。流唯一的作用就是充当 :ref:`流视图<continuous-views>` 的输入。

..	Streams are represented in PipelineDB as `foreign tables`_ managed by the :code:`pipelinedb` `foreign server`_. The syntax for creating a foreign table is similar to that of creating a regular PostgreSQL table:

流在PipelineDB中是作为 :code:`pipelinedb` `外部服务`_ 管理下的 `外部表`_ 存在的。创建外部表的语法跟创建普通的PostgreSQL数据表类似：

.. _`foreign tables`: https://www.postgresql.org/docs/current/static/sql-createforeigntable.html
.. _`外部表`: https://www.postgresql.org/docs/current/static/sql-createforeigntable.html
.. _`foreign server`: https://www.postgresql.org/docs/current/static/sql-createserver.html
.. _`外部服务`: https://www.postgresql.org/docs/current/static/sql-createserver.html

.. code-block:: sql

	CREATE FOREIGN TABLE stream_name ( [
	   { column_name data_type [ COLLATE collation ] } [, ... ]
	] )
	SERVER pipelinedb;


**stream_name**
	..	The name of the stream to be created.

	要创建的流的名称

**column_name**
	..	The name of a column to be created in the new table.

	新表中列的名称

**data_type**
	..	The data type of the column. This can include array specifiers. For more information on the data types supported by PipelineDB, see :ref:`builtin` and the `PostgreSQL supported types`_ .

	列的数据类型，可以声明为数组。您可以查看 :ref:`内置功能<builtin>` 和 `PostgreSQL类型支持`_ 了解更多PipelineDB支持的数据类型。

.. _PostgreSQL supported types: https://www.postgresql.org/docs/current/static/datatype.html
.. _PostgreSQL类型支持: https://www.postgresql.org/docs/current/static/datatype.html

**COLLATE collation**
	..	The :code:`COLLATE` clause assigns a collation to the column (which must be of a collatable data type). If not specified, the column data type's default collation is used.

	:code:`COLLATE` 子句可以给指定列（必须是可排序的数据类型）赋予排序规则。若未指定，则使用默认的排序规则。

..	Columns can be added to streams using :code:`ALTER STREAM`:

可以通过 code:`ALTER STREAM` 给流添加列：

.. code-block:: psql

  postgres=# ALTER FOREIGN TABLE stream ADD COLUMN x integer;
  ALTER FOREIGN TABLE

.. note::
	..	Columns cannot be dropped from streams.

	流中的列是不可删除的。

	..	Streams can be dropped with the :code:`DROP FOREIGN TABLE` command. Below is an example of creating a simple continuous view that reads from a stream.

	可以通过 :code:`DROP FOREIGN TABLE` 指令删除流。下面是一个基于流创建流视图的例子。

.. code-block:: psql

  postgres=# CREATE FOREIGN TABLE stream (x integer, y integer) SERVER pipelinedb;
  CREATE FOREIGN TABLE
  postgres=# CREATE VIEW v AS SELECT sum(x + y) FROM stream;
  CREATE VIEW

..	Writing To Streams

写入数据到流
----------------------

.. =========
.. INSERT
.. =========

==============
INSERT写入
==============

..	Stream writes are just regular PostgreSQL :code:`INSERT` statements. Here's the syntax:

使用PostgreSQL的 :code:`INSERT` 语句就可以向流中写入数据，语法如下：

.. code-block:: sql

  INSERT INTO stream_name ( column_name [, ...] )
    { VALUES ( expression [, ...] ) [, ...] | query }

..	Where **query** is a :code:`SELECT` query.

**query** 是一个 :code:`SELECT` 查询。

..	Let's look at a few examples...

下面是一些示例：

..	Stream writes can be a single event at a time:

可以向流中写入单条数据：

.. code-block:: sql

	INSERT INTO stream (x, y, z) VALUES (0, 1, 2);
	INSERT INTO json_stream (payload) VALUES (
	  '{"key": "value", "arr": [92, 12, 100, 200], "obj": { "nested": "value" } }'
	);

..	Or they can be batched for better performance:

也可以批量写入以提高性能：

.. code-block:: sql

	INSERT INTO stream (x, y, z) VALUES (0, 1, 2), (3, 4, 5), (6, 7, 8)
	(9, 10, 11), (12, 13, 14), (15, 16, 17), (18, 19, 20), (21, 22, 23), (24, 25, 26);

..	Stream inserts can also contain arbitrary expressions:

同时也支持在写入时使用表达式。

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


.. ================
.. Prepared INSERT
.. ================

==============
预写入
==============

..	Stream inserts also work with prepared inserts in order to reduce network overhead:

可以通过预写入来减小流写入时的网络负载：

.. code-block:: sql

	PREPARE write_to_stream AS INSERT INTO stream (x, y, z) VALUES ($1, $2, $3);
	EXECUTE write_to_stream(0, 1, 2);
	EXECUTE write_to_stream(3, 4, 5);
	EXECUTE write_to_stream(6, 7, 8);

.. ==============
.. COPY
.. ==============

==============
COPY写入
==============

..	Finally, it is also possible to use COPY_ to write data from a file into a stream:

也可以通过 COPY_ 的方式将文件写入到流中：

.. code-block:: sql

	COPY stream (data) FROM '/some/file.csv'

.. _COPY: http://www.postgresql.org/docs/current/static/sql-copy.html

..	:code:`COPY` can be very useful for retroactively populating a continuous view from archival data. Here is how one might stream compressed archival data from S3 into PipelineDB:

:code:`COPY` 在向流中回填归档数据时是很有用的。下面的指令演示了如何将S3中压缩的归档数据写入到PipelineDB：

.. code-block:: sh

	aws s3 cp s3://bucket/logfile.gz - | gunzip | pipeline -c "COPY stream (data) FROM STDIN"


.. ==============
.. Other Clients
.. ==============

==============
其它客户端
==============

..	Since PipelineDB is an extension of PostgreSQL, writing to streams is possible from any client that works with PostgreSQL (and probably most clients that work with any SQL database for that matter), so it's not necessary to manually construct stream inserts. To get an idea of what that looks like, you should check out the :ref:`clients` section.

鉴于PipelineDB是PostgreSQL的插件，可以借助任何PostgreSQL客户端向流中写入数据（可能大多数SQL数据库的客户端可可以），所以没有必要手动构建流的写入。您可以查看 :ref:`客户端<clients>` 来具体了解。

.. _output-streams:

Output Streams
----------------------

..	Output streams make it possible to read from the stream of incremental changes made to any continuous view, or rows selected by a continuous transform. Output streams are regular PipelineDB streams and as such can be read by other continuous views or transforms. They're accessed via the the :code:`output_of` function invoked on a continuous view or transform.

流输出让我们可以将流数据的变化动态、增量地更新到流视图或流转换。流输出同普通的PipelineDB流一样，可以成为流视图或流转换的数据源，它可以在流视图或流转换中通过 :code:`output_of` 函数进行调用。

..	For continuous views, each row in an output stream always contains an **old** and **new** tuple representing a change made to the underlying continuous view. If the change corresponds to a continuous view insert, the old tuple will be :code:`NULL`. If the change corresponds to a delete (currently this is only possible when a sliding-window tuple goes out of window), the new tuple is :code:`NULL`.

在流视图中，流输出中的每行数据都包含 **old** 和 **new** 元组，用以体现流视图的变化。如果执行了 **写入**，**old** 元组为空，如果是 **删除**，（目前只可能发生出现在滑动窗口元组超过窗口范围的情况下） **new** 元组为空。

..	Let's look at a simple example to illustrate some of these concepts in action. Consider a trivial continuous view that simply sums a single column of a stream:

下面通过操作示例来说明这些概念。创建一个简单的流视图，只对流中的一列进行求和：

.. code-block:: sql

	CREATE VIEW v_sum AS SELECT sum(x) FROM stream;

..	Now imagine a scenario in which we'd like to make a record of each time the sum changes by more than 10. We can create another continuous view that reads from :code:`v_sum`'s output stream to easily accomplish this:

设计一个场景：在流输出每次触发sum的时候，若sum的变化超过10，则将这次增量记录下来。我们可以创建另一个以 :code:`v_sum` 的流输出为数据源的流视图来轻松完成这个构想：

.. code-block:: sql

  CREATE VIEW v_deltas AS SELECT abs((new).sum - (old).sum) AS delta
    FROM output_of('v_sum')
    WHERE abs((new).sum - (old).sum) > 10;

.. note::
	..	**old** and **new** tuples must be wrapped in parentheses
	**old** 和 **new** 元组必须用括号包起来。

..	Check out :ref:`ct-output-streams` for more information about output streams on continuous transforms.

查看 :ref:`流转换输出到流<ct-output-streams>` 了解更多流输出在流转换中的应用。

.. ==================================
.. Output Streams on Sliding Windows
.. ==================================

==================================
基于滑动窗口的流输出
==================================

..	For non-sliding-window continuous views, output streams are simply written to whenever a write to a stream yields a change to the continuous view's result. However, since sliding-window continuous views' results are also dependent on time, their output streams are automatically written to as their results change with time. That is, sliding-window continuous views' output streams will receive writes even if their input streams are not being written to.

对于不存在滑动窗口的流视图，流输出只是在流视图的结果发生变化时执行写入操作。然而基于滑动窗口的流视图的结果也是依赖时间的，它的流输出会自动随着时间写入到结果中。也就是说，即使没有新的写入，基于滑动窗口的流视图的结果也会更新。

Delta Streams
---------------------------

..	In addition to **old** and **new** tuples written to a continuous view's output stream, a **delta** tuple is also emitted for each incremental change made to the continuous view. The **delta** tuple contains the value representing the "difference" between the **old** and **new** tuples. For trivial aggregates such as :code:`sum`, the delta between an **old** and **new** value is simply the scalar value :code:`(new).sum - (old).sum`, much like we did manually in the above example.

在写入到流视图输出中 **old** 和 **new** 元组之外还有一个 **delta** 元组也会被提交到流视图的变化中。**delta** 元组中包含 **old** 元组 和 **old** 元组的“差值”。在sum这类聚合操作中，**delta** 就只是 :code:`(new).sum - (old).sum` 的标量，就像前面示例中的那样。

..	Let's see what this actually looks like:

代码及结果如下：

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

..	As you can see, **v_real_deltas** records the incremental changes resulting from each insertion. But :code:`sum` is relatively boring. The real magic of **delta** streams is that they work for all aggregates, and can even be used in conjunction with :ref:`combine` to efficiently aggregate continuous views' output at different granularities/groupings.

如您所见，**v_real_deltas** 记录了每次插入后值的变化，只是 :code:`sum` 这种操作比较简单。 **delta** 最吸引人之处是支持所有的聚合，甚至能与 :ref:`组合<combine>` 一起使用来高效聚合不同粒度和分组下的流视图。

..	Let's look at a more interesting example. Suppose we have a continuous view counting the number of distinct users per minute:

来看一个更有趣的例子，有一个计算每分钟去重后用户数的流视图：

.. code-block:: sql

  CREATE VIEW uniques_1m AS
    SELECT minute(arrival_timestamp) AS ts, COUNT(DISTINCT user_id) AS uniques
  FROM s GROUP BY ts;

..	For archival and performance purposes we may want to down aggregate this continuous view to an hourly granularity after a certain period of time. With an aggregate such as :code:`COUNT(DISTINCT)`, we obviously can't simply sum the counts over all the minutes in an hour, because there would be duplicated uniques across the original **minute** boundaries. Instead, we can :ref:`combine` the distinct **delta** values produced by the output of the minute-level continuous view:

为了归档以及提升性能，我们想在每个固定的周期后将流视图中的数据聚合为小时粒度。显然我们不可能通过将每分钟的 :code:`COUNT(DISTINCT)` 相加来得到一个小时的 :code:`COUNT(DISTINCT)`，因为这种操作需要整个时间段内的明细。但是我们将流视图中分钟级别的 **delta** :ref:`组合<combine>` 成小时级别的数据。

.. code-block:: sql

  CREATE VIEW uniques_hourly AS
    SELECT hour((new).ts) AS ts, combine((delta).uniques) AS uniques
  FROM output_of('uniques_1m') GROUP BY ts;

..	The **uniques_hourly** continuous view will now contain hourly uniques rows that contain the *exact same information as if all of the original raw values were aggregated at the hourly level*. But instead of duplicating the work performed by reading the raw events, we only had to further aggregate the output of the minute-level aggregation.

**uniques_hourly** 流视图目前包含小时级别的去重用户数，并且 *结果也同基于原始数据计算小时粒度的去重用户数完全一致*。

pipelinedb.stream_targets
----------------------------------

..	Sometimes you might want to update only a select set of continuous queries (views and transforms) when writing to a stream, for instance, when replaying historical data into a newly created continuous view. You can use the :code:`pipelinedb.stream_targets` configuration parameter to specify the continuous queries that should read events written to streams from the current session. Set :code:`pipelinedb.stream_targets` to a comma-separated list of continuous queries you want to consume the events:

有时您可能只想更新基于同一个流的多个流视图中的一个或几个，可以临时调整 :code:`pipelinedb.stream_targets` 来修改流的目标视图，这样结算结果就只会更新到指定的流视图中：

.. code-block:: psql

  postgres=# CREATE VIEW v0 AS SELECT COUNT(*) FROM stream;
  CREATE VIEW
  postgres=# CREATE VIEW v1 AS SELECT COUNT(*) FROM stream;
  CREATE VIEW
  postgres=# INSERT INTO stream (x) VALUES (1);
  INSERT 0 1
  postgres=# SET pipelinedb.stream_targets TO v0;
  SET
  postgres=# INSERT INTO stream (x) VALUES (1);
  INSERT 0 1
  postgres=# SET pipelinedb.stream_targets TO DEFAULT;
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

..	By design, PipelineDB uses **arrival ordering** for event ordering. What this means is that events are timestamped when they arrive at the PipelineDB server, and are given an additional attribute called :code:`arrival_timestamp` containing that timestamp. The :code:`arrival_timestamp` can then be used in :ref:`continuous-views` with a temporal component, such as :ref:`sliding-windows` .

您可以将 **arrival ordering** 用于 **event** 排序，也就是说 **event** 在到达PipelineDB服务时都带有一个 :code:`arrival_timestamp` 属性，这个属性可以在 :ref:`流视图<continuous-views>` 中诸如 :ref:`滑动窗口<sliding-windows>` 之类的操作中起辅助作用 。

Event Expiration
------------------

..	After each event arrives at the PipelineDB server, it is given a small bitmap representing all of the :ref:`continuous-views` that still need to read the event. When a continuous view is done reading an event, it flips a single bit in the bitmap. When all of the bits in the bitmap are set to :code:`1`, the event is discarded and can never be accessed again.

**event** 在到达PipelineDB时带有一个对应所有需要读取该 **event** 的流视图的位图。当一个流视图读完 **event** 后，当某个流视图读完该 **event** 后，它对应的那一位会从0变成1。当整个位图都变成1后，该 **event** 会被销毁。

----------

..	Now that you know what :ref:`continuous-views` are and how to write to streams, it's time to learn about PipelineDB's expansive :ref:`builtin`!

现在您已经知道了 :ref:`流视图<continuous-views>` 的相关信息以及如何向流中写入数据，是时候学习PipelineDB丰富的 :ref:`内置功能<builtin>` 了！
