.. _continuous-views:

..  Continuous Views

流视图
=================
..
	PipelineDB's fundamental abstraction is called a continuous view. A continuous view is much like a regular view, except that it selects from a combination of streams and tables as its inputs and is incrementally updated in realtime as new data is written to those inputs.

	As soon as a stream row has been read by the continuous views that must read it, it is discarded. Raw, granular data is not stored anywhere. The only data that is persisted for a continuous view is whatever is returned by running a :code:`SELECT * FROM that_view`. Thus you can think of a continuous view as a very high-throughput, realtime materialized view.

流视图（continuous view）是PipelineDB的基础概念抽象。流视图跟普通的数据库视图非常相似，但它是将流和表中的数据组合后作为输入并进行实时增量更新。

流数据一旦被流视图读取后就会被销毁，流数据不会存储在任何地方。只有诸如 :code:`SELECT * FROM that_view` 查询返回的结果才会被持久化，也就是说，流视图可以被视为高吞吐量、实时的物化视图。

..	Creating Continuous Views

创建流视图
---------------------------

..	Continuous views are defined as PostgreSQL views with the :code:`action` parameter set to :code:`materialize`. Here's the syntax for creating a continuous view:

流视图可以通过将PostgreSQL的 :code:`action` 参数设为 :code:`materialize` 来创建。语法如下：

.. code-block:: sql

	CREATE VIEW name [WITH (action=materialize [, ...])]  AS query

.. note::
	..	The default :code:`action` is :code:`materialize`, and thus :code:`action` may be ommitted for creating continuous views. As long as a stream is being selected from, PipelineDB will interpret the :code:`CREATE VIEW` statement with an :code:`action` of :code:`materialize`.

	:code:`action` 默认值为 :code:`materialize`，创建流视图时可以省略 :code:`action` 参数。只要对流视图执行了 **select from** 操作，PipelineDB就会为 :code:`CREATE VIEW` 赋值 :code:`action=materialize`。

..	where **query** is a subset of a PostgreSQL :code:`SELECT` statement:

**query** 是PostgreSQL :code:`SELECT` 语句的子集：

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

.. note::
	..	This section references streams, which are similar to tables and are what continuous views and transforms read from in their :code:`FROM` clause. They're explained in more depth in the :ref:`streams` section, but you can think of them as append-only tables for now.

	本节提到了流的概念，流跟数据表类似，并且是流视图和流转换 :code:`FROM` 子句的数据源。关于流的解释，在 :ref:`流（foreign table）<streams>` 一节中有更详细的解释，您目前可以认为流是只支持添加（append-only）的数据表。

**expression**

	..	A PostgreSQL expression_ or `grouping sets specification`_.

	参考PostgreSQL `表达式`_ 或 `分组查询说明`_。


.. _expression: https://www.postgresql.org/docs/current/static/sql-expressions.html
.. _表达式: https://www.postgresql.org/docs/current/static/sql-expressions.html
.. _grouping sets specification: https://www.postgresql.org/docs/current/static/queries-table-expressions.html#QUERIES-GROUPING-SETS
.. _分组查询说明: https://www.postgresql.org/docs/current/static/queries-table-expressions.html#QUERIES-GROUPING-SETS

**output_name**
	..	An optional identifier to name an expression with

	一个可选的表达式标识符。

**condition**
	..	Any expression that evaluates to a result of type :code:`boolean`. Any row that does not satisfy this condition will be eliminated from the output. A row satisfies the condition if it returns :code:`true` when the actual row values are substituted for any variable references.

	任何结果为 :code:`boolean` 类型的表达式。任何不满足条件的数据都会被从输出中过滤掉。如果原始数据在替换变量引用后的结果为 :code:`true`，则认为其满足条件。

.. note::
	..	This has mainly covered only the syntax for creating continuous views. To learn more about the semantics of each of these query elements, you should consult the `PostgreSQL SELECT documentation`_.

	上述说明只涉及到了创建流视图的语法。要了解更多关于上述查询的语义细节，请查看 `PostgreSQL SELECT 文档`_。

.. _PostgreSQL SELECT documentation: https://www.postgresql.org/docs/current/static/sql-select.html
.. _PostgreSQL SELECT 文档: https://www.postgresql.org/docs/current/static/sql-select.html

..	Dropping Continuous Views

删除（drop）流视图
---------------------------

..	To :code:`DROP` a continuous view from the system, use the :code:`DROP VIEW` command. Its syntax is simple:

使用 :code:`DROP VIEW` 指令从系统中删除流视图，语法如下：

.. code-block:: sql

	DROP VIEW name

..	This will remove the continuous view from the system along with all of its associated resources.

这将从系统中移除流视图相关的所有资源。

