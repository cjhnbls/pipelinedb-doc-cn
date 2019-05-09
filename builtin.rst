.. _builtin:

..  Built-in Functionality

内置函数
=======================

..	General

综述
----------

..	We strive to ensure that PipelineDB maintains full compatibility with PostgreSQL 10.1+ and 11.0+. As a result, all of `PostgreSQL's built-in functionality`_ is available to PipelineDB users.

我们努力保证PipelineDB同PostgreSQL的10.1+和11.0+版本保持完全兼容。因此，所有 `PostgreSQL内置函数`_ 都可以直接在PipelineDB中使用。

.. _`PostgreSQL's built-in functionality`: http://www.postgresql.org/docs/current/static/functions.html
.. _`PostgreSQL内置函数`: http://www.postgresql.org/docs/current/static/functions.html
.. _pg-built-in: http://www.postgresql.org/docs/current/static/functions.html

..	Aggregates

聚合
-------------

..	As one of PipelineDB's fundamental design goals is to **facilitate high-performance continuous aggregation**, PostgreSQL aggregate functions are fully supported for use in :ref:`continuous-views` (with a couple of rare exceptions). In addition to this large suite of standard aggregates, PipelineDB has also added some of its own :ref:`aggregates` that are purpose-built for continuous time-series data processing.

PipelineDB的设计初衷之一就是 **提供简便、高性能的流式聚合**，PostgreSQL的聚合函数在 :ref:`流视图<continuous-views>` 中是完全支持的（还存在少数几个罕见的异常）。除了这一大票标准的聚合，PipelineDB也为时序流数据处理添加了一些特有 :ref:`流聚合<aggregates>` 算子。

..	See :ref:`aggregates` for more information about some of PipelineDB's most useful features.

查看 :ref:`流聚合<aggregates>` 部分了解更多PipelineDB的实用特性。

..	PipelineDB-specific Types

PipelineDB特有数据类型
----------------------------

..	PipelineDB supports a number of native types for efficiently leveraging :ref:`probabilistic` on streams. You'll likely never need to manually create tables with these types but often they're the result of :ref:`aggregates`, so they'll be transparently created by :ref:`continuous-views`. Here they are:

PipelineDB提供了一系列原生数据类型来有力支持 :ref:`概率数据结构和算法<probabilistic>`。您不会在数据表中手动创建这些类型，它们会被 :ref:`流视图<continuous-views>` 显式地创建并作为 :ref:`流聚合<aggregates>` 的结果存储下来：

**bloom**
	:ref:`bloom-filter`

**cmsketch**
	:ref:`count-min-sketch`

**topk**
	:ref:`topk`

**hyperloglog**
	:ref:`hll`

**tdigest**
	:ref:`t-digest`

.. _pipeline-funcs:

..	PipelineDB-specific Functions

PipelineDB特有函数
---------------------------------

..	PipelineDB ships with a number of functions that are useful for interacting with these types. They are described below.

使用PipelineDB的特有函数可以很好地与它自己的数据类型进行交互，如下所示：

.. _bloom-funcs:

..	Bloom Filter Functions

Bloom Filter函数
------------------------------

.. note::
	译者注：建议您先看看这些概念

	- `Bloom Filter`_

	- `Count-Min Sketch`_

	- `Bloom Filter和Count-Min Sketch介绍`_

	- `HyperLogLog`_

	- `T-Digest`_

.. _`Bloom Filter`: https://zh.wikipedia.org/wiki/%E5%B8%83%E9%9A%86%E8%BF%87%E6%BB%A4%E5%99%A8
.. _`Count-Min Sketch`: https://en.wikipedia.org/wiki/Count%E2%80%93min_sketch
.. _`Bloom Filter和Count-Min Sketch介绍`: https://blog.csdn.net/u012315428/article/details/79338773
.. _`HyperLogLog`: http://www.rainybowe.com/blog/2017/07/13/%E7%A5%9E%E5%A5%87%E7%9A%84HyperLogLog%E7%AE%97%E6%B3%95/index.html
.. _`T-Digest`: https://op8867555.github.io/posts/2018-04-09-tdigest.html


