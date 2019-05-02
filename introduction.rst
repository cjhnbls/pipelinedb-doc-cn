.. _introduction:

..
    Introduction
引言
=============
..
    If you'd prefer to get right into it, check out the :ref:`quickstart` section.
您可以跳转到 :ref:`quickstart` 直接开始使用PipelineDB.

..
    Overview
概述
-----------
..
    PipelineDB is a high-performance PostgreSQL extension built to run SQL queries continuously on time-series data. The output of these continuous queries is stored in regular tables which can be queried like any other table or view. Thus continuous queries can be thought of as very high-throughput, incrementally updated materialized views. As with any data processing system, PipelineDB is built to shine under particular workloads, and simply not intended for others.

    Check out the :ref:`clients` and :ref:`quickstart` sections for examples of PipelineDB in action.

PipelineDB是一个用于在时序数据上持续执行SQL查询的高性能PostgreSQL插件。SQL查询的输出被持久化到普通的表中，可以像其它的表或视图一样进行查询。可以认为持续查询的结果是一个高吞吐量并且快速更新的物化视图。PipelineDB可以在某些任务场景下表现得十分优秀，若超出这个范畴，可能就会同其它数据处理系统一样面临一些问题。

:ref:`clients` 和 :ref:`quickstart` 部分包含了一些PipelineDB的操作示例。

..
    What PipelineDB is
PipelineDB的长处
-------------------
..
    **PipelineDB is designed to excel at SQL queries that reduce the cardinality of time-series datasets**. For example: summarizations and aggregations; performing computations across sliding time windows; text search filtering; geospatial filtering, etc. By reducing the cardinality of its input streams, PipelineDB can dramatically reduce the amount of information that needs to be persisted to disk because only the output of continuous queries is stored. Raw data is discarded once it has been read by the continuous queries that need to read it.

    Much of the data that is passed through PipelineDB can thus be thought of as **virtual data**. This idea of data virtualization is at the core of what PipelineDB is all about, and is what allows it to process large volumes of data very efficiently using a relatively small hardware footprint.

    **PipelineDB aims to eliminate the necessity of an ETL stage for many common data pipelines**. Raw data can be streamed directly into PipelineDB, where it is continuously refined and distilled in real time by the continuous queries you've declared. This makes it unnecessary to periodically process granular data before loading its refined output into the database--as long as that processing can be defined by SQL queries, of course.

    **PipelineDB is designed with pragmatism as a first-class consideration**, which is why we've packaged it as a PostgreSQL extension. All data storage and manipulation is delegated to PostgreSQL, an extremely, stable, mature and ubiquitous database. Additionally, PipelineDB is compatible with all tooling in the vibrant PostgreSQL ecosystem. We have not invented our own proprietary syntax, and we don't even have a PipelineDB client because it works with any libraries that already work with PostgreSQL.

**PipelineDB被设计用来在精简后的流式数据集上进行SQL查询**。比如：概要和聚合；基于滑动时间窗口的性能计算；文本索引和过滤；空间信息过滤等。通过减少流数据的输入，PipelineDB可以显著地减少持久化到磁盘中的数据量，因为之后聚合后的结果被存储下来。原始数据（foreign table）一旦被需要它的持续查询读取后就会被销毁。

大多数写入到PipelineDB中的数据可以被视为 **虚拟数据**。数据虚拟化是PipelineDB设计的精髓，凭着这种设计，PipelineDB可以只占用较小的硬件资源实现高效的大数据量处理。

**PipelineDB的目标是消除许多数据传输中的ETL过程**。原始数据流式写入PipelineDB，被已声明的流式查询实时地转换和提取，这使得它在将成型的输出加载到数据库前不必周期性地处理颗粒数据 -- 前提是这些处理过程可以通过SQL定义。

**在PipelineDB的设计理念中，实用性是第一要素**，这也是我们将其包装为PostgreSQL插件的原因。所有数据存储和处理都交由PostgreSQL：一个极其稳定、成熟以及运用广泛的数据库。此外，PipelineDB兼容活跃的PostgreSQL生态中的所有工具。我们没有为PipelineDB设计特有的语法甚至是客户端，因为它可以很好地兼容任何基于PostgreSQL开发的库。

..
    What PipelineDB is not

PipelineDB的短板
-------------------------
..
    Given that continuous queries must be known *a priori*, PipelineDB is not an ad-hoc data warehouse. While the output of continuous queries may be explored in an ad-hoc fashion, all of the raw data that has ever passed through PipelineDB may not be because datapoints are discarded after they've been read. Additionally, if streaming computations which cannot be expressed in SQL are needed, PipelineDB probably isn't the right tool for the job!

鉴于流查询需要一些**先验知识**，PipelineDB不是一个特定的数据仓库。流式查询的输出可能在特定方式下被访问，所有写入到PipelineDB中的原始数据都不会被持久化因为它们都会在读取后被销毁。此外，如果流式计算不能以SQL的形式表达，PipelineDB可能并不是一个合适的选择！
