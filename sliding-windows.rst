.. _sliding-windows:

..  Sliding Windows

滑动窗口
============================

..	Since :ref:`continuous-views` are continuously and incrementally updated over time, PipelineDB has the capability to consider the current time when updating the result of a continuous view. Queries that include a :code:`WHERE` clause with a temporal component relating to the **current time** are called **sliding-window queries**. The set of events that a sliding :code:`WHERE` clause filters or accepts perpetually changes over time.

由于 :ref:`流视图<continuous-views>` 是实时增量更新的，PipelineDB可以在更新流视图结果集时考虑当前时间。:code:`WHERE` 子句中包含与 **当前时间** 相关信息的查询被称作 **滑动窗口查询**。滑动的 :code:`WHERE` 子句过滤或接收到的 **event** 集合是随时间持续变化的。

..	There are two important components of a sliding :code:`WHERE` clause:

滑动 :code:`WHERE` 子句包含两个重要元素：

**clock_timestamp ( )**

	..	A built-in function that always returns the current timestamp.

	内置函数，总返回当前时间戳。

**arrival_timestamp**

	..	A special attribute of all incoming events containing the time at which PipelineDB received them, as described in :ref:`arrival-ordering`.

	**event** 的特殊属性，它包含 PipelineDB接收数据的时间，在 :ref:`arrival-ordering` 中有详细描述。

..	However, it is not necessary to explicitly add a :code:`WHERE` clause referencing these values. PipelineDB does this internally and it is only necessary to specify the :code:`sw` storage parameter in a continuous view's definition.

然而，我们无须显式添加 :code:`WHERE` 子句来引用这些值，PipelineDB已经在内部执行了，我们在定义流视图时指定 :code:`sw` 存储参数即可。

..	These concepts are probably best illustrated by an example.
下面的例子可以很好地阐明这些概念。


..	Examples

示例
------------

..	Even though sliding windows are a new concept for a SQL database, PipelineDB does not use any sort of new or proprietary windowing syntax. Instead, PipelineDB uses standard PostgreSQL 9.5 syntax. Here's a simple example:

即使滑动窗口对SQL数据库来说是一个新概念，PipelineDB没有引入任何新的或特有的窗口语法，而是使用PostgreSQL 9.5的标准语法，如下所示：

..	**What users have I seen in the last minute?**

**最近一分钟的用户**

.. code-block:: sql

	CREATE VIEW recent_users WITH (sw = '1 minute') AS
	   SELECT user_id::integer FROM stream;

..	Internally, PipelineDB will rewrite this query to the following:

可以通过如下SQL实现相同的逻辑：

.. code-block:: sql

  CREATE VIEW recent_users AS
     SELECT user_id::integer FROM stream
  WHERE (arrival_timestamp > clock_timestamp() - interval '1 minute');

.. note::
	..	PipelineDB allows users to manually construct a sliding window :code:`WHERE` clause when defining sliding-window continuous views, although it is recommended that :code:`sw` be used in order to avoid tedium.

	尽管 :code:`sw` 显得更加简洁，PipelineDB仍然允许用户通过 :code:`WHERE` 手动构造基于滑动窗口的流视图。

..	The result of a :code:`SELECT` on this continuous view would only contain the specific users seen within the last minute. That is, repeated :code:`SELECT` s would contain different rows, even if the continuous view wasn't explicitly updated.

上面的流视图只会包含最后一分钟的用户。也就是说，即使流视图中的数据没有更新，每次 :code:`SELECT` 查询仍然会返回不同的结果，

..	Let's break down what's going on with the :code:`(arrival_timestamp > clock_timestamp() - interval '1 minute')` predicate.

让我们对 :code:`(arrival_timestamp > clock_timestamp() - interval '1 minute')` 语句进行分解。

..	Each time :code:`clock_timestamp() - interval '1 minute'` is evaluated, it will return a timestamp corresponding to 1 minute in the past. Adding in :code:`arrival_timestamp` and :code:`>` means that this predicate will evaluate to :code:`true` if the :code:`arrival_timestamp` for a given event is greater than 1 minute in the past. Since the predicate is evaluated every time a new event is read, this effectively gives us a sliding window that is 1 minute width.

