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

  ..    Same as above, but allows a **timestamp** expression to determine bucket entry order. That is, only a value's *latest* entry will cause it to change buckets.

  同上，但允许通过 **timestamp** 表达式来决定桶的条目顺序。也就是说，只有一个值 *最后的* 条目才会使它切换到别的桶中。

..	See :ref:`misc-funcs` for functionality that can be used to manipulate **bucket_agg** objects.

查看 :ref:`Miscellaneous函数<misc-funcs>` 了解更多 **bucket_agg** 相关函数。

**exact_count_distinct ( expression )**

  ..    Counts the exact number of distinct values for the given expression. Since **count distinct** used in continuous views implicitly uses HyperLogLog for efficiency, **exact_count_distinct** can be used when the small margin of error inherent to using HyperLogLog is not acceptable.

  对给定的 **expression** 进行精确的去重计数。由于流视图中的 **count distinct** 内部使用HyperLogLog以提高效率，如果不能接受Hyperloglog产生的小概率误差，可以使用 **exact_count_distinct**。

..  .. important:: **exact_count_distinct** must store all unique values observed in order to determine uniqueness, so it is not recommended for use when many unique values are expected.

.. important:: **exact_count_distinct** 必须存储所有去重后的值以确定值的唯一性，所以不建议在基数很大的情况下使用。

**first_values ( n ) WITHIN GROUP (ORDER BY sort_expression)**

  ..    An ordered-set aggregate that stores the first **n** values ordered by the provided sort expression.

  一个有序的聚合集合，按序保存前 **n** 个值。

..  .. note:: See also: :ref:`pipeline-funcs`, which explains some of the PipelineDB's non-aggregate functionality for manipulating Bloom filters, Count-min sketches, HyperLogLogs and T-Digests. Also, check out :ref:`probabilistic` for more information about what they are and how you can leverage them.

.. note:: 可见 :ref:`PipelineDB特有函数<pipeline-funcs>`，其中介绍了与Bloom filters, Count-min sketches, HyperLogLogs 以及 T-Digests相关的非聚合函数。您可以查看 :ref:`概率数据结构和算法<probabilistic>` 了解其原理及使用方法。

**keyed_max ( key, value )**

	.. Returns the **value** associated with the "highest" **key**.

    返回与 “最高的” **key** 相关的 **value**。

**keyed_min ( key, value )**

	.. Returns the **value** associated with the "lowest" **key**.

    返回与 “最低的” **key** 相关的 **value**。

.. _set-agg:

**set_agg ( expression )**

  .. Adds all input values to a set.

  将所有输入值加入到集合中。

.. See :ref:`misc-funcs` for functionality that can be used to manipulate sets.

查看 :ref:`Miscellaneous函数<misc-funcs>` 了解更多可用于操作集合的函数。

------------------------------------

.. _combine:

Combine
------------

..  Since PipelineDB can incrementally update aggregate values, it has the capability to combine existing aggregates using more information than simply their current raw values. For example, combining multiple averages isn't simply a matter of taking the average of the averages. Their weights must be taken into account.

由于PipelineDB实现增量更新聚合后的值，所以它可以将当前数据之外的信息同已有的聚合相结合。比如，我们无法在不加权的情况下通过对各个子区间的平均值取平均值来算出总区间的平均值。

..  For this type of operation, PipelineDB exposes the special **combine** aggregate. Its description is as follows:

对于这类操作，PipelineDB提供 **combine** 聚合，具体功能如下：

**combine ( aggregate column )**

	.. Given an aggregate column, combines all values into a single value as if all of the individual aggregates' inputs were aggregated a single time.

    给定一个聚合列，将所有值结合到单个值中，如同所有独立的聚合任务的输入同时输入到了当前的聚合任务中。

..  .. note:: **combine** only works on aggregate columns that belong to continuous views.

.. note:: **combine** 只能作用与流视图的聚合列上。

..  Let's look at an example:

下面是一些例子：

