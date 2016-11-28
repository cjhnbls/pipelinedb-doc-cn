.. _aggregates:

Continuous Aggregates
======================

One of the fundamental goals of PipelineDB is to **facilitate high-performance continuous aggregation**, so not suprisingly aggregates are a central component of PipelineDB's utility. Continuous aggregates can be very powerful--in the most general sense they make it possible to keep the amount of data persisted in PipelineDB constant relative to the amount of data that has been pushed through it. This can enable sustainable and very high data throughput on modest hardware.

Continuous aggregates are **incrementally updated** in real time as new events are read by the the continuous view that they're a part of. For simple aggregates such as count_ and sum_, it is easy to see how their results can be incrementally updated--just add the new value to the existing result.

But for more complicated aggregates, such as avg_, stddev_, percentile_cont_, etc., more advanced infrastructure is required to support efficient incremental updates, and PipelineDB handles all of that complexity for you transparently.

Below you'll find a description of all the aggregates that PipelineDB supports. A few of them behave slightly differently than their standard counterparts in order to efficiently operate on infinite streams of data. Such aggregates have been annotated with an explanation of how exactly their behavior differs.

.. note:: For the aggregates that have PostgreSQL and PostGIS equivalents, it may be helpful for you to consult the excellent `PostgreSQL aggregates`_ or `PostGIS aggregates`_ documentation.

.. _`PostgreSQL aggregates`: http://www.postgresql.org/docs/current/static/functions-aggregate.html
.. _`PostGIS aggregates`: http://postgis.net/docs/manual-1.4/ch08.html#PostGIS_Aggregate_Functions

----------------------------

.. _pipeline-aggs:

PipelineDB-specific Aggregates
----------------------------------

**bloom_agg ( expression )**

	Adds all input values to a :ref:`bloom-filter`

**bloom_agg ( expression , p , n )**

	Adds all input values to a Bloom filter and sizes it according to the given parameters. **p** is the desired false-positive rate, and **n** is the expected number of unique elements to add.

**bloom_union_agg ( bloom filter )**

	Takes the union of all input Bloom filters, resulting in a single Bloom filter containing all of the input Bloom filters' information.

**bloom_intersection_agg ( bloom filter )**

	Takes the intersection of all input Bloom filters, resulting in a single Bloom filter containing only the information shared by all of the input Bloom filters.

**cmsketch_agg ( expression )**

	Adds all input values to a :ref:`count-min-sketch`.

**cmsketch_agg ( expression, epsilon, p )**

	Same as above, but accepts **epsilon** and **p** as parameters for the underlying **cmsketch**. **epsilon** determines the acceptable error rate of the **cmsketch**, and defaults to **0.002** (0.2%). **p** determines the confidence, and defaults to **0.995** (99.5%). Lower **epsilon** and **p** will result in smaller **cmsketch** structures, and vice versa.

**cmsketch_merge_agg ( count-min sketch )**

	Merges all input Count-min sketches into a single one containing all of the information of the input Count-min sketches.

**exact_count_distinct ( expression )**

  Counts the exact number of distinct values for the given expression. Since **count distinct** used in continuous views implicitly uses HyperLogLog for efficiency, **exact_count_distinct** can be used when the small margin of error inherent to using HyperLogLog is not acceptable.

.. important:: **exact_count_distinct** must store all unique values observed in order to determine uniqueness, so it is not recommended for use when many unique values are expected.

**fss_agg ( expression , k )**

	Adds all input values to a :ref:`fss` data structure sized for the given k, incrementing each value's count by 1 each time it is added.

**fss_agg_weighted (expression, k, weight )**

	Adds all input values to an FSS sized for the given k, incrementing each value's count by the given weight each time it is added.

**fss_merge_agg ( fss )**

	Merges all FSS inputs into a single FSS.

**keyed_max ( key, value )**

	Most recent value associated with the maximum key.

**keyed_min ( key, value )**

	Most recent value associated with the minimum key.

**hll_agg ( expression )**

	Adds all input values to a :ref:`hll`.

**hll_agg ( expression, p )**

	Adds all input values to a :ref:`hll` with the given **p**. A larger **p** reduces the HyperLogLog's error rate, at the expense of a larger size.

**hll_union_agg ( hyperloglog )**

	Takes the union of all input HyperLogLogs, resulting in a single HyperLogLog that contains all of the information of the input HyperLogLogs.

.. _set-agg:

**set_agg ( expression )**

  Adds all input values to a set.

**tdigest_agg ( expression )**

	Adds all input values to a :ref:`t-digest`.

**tidgest_merge_agg ( tdigest )**

  Merges all input T-Digest's into a single one representing all of the information contained in the input T-Digests.

**first_values ( n ) WITHIN GROUP (ORDER BY sort_expression)**

  An ordered-set aggregate that stores the first **n** values ordered by the provided sort expression.

.. note:: See also: :ref:`pipeline-funcs`, which explains some of the PipelineDB's non-aggregate functionality for manipulating Bloom filters, Count-min sketches, HyperLogLogs and T-Digests. Also, check out :ref:`probabilistic` for more information about what they are and how you can leverage them.

------------------------------------

Combine
------------