:code:`clock_timestamp() - interval '1 minute'` 每次执行时，它将返回一分钟前的时间戳。如果 :code:`arrival_timestamp` 大于一分钟前的时间， :code:`arrival_timestamp` and :code:`>` 将返回 :code:`true`。由于每次读取到新的 **event** 时这个不等式都会被执行，所以窗口中总是维系着最近一分钟的数据。

.. note::

	..	exposes the :code:`current_date`, :code:`current_time`, and :code:`current_timestamp` values to use within queries, but by design these don't work with sliding-window queries because they remain constant within a transaction and thus don't necessarily represent the current moment in time.

	虽然PipelineDB曝露了 :code:`current_date`、:code:`current_time`以及 :code:`current_timestamp` 来配合查询使用，但他们不是被设计用于滑动窗口的，因为他们在一次事务中是常量，所以不能准备地表示当前时间。


..	Sliding Aggregates

滑动聚合
-------------------

..	Sliding-window queries also work with aggregate functions. Sliding aggregates work by aggregating their inputs as much as possible, but without losing the granularity needed to know how to remove information from the window as time progresses. This partial aggregatation is all transparent to the user--only fully aggregated results will be visible within sliding-window aggregates.

滑动窗口也同聚合函数一起使用。滑动聚合可以尽可能地聚合输入数据，但不会随时间演进而丢失用于维系窗口的粒度。这部分聚合函数对用户是完全透明的，只有滑动聚合后的结果才对用户可见。

..	Let's look at a few examples:

让我们来看看下面的例子：

..	**How many users have I seen in the last minute?**

**最近一分钟的用户日志数**

.. code-block:: psql

	CREATE VIEW count_recent_users WITH (sw = '1 minute') AS
	   SELECT COUNT(*) FROM stream;

..	Each time a :code:`SELECT` is run on this continuous view, the count it returns will be the count of only the events seen within the last minute. For example, if events stopped coming in, the count would decrease each time a :code:`SELECT` was run on the continuous view. This behavior works for all of the :ref:`aggregates` that PipelineDB supports:

流视图中的 :code:`SELECT` 每次执行时，**count** 只会返回最近一分钟的计数。如果没有新数据写入，**count** 的值会随着流视图的计算而越来越小。滑动窗口以用于PipelineDB支持的所有 :ref:`流聚合<aggregates>` 函数中。

..	**What is the 5-minute moving average temperature of my sensors?**

**传感器近5分钟的平均温度**

.. code-block:: sql

	CREATE VIEW sensor_temps WITH (sw = '5 minutes') AS
	   SELECT sensor::integer, AVG(temp::numeric) FROM sensor_stream
	GROUP BY sensor;

..	**How many unique users have we seen over the last 30 days?**

**近30天的去重用户数**

.. code-block:: sql

	CREATE VIEW uniques WITH (sw = '30 days') AS
	   SELECT COUNT(DISTINCT user::integer) FROM user_stream;

..	**What is my server's 99th precentile response latency over the last 5 minutes?**

**服务器近5分钟延时的99分位数**

.. code-block:: sql

	CREATE VIEW latency WITH (sw = '5 minutes') AS
	   SELECT server_id::integer, percentile_cont(0.99)
	   WITHIN GROUP (ORDER BY latency::numeric) FROM server_stream
	GROUP BY server_id;

..	Temporal Invalidation

时间失效
-----------------------

..	Obviously, sliding-window rows in continuous views become invalid after a certain amount of time because they've become too old to ever be included in a continuous view's result. Such rows must thus be **garbage collected**, which can happen in two ways:

显然，流视图中的窗口期数据会在一段时间后失效，因为这些数据的时间已经小于窗口的下限。这些数据可以通过两种方式被 **垃圾回收**：

**Background invalidation**

	..	A background process similar to PostgreSQL's autovacuumer_ periodically runs and physically removes any expired rows from sliding-window continuous views.

	一个类似于PostgreSQL中 autovacuumer_ 的后台进程周期性地对基于滑动窗口的流视图中的过期数据执行物理删除。