..  .. code-block:: psql

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

    postgres=# -- 上面这种求平均值的方式没有考虑到：10写入了5次，而20只写入了1次。
    postgres=#
    postgres=# SELECT combine(avg) FROM v;
           combine
    ---------------------
     11.6666666666666667
    (1 row)

    postgres=# -- 如上所示，combine(avg)的计算方式是（10*5+20*1）/（5+1），而不仅仅是（10+20）/2。

------------------------------

  General Aggregates
----------------------

**array_agg ( expression )**

	.. Input values, including nulls, concatenated into an array

    将包括null在内的所有值连接到数组中。

.. _avg:

**avg ( expression )**

	.. The average of all input values

    取平均值。

**bit_and ( expression )**

	.. The bitwise AND of all non-null input values, or null if none

    对所有非空输入执行 **按位与** 操作，无输入的情况下按照null处理。

**bit_or ( expression )**

	.. The bitwise OR of all non-null input values, or null if none

    对所有非空输入执行 **按位或** 操作，无输入的情况下按照null处理。

**bool_and ( expression )**

	.. True if all input values are true, otherwise false

    输入全部为true时才返回true，否则为false。

**bool_or ( expression )**

	.. True if at least one input value is true, otherwise false

    只要输入中有一个为true，就返回true。

.. _count:

**count ( * )**

	.. Number of input rows

    计算行数。

**count ( DISTINCT expression)**

	.. Number of rows for which **expression** is distinct.

    **expression** 去重后的行数。

	.. .. note:: Counting the distinct number of expressions on an infinite stream would require infinite memory, so continuous views use :ref:`hll` to accomplish distinct counting in constant space and time, at the expense of a small margin of error. Empirically, PipelineDB's implementation of :ref:`hll` has an error rate of ~0.81%. For example, **count distinct** might show :code:`1008` when the actual number of unique expressions was :code:`1000`.

	.. note:: 在连续不断的流上进行去重计数需要无限大的内存，所以流视图通过 :ref:`hll` 以常数空间和时间的代价完成去重计数任务，代价就是会有小概率的误差。通常情况下，PipelineDB的 :ref:`hll` 有大约0.81%的误差。比如，当去重后的实际数量为 :code:`1000` 时，**count distinct** 的结果可能为 :code:`1008`。


**count ( expression )**

	.. Number of rows for which **expression** is non-null.

    **expression** 的非空个数。

**every ( expression )**

	.. Equivalent to **bool_and**

    同 **bool_and**。

**json_agg ( expression )**

	.. Aggregates values as a JSON array

    将结果聚合为JSON数组。

**json_object_agg ( key, value )**

	.. Aggregates **key**-**value** pairs as a JSON object

    将键值对聚合为JSON。

**jsonb_agg ( expression )**

	.. Aggregates values as a JSONB array

    将结果聚合为JSONB数组。

**jsonb_object_agg ( key, value )**

	.. Aggregates **key**-**value** pairs as a JSONB object

    将键值对聚合为JSONB。

**max ( expression )**

	.. Maximum value of expression across all input values

    对 **expression** 取最大值。

**min ( expression )**

	.. Minimum value of expression across all input values

    对 **expression** 取最小值。

**string_agg ( expression, delimiter )**

	.. Input values concatenated into a string, separated by **delimiter**

    将 **expression** 以 **delimiter** 连接为字符串。

.. _sum:

**sum ( expression )**

    ..  Sum of **expression** across all input values

    对所有输入的 **expression** 求和。

----------------------------

Statistical Aggregates
-------------------------

**corr ( y, x )**

	.. Correlation coefficient

    相关系数。

**covar_pop ( y, x )**

	.. Population covariance

    总体协方差。

**covar_samp ( y, x )**

	.. Sample covariance

    样本协方差。

**regr_avgx ( y, x )**

	.. Average of the independent variable :code:`(sum(x)/N)`

    自变量平均值：:code:`(sum(x)/N)`。

**regr_avgy ( y, x )**

	.. Average of the independent variable :code:`(sum(y)/N)`

    自变量平均值：:code:`(sum(y)/N)`。

**regr_count ( y, x )**

	.. Number of input rows in which both expressions are non-null

    x,y都不为空的数量。

