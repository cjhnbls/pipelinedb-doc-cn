.. _builtin:

Built-in functionality
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

As one of PipelineDB's fundamental design goals is to **facilitate high-performance continuous aggregation**, PostgreSQL and PostGIS aggregate functions are fully supported for use in :ref:`continuous-views` (with a couple of rare exceptions). In addition to this large suite of standard aggregates, PipelineDB has also added some of its own useful aggregation functionality that is purpose-built for streaming datasets.

See :ref:`aggregates` for more information about some of PipelineDB's most useful features.
