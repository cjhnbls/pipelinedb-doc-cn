.. _aggregates:

..  Continuous Aggregates

流聚合
======================

..	One of the fundamental goals of PipelineDB is to **facilitate high-performance continuous aggregation**, so not suprisingly aggregates are a central component of PipelineDB's utility. Continuous aggregates can be very powerful--in the most general sense they make it possible to keep the amount of data persisted in PipelineDB constant relative to the amount of data that has been pushed through it. This can enable sustainable and very high data throughput on modest hardware.

PipelineDB最核心的追求之一就是 **促进高性能的连续聚合**，所以聚合函数毫无疑问是PipelineDB的核心功能。连续聚合在大多数通用场景是非常有用的，它使得PipelineDB中持久化的数据始终与写入的数据保持同步。它可以通过一定的硬件实现稳定和高吞度量的服务。

..	Continuous aggregates are **incrementally updated** in real time as new events are read by the the continuous view that they're a part of. For simple aggregates such as count_ and sum_, it is easy to see how their results can be incrementally updated--just add the new value to the existing result.

连续聚合是随着流视图新 **event** 的生成实时 **增量更新的**。对于如 count_ 和 sum_ 之类的简单聚合，我们很容易理解结果是如何增量更新的--将新值累加到已有结果上而已。

..	But for more complicated aggregates, such as avg_, stddev_, percentile_cont_, etc., more advanced infrastructure is required to support efficient incremental updates, and PipelineDB handles all of that complexity for you transparently.

但对更加复杂的聚合而言，如  avg_, stddev_, percentile_cont_ 等，需要更优化的架构来支持高效的增量更新，PipelineDB在内部自动实现了这些复杂的逻辑。

..	Below you'll find a description of all the aggregates that PipelineDB supports. A few of them behave slightly differently than their standard counterparts in order to efficiently operate on infinite streams of data. Such aggregates have been annotated with an explanation of how exactly their behavior differs.

下面是所有PipelineDB支持的聚合函数的说明。有一些函数与标准的聚合函数有略微的差别以高效处理源源不断的流式数据，文中已经标注出了这部分函数的区别。

.. note::
	..	It may be helpful for you to consult the excellent `PostgreSQL aggregates`_ documentation.

	阅读下面的说明前建议您想阅读 `PostgreSQL聚合函数`_ 文档。


..	.. _`PostgreSQL aggregates`: http://www.postgresql.org/docs/current/static/functions-aggregate.html
.. _`PostgreSQL聚合函数`: http://www.postgresql.org/docs/current/static/functions-aggregate.html

----------------------------

.. _bloom-aggs:

Bloom Filter Aggregates
-----------------------------

**bloom_agg ( expression )**

	..	Adds all input values to a :ref:`bloom-filter`

	将所有输入的值添加到 :ref:`bloom-filter`

**bloom_agg ( expression , p , n )**

	..	Adds all input values to a Bloom filter and sizes it according to the given parameters. **p** is the desired false-positive rate, and **n** is the expected number of unique elements to add.

	将所有输入的值添加到Bloom filter中并且根据给定参数计算大小。**p** 是期待的假阳率，**n** 是期望添加的唯一元素数量。

**bloom_union_agg ( bloom filter )**

	..	Takes the union of all input Bloom filters, resulting in a single Bloom filter containing all of the input Bloom filters' information.

	对所有输入的Bloom filters取并集。

**bloom_intersection_agg ( bloom filter )**

	..	Takes the intersection of all input Bloom filters, resulting in a single Bloom filter containing only the information shared by all of the input Bloom filters.

	对所有输入的Bloom filters取交集。

..	See :ref:`bloom-funcs` for functionality that can be used to manipulate Bloom filters.

其它与Bloom filters相关的函数可查看 :ref:`Bloom Filter函数<bloom-funcs>`。

.. _cmsketch-aggs:

Frequency Tracking Aggregates
-----------------------------