**regr_intercept ( y, x )**

	.. y-intercept of the least-squares-fit linear equation determined by the (x, y) pairs

    基于（x，y），按照最小二乘法拟合得到的方程的y轴截距。

**regr_r2 ( y, x )**

	.. Square of the correlation coefficient

    相关系数的平方。

**regr_slope ( y, x )**

	.. Slope of the least-squares-fit linear equation determined by the (x, y) pairs

    基于（x，y），按照最小二乘法拟合得到的方程的斜率。

**regr_sxx ( y, x )**

	.. :code:`sum(X^2) - sum(X)^2/N` -- sum of squares of the independent variable

    x的离差平方和，即：:code:`sum(X^2) - sum(X)^2/N`。

**regr_sxy ( y, x )**

	.. :code:`sum(X*Y) - sum(X) * sum(Y)/N` -- sum of products of independent times dependent variable

    x,y的离差积和，即：:code:`sum(X*Y) - sum(X) * sum(Y)/N`。

**regr_syy ( y, x )**

	.. :code:`sum(Y^2) - sum(Y)^2/N` -- sum of squares of the independent variable

    y的离差平方和，即：:code:`sum(Y^2) - sum(Y)^2/N`

.. _stddev:

**stddev ( expression )**

	.. Sample standard deviation of the input values

    样本标准差。

**stddev_pop ( expression )**

	.. Population standard deviation of the input values

    总体标准差。

**variance ( expression )**

	.. Sample variance of the input values (square of the sample standard deviation)

    样本方差，即样本标准差的平方。

**var_pop ( expression )**

	.. Population variance of the input values (square of the population standard deviation)

    整体方差，即整体标准差的平方。

----------------------------

Ordered-set Aggregates
------------------------

..  **ordered-set** aggregates apply ordering to their input in order to obtain their results, so they use the :code:`WITHIN GROUP` clause. Its syntax is as follows:

**ordered-set** 聚合对输入进行排序并得到结果，所以用 :code:`WITHIN GROUP` 语句。语法如下：

.. code-block:: sql

	aggregate_name ( [ expression [ , ... ] ] ) WITHIN GROUP ( order_by_clause )

..  Let's look at a couple examples.

下面是一些例子：

..  Compute the 99th percentile of **value**:

计算99分位数：

.. code-block:: sql

	SELECT percentile_cont(0.99) WITHIN GROUP (ORDER BY value) FROM some_table;

..  Or with a continuous view:

应用于流视图：

.. code-block:: sql

	CREATE VIEW percentile AS
	SELECT percentile_cont(0.99) WITHIN GROUP (ORDER BY value::float8)
	FROM some_stream;

.. _percentile_cont:

**percentile_cont ( fraction )**

	.. Continuous percentile: returns a value corresponding to the specified fraction in the ordering, interpolating between adjacent input items if needed

    流式百分位数：返回对应排序中特定分位的值。如有必要，可在相邻的输入项间进行插入值。

**percentile_cont ( array of fractions )**

	.. Multiple continuous percentile: returns an array of results matching the shape of the fractions parameter, with each non-null element replaced by the value corresponding to that percentile

    多重流式百分位数：返回与分数参数形状匹配的结果数组，将每个非空元素替换为其对应的百分位数。

	.. .. note:: Computing percentiles on infinite streams would require infinite memory, so both forms of **percentile_cont**, when used by continuous views, use :ref:`t-digest` as a way to estimate percentiles with a very high degree of accuracy. In general, percentiles in continuous views are more accurate the closer they are to the upper or lower bounds of :code:`[0, 1)`.

    .. note:: 在连续不断的流上计算百分位数需要无限大的内存，因此当流视图使用 **percentile_cont** 的两种形式时，都使用 :ref:`t-digest` 来计算高精度的百分位数。通常，流视图中的百分位数越接近 :code:`[0, 1)` 的上下界，结果就越精确。

----------------------------

Hypothetical-set Aggregates
-------------------------------

..  **hypothetical-set** aggregates take an expression and compute something about it within the context of a set of input rows. For example, **rank(2)** computes the :code:`rank` of :code:`2` within the context of whatever the input rows end up being.

