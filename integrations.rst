.. _integrations:

Integrations
============================

Kafka
----------

PipelineDB supports ingesting data from Kafka topics into streams. All of this functionality is contained in the **pipeline_kafka** extension. Internally, **pipeline_kafka** uses `PostgreSQL's COPY`_ infrastructure to transform Kafka messages into rows that PipelineDB understands. To enable the extension, it must be explicitly loaded:

.. _`PostgreSQL's COPY`: http://www.postgresql.org/docs/current/static/sql-copy.html

.. code-block:: pipeline

	# CREATE EXTENSION pipeline_kafka;
	CREATE EXTENSION

.. note:: All binary distributions of PipelineDB include the **pipeline_kafka** extension, but if you're building from source you must compile and install it from **contrib/pipeline_kafka**.

**pipeline_kafka** exposes all of its functionality through the following functions:

**kafka_consume_begin ( topic text, stream text, format := 'text', delimiter := E'\\t', quote := NULL, escape := NULL, batchsize := 1000, parallelism := 1, start_offset := NULL )**

Launches **parallelism** background worker processes that each reads messages from the given Kafka topic into the given stream. The target stream must be created with :code:`CREATE STREAM` beforehand. All partitions of the given topic will be spread evenly across each worker process. The optional **format**, **delimiter**, **escape** and **quote** arguments are analagous to the :code:`FORMAT`, :code:`DELIMITER` :code:`ESCAPE` and :code:`QUOTE` options for the `PostgreSQL COPY`_ command. **batchsize** controls the :code:`batch_size` parameter passed to the Kafka client. **start_offset** specifies the offset from which to start reading the Kafka topic. If its :code:`NULL`, then we start from the first unread message. :code:`pipeline_kafka` continuously saves the offset its read till durably in the database. A start_offset of -1 will start reading end of the parition and -2 will start consuming from the beginning from the parition.

.. _`PostgreSQL COPY`: http://www.postgresql.org/docs/current/static/sql-copy.html

**kafka_consume_begin ( )**

	Same as above, but launches all previously created consumers instead of for a specific stream-topic pair.

**kafka_consume_end ( topic text, stream text )**

	Terminates background consumer processes for the given stream-topic pair.

**kafka_consume_end ( )**

	Same as above, but terminates all consumer processes.

---------------------

**pipeline_kafka** uses several tables to durably keep track of its own state across system restarts:

**pipeline_kafka_consumers**

	Stores the metadata for each stream-topic consumer that is created by **kafka_consume_begin**.

**pipeline_kafka_brokers**

	Stores all Kafka brokers that consumers can connect to.

**pipeline_kafka_offsets**

	Stores Kafka topic offsets so that consumers can begin reading messages from where they left off before termination or system restarts.

-----------------------

.. note:: See `SQL on Kafka`_ for an in-depth tutorial on using Kafka with PipelineDB.

.. _`SQL on Kafka`: https://www.pipelinedb.com/blog/sql-on-kafka