..	Truncating Continuous Views

截断（truncate）流视图
-----------------------------

..	To remove all of a continuous view's data without removing the continuous view itself, the :code:`truncate_continuous_view` function can be used:

通过 :code:`truncate_continuous_view` 指令删除流视图中的所有所有数据，保留流视图本身。使用方法如下：

.. code-block:: sql

    SELECT truncate_continuous_view('name');

..	This command will efficiently remove all of the continuous view's rows, and is therefore analagous to `PostgreSQL's TRUNCATE`_ command.

这个指令将有效地移除流视图中的所有行式数据，与 `PostgreSQL截断`_ 指令类似。

.. _`PostgreSQL's TRUNCATE`: https://www.postgresql.org/docs/current/static/sql-truncate.html
.. _`PostgreSQL截断`: https://www.postgresql.org/docs/current/static/sql-truncate.html

.. _pipeline-query:

..	Viewing Continuous Views

查看流视图
---------------------------

..	To view the continuous views and their definitions currently in the system, you can run the following query:

可以通过下列查询语句查看当前系统中的所有流视图：

.. code-block:: sql

	SELECT * FROM pipelinedb.views;

..	Data Retrieval

数据检索
-------------------

..	Since continuous views are a lot like regular views, retrieving data from them is simply a matter of performing a :code:`SELECT` on them:

由于流视图跟普通的数据很类似，检索数据只需要执行 :code:`SELECT` 即可：

.. code-block:: sql

  SELECT * FROM some_continuous_view

========  ===========
  user    event_count
========  ===========
a         10
b         20
c         30
========  ===========

..	Any :code:`SELECT` statement is valid on a continuous view, allowing you to perform further analysis on their perpetually updating contents:

任何 :code:`SELECT` 语句在流视图中都是有效的，这让您可以基于持续更新的内容进行更加精细化的分析：

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

..	Time-to-Live (TTL) Expiration

保留时间（TTL）
---------------------------------

..	A common PipelineDB pattern is to include a time-based column in aggregate groupings and removing old rows that are no longer needed, as determined by that column. While there are a number of ways to achieve this behavior, PipelineDB provides native support for row expiration via time-to-live (TTL) critera specified at the continuous view level.

PipelineDB可以通过指定一个时间类型列并且设置保留时间（TTL）来清理流视图中的过期数据。

..	TTL expiration behavior can be assigned to continuous views via the :code:`ttl` and :code:`ttl_column` storage parameters. Expiration is handled by one or more **"reaper"** processes that will :code:`DELETE` any rows having a :code:`ttl_column` value that is older than the interval specified by :code:`ttl` (relative to wall time). Here's an example of a continuous view definition that will tell the reaper to delete any rows whose **minute** column is older than one month:

保留时间（TTL）可以通过 :code:`ttl` 和 :code:`ttl_column` 参数来设定。过期数据会在 **"reaper"** 阶段被 :code:`DELETE`。任何 :code:`ttl_column` 值小于 当前时间减去 :code:`ttl` 的数据都会被删除。下面的指令创建了一个过期时间为 **1个月** 的流视图：

.. code-block:: sql

  CREATE VIEW v_ttl WITH (ttl = '1 month', ttl_column = 'minute') AS
    SELECT minute(arrival_timestamp), COUNT(*) FROM some_stream GROUP BY minute;

..	Note that TTL behavior is a hint to the **reaper**, and thus will not guarantee that rows will be physically deleted exactly when they are expired.

要注意的是，TTL只会暗示数据库执行 **reaper** 操作，不保证数据在过期时被立刻物理删除。

..	If you'd like to guarantee that no TTL-expired rows will be read, you should create a view over the continuous view with a :code:`WHERE` clause that excludes expired rows at read time.

如果您想保已过期的数据不被读到，您应该在查询时通过 :code:`WHERE` 子句来对数据进行过滤。

..	Modifying TTLs

修改TTL
----------------------------

..	TTLs can be added, modified, and removed from continuous views via the **pipelinedb.set_ttl** function:

可以通过 **pipelinedb.set_ttl** 添加、修改和移除TTL：

**pipelinedb.set_ttl ( cv_name, ttl, ttl_column )**

	Update the given continuous view's TTL with the given paramters. **ttl** is an interval expressed as a string (e.g. :code:`'1 day'`), and **ttl_column** is the name of a timestamp-based column.

	Passing :code:`NULL` for both the **ttl** and **ttl_column** parameters will effectively remove a TTL from the given continuous view. Note that a TTL cannot be modified on or removed from a sliding-window continuous view.


.. _activation-deactivation:

..	Activation and Deactivation

激活和中止
----------------------------

