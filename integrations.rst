.. _integrations:

Integrations
============================

Kafka
----------

PipelineDB supports ingesting data from Kafka topics into streams. All of this functionality is contained in the **pipeline_kafka** extension. Internally, **pipeline_kafka** uses `PostgreSQL's COPY`_ infrastructure to transform Kafka messages into rows that PipelineDB understands. To enable the extension, it must be explicitly loaded:

.. _`PostgreSQL's COPY`: http://www.postgresql.org/docs/9.4/static/sql-copy.html

.. code-block:: pipeline

	# CREATE EXTENSION pipeline_kafka;
	CREATE EXTENSION

.. note:: All binary distributions of PipelineDB include the **pipeline_kafka** extension, but if you're building from source you must compile and install it. It is located at **contrib/pipeline_kafka**.

**pipeline_kafka** exposes all of its functionality through the following functions:

**kafka_consume_begin ( stream text, topic text, parallelism = 1, format = 'text', delimiter = '\\t' )**

	Launches **$parallelism** background worker processes that each read's messages from the given Kafka topic into the given stream. The target stream must be created with :code:`CREATE STREAM` beforehand. All partitions of the given topic will be spread evenly across each worker process launched. The optional **format** and **delimiter** arguments are analagous to the :code:`FORMAT` and :code:`DELIMITER` options for `PostgreSQL's COPY`_ command.

.. _`PostgreSQL's COPY`: http://www.postgresql.org/docs/9.4/static/sql-copy.html

**kafka_consume_begin ( parallelism = 1, format = 'text', delimiter = '\\t' )**

	Same as above, but launches all previously created consumers instead of for a specific stream-topic pair.

**kafka_consume_end ( text, text )**

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