**freq_agg ( expression )**

	..	Adds all input values to an internal :ref:`count-min-sketch`, enabling efficient online computation of the frequency of each input expression.

	将所有输入的值添加到 :ref:`count-min-sketch` 中，实现实时高效计算所有输入的频数。

**freq_agg ( expression, epsilon, p )**

	..	Same as above, but accepts **epsilon** and **p** as parameters for the underlying **cmsketch**. **epsilon** determines the acceptable error rate of the **cmsketch**, and defaults to **0.002** (0.2%). **p** determines the confidence, and defaults to **0.995** (99.5%). Lower **epsilon** and **p** will result in smaller **cmsketch** structures, and vice versa.

	同上，可以给 **cmsketch** 指定 **epsilon** 和 **p** 参数。 **epsilon** 决定 **cmsketch** 可接受的错误率，默认为 **0.002** (0.2%)。**p** 代表可信度，默认为 **0.995** (99.5%)。**epsilon** 和 **p** 越小，**cmsketch** 的空间占用越小，反之亦然。

**freq_merge_agg ( count-min sketch )**

	..	Merges all input Count-min sketches into a single one containing all of the information of the input Count-min sketches.

	对所有输入的cms取并集。

..	See :ref:`cmsketch-funcs` for functionality that can be used to manipulate Count-Min sketches.

其它与Count-Min sketches相关的函数可查看 :ref:`Frequency函数<cmsketch-funcs>`。

.. _topk-aggs:

Top-K Aggregates
--------------------------------------

**topk_agg ( expression , k )**

	..	Tracks the top k input expressions by adding all input values to a :ref:`topk` data structure sized for the given **k**, incrementing each value's count by **1** each time it is added.

	根据输入的参数 **k** 对输入的 **expression** 取 :ref:`topk`，每添加一个元素，其对应的值会加1。

**topk_agg (expression, k, weight )**

	..	Same as above, but associates the given weight to the input expression (rather than a default weight of 1).

	同上，但会为 **expression** 赋予指定权重（而不是默认的1）。

**topk_merge_agg ( topk )**

	..	Merges all **topk** inputs into a single **topk** data structure.

	将所有的 **topk** 合并到一个 **topk** 中。

..	See :ref:`topk-funcs` for functionality that can be used to manipulate **topk** objects.

查看 :ref:`Top-K 函数<topk-funcs>` 了解更多 **topk** 相关函数。

.. _hll-aggs:

HyperLogLog Aggregates
-----------------------------

**hll_agg ( expression )**

	..	Adds all input values to a :ref:`hll`.

	将所有输入的 **expression** 添加到 :ref:`hll` 中。

**hll_agg ( expression, p )**

	..	Adds all input values to a :ref:`hll` with the given **p**. A larger **p** reduces the HyperLogLog's error rate, at the expense of a larger size.

	将所有输入的 **expression** 添加到 :ref:`hll` 中，指定参数 **p**， **p** 越大，:ref:`hll` 的偏差越小，内存占用越大。

**hll_union_agg ( hyperloglog )**

	..	Takes the union of all input HyperLogLogs, resulting in a single HyperLogLog that contains all of the information of the input HyperLogLogs.

	对所有输入的 HyperLogLogs 取并集。

..	See :ref:`hll-funcs` for functionality that can be used to manipulate HyperLogLog objects.

查看 :ref:`HyperLogLog函数<hll-funcs>` 了解更多 **HyperLogLog** 相关函数。

.. _tdigest-aggs:

Distribution Aggregates
-------------------------------

**dist_agg ( expression )**

	..	Adds all input values to a :ref:`t-digest` in order to track the distribution of all input expressions.

	将所有输入的值添加到 :ref:`t-digest` 中来追溯数据分布。

**dist_agg ( expression, compression )**

	..	Same as above, but builds the underyling **tdigest** using the given **compression**. **compression** must be an integer in the range :code:`[20, 1000]`. A higher value for **compression** will yield a larger **tdigest** with but with more precision than a smaller **tdigest** with a lower **compression** value.

	同上，但使用给定的 **compression** 构建底层 **tdigest**。**compression** 必须是 :code:`[20, 1000]` 中的整数。**compression** 越大，**tdigest** 空间占用越高，精确度也越高。

