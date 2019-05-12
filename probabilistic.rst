.. _probabilistic:

..  Probabilistic Data Structures & Algorithms

概率数据结构和算法
================================================

..  PipelineDB ships with a number of types and aggregates that aren't commonly exposed to users by most database systems, but are extremely useful for continuous data processing. Here you'll find an overview of what these types and functions are, and what you can do with them.

PipelineDB包含许多在大多数数据库中不对用户开放的数据类型和聚合函数，这些对流式数据处理是非常有用的，下面将围绕这些数据类型和函数进行说明。

.. _bloom-filter:

Bloom Filter
------------------------

..  `Bloom filters`_ are space-optimized data structures designed to estimate set cardinalities as well as determining, with a high degree of likelihood, if they contain a specific element. Bloom filters work by mapping an added element to one or more bits in a bitmap. When an element is added to a Bloom filter, these bits (ideally just one bit) are set to 1.

`Bloom filters`_ 是一种空间优化的数据结构，它用于评估集合的基数，以及判断一个元素是否在集合中。Bloom filters通过将元素映射到位图中的一位或多位来实现。当元素添加到Bloom filter中，其对应的位点（理想状态下只会对应1）就被会置为1。

..  Intuitively, this means that an n-bit input can be compressed down to a single bit. While an enormous amount of space can be saved using Bloom filters, the tradeoff is that a small possibility of false positives is introduced when determining if a given element has been added to it, because a single input element can potentially map to multiple bits.

简而言之，这意味着一个n位的输入可以被压缩到1位。Bloom filters可以节省巨大的空间，代价就是判断元素时存在一定的假阳性，因为单个输入可能会映射到多个位点上。

..  **How Bloom filters are used in PipelineDB**

**Bloom filters如何应用于PipelineDB**

..  Continuous views containing a :code:`SELECT DISTINCT (...)` clause use Bloom filters to determine if a given expression is unique, and thus whether or not to include it in the continuous result. This allows such continuous views to use constant space while determining uniqueness for an infinite stream of expressions with a high degree of accuracy.

包含 :code:`SELECT DISTINCT (...)` 语句的流视图使用Bloom filters来判断元素是否唯一以及是否将其输出到结果中。这使得流视图可以在常数空间复杂度的情况下对无限的流数据进行高精度的去重。

..  Users are also free to construct their own Bloom filters and manipulate them with the :ref:`pipeline-funcs` that expose them.

用于也可以自由构造Bloom filters并且通过 :ref:`PipelineDB特有函数<pipeline-funcs>` 进行操作。

.. _`Bloom filters`: http://en.wikipedia.org/wiki/Bloom_filter

.. _count-min-sketch:

Count-Min Sketch
------------------

..  A `Count-min sketch`_ is a data structure that is similar to a Bloom filter, with the main difference being that a Count-min sketch estimates the frequency of each element that has been added to it, whereas a Bloom filter only records whether or not a given item has likely been added or not.

`Count-min sketch`_ 是一种与Bloom filter相似的数据结构，主要的区别是Count-min sketch评估每个元素的频数，而Bloom filter只记录元素是否在集合中。

..  Currently no PipelineDB functionality internally uses Count-min sketch, although users are free to construct their own Count-min sketch data structures and manipulate them with the :ref:`pipeline-funcs` that expose them.

尽管用户可以自由构造Count-min sketch数据结构并通过 :ref:`PipelineDB特有函数<pipeline-funcs>` 进行操作，但目前Count-min sketch没有在PipelineDB函数中隐式使用。

.. _`Count-Min Sketch`: https://en.wikipedia.org/wiki/Count%E2%80%93min_sketch

.. _topk:

Top-K
----------------------------

..  `Filtered-Space Saving`_ (FSS) is a data structure and algorithm combination useful for accurately estimating the top k most frequent values appearing in a stream while using a constant, minimal memory footprint. The obvious approach to computing top-k is to simply keep a table of values and their associated frequencies, which is not practical for streams.

`Filtered-Space Saving`_ (FSS)是一种只消耗最小常数内存来精确评估流中最频繁的k个值的数据结构和算法。计算top-k最普通的方法就是维系一张值的计数表，但这在流中不实用。

..  Instead, FSS works by hashing incoming values into buckets, where each bucket has a collection of values already added. If the incoming element already exists at a given bucket, its frequency is incremented. If the element doesn't exist, it will be added as long as a few certain configurable conditions are met.

FSS通过哈希运算将输入值归入对应的桶中。若输入值已在桶中，会增加其频数。若不存在，它会在满足一定条件时被添加进去。

