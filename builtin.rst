.. _builtin:

Built-in Functionality
=======================

General
----------

We strive to ensure that PipelineDB maintains full compatibility with PostgreSQL 9.4. As a result, all of `PostgreSQL's built-in functionality`_ is available to PipelineDB users.

.. _`PostgreSQL's built-in functionality`: http://www.postgresql.org/docs/9.4/static/functions.html

.. _pg-built-in: http://www.postgresql.org/docs/9.4/static/functions.html

In addition to PostgreSQL 9.4 compatibility, PipelineDB is also natively compatible with PostGIS 2.1, so all of `PostGIS' builtin functionality`_ is also availble to PipelineDB users.

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

**hll**
	:ref:`hll`

**tdigest**
	:ref:`t-digest`

.. _pipeline-funcs:

PipelineDB-specific Functions
---------------------------------

PipelineDB ships with a number of functions that are useful for interacting with these types. They are described below.

**bloom_cardinality ( bloom filter )**

	Returns the cardinality of the given :ref:`bloom-filter`. This is the number of **unique** elements that were added to the Bloom filter, with a small margin or error.

**bloom_contains ( bloom filter, expression )**

	Returns true if the Bloom filter **probably** contains the given value, with a small false positive rate.

**cmsketch_frequency ( count-min sketch, expression )**

	Returns the number of times the value of **expression** was added to the given :ref:`count-min-sketch`, with a small margin of error.

**hll_cardinality ( hyperloglog )**

	Returns the cardinality of the given :ref:`hll`, with roughly a ~0.2% margin of error.

**tdigest_cdf ( tdigest, expression )**

	Given a :ref:`t-digest`, returns the value of its cumulative-distribution function evaluated at the value of **expression**, with a small margin of error.

**tdigest_quantile ( tdigest, float )**

	Given a T-Digest, returns the value at the given quantile, **float**. **float** must be in :code:`[0, 1]`.

.. note:: See also: :ref:`pipeline-aggs`, which are typically how these types are actually created.

Miscellaneous Functions
---------------------------------

**pipeline_version ( )**

        Returns a string containing all of the version information for your PipelineDB installation.