..	See :ref:`tdigest-funcs` for functionality that can be used to manipulate **tdigest** objects.

查看 :ref:`Distribution函数<tdigest-funcs>` 了解更多 **tdigest** 相关函数。

.. _misc-aggs:

Miscellaneous Aggregates
----------------------------

**bucket_agg ( expression , bucket_id )**

  ..	Adds 4-byte hashes of each input expression to the bucket with the given . Each hash may only be present precisely once in one bucket at any given time. Buckets can therefore be thought of as exclusive sets of hashes of the input expressions.

  根据 **bucket_id** 为每个输入的 **expression** 添加4字节的哈希值。在任意给定时间，每个哈希值可能只在一个桶中出现一次。因此，这些桶可以被认定为输入的 **expressions** 的排它散列集。

**bucket_agg ( expression , bucket_id , timestamp )**

  ..	Same as above, but allows a **timestamp** expression to determine bucket entry order. That is, only a value's *latest* entry will cause it to change buckets.

  同上，但允许通过 **timestamp** 表达式来决定桶的条目顺序。也就是说，只有一个值 *最后的* 条目才会使它切换到别的桶中。

..	See :ref:`misc-funcs` for functionality that can be used to manipulate **bucket_agg** objects.

查看 :ref:`Miscellaneous函数<misc-funcs>` 了解更多 **bucket_agg** 相关函数。

**exact_count_distinct ( expression )**

  Counts the exact number of distinct values for the given expression. Since **count distinct** used in continuous views implicitly uses HyperLogLog for efficiency, **exact_count_distinct** can be used when the small margin of error inherent to using HyperLogLog is not acceptable.

.. important:: **exact_count_distinct** must store all unique values observed in order to determine uniqueness, so it is not recommended for use when many unique values are expected.

**first_values ( n ) WITHIN GROUP (ORDER BY sort_expression)**

  An ordered-set aggregate that stores the first **n** values ordered by the provided sort expression.

.. note:: See also: :ref:`pipeline-funcs`, which explains some of the PipelineDB's non-aggregate functionality for manipulating Bloom filters, Count-min sketches, HyperLogLogs and T-Digests. Also, check out :ref:`probabilistic` for more information about what they are and how you can leverage them.

**keyed_max ( key, value )**

	Returns the **value** associated with the "highest" **key**.

**keyed_min ( key, value )**

	Returns the **value** associated with the "lowest" **key**.

.. _set-agg:

**set_agg ( expression )**

  Adds all input values to a set.

See :ref:`misc-funcs` for functionality that can be used to manipulate sets.

------------------------------------

.. _combine:

Combine
------------

Since PipelineDB can incrementally update aggregate values, it has the capability to combine existing aggregates using more information than simply their current raw values. For example, combining multiple averages isn't simply a matter of taking the average of the averages. Their weights must be taken into account.

For this type of operation, PipelineDB exposes the special **combine** aggregate. Its description is as follows:

**combine ( aggregate column )**

	Given an aggregate column, combines all values into a single value as if all of the individual aggregates' inputs were aggregated a single time.

.. note:: **combine** only works on aggregate columns that belong to continuous views.

Let's look at an example:

