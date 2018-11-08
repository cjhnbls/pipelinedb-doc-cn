.. _builtin:

Built-in Functionality
=======================

General
----------

We strive to ensure that PipelineDB maintains full compatibility with PostgreSQL 10.1+ and 11.0+. As a result, all of `PostgreSQL's built-in functionality`_ is available to PipelineDB users.

.. _`PostgreSQL's built-in functionality`: http://www.postgresql.org/docs/current/static/functions.html

.. _pg-built-in: http://www.postgresql.org/docs/current/static/functions.html

Aggregates
-------------

As one of PipelineDB's fundamental design goals is to **facilitate high-performance continuous aggregation**, PostgreSQL aggregate functions are fully supported for use in :ref:`continuous-views` (with a couple of rare exceptions). In addition to this large suite of standard aggregates, PipelineDB has also added some of its own :ref:`aggregates` that are purpose-built for continuous time-series data processing.

See :ref:`aggregates` for more information about some of PipelineDB's most useful features.

PipelineDB-specific Types
----------------------------

PipelineDB supports a number of native types for efficiently leveraging :ref:`probabilistic` on streams. You'll likely never need to manually create tables with these types but often they're the result of :ref:`pipeline-aggs`, so they'll be transparently created by :ref:`continuous-views`. Here they are:

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

PipelineDB-specific Functions
---------------------------------

PipelineDB ships with a number of functions that are useful for interacting with these types. They are described below.

.. _bloom-funcs:

Bloom Filter Functions
------------------------------

**bloom_add ( bloom, expression )**

	Adds the given expression to the :ref:`bloom-filter`.

**bloom_cardinality ( bloom )**

	Returns the cardinality of the given :ref:`bloom-filter`. This is the number of **unique** elements that were added to the Bloom filter, with a small margin or error.

**bloom_contains ( bloom, expression )**

	Returns true if the Bloom filter **probably** contains the given value, with a small false positive rate.

**bloom_intersection ( bloom, bloom, ... )**

	Returns a Bloom filter representing the intersection of the given Bloom filters.

**bloom_union ( bloom, bloom, ... )**

	Returns a Bloom filter representing the union of the given Bloom filters.

See :ref:`bloom-aggs` for aggregates that can be used to generate Bloom filters.

.. _topk-funcs:

Top-K Functions
---------------------------------

**topk_increment ( topk, expression )**

	Increments the frequency of the given expression within the given **topk** and returns the resulting :ref:`topk`.

**topk_increment ( topk, expression, weight )**

	Increments the frequency of the given expression by the specified weight within the given :ref:`topk` and returns the resulting :ref:`topk`.

**topk ( topk )**

	Returns up to k tuples representing the given :ref:`topk` top-k values and their associated frequencies.

**topk_freqs ( topk )**

	Returns up to k frequencies associated with the given :ref:`topk` top-k most frequent values.

**topk_values ( topk )**

	Returns up to k values representing the given :ref:`topk` top-k most frequent values.

See :ref:`topk-aggs` for aggregates that can be used to generate **topk** objects.

.. _cmsketch-funcs:

Frequency Functions
------------------------------

**freq_add ( cmsketch, expression, weight )**

	Increments the frequency of the given expression by the specified weight within the given :ref:`count-min-sketch`.

**freq ( cmsketch, expression )**

	Returns the number of times the value of **expression** was added to the given :ref:`count-min-sketch`, with a small margin of error.

**freq_norm ( cmsketch, expression )**

	Returns the normalized frequency of **expression** in the given :ref:`count-min-sketch`, with a small margin of error.

**freq_total ( cmsketch )**

	Returns the total number of items added to the given :ref:`count-min-sketch`.

See :ref:`cmsketch-aggs` for aggregates that can be used to generate **cmsketches**.

.. _hll-funcs:

HyperLogLog Functions
-------------------------

**hll_add ( hyperloglog, expression )**

	Adds the given expression to the :ref:`hll`.

**hll_cardinality ( hyperloglog )**

	Returns the cardinality of the given :ref:`hll`, with roughly a ~0.2% margin of error.