**bloom_add ( bloom, expression )**

	..	Adds the given expression to the :ref:`bloom-filter`.

	将给定的表达式作用到 :ref:`bloom-filter` 上。

**bloom_cardinality ( bloom )**

	..	Returns the cardinality of the given :ref:`bloom-filter`. This is the number of **unique** elements that were added to the Bloom filter, with a small margin or error.

	返回 :ref:`bloom-filter` 的基数，即集合中元素 **去重后** 的个数，误差非常小。

**bloom_contains ( bloom, expression )**

	..	Returns true if the Bloom filter **probably** contains the given value, with a small false positive rate.

	当Bloom filter **可能** 包含给定的值时，会返回true，假阳率较小。

**bloom_intersection ( bloom, bloom, ... )**

	..	Returns a Bloom filter representing the intersection of the given Bloom filters.

	返回给定的多个Bloom filter的交集。

**bloom_union ( bloom, bloom, ... )**

	..	Returns a Bloom filter representing the union of the given Bloom filters.

	返回给定的多个Bloom filter的并集。

..	See :ref:`bloom-aggs` for aggregates that can be used to generate Bloom filters.

:ref:`bloom-aggs` 中的聚合函数可用于生成Bloom filter数据。

.. _topk-funcs:

..	Top-K Functions

Top-K 函数
---------------------------------

**topk_increment ( topk, expression )**

	..	Increments the frequency of the given expression within the given **topk** and returns the resulting :ref:`topk`.

	增加给定 **topk** 中给定表达式的频数，结果返回 :ref:`topk`。

**topk_increment ( topk, expression, weight )**

	..	Increments the frequency of the given expression by the specified weight within the given :ref:`topk` and returns the resulting :ref:`topk`.

	给 **topk** 中给定表达式增加指定权重的频数，结果返回 :ref:`topk`。

**topk ( topk )**

	..	Returns up to k tuples representing the given :ref:`topk` top-k values and their associated frequencies.

	返回给定 :ref:`topk` 及其相关频数的k个元组。

**topk_freqs ( topk )**

	..	Returns up to k frequencies associated with the given :ref:`topk` top-k most frequent values.

	返回给定 :ref:`topk` 相关的最大k个频数。

**topk_values ( topk )**

	..	Returns up to k values representing the given :ref:`topk` top-k most frequent values.

	返回给定 :ref:`topk` 相关的最大k个频率项。

..	See :ref:`topk-aggs` for aggregates that can be used to generate **topk** objects.

:ref:`topk-aggs` 可用于聚合并产生 **topk** 数据。

.. _cmsketch-funcs:

..	Frequency Functions

Frequency函数
------------------------------

**freq_add ( cmsketch, expression, weight )**

	..	Increments the frequency of the given expression by the specified weight within the given :ref:`count-min-sketch`.

	在给定的 :ref:`count-min-sketch` 上根据指定权重增加 **表达式** 的频数。

**freq ( cmsketch, expression )**

	..	Returns the number of times the value of **expression** was added to the given :ref:`count-min-sketch`, with a small margin of error.

	返回 **表达式** 在给定 :ref:`count-min-sketch` 上的频数，存在很小的误差。

**freq_norm ( cmsketch, expression )**

	Returns the normalized frequency of **expression** in the given :ref:`count-min-sketch`, with a small margin of error.

	返回 **表达式** 在给定 :ref:`count-min-sketch` 上的归一化频率，存在很小的误差

**freq_total ( cmsketch )**

	..	Returns the total number of items added to the given :ref:`count-min-sketch`.

	返回给定 :ref:`count-min-sketch` 中的对象总数。

..	See :ref:`cmsketch-aggs` for aggregates that can be used to generate **cmsketches**.

查看 :ref:`cmsketch-aggs` 张杰了解 **cmsketches** 的相关聚合操作。