.. code-block:: psql

  postgres=# CREATE VIEW v AS SELECT g::integer, AVG(x::integer) FROM stream GROUP BY g;
  CREATE VIEW
  postgres=# INSERT INTO stream (g, x) VALUES (0, 10), (0, 10), (0, 10), (0, 10), (0, 10);
  INSERT 0 5
  postgres=# INSERT INTO stream (g, x) VALUES (1, 20);
  INSERT 0 1
  postgres=# SELECT * FROM v;
   g |         avg
  ---+---------------------
   0 | 10.0000000000000000
   1 | 20.0000000000000000
  (2 rows)

  postgres=# SELECT avg(avg) FROM v;
           avg
  ---------------------
   15.0000000000000000
  (1 row)

  postgres=# -- But that didn't take into account that the value of 10 weighs much more,
  postgres=# -- because it was inserted 5 times, whereas 20 was only inserted once.
  postgres=# -- combine() will take this weight into account
  postgres=#
  postgres=# SELECT combine(avg) FROM v;
         combine
  ---------------------
   11.6666666666666667
  (1 row)

  postgres=# -- There we go! This is the same average we would have gotten if we ran
  postgres=# -- a single average on all 6 of the above inserted values, yet we only
  postgres=# -- needed two rows to do it.


------------------------------

General Aggregates
----------------------

**array_agg ( expression )**

	Input values, including nulls, concatenated into an array

.. _avg:

**avg ( expression )**

	The average of all input values

**bit_and ( expression )**

	The bitwise AND of all non-null input values, or null if none

**bit_or ( expression )**

	The bitwise OR of all non-null input values, or null if none

**bool_and ( expression )**

	True if all input values are true, otherwise false

**bool_or ( expression )**

	True if at least one input value is true, otherwise false

.. _count:

**count ( * )**

	Number of input rows

**count ( DISTINCT expression)**

	Number of rows for which **expression** is distinct.

	.. note:: Counting the distinct number of expressions on an infinite stream would require infinite memory, so continuous views use :ref:`hll` to accomplish distinct counting in constant space and time, at the expense of a small margin of error. Empirically, PipelineDB's implementation of :ref:`hll` has an error rate of ~0.81%. For example, **count distinct** might show :code:`1008` when the actual number of unique expressions was :code:`1000`.

**count ( expression )**

	Number of rows for which **expression** is non-null.

**every ( expression )**

	Equivalent to **bool_and**

**json_agg ( expression )**

	Aggregates values as a JSON array

**json_object_agg ( key, value )**

	Aggregates **key**-**value** pairs as a JSON object

**jsonb_agg ( expression )**

	Aggregates values as a JSONB array

**jsonb_object_agg ( key, value )**

	Aggregates **key**-**value** pairs as a JSONB object

**max ( expression )**

	Maximum value of expression across all input values

**min ( expression )**

	Minimum value of expression across all input values

**string_agg ( expression, delimiter )**

	Input values concatenated into a string, separated by **delimiter**

.. _sum:

**sum ( expression )**

	Sum of **expression** across all input values

----------------------------

Statistical Aggregates
-------------------------

**corr ( y, x )**

	Correlation coefficient

**covar_pop ( y, x )**

	Population covariance

**covar_samp ( y, x )**

	Sample covariance

**regr_avgx ( y, x )**

	Average of the independent variable :code:`(sum(x)/N)`

**regr_avgy ( y, x )**

	Average of the independent variable :code:`(sum(y)/N)`

**regr_count ( y, x )**

	Number of input rows in which both expressions are non-null

**regr_intercept ( y, x )**

	y-intercept of the least-squares-fit linear equation determined by the (x, y) pairs

**regr_r2 ( y, x )**

	Square of the correlation coefficient

**regr_slope ( y, x )**

	Slope of the least-squares-fit linear equation determined by the (x, y) pairs

**regr_sxx ( y, x )**

	:code:`sum(X^2) - sum(X)^2/N` -- sum of squares of the independent variable

**regr_sxy ( y, x )**

	:code:`sum(X*Y) - sum(X) * sum(Y)/N` -- sum of products of independent times dependent variable

**regr_syy ( y, x )**

	:code:`sum(Y^2) - sum(Y)^2/N` -- sum of squares of the independent variable

.. _stddev:

**stddev ( expression )**

	Sample standard deviation of the input values

**stddev_pop ( expression )**

	Population standard deviation of the input values

**variance ( expression )**

	Sample variance of the input values (square of the sample standard deviation)

**var_pop ( expression )**

	Population variance of the input values (square of the population standard deviation)