..	Because continuous views are continuously processing input streams, it can be useful to have a notion of starting and stopping that processing without having to completely shutdown PipelineDB. For example, if a continuous view incurs an unexpected amount of system load or begins throwing errors, it may be useful to temporarily stop continuous processing for that view (or all of them) until the issue is resolved.

由于流视图会持续处理流数据，所以在不停止PipelineDB的时候启动和停止这个处理过程是很实用的。比如，一个流视图造成了未预估的系统负载或抛出异常，我们很需要在问题修复前让流视图暂时停止工作。

..	This level of control is provided by the :code:`activate` and :code:`deactivate` functions, which are synonymous with "play" and "pause". When continuous views are *active*, they are actively reading from their input streams and incrementally updating their results accordingly. Conversely, *inactive* continuous views are not reading from their input streams and are not updating their results. PipelineDB remains functional when continuous views are inactive, and continuous views themselves are still readable--they're just not updating.

:code:`activate` 和 :code:`deactivate` 函数提供了上述粒度的控制，就像"播放"和"pause"。当流视图处于 *active* 状态，它们将实时读取流数据并对数据进行增量更新。相反，处于 *inactive* 的流视图将会停止前面的工作。

..	The function signatures take only a continuous view or transform name:

函数只需要传入一个流视图或流转换的名称：

.. code-block:: sql

	SELECT pipelinedb.activate('continuous_view_or_transform');
	SELECT pipelinedb.deactivate('continuous_view_or_transform');

..	:ref:`continuous-transforms` can also be activated and deactivated.

:ref:`流转换<continuous-transforms>` 也可以被激活或中止。

.. important::
	..	When continuous queries (views or transforms) are inactive, any events written to their input streams while they're inactive will never be read by that continuous query, even after they're activated again.

	当流查询（视图或转换）处于中止（inactive）状态的这段时间里，所有写入到流（foreign table）中的数据将不会再被读到，即使流查询被重新激活（activate）。

..	See :ref:`operations` for more information.

查看 :ref:`操作<operations>` 获取更多信息。

..	Examples

示例
---------------------

..	Putting this all together, let's go through a few examples of continuous views and understand what each one accomplishes.

让我们通过下列示例来学习每个流视图完成的功能。

.. important::
	..	It is important to understand that the only data persisted by PipelineDB for a continuous view is whatever would be returned by running a :code:`SELECT * FROM my_cv` on it (plus a small amount of metadata). This is a relatively new concept, but it is at the core of what makes continuous views so powerful!

	需要明确理解的一点是：PipelineDB只会将诸如 :code:`SELECT * FROM my_cv` 的查询结果以及少量的源信息进行持久化存储。这是一个比较新的概念，但却是流视图的灵魂！

..	Emphasizing the above notice, this continuous view would only ever store a single row in PipelineDB (just a few bytes), even if it read a trillion events over time:

值得注意的是，即使流视图读取了流中数以万亿计的数据，经过分组聚合后，在PipelineDB中可能也只存在1条行数据。

.. code-block:: sql

  CREATE VIEW avg_of_forever AS SELECT AVG(x) FROM one_trillion_events_stream;


..	**Calculate the number of unique users seen per url referrer each day using only a constant amount of space per day:**

**referrer的UV**

.. code-block:: sql

  CREATE VIEW uniques AS
  SELECT date_trunc('day', arrival_timestamp) AS day,
    referrer, COUNT(DISTINCT user_id)
  FROM users_stream GROUP BY day, referrer;

..	**Compute the linear regression of a stream of datapoints bucketed by minute:**

**线性回归**

.. code-block:: sql

  CREATE VIEW lreg AS
  SELECT date_trunc('minute', arrival_timestamp) AS minute,
    regr_slope(y, x) AS mx,
    regr_intercept(y, x) AS b
  FROM datapoints_stream GROUP BY minute;

..	**How many ad impressions have we served in the last five minutes?**

**最近5分钟的广告访问量**

.. code-block:: sql

  CREATE VIEW imps AS
    SELECT COUNT(*) FROM imps_stream
  WHERE (arrival_timestamp > clock_timestamp() - interval '5 minutes');

..	**What are the 90th, 95th, and 99th percentiles of my server's request latency?**

**服务请求延时的90、95和99分位数**

.. code-block:: sql

  CREATE VIEW latency AS
    SELECT percentile_cont(array[90, 95, 99]) WITHIN GROUP (ORDER BY latency)
  FROM latency_stream;

----------

..	We hope you enjoyed learning all about continuous views! Next, you should probably check out how :ref:`streams` work.

希望您能喜欢关于流视图的说明！接下来，您可能需要了解 :ref:`流（foreign table）<streams>` 是如何工作的。
