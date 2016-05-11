.. _integrations:

Integrations
============================

Apache Kafka
------------

PipelineDB supports ingesting data from Kafka topics into streams. All of this functionality is contained in the **pipeline_kafka** extension. Internally, **pipeline_kafka** uses `PostgreSQL's COPY`_ infrastructure to transform Kafka messages into rows that PipelineDB understands.

The repository for the extension is located `here <https://github.com/pipelinedb/pipeline_kafka>`_. Instructions for building and installing the extension can be found in the :code:`README.md` file.

To enable the extension, it must be explicitly loaded:

.. _`PostgreSQL's COPY`: http://www.postgresql.org/docs/current/static/sql-copy.html

.. code-block:: pipeline

	# CREATE EXTENSION pipeline_kafka;
	CREATE EXTENSION

Before you can start using **pipeline_kafka**, you must add a broker for your Kafka deployment.

**pipeline_kafka.add_broker ( hostname text )**

**hostname** is a string of the form :code:`<host>[:<port>]`. Multiple brokers can be added by calling **pipeline_kafka.add_broker** for each host.

Consuming Messages
~~~~~~~~~~~~~~~~~~

**pipeline_kafka.consume_begin ( topic text, stream text, format := 'text', delimiter := E'\\t', quote := NULL, escape := NULL, batchsize := 1000, parallelism := 1, start_offset := NULL )**

Launches **parallelism** number of background worker processes that each reads messages from the given Kafka topic into the given stream. The target stream must be created with :code:`CREATE STREAM` beforehand. All partitions of the given topic will be spread evenly across each worker process. The optional **format**, **delimiter**, **escape** and **quote** arguments are analagous to the :code:`FORMAT`, :code:`DELIMITER` :code:`ESCAPE` and :code:`QUOTE` options for the `PostgreSQL COPY`_ command. **batchsize** controls the :code:`batch_size` parameter passed to the Kafka client. **start_offset** specifies the offset from which to start reading the Kafka topic partitions. **pipeline_kafka** continuously saves the offset its read till durably in the database. If start_offset is :code:`NULL`, we start from the saved offset or the end of the partition if there is no saved offset. A start_offset of -1 will start reading end of each partition and -2 will start consuming from the beginning of each partition. Using any other start_offset would be an odd thing to do, since offsets are unrelated among partitions.

.. _`PostgreSQL COPY`: http://www.postgresql.org/docs/current/static/sql-copy.html

**pipeline_kafka.consume_begin ( )**

	Same as above, but launches all previously created consumers instead of for a specific stream-topic pair.

**pipeline_kafka.consume_end ( topic text, stream text )**

	Terminates background consumer processes for the given stream-topic pair.

**pipeline_kafka.consume_end ( )**

	Same as above, but terminates all consumer processes.

Producing Messages
~~~~~~~~~~~~~~~~~~

.. versionadded:: 0.9.1

**pipeline_kafka.produce_message ( topic text, message bytea, partition := NULL, key := NULL )**

Produces a single **message** into the target **topic**. Both **partition** and **key** are optional. By default the parition remains unassigned so the broker will decide which parition to produce the message to depending on the topic's paritioner function. If you want to produce the message into a specific partition, specify it as an :code:`integer`. **key** is a :code:`bytea` argument which will be used as the key to the partition function.

**pipeline_kafka.emit_tuple ( topic, partition, key )**

This is a trigger function that can be used to emit tuples into a Kafka stream in JSON format. It can only be used for a :code:`AFTER INSERT OR UPDATE` and :code:`FOR EACH ROW` trigger. In case of an :code:`UPDATE`, the new updated tuples is emitted. A **topic** must be provided, where as **partition** and **key** are both optional. Since this is a trigger function, all arguments must be passed as string literals and there is no way to specify keyword arguments. If you only want to specify a **topic** and **key**, use :code:`'-1'` as the parition which will keep the partition unassigned. **key** is the name of the column in the tuple being emitted whose value should be used as the parition key.

Metadata
~~~~~~~~

**pipeline_kafka** uses several tables to durably keep track of its own state across system restarts:

**pipeline_kafka.consumers**

	Stores the metadata for each stream-topic consumer that is created by **pipeline_kafka.consume_begin**.