.. _hll-funcs:

..	HyperLogLog Functions

HyperLogLog函数
-------------------------

**hll_add ( hyperloglog, expression )**

	..	Adds the given expression to the :ref:`hll`.

	将给定 **表达** 的结果添加给 :ref:`hll`。

**hll_cardinality ( hyperloglog )**

	..	Returns the cardinality of the given :ref:`hll`, with roughly a ~0.2% margin of error.

	返会给定 :ref:`hll` 的基数，大约有0.2%的误差。

**hll_union ( hyperloglog, hyperloglog, ... )**

	..	Returns a **hyperloglog** representing the union of the given **hyperloglog**.

	返回多个 **hyperloglog** 的并集。

	..	See :ref:`hll-aggs` for aggregates that can be used to generate **hyperloglog** objects.

	查看 :ref:`hll-aggs` 部分了解如何对 **hyperloglog** 进行聚合。

.. _tdigest-funcs:

..	Distribution Functions

Distribution函数
-----------------------

**dist_add ( tdigest, expression, weight )**

	..	Increments the frequency of the given expression by the given weight in the :ref:`t-digest`.

	通过给定 :ref:`t-digest<t-digest>` 中的权重增加给定 **表达式** 的频率。

**dist_cdf ( tdigest, expression )**

	..	Given a :ref:`t-digest`, returns the value of its cumulative-distribution function evaluated at the value of **expression**, with a small margin of error.

	给定一个 :ref:`t-digest`，返回其基于 **表达式** 评估的分布函数，有很小的误差。

**dist_quantile ( tdigest, float )**

	..	Given a **tdigest**, returns the value at the given quantile, **float**. **float** must be in :code:`[0, 1]`.

	给定一个 **tdigest**，返回 **float** 对应的分位数，**float** 区间为 :code:`[0, 1]`。

..	See :ref:`tdigest-aggs` for aggregates that can be used to generate **tdigest** objects.

查看 :ref:`tdigest-aggs` 学习 **tdigest** 聚合函数。

.. _misc-funcs:

..	Miscellaneous Functions

Miscellaneous函数
-----------------------------

**bucket_cardinality ( bucket_agg, bucket_id )**

	..	Returns the cardinality of the given **bucket_id** within the given **bucket_agg**.

	返回 **bucket_id** 在 **bucket_agg** 中的基数。

**bucket_ids ( bucket_agg )**

	..	Returns an array of all bucket ids contained within the given **bucket_agg**.

	以数组形式返回包含给定 **bucket_agg** 的所有 **bucket id**。

**bucket_cardinalities ( bucket_agg )**

  	..	Returns an array of cardinalities contained within the given **bucket_agg**, one for each bucket id.

	以数组形式返回给定 **bucket_agg** 中的基数，每个桶都会被计算到。

..	See :ref:`misc-aggs` for aggregates that can be used to generate **bucket_agg** objects.

查看 :ref:`misc-aggs` 了解更多 :ref:`misc-aggs` 的聚合函数。

**date_round ( timestamp, resolution )**

 	..	"Floors" a date down to the nearest **resolution** (or bucket) expressed as an interval. This is typically useful for summarization. For example, to summarize events into 10-minute buckets:

	对 **resolution** （或者桶）以一定的间隙 “向下取整”，您可以通郭一下操作将每10分钟的数据都归到一个桶中：

.. code-block:: sql

    CREATE VIEW v AS SELECT
      date_round(arrival_timestamp, '10 minutes') AS bucket_10m, COUNT(*) FROM stream
      GROUP BY bucket_10m;

**year ( timestamp )**

  	..	Truncate the given timestamp down to its **year**.

	截取给定时间戳对应的年。

**month ( timestamp )**

  	..	Truncate the given timestamp down to its **month**.

	截取给定时间戳对应的月。

**day ( timestamp )**

  	..	Truncate the given timestamp down to its **day**.

	截取给定时间戳对应的天。