..  Currently no PipelineDB functionality implicitly uses FSS. The FSS type and its associated functions can be accessed via the various :ref:`pipeline-funcs` and :ref:`aggregates` that expose them.

目前FSS没有在PipelineDB中隐式使用。FSS及其相关函数通过 :ref:`PipelineDB特有函数<pipeline-funcs>` 和 :ref:`流聚合<aggregates>` 使用。

.. _`Filtered-Space Saving`: http://www.l2f.inesc-id.pt/~fmmb/wiki/uploads/Work/dict.refd.pdf

.. _hll:

HyperLogLog
----------------------------

..  `HyperLogLog`_ is a data structure and algorithm combination that, similarly to Bloom filters, is designed to estimate the cardinality of sets with a very high degree of accuracy. In terms of functionality, HyperLogLog only supports adding elements and estimating the cardinality of the set of all elements that have been added. They do not support membership checks of specific elements like Bloom filters do. However, they are drastically more space efficient than Bloom filters.

`HyperLogLog`_ 是一种与Bloom filters相似的数据结构，被设计用于高准确度地评估集合基数。就功能而言，HyperLogLog只支持添加元素以及评估集合中的所有元素。它不能像Bloom filters一样进行单个元素的检索，但空间利用率比Bloom filters更高。

..  HyperLogLog works by subdividing its input stream of added elements and storing the maximum number of leading zeros that have been observed within each subdivision. Since elements are uniformly hashed before checking for the number of leading zeros, the general idea is that the greater the number of leading zeros observed, the higher the probability that many unique elements have been added. Empirically, this estimation turns out to be very accurate--PipelineDB's HyperLogLog implementation has a margin of error of only about 0.81% (that's about 8 out of 1,000).

HyperLogLog将输入流添加的元素进行分割，并存储每个子集中前导0的最大数量。由于在检查前导0个数前会对元素进行均匀散列，通常的观点是：前导0的个数越多，许多唯一元素被添加的概率就越高。根据经验，HyperLogLog估计的准确度是很高的，PipelineDB的HyperLogLog误差率只有大约0.81%（约千分之八）。

..  **How HyperLogLog is used in PipelineDB**

**HyperLogLog在PipelineDB中的应用**

..  Continuous views containing a :code:`COUNT(DISTINCT ...)` clause use HyperLogLog to accurately estimate the number of unique expressions read using a constant amount of space for an infinite stream of expressions. The hypothetical-set aggregate, **dense_rank** also uses HyperLogLog to accurately estimate the number of unique lower-ranking expressions that have been read in order to determine the rank of the hypothetical value.

包含 :code:`COUNT(DISTINCT ...)` 语句的流视图借助HyperLogLog在常数空间复杂度下从无限的流数据中精确计算集合基数。hypothetical-set聚合函数，**dense_rank** 函数也使用HyperLogLog精确计算低排名元素并决定其排名。

..  Users are also free to construct their own HyperLogLog data structures and manipulate them with the :ref:`pipeline-funcs` that expose them.

用户也可以自由构造HyperLogLog数据结构并通过 :ref:`PipelineDB特有函数<pipeline-funcs>` 使用。

.. _`HyperLogLog`: http://en.wikipedia.org/wiki/HyperLogLog

.. _t-digest:

T-Digest
----------------------

..  `T--Digest`_ is a data structure that supports very accurate estimations of rank-based statistics such as percentiles and medians while only using a constant amount of space. Space efficiency at the expense of a small margin of error makes T-Digest well-suited for rank-based computatations on streams, which normally require their input to be finite and ordered for perfect accuracy. T-Digest is essentially an adaptive histogram that intelligently adjusts its buckets and frequencies as more elements are added to it.

`T--Digest`_ 是一种支持精确排序统计的数据结构，如在常数空间复杂度下计算百分位数和中位数。以很小的误差达到高效的性能，使得T-Digest很适合用于流的排序计算，这要求输入是有限且有序以达到高度的精确性。T-Digest本质上是一种在元素被添加时智能调整桶和频数的自适应的直方图。

.. _`T--Digest`: https://github.com/tdunning/t-digest/blob/master/docs/t-digest-paper/histo.pdf

..  **How T--Digest is used in PipelineDB**

**T--Digest在PipelineDB中的应用**

..  The **percentile_cont** aggregate internally uses T-Digest when operating on a stream. Users are also free to construct their own T-Digest data structures and manipulate them with the :ref:`pipeline-funcs` that expose them.

**percentile_cont** 聚合函数在处理流数据时隐式使用T-Digest。用户可以自由构造T-Digest数据并通过 :ref:`PipelineDB特有函数<pipeline-funcs>` 使用。