**pipeline_kafka.brokers**

	Stores all Kafka brokers that consumers can connect to.

**pipeline_kafka.offsets**

	Stores Kafka topic offsets so that consumers can begin reading messages from where they left off before termination or system restarts.

-----------------------

.. note:: See `SQL on Kafka`_ for an in-depth tutorial on using Kafka with PipelineDB.

.. _`SQL on Kafka`: https://www.pipelinedb.com/blog/sql-on-kafka

Amazon Kinesis
--------------

PipelineDB also supports ingesting data from Amazon Kinesis streams. This functionality is provided by the **pipeline_kinesis** extension. Internally, the extension manages bgworkers that are consuming data using the `AWS SDK`_, and copying it into pipeline streams.

The repository for the extension is located `here <https://github.com/pipelinedb/pipeline_kinesis>`_. Instructions for building and installing the extension can be found in the :code:`README.md` file.

To enable the extension, it must be explicitly loaded:

.. code-block:: pipeline

	# CREATE EXTENSION pipeline_kinesis;
	CREATE EXTENSION

To start ingestion, you must first tell pipeline where and how to get kinesis 
data by configuring an endpoint:

**pipeline_kinesis.add_endpoint( name text, region text, credfile text := NULL, url text := NULL )**

**name** is a unique identifier for the endpoint. **region** is a string identifying the AWS region, e.g. :code:`us-east-1` or :code:`us-west-2`. 

**credfile** is an optional parameter that allows overriding the default file location for AWS credentials. 

**url** is an optional parameter that allows the use a different (non-AWS) kinesis server. This is mostly useful for testing with local kinesis servers such as `kinesalite`_.

.. _`kinesalite`: https://github.com/mhart/kinesalite
.. _`AWS SDK`: https://github.com/aws/aws-sdk-cpp

Consuming Messages
~~~~~~~~~~~~~~~~~~

**pipeline_kinesis.consume_begin ( endpoint text, stream text, relation text, format text := 'text', delimiter text := E'\\t', quote text := NULL, escape text := NULL, batchsize int := 1000, parallelism int := 1, start_offset int := NULL )**

Starts a logical consumer group that consumes kinesis messages from kinesis **stream** at **endpoint** and copies them into the pipeline stream **relation**.

**parallelism** is used to specify the number of background worker processes that should be used per consumer to balance load. Note - this does not need to be set to the number of shards, since the extension is internally threaded. The default value of 1 is sufficient unless the consumer starts to fall behind.

**format**, **delimiter**, **escape** and **quote** are optional parameters used to control the format of the copied rows, as in `PostgreSQL COPY`_.

**batchsize** is passed on to the AWS SDK and controls the :code:`Limit` parameter used in `Kinesis GetRecords`_.

**start_offset** is used to control the stream position that the extension starts reading from. -1 is used to start reading from the end of the stream, and -2 to read from the start. Internally, these map to :code:`TRIM_HORIZON` and :code:`LATEST`. See `Kinesis GetShardIterator`_ for more details.

.. _`PostgreSQL COPY`: http://www.postgresql.org/docs/current/static/sql-copy.html
.. _`Kinesis GetRecords`: https://docs.aws.amazon.com/kinesis/latest/APIReference/API_GetRecords.html
.. _`Kinesis GetShardIterator`: https://docs.aws.amazon.com/kinesis/latest/APIReference/API_GetShardIterator.html

**pipeline_kinesis.consume_end (endpoint text, stream text, relation text)**

    Terminates all background worker process for a particular consumer.

**pipeline_kinesis.consume_begin()**

	Launches all previously created consumers.

**pipeline_kinesis.consume_end()**

    Terminates all background worker processes for all previously started consumers. 

Metadata
~~~~~~~~

**pipeline_kinesis** uses several tables to durably keep track of its own state across system restarts:

**pipeline_kinesis.endpoints**

	Stores the metadata for each endpoint that is created by **kinesis_add_endpoint**

**pipeline_kinsesis.consumers**

	Stores the metadata for each consumer that is created by **kinesis_consume_begin**.

**pipeline_kinsesis.seqnums**

	Stores the per-shard metadata for each consumer. Namely, seqnums.

-----------------------