----------------------------

Ordered-set Aggregates
------------------------

**ordered-set** aggregates apply ordering to their input in order to obtain their results, so they use the :code:`WITHIN GROUP` clause. Its syntax is as follows:

.. code-block:: sql

	aggregate_name ( [ expression [ , ... ] ] ) WITHIN GROUP ( order_by_clause )

Let's look at a couple examples.

Compute the 99th percentile of **value**:

.. code-block:: sql

	SELECT percentile_cont(0.99) WITHIN GROUP (ORDER BY value) FROM some_table;

Or with a continuous view:

.. code-block:: sql

	CREATE VIEW percentile AS
	SELECT percentile_cont(0.99) WITHIN GROUP (ORDER BY value::float8)
	FROM some_stream;

.. _percentile_cont:

**percentile_cont ( fraction )**

	Continuous percentile: returns a value corresponding to the specified fraction in the ordering, interpolating between adjacent input items if needed

**percentile_cont ( array of fractions )**

	Multiple continuous percentile: returns an array of results matching the shape of the fractions parameter, with each non-null element replaced by the value corresponding to that percentile

	.. note:: Computing percentiles on infinite streams would require infinite memory, so both forms of **percentile_cont**, when used by continuous views, use :ref:`t-digest` as a way to estimate percentiles with a very high degree of accuracy. In general, percentiles in continuous views are more accurate the closer they are to the upper or lower bounds of :code:`[0, 1)`.

----------------------------

Hypothetical-set Aggregates
-------------------------------

**hypothetical-set** aggregates take an expression and compute something about it within the context of a set of input rows. For example, **rank(2)** computes the :code:`rank` of :code:`2` within the context of whatever the input rows end up being.

The hypothetical-set aggregates use the :code:`WITHIN GROUP` clause to define the input rows. Its syntax is as follows:

.. code-block:: sql

	aggregate_name ( [ expression [ , ... ] ] ) WITHIN GROUP ( order_by_clause )

Here is an example of of a hypothetical-set aggregate being used by a continuous view:

.. code-block:: sql

	CREATE VIEW continuous_rank AS
	SELECT rank(42) WITHIN GROUP (ORDER BY value::float8)
	FROM some_stream;

This continuous view will continuously update the rank of :code:`42` given all of the events it has read.

**rank ( arguments )**

	Rank of the hypothetical row, with gaps for duplicate rows

.. _dense-rank:

**dense_rank ( arguments )**

	Rank of the hypothetical row, without gaps

	.. note:: Computing the hypothetical **dense_rank** of a value given an infinite stream of values would require infinite memory, so continuous views use :ref:`hll` to do it in constant time and space, at the expense of a small margin of error. Empirically, PipelineDB's implementation of :ref:`hll` has an error rate of ~0.2%. In other words, **dense_rank (1000)** in a continuous view might show 998 when the actual number of unique lower-ranking values seen was :code:`1000`.

**percent_rank ( arguments )**

	Relative rank of the hypothetical row, ranging from 0 to 1

**cume_dist ( arguments )**

	Relative rank of the hypothetical row, ranging from 1/N to 1

----------------------------

Unsupported Aggregates
---------------------------------

**mode ( )**

	Future releases of PipelineDB will include an implementation of an online mode estimation algorithm, but for now it's not supported

**percentile_disc ( arguments )**

	Given an input percentile (such as 0.99), **percentile_disc** returns the very first value in the input set that is within that percentile. This requires actually sorting the input set, which is obviously impractical on an infinite stream, and doesn't even allow for a highly accurate estimation algorithm such as the one we use for **percentile_cont**.

**xmlagg ( xml )**

	:(

**<aggregate_name> (DISTINCT expression)**

	Only the :code:`count` aggregate function is supported with a :code:`DISTINCT` expression as noted above in the General Aggregates section. In future releases, we might leverage :ref:`bloom-filter` to allow :code:`DISTINCT` expressions for all aggregate functions.
