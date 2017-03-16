.. _builtin:

Built-in Functionality
=======================

General
----------

We strive to ensure that PipelineDB maintains full compatibility with PostgreSQL 9.5. As a result, all of `PostgreSQL's built-in functionality`_ is available to PipelineDB users.

.. _`PostgreSQL's built-in functionality`: http://www.postgresql.org/docs/current/static/functions.html

.. _pg-built-in: http://www.postgresql.org/docs/current/static/functions.html

In addition to PostgreSQL 9.5 compatibility, PipelineDB is also natively compatible with PostGIS 2.1, so all of `PostGIS' builtin functionality`_ is also availble to PipelineDB users.

.. _`PostGIS' builtin functionality`: http://postgis.net/docs/manual-2.1/

Aggregates
-------------

As one of PipelineDB's fundamental design goals is to **facilitate high-performance continuous aggregation**, PostgreSQL and PostGIS aggregate functions are fully supported for use in :ref:`continuous-views` (with a couple of rare exceptions). In addition to this large suite of standard aggregates, PipelineDB has also added some of its own :ref:`pipeline-aggs` that are purpose-built for continuous data processing.

See :ref:`aggregates` for more information about some of PipelineDB's most useful features.

PipelineDB-specific Types
----------------------------

PipelineDB supports a number of native types for efficiently leveraging :ref:`probabilistic` on streams. You'll likely never need to manually create tables with these types but often they're the result of :ref:`pipeline-aggs`, so they'll be transparently created by :ref:`continuous-views`. Here they are:

**bloom**
	:ref:`bloom-filter`

**cmsketch**
	:ref:`count-min-sketch`

**fss**
	:ref:`fss`

**hll**
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

.. _fss-funcs:

Filtered-Space Saving Functions
---------------------------------

**fss_increment ( fss, expression )**

	Increments the frequency of the given expression within the given FSS and returns the resulting :ref:`fss`.

**fss_increment_weighted ( fss, expression, weight )**

	Increments the frequency of the given expression by the specified weight within the given :ref:`fss` and returns the resulting :ref:`fss`.

**fss_topk ( fss )**

	Returns up to k tuples representing the given :ref:`fss` top-k values and their associated frequencies.

**fss_topk_freqs ( fss )**

	Returns up to k frequencies associated with the given :ref:`fss` top-k most frequent values.

**fss_topk_values ( fss )**

	Returns up to k values representing the given :ref:`fss` top-k most frequent values.

See :ref:`fss-aggs` for aggregates that can be used to generate Filtered-Space Saving objects.

.. _cmsketch-funcs:

Count-Min Sketch Functions
------------------------------

**cmsketch_add ( cmsketch, expression, weight )**

	Increments the frequency of the given expression by the specified weight within the given :ref:`count-min-sketch`.

**cmsketch_frequency ( cmsketch, expression )**

	Returns the number of times the value of **expression** was added to the given :ref:`count-min-sketch`, with a small margin of error.

**cmsketch_norm_frequency ( cmsketch, expression )**

	Returns the normalized frequency of **expression** in the given :ref:`count-min-sketch`, with a small margin of error.

**cmsketch_total ( cmsketch )**

	Returns the total number of items added to the given :ref:`count-min-sketch`.

See :ref:`cmsketch-aggs` for aggregates that can be used to generate Count-Min Sketches.

.. _hll-funcs:

HyperLogLog Functions
-------------------------

**hll_add ( hll, expression )**

	Adds the given expression to the :ref:`hll`.

**hll_cardinality ( hll )**

	Returns the cardinality of the given :ref:`hll`, with roughly a ~0.2% margin of error.

**hll_union ( hll, hll, ... )**

	Returns a HyperLogLog representing the union of the given HyperLogLogs.

See :ref:`hll-aggs` for aggregates that can be used to generate HyperLogLog objects.

.. _tdigest-funcs:

T-Digest Functions
-----------------------

**tdigest_add ( tdigest, expression, weight )**

	Increments the frequency of the given expression by the given weight in the :ref:`t-digest`.

**tdigest_cdf ( tdigest, expression )**

	Given a :ref:`t-digest`, returns the value of its cumulative-distribution function evaluated at the value of **expression**, with a small margin of error.

**tdigest_quantile ( tdigest, float )**

	Given a T-Digest, returns the value at the given quantile, **float**. **float** must be in :code:`[0, 1]`.

.. note:: See also: :ref:`pipeline-aggs`, which are typically how these types are actually created.

See :ref:`tdigest-aggs` for aggregates that can be used to generate T-Digest objects.

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

.. code-block:: pipeline

    CREATE CONTINUOUS VIEW v AS SELECT
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

**pipeline_version ( )**

        Returns a string containing all of the version information for your PipelineDB installation.

**pipeline_views ( )**

        Returns the set of all continuous views.

**pipeline_transforms ( )**

        Returns the set of all continuous transforms.
