.. _probabilistic:

Probabilistic Data Structures & Algorithms
================================================

PipelineDB ships with a number of types and aggregates that aren't commonly exposed to users by most database systems, but are extremely useful for continuous data processing. Here you'll find an overview of what these types and functions are, and what you can do with them.

.. _bloom-filter:

Bloom Filter
------------------------

`Bloom filters`_ are space-optimized data structures designed to estimate set cardinalities as well as determining, with a high degree of likelihood, if they contain a specific element. Bloom filters work by mapping an added element to one or more bits in a bitmap. When an element is added to a Bloom filter, these bits (ideally just one bit) are set to 1.

Intuitively, this means that an n-bit input can be compressed down to a single bit. While an enormous amount of space can be saved using Bloom filters, the tradeoff is that a small possibility of false positives is introduced when determining if a given element has been added to it, because a single input element can potentially map to multiple bits.

**How Bloom filters are used in PipelineDB**

Continuous views containing a :code:`SELECT DISTINCT (...)` clause use Bloom filters to determine if a given expression is unique, and thus whether or not to include it in the continuous result. This allows such continuous views to use constant space while determining uniqueness for an infinite stream of expressions with a high degree of accuracy.

Users are also free to construct their own Bloom filters and manipulate them with the :ref:`pipeline-funcs` that expose them.

.. _`Bloom filters`: http://en.wikipedia.org/wiki/Bloom_filter

.. _count-min-sketch:

Count-Min Sketch
------------------

A `Count-min sketch`_ is a data structure that is similar to a Bloom filter, with the main difference being that a Count-min sketch estimates the frequency of each element that has been added to it, whereas a Bloom filter only records whether or not a given item has likely been added or not.

Currently no PipelineDB functionality internally uses Count-min sketch, although users are free to construct their own Count-min sketch data structures and manipulate them with the :ref:`pipeline-funcs` that expose them.

.. _`Count-Min Sketch`: https://en.wikipedia.org/wiki/Count%E2%80%93min_sketch

.. _topk:

Top-K
----------------------------

`Filtered-Space Saving`_ (FSS) is a data structure and algorithm combination useful for accurately estimating the top k most frequent values appearing in a stream while using a constant, minimal memory footprint. The obvious approach to computing top-k is to simply keep a table of values and their associated frequencies, which is not practical for streams.

Instead, FSS works by hashing incoming values into buckets, where each bucket has a collection of values already added. If the incoming element already exists at a given bucket, its frequency is incremented. If the element doesn't exist, it will be added as long as a few certain configurable conditions are met.

Currently no PipelineDB functionality implicitly uses FSS. The FSS type and its associated functions can be accessed via the various :ref:`pipeline-funcs` and :ref:`aggregates` that expose them.

.. _`Filtered-Space Saving`: http://www.l2f.inesc-id.pt/~fmmb/wiki/uploads/Work/dict.refd.pdf

.. _hll:

HyperLogLog
----------------------------

`HyperLogLog`_ is a data structure and algorithm combination that, similarly to Bloom filters, is designed to estimate the cardinality of sets with a very high degree of accuracy. In terms of functionality, HyperLogLog only supports adding elements and estimating the cardinality of the set of all elements that have been added. They do not support membership checks of specific elements like Bloom filters do. However, they are drastically more space efficient than Bloom filters.

HyperLogLog works by subdividing its input stream of added elements and storing the maximum number of leading zeros that have been observed within each subdivision. Since elements are uniformly hashed before checking for the number of leading zeros, the general idea is that the greater the number of leading zeros observed, the higher the probability that many unique elements have been added. Empirically, this estimation turns out to be very accurate--PipelineDB's HyperLogLog implementation has a margin of error of only about 0.81% (that's about 8 out of 1,000).

**How HyperLogLog is used in PipelineDB**

Continuous views containing a :code:`COUNT(DISTINCT ...)` clause use HyperLogLog to accurately estimate the number of unique expressions read using a constant amount of space for an infinite stream of expressions. The hypothetical-set aggregate, **dense_rank** also uses HyperLogLog to accurately estimate the number of unique lower-ranking expressions that have been read in order to determine the rank of the hypothetical value.

Users are also free to construct their own HyperLogLog data structures and manipulate them with the :ref:`pipeline-funcs` that expose them.

.. _`HyperLogLog`: http://en.wikipedia.org/wiki/HyperLogLog

.. _t-digest:

T-Digest
----------------------

`T--Digest`_ is a data structure that supports very accurate estimations of rank-based statistics such as percentiles and medians while only using a constant amount of space. Space efficiency at the expense of a small margin of error makes T-Digest well-suited for rank-based computatations on streams, which normally require their input to be finite and ordered for perfect accuracy. T-Digest is essentially an adaptive histogram that intelligently adjusts its buckets and frequencies as more elements are added to it.

.. _`T--Digest`: https://github.com/tdunning/t-digest/blob/master/docs/t-digest-paper/histo.pdf

**How T--Digest is used in PipelineDB**

The **percentile_cont** aggregate internally uses T-Digest when operating on a stream. Users are also free to construct their own T-Digest data structures and manipulate them with the :ref:`pipeline-funcs` that expose them.