.. _autovacuumer: http://www.postgresql.org/docs/current/static/runtime-config-autovacuum.html

**Read-time invalidation**

	..	When a continuous view is read with a :code:`SELECT`, any data that are too old to be included in the result are discarded on the fly while generating the result. This ensures that even if invalid rows still exist, they aren't actually included in any query results.

	当通过 :code:`SELECT` 读取流视图时，过期数据会在新数据产生时被 **丢弃** 。这样可以保证无效数据即使 **存在** 也不会被查询到。

	.. note::
		译者注：根据本人粗浅的理解，上面原文中的 **丢弃** 和 **存在** 分别代表 **逻辑删除** 和 **未被物理删除**，上面的 **Read-time invalidation** 和 **Background invalidation** 是相互配置来进行GC的，**Read-time invalidation** 先逻辑删除，然后 **Background invalidation** 执行物理删除。

-----------------------


step_factor
-------------------------

..	Internally, the materialization tables backing sliding-window queries are aggregated as much as possible. However, rows can't be aggregated down to the same level of granularity as the query's final output because data must be removed from aggregate results when it goes out of window.

支持滑动窗口查询的物化表会在内部尽可能聚合数据。然而，查询的粒度不能与原始数据的粒度一致，因为数据在超出窗口后会被移除。

..	For example, a sliding-window query that aggregates by hour may actually have minute-level aggregate data on disk so that only the last 60 minutes are included in the final aggregate result returned to readers. These internal, more granular aggregate levels for sliding-window queries are called "steps". An "overlay" view is placed over these step aggregates in order to perform the final aggregation at read time.

比如，一个基于小时的滑动窗口查询可能是由磁盘中分钟级别的数据聚合而成的，以便最近60分钟的数据都包含在最终返回给用户的聚合结果中。也就是说，用户滑动窗口查询的更小粒度的聚合等级被称作 **steps**。**overlay** 视图位于这些 **steps** 聚合中，以便更“平滑”地获取最终的聚合结果。

..	You have probably noticed at this point that step aggregates can be a significant factor in determining sliding-window query read performance, because each final sliding-window aggregate group will internally be composed of a number of steps. The number of steps that each sliding-window aggregate group will have is tunable via the **step_factor** parameter:

您可能已经注意到 **step** 聚合是影响滑动窗口查询性能的重要因素，因为每个滑动窗口聚合的分组内部都是由一系列 **steps** 组成的。 每个滑动窗口聚合的分组所包含的 **stpes** 的数量可以通过 **step_factor** 参数配置：

**step_factor**

  ..	An integer between 1 and 50 that specifices the size of a sliding-window step as a percentage of window size given by **sw**. A smaller **step_factor** will provide more granularity in terms of when data goes out of window, at the cost of larger on-disk materialization table size. A larger **step_factor** will reduce on-disk materialization table size at the expense of less out-of-window granularity.

  可以通过1～50的整数来指定滑动窗口 **step** 长度相对于 **sw** 窗口的百分比。**step_factor** 越小，数据离开窗口的时间精度就越高，物化表的磁盘开销越大。**step_factor** 越大，数据离开窗口的时间精度就越低，物化表的磁盘开销越小。

  .. note::

  	译者注：一言以蔽之，**step** 即滑动步长，**step_factor** 即滑动步长相对窗口长度的百分比。

..	Here's an example of using **step_factor** in conjunction with **sw** to aggregate over an hour with a step size of 30 minutes:

下面例子将 **step_factor** 结合 **sw** 使用，将1小时窗口长度的 **step** 设置为30分钟：


.. code-block:: sql

  CREATE VIEW hourly (WITH sw = '1 hour', step_factor = 50)
    AS SELECT COUNT(*) FROM stream;

-----------------------------

..	Now that you know how sliding-window queries work, it's probably a good time to learn about :ref:`joins`.

滑动窗口介绍完毕，现在是时候看看 :ref:`流关联<joins>` 了。