**hll_union ( hyperloglog, hyperloglog, ... )**

	Returns a **hyperloglog** representing the union of the given **hyperloglog**.

See :ref:`hll-aggs` for aggregates that can be used to generate **hyperloglog** objects.

.. _tdigest-funcs:

Distribution Functions
-----------------------

**dist_add ( tdigest, expression, weight )**

	Increments the frequency of the given expression by the given weight in the :ref:`t-digest`.

**dist_cdf ( tdigest, expression )**

	Given a :ref:`t-digest`, returns the value of its cumulative-distribution function evaluated at the value of **expression**, with a small margin of error.

**dist_quantile ( tdigest, float )**

	Given a **tdigest**, returns the value at the given quantile, **float**. **float** must be in :code:`[0, 1]`.

See :ref:`tdigest-aggs` for aggregates that can be used to generate **tdigest** objects.

.. _misc-funcs:

Miscellaneous Functions
-----------------------------

**bucket_cardinality ( bucket_agg, bucket_id )**

  Returns the cardinality of the given **bucket_id** within the given **bucket_agg**.

**bucket_ids ( bucket_agg )**

  Returns an array of all bucket ids contained within the given **bucket_agg**.

**bucket_cardinalities ( bucket_agg )**

  Returns an array of cardinalities contained within the given **bucket_agg**, one for each bucket id.

See :ref:`misc-aggs` for aggregates that can be used to generate **bucket_agg** objects.

**date_round ( timestamp, resolution )**

  "Floors" a date down to the nearest **resolution** (or bucket) expressed as an interval. This is typically useful for summarization. For example, to summarize events into 10-minute buckets:

.. code-block:: sql

    CREATE VIEW v AS SELECT
      date_round(arrival_timestam, '10 minutes') AS bucket_10m, COUNT(*) FROM stream
      GROUP BY bucket_10m;

**year ( timestamp )**

  Truncate the given timestamp down to its **year**.

**month ( timestamp )**

  Truncate the given timestamp down to its **month**.

**day ( timestamp )**

  Truncate the given timestamp down to its **day**.

**hour ( timestamp )**

  Truncate the given timestamp down to its **hour**.

**minute ( timestamp )**

  Truncate the given timestamp down to its **minute**.

**second ( timestamp )**

  Truncate the given timestamp down to its **second**.

**set_cardinality ( array )**

  Returns the cardinality of the given set array. Sets can be built using **set_agg**.

.. _operations:

Operational Functions
------------------------------------------

**pipelinedb.activate ( name )**

  Acitvates the given continuous view or transform. See :ref:`activation-deactivation` for more information.

**pipelinedb.deactivate ( name )**

	Deacitvates the given continuous view or transform. See :ref:`activation-deactivation` for more information.

**pipelinedb.combine_table( continuous view name, table )**

	:ref:`combine` the rows from the given **table** into the given continuous view. **combine_table** uses the given continuous view's query definition to combine aggregate values from both relations with no loss of information.

	**combine_table** can be used for purposes such as backfilling a continuous view (possibly running on a completely separate installation) by combining the backfilled rows into the "live" continuous view only once they have been fully populated.

**pipelinedb.get_views ( )**

        Returns the set of all continuous views.

**pipelinedb.get_transforms ( )**

        Returns the set of all continuous transforms.

**pipelinedb.truncate_continuous_view ( name )**

  Truncates all rows from the given continuous view.

**pipelinedb.version ( )**

        Returns a string containing all of the version information for your PipelineDB installation.

System Views
---------------------------

PipelineDB includes a number of system views for viewing useful information about your continuous views and transforms:

**pipelinedb.views**

Describes :ref:`continuous-views`.

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

Describes :ref:`continuous-transforms`.

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

For each stream, shows all of the continuous queries that are reading from it.

.. code-block:: psql

     View "pipelinedb.transforms"
  Column             |  Type     |
  -------------------+-----------+
  stream             | text      |
  continuous_queries | text[]    |

More system views are available for viewing :ref:`stats` for PipelineDB processes, continuous queries, and streams.