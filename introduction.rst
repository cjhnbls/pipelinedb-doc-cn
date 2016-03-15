.. _introduction:

Introduction
=============

If you'd prefer to get right into it, check out the :ref:`quickstart` section.

Overview
-----------

PipelineDB is built to run SQL queries continuously on streaming data. The output of these continuous queries is stored in regular tables which can be queried like any other table or view. Thus continuous queries can be thought of as very high-throughput, incrementally updated materialized views. As with any data processing system, PipelineDB is built to shine under particular workloads, and simply not intended for others.

Check out the :ref:`clients` and :ref:`quickstart` sections for examples of PipelineDB in action.


What PipelineDB is
-------------------

**PipelineDB is designed to excel at SQL queries that reduce the cardinality of streaming datasets**. For example: summarizations and aggregations; performing computations across sliding time windows; text search filtering; geospatial filtering, etc. By reducing the cardinality of its input streams, PipelineDB can dramatically reduce the amount of information that needs to be persisted to disk because only the output of continuous queries is stored. Raw data is discarded once it has been read by the continuous queries that need to read it.

Much of the data that is passed through PipelineDB can thus be thought of as **virtual data**. This idea of data virtualization is at the core of what PipelineDB is all about, and is what allows it to process large volumes of data very efficiently using a relatively small hardware footprint.

**PipelineDB aims to eliminate the necessity of an ETL stage for many common data pipelines**. Raw data can be streamed directly into PipelineDB, where it is continuously refined and distilled in real time by the continuous queries you've declared. This makes it unnecessary to periodically process granular data before loading its refined output into the database--as long as that processing can be defined by SQL queries, of course.

**PipelineDB is designed with pragmatism as a first-class consideration**, which is why we've built it to be fully compatible with PostgreSQL 9.5. We have not invented our own proprietary syntax, and we don't even have a PipelineDB client because it works with any libraries that already work with PostgreSQL.

What PipelineDB is not
-------------------------

Given that continuous queries must be known *a priori*, PipelineDB is not an ad-hoc data warehouse. While the output of continuous queries may be explored in an ad-hoc fashion, all of the raw data that has ever passed through PipelineDB may not be because datapoints are discarded after they've been read. Additionally, if streaming computations which cannot be expressed in SQL are needed, PipelineDB probably isn't the right tool for the job!