Since PipelineDB can incrementally update aggregate values, it has the capability to combine existing aggregates using more information than simply their current raw values. For example, combining multiple averages isn't simply a matter of taking the average of the averages. Their weights must be taken into account.

For this type of operation, PipelineDB exposes the special **combine** aggregate. Its description is as follows:

**combine ( aggregate column )**

	Given an aggregate column, combines all values into a single value as if all of the individual aggregates' inputs were aggregated a single time.

.. note:: **combine** only works on aggregate columns that belong to continuous views.

Let's look at an example:

.. code-block:: pipeline

  pipeline=# CREATE CONTINUOUS VIEW v AS
	SELECT g::integer, AVG(x::integer) FROM stream GROUP BY g;
  CREATE CONTINUOUS VIEW
  pipeline=# INSERT INTO stream (g, x) VALUES (0, 10), (0, 10), (0, 10), (0, 10), (0, 10);
  INSERT 0 5
  pipeline=# INSERT INTO stream (g, x) VALUES (1, 20);
  INSERT 0 1
  pipeline=# SELECT * FROM v;
   g |         avg
  ---+---------------------
   0 | 10.0000000000000000
   1 | 20.0000000000000000
  (2 rows)

  pipeline=# SELECT avg(avg) FROM v;
           avg
  ---------------------
   15.0000000000000000
  (1 row)

  pipeline=# -- But that didn't take into account that the value of 10 weighs much more,
  pipeline=# -- because it was inserted 5 times, whereas 20 was only inserted once.
  pipeline=# -- combine() will take this weight into account
  pipeline=#
  pipeline=# SELECT combine(avg) FROM v;
         combine
  ---------------------
   11.6666666666666667
  (1 row)

  pipeline=# -- There we go! This is the same average we would have gotten if we ran
  pipeline=# -- a single average on all 6 of the above inserted values, yet we only
  pipeline=# -- needed two rows to do it.


------------------------------

CREATE AGGREGATE
-------------------

In addition to PipelineDB's built-in aggregates, user-defined aggregates also work with continuous views. User-defined combinable aggregates can be created with PostgreSQL's `CREATE AGGREGATE`_ command. To make an aggregate combinable, a **combinefunc** must be given. **combineinfunc** and **transoutfunc** are optional:

.. code-block:: pipeline

	CREATE AGGREGATE name ( [ argmode ] [ argname ] arg_data_type [ , ... ] ) (
		...
		COMBINEFUNC = combinefunc,
		[ , COMBINEINFUNC = combineinfunc ]
		[ , TRANSOUTFUNC = transoutfunc ]
	)

.. _CREATE AGGREGATE: http://www.postgresql.org/docs/current/static/sql-createaggregate.html


**combinefunc ( stype, stype )**

	A function that takes two transition states and returns a single transition state. For example, here's an example of a combine function for an integer :code:`avg` implementation:

.. code-block:: pipeline

	CREATE FUNCTION avg_combine(state integer[], incoming integer[]) RETURNS integer[] AS $$
	BEGIN
		RETURN ARRAY[state[1] + incoming[1], state[2] + incoming[2]];
	END;
	$$
	LANGUAGE plpgsql

The transition state is represented as a 2-element array containing the number of elements and their sum, which can be used to compute a final.

**combineinfunc ( any )**

	A function that deserializes the aggregate's transition state from an external to internal representation. **Deserialization is only necessary when the transition state type is not a native type.**

**transoutfunc ( stype )**

	A function that serializes the aggregate's transition state from an internal to external representation that can be stored in a table cell. **Serialization is only necessary when the transition state type is not a native type.**

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

.. code-block:: pipeline

	aggregate_name ( [ expression [ , ... ] ] ) WITHIN GROUP ( order_by_clause )

Let's look at a couple examples.

Compute the 99th percentile of **value**:

.. code-block:: pipeline

	SELECT percentile_cont(0.99) WITHIN GROUP (ORDER BY value) FROM some_table;

Or with a continuous view:

.. code-block:: pipeline

	CREATE CONTINUOUS VIEW percentile AS
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

.. code-block:: pipeline

	aggregate_name ( [ expression [ , ... ] ] ) WITHIN GROUP ( order_by_clause )

Here is an example of of a hypothetical-set aggregate being used by a continuous view:

.. code-block:: pipeline

	CREATE CONTINUOUS VIEW continuous_rank AS
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

**jsonb_agg ( any )**

	Coming soon! For now use :code:`json_agg`.

**jsonb_object_agg ( any, any )**

	Coming soon! For now use :code:`json_object_agg`.

**mode ( )**

	Future releases of PipelineDB will include an implementation of an online mode estimation algorithm, but for now it's not supported

**percentile_disc ( arguments )**

	Given an input percentile (such as 0.99), **percentile_disc** returns the very first value in the input set that is within that percentile. This requires actually sorting the input set, which is obviously impractical on an infinite stream, and doesn't even allow for a highly accurate estimation algorithm such as the one we use for **percentile_cont**.

**xmlagg ( xml )**

	:(

**aggregate_name (DISTINCT expression)**

	Only the :code:`count` aggregate function is supported with a :code:`DISTINCT` expression as noted above in the General Aggregates section. In future releases, we might leverage :ref:`bloom-filter` to allow :code:`DISTINCT` expressions for all aggregate functions.