**hypothetical-set** 聚合通过一个 **表达式** 在上下文中进行计算。比如，**rank(2)** 会计算 :code:`2` 的 :code:`rank`，无论输入的是什么。

..  The hypothetical-set aggregates use the :code:`WITHIN GROUP` clause to define the input rows. Its syntax is as follows:

hypothetical-set聚合通过 :code:`WITHIN GROUP` 语句来定义输入，语法如下：

.. code-block:: sql

	aggregate_name ( [ expression [ , ... ] ] ) WITHIN GROUP ( order_by_clause )

..  Here is an example of of a hypothetical-set aggregate being used by a continuous view:

下面是一些hypothetical-set聚合在流视图上应用的例子：

.. code-block:: sql

	CREATE VIEW continuous_rank AS
	SELECT rank(42) WITHIN GROUP (ORDER BY value::float8)
	FROM some_stream;

..  This continuous view will continuously update the rank of :code:`42` given all of the events it has read.

流视图将不断更新 :code:`42` 的 :code:`rank`。

**rank ( arguments )**

	Rank of the hypothetical row, with gaps for duplicate rows

    行的rank，存在并列的情况。

.. _dense-rank:

**dense_rank ( arguments )**

	.. Rank of the hypothetical row, without gaps

    行的rank，不存在并列的情况。

	.. .. note:: Computing the hypothetical **dense_rank** of a value given an infinite stream of values would require infinite memory, so continuous views use :ref:`hll` to do it in constant time and space, at the expense of a small margin of error. Empirically, PipelineDB's implementation of :ref:`hll` has an error rate of ~0.2%. In other words, **dense_rank (1000)** in a continuous view might show 998 when the actual number of unique lower-ranking values seen was :code:`1000`.

    .. note:: 在连续不断的流上计算 **dense_rank**  需要无限大的内存，所以流视图通过 :ref:`hll` 以常数时间和空间实现 **dense_rank**，代价就是小概率的误差。通常PipelineDB 实现的 :ref:`hll` 存在大约0.2%的误差。换言之，在流视图中，当真正的rank值为1000时，计算出的结果可能为998。

**percent_rank ( arguments )**

	.. Relative rank of the hypothetical row, ranging from 0 to 1

    假定行的分位数，0到1。

**cume_dist ( arguments )**

	.. Relative rank of the hypothetical row, ranging from 1/N to 1

    假定行的相对分位数，1/N到1。

----------------------------

..  Unsupported Aggregates

不支持的聚合
---------------------------------

**mode ( )**

	.. Future releases of PipelineDB will include an implementation of an online mode estimation algorithm, but for now it's not supported

    PipelineDB未来的版本将实现一种实时的模式估计算法，但目前还不支持。

**percentile_disc ( arguments )**

	.. Given an input percentile (such as 0.99), **percentile_disc** returns the very first value in the input set that is within that percentile. This requires actually sorting the input set, which is obviously impractical on an infinite stream, and doesn't even allow for a highly accurate estimation algorithm such as the one we use for **percentile_cont**.

    给定一个输入百分位数(比如0.99)，**percentile_disc** 返回该百分位数内输入集中的第一个值。实际上这需要对输入集进行排序，这在连续不断的流中显然是不切实际的，甚至不允许我们使用 **percentile_cont** 中的那种高精度估计算法。

**xmlagg ( xml )**

	:(

**<aggregate_name> (DISTINCT expression)**

	.. Only the :code:`count` aggregate function is supported with a :code:`DISTINCT` expression as noted above in the General Aggregates section. In future releases, we might leverage :ref:`bloom-filter` to allow :code:`DISTINCT` expressions for all aggregate functions.

    在通常的聚合中，只有 :code:`count` 聚合函数支持 :code:`DISTINCT`。在未来的版本中，我们可能让 :ref:`bloom-filter` 也支持 :code:`DISTINCT`。

.. note::

	鉴于PipelineDB已被Confluent收购，所有原来规划的新功能只能指望社区来实现了。