**hour ( timestamp )**

  	..	Truncate the given timestamp down to its **hour**.

	截取给定时间戳对应的小时。

**minute ( timestamp )**

  	..	Truncate the given timestamp down to its **minute**.

	截取给定时间戳对应的分钟。

**second ( timestamp )**

  	..	Truncate the given timestamp down to its **second**.

	截取给定时间戳对应的秒。

**set_cardinality ( array )**

  	..	Returns the cardinality of the given set array. Sets can be built using **set_agg**.

	返回给定集合数组的基数，集合可用通过 **set_agg** 构造。

.. _operations:

Operational Functions
------------------------------------------

**pipelinedb.activate ( name )**

  	..	Acitvates the given continuous view or transform. See :ref:`activation-deactivation` for more information.

	激活给定的流视图或流转换，详情见 :ref:`激活和中止<activation-deactivation>`

**pipelinedb.deactivate ( name )**

	..	Deacitvates the given continuous view or transform. See :ref:`activation-deactivation` for more information.

	中止给定的流视图或流转换，详情见 :ref:`激活和中止<activation-deactivation>`

**pipelinedb.combine_table( continuous view name, table )**

	..	:ref:`combine` the rows from the given **table** into the given continuous view. **combine_table** uses the given continuous view's query definition to combine aggregate values from both relations with no loss of information.

	将数据从 **数据表** :ref:`组合<combine>` 到流视图中。 **组合表** 使用给定的流视图同数据表进行组合和聚合并产生结果

	..	**combine_table** can be used for purposes such as backfilling a continuous view (possibly running on a completely separate installation) by combining the backfilled rows into the "live" continuous view only once they have been fully populated.

	**组合表** 在回填流视图（可能是运行在一起完全隔离的环境中）时，组合形成的数据会在完全形成后回填到“实时的”的流视图中。

**pipelinedb.get_views ( )**

        ..	Returns the set of all continuous views.

		返回所有流视图组成的集合。

**pipelinedb.get_transforms ( )**

        ..	Returns the set of all continuous transforms.

		返回所有流转换组成的集合。

**pipelinedb.truncate_continuous_view ( name )**

  	..	Truncates all rows from the given continuous view.

	将流视图中的所有数据都清除。

**pipelinedb.version ( )**

	..	Returns a string containing all of the version information for your PipelineDB installation.

	返回包含所有与PipelineDB版本相关的安装信息。

..	System Views

系统视图
---------------------------

..	PipelineDB includes a number of system views for viewing useful information about your continuous views and transforms:

PipelineDB包含一些用于查看流视图和流转换信息的视图。

**pipelinedb.views**

..	Describes :ref:`continuous-views`.

查看 :ref:`流视图<continuous-views>` 相关信息。

.. code-block:: psql

	  View "pipelinedb.views"
 	Column |  Type   |
	-------+---------+
 	id     | oid     |
 	schema | text    |
 	name   | text    |
 	active | boolean |
 	query  | text    |

**pipelinedb.transforms**

..	Describes :ref:`continuous-transforms`.

查看 :ref:`流转换<continuous-transforms>` 相关信息。

.. code-block:: psql

	  View "pipelinedb.transforms"
 	Column |  Type   |
	-------+---------+
 	id     | oid     |
 	schema | text    |
 	name   | text    |
 	active | boolean |
	tgfunc | text    |
	tgargs | text[]  |
 	query  | text    |

**pipelinedb.stream_readers**

..	For each stream, shows all of the continuous queries that are reading from it.

显示基于某个流产生的所有流视图。

.. code-block:: psql

     View "pipelinedb.transforms"
  Column             |  Type     |
  -------------------+-----------+
  stream             | text      |
  continuous_queries | text[]    |

..	More system views are available for viewing :ref:`stats` for PipelineDB processes, continuous queries, and streams.

更多的系统视图可以通过 :ref:`内置统计视图<stats>` 查看，其中包含PipelineDB进程，流查询和流的相关信息。
