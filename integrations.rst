.. _integrations:

..  Integrations

应用集成
============================

Apache Kafka
------------

..  PipelineDB supports ingesting data from Kafka topics into streams. All of this functionality is contained in the **pipeline_kafka** extension. Internally, **pipeline_kafka** uses `PostgreSQL's COPY`_ infrastructure to transform Kafka messages into rows that PipelineDB understands.

PipelineDB支持从Kafka的topic中提取数据到流中。相关的功能集成在 **pipeline_kafka** 插件中。**pipeline_kafka** 以  `COPY <PostgreSQL's COPY>`_ 方式将Kafka中的数据写入到流中。

.. note::
    ..  The **pipeline_kafka** extension is officially supported but does not ship with the PipelineDB packages and therefore must be installed separately. The repository for the extension is located `here <https://github.com/pipelinedb/pipeline_kafka>`_. Instructions for building and installing the extension can be found in the :code:`README.md` file.

    **pipeline_kafka** 插件是官方支持的但没有打包到PipelineDB中，因此需要单独安装。可直接访问位于github中的 `项目仓库 <https://github.com/pipelinedb/pipeline_kafka>`_，:code:`README.md` 中说明了插件的安装方法。

..  **pipeline_kafka** internally uses shared memory to sync state between background workers, so it must be loaded as a shared library. You can do so by adding the following line to your :code:`pipelinedb.conf` file. If you're already loading some shared libraries, then simply add :code:`pipeline_kafka` as a comma-separated list.

**pipeline_kafka** 通过共享内存来同步后台worker间的状态，所以必须作为共享库被载入。您可以在 :code:`pipelinedb.conf` 配置文件中将添加到配置中。如果已添加了其它共享库，以逗号分隔将其依次添加即可：

.. code-block:: sh

  shared_preload_libraries = pipeline_kafka


..  You can now load the extention into a database:

在客户端中创建插件：

.. _`PostgreSQL's COPY`: http://www.postgresql.org/docs/current/static/sql-copy.html

.. code-block:: psql

	postgres=# CREATE EXTENSION pipeline_kafka;
	CREATE EXTENSION

..  Before you can start using **pipeline_kafka**, you must add a broker for your Kafka deployment.

使用 **pipeline_kafka** 前必须配置好Kafka Broker。

**pipeline_kafka.add_broker ( hostname text )**

    ..  **hostname** is a string of the form :code:`<host>[:<port>]`. Multiple brokers can be added by calling **pipeline_kafka.add_broker** for each host.

    **hostname** 是格式为 :code:`<host>[:<port>]` 的字符。多个Broker可以使用 **pipeline_kafka.add_broker** 添加多次。

..  Consuming Messages

消费Kafka消息
~~~~~~~~~~~~~~~~~~

**pipeline_kafka.consume_begin ( topic text, stream text, format := 'text', delimiter := E'\\t', quote := NULL, escape := NULL, batchsize := 1000, maxbytes := 32000000, parallelism := 1, start_offset := NULL )**

    ..  Launches **parallelism** number of background worker processes that each reads messages from the given Kafka topic into the given stream. The target stream must be created with :code:`CREATE STREAM` beforehand. All partitions of the given topic will be spread evenly across each worker process.

    **parallelism** 是后台将Kafka Topic数据消费到流中的worker进程数。输出的流必须提前通过 :code:`CREATE STREAM` 创建。指定Topic中所有分区的数据会分摊给每个woker进程进行处理。

    ..  The optional **format**, **delimiter**, **escape** and **quote** arguments are analagous to the :code:`FORMAT`, :code:`DELIMITER` :code:`ESCAPE` and :code:`QUOTE` options for the `PostgreSQL COPY`_ command, except that **pipeline_kafka** supports one additional format: **json**. The **json** format interprets each Kafka message as a JSON object.

    可选的 **format**, **delimiter**, **escape** 和 **quote** 参数类似于 `COPY <PostgreSQL COPY>`_ 指令中的 :code:`FORMAT`, :code:`DELIMITER` :code:`ESCAPE` 和 :code:`QUOTE`，除此之外，**pipeline_kafka** 还额外地支持 **json** 格式，插件会将Kafka消息转换为JSON对象。

    ..  **batchsize** controls the :code:`batch_size` parameter passed to the Kafka client. We force a :code:`COPY` and commit cycle after :code:`batchsize` messages have been buffered.

    **batchsize** 控制了Kafka客户端的 :code:`batch_size` 参数。客户端会在消费并缓存了 :code:`batch_size` 条消息后会以 :code:`COPY` 的形式写入到流中，一直循环此过程。

    ..  **maxbytes** controls the :code:`fetch.message.max.bytes` parameter passes to the Kafka client. We force a :code:`COPY` and commit cycle after :code:`maxbytes` data has been buffered.

    **maxbytes** 控制了Kafka客户端的 :code:`fetch.message.max.bytes` 参数。客户端会在消费并缓存了 :code:`maxbytes` 大小的消息后会以 :code:`COPY` 的形式写入到流中，一直循环此过程。

    ..  **start_offset** specifies the offset from which to start reading the Kafka topic partitions.

    **start_offset** 指定Kafka Topic分区消费的起点offset。

    ..  **pipeline_kafka** continuously saves the offset its read till durably in the database. If start_offset is :code:`NULL`, we start from the saved offset or the end of the partition if there is no saved offset. A start_offset of -1 will start reading end of each partition and -2 will start consuming from the beginning of each partition. Using any other start_offset would be an odd thing to do, since offsets are unrelated among partitions.

**pipeline_kafka** 会将Kafka消费的进度保存在数据库中。如果起始offset为 :code:`NULL`，客户端会从数据库记录的offset开始消费，若没有记录，则会以 :code:`offset='latest'` 方式开始消费。start_offset为-1时，会以 :code:`offset='latest'` 方式消费；start_offset为-2时，会以 :code:`offset='earliest'` 方式开始消费。使用其它的offset是很荒诞的，因为这些offset与分区无关。

.. note::

	译者注：如果想指定所有分区的起点offset，直接通过start_offset肯定是不行的，可以手动修改pipeline_kafka.offsets中分区的offset，然后以 :code:`start_offset := NULL` 的方式启动消费。

.. _`PostgreSQL COPY`: http://www.postgresql.org/docs/current/static/sql-copy.html

**pipeline_kafka.consume_begin ( )**

	.. Same as above, but launches all previously created consumers instead of for a specific stream-topic pair.

    启动当前已生成的所有消费者，而不是指定某个topic到stream的消费任务。

**pipeline_kafka.consume_end ( topic text, stream text )**

	.. Terminates background consumer processes for the given stream-topic pair.

    停止某个topic到stream的消费任务。

**pipeline_kafka.consume_end ( )**

	.. Same as above, but terminates all consumer processes.

    停止当前所有的消费任务。

..  Producing Messages

生产Kafka消息
~~~~~~~~~~~~~~~~~~

.. versionadded:: 0.9.1

**pipeline_kafka.produce_message ( topic text, message bytea, partition := NULL, key := NULL )**

    ..  Produces a single **message** into the target **topic**. Both **partition** and **key** are optional. By default the parition remains unassigned so the broker will decide which parition to produce the message to depending on the topic's paritioner function. If you want to produce the message into a specific partition, specify it as an :code:`integer`. **key** is a :code:`bytea` argument which will be used as the key to the partition function.

    生产单条 **message** 到指定 **topic** 中，**partition** 和 **key** 都是可选的。默认情况下，分区是未指定的，所以由Kafka分区函数决定消息写入哪个分区。如果您想将消息写入指定分区，指定将分区对应的 :code:`integer` 赋值给partition参数即可。**key** 是消息中的一个键，用于kafka分区函数计算消息应写入的分区。

**pipeline_kafka.emit_tuple ( topic, partition, key )**

    ..  This is a trigger function that can be used to emit tuples into a Kafka stream in JSON format. It can only be used for a :code:`AFTER INSERT OR UPDATE` and :code:`FOR EACH ROW` trigger. In case of an :code:`UPDATE`, the new updated tuples is emitted. A **topic** must be provided, where as **partition** and **key** are both optional. Since this is a trigger function, all arguments must be passed as string literals and there is no way to specify keyword arguments. If you only want to specify a **topic** and **key**, use :code:`'-1'` as the parition which will keep the partition unassigned. **key** is the name of the column in the tuple being emitted whose value should be used as the parition key.

    这是一个可用于将元组以json格式写入kafka中的触发器函数。它只能作为 :code:`AFTER INSERT OR UPDATE` 和 :code:`FOR EACH ROW` 触发器使用。在 :code:`UPDATE` 情况下，新增的元组会被写入。topic必须指定，**partition** 和 **key** 是可选的。由于这是一个触发器函数，所有参数必须以字符串传递，并且不能指定keyword参数。如果您只想指定 **topic** 和 **key** 而不想指定分区，赋值 :code:`'partition := -1'` 即可。**key** 是元组中列名，用于消息写入到Topic时的分区计算。

Metadata
~~~~~~~~

..  **pipeline_kafka** uses several tables to durably keep track of its own state across system restarts:

**pipeline_kafka** 使用多张表来维系Kafka客户端状态的记录，以支持系统重启后的断点再续：

**pipeline_kafka.consumers**

	.. Stores the metadata for each stream-topic consumer that is created by **pipeline_kafka.consume_begin**.

    存储了每个 stream-topic 消费者的源信息，在 **pipeline_kafka.consume_begin** 时自动创建。

**pipeline_kafka.brokers**

	.. Stores all Kafka brokers that consumers can connect to.

    存储了消费者可连接的所有Kafka Broker。

**pipeline_kafka.offsets**

	.. Stores Kafka topic offsets so that consumers can begin reading messages from where they left off before termination or system restarts.

    存储Kafka Topic消费位移（offset），这样消费者就可以在上次消费中止的offset处重新开始消费。

-----------------------

.. note::
    ..  See `SQL on Kafka`_ for an in-depth tutorial on using Kafka with PipelineDB.

    查看 `SQL on Kafka`_ 深入了解Kafka和PipelineDB的集成。

.. _`SQL on Kafka`: https://www.pipelinedb.com/blog/sql-on-kafka

Amazon Kinesis
--------------

..  PipelineDB also supports ingesting data from Amazon Kinesis streams. This functionality is provided by the **pipeline_kinesis** extension. Internally, the extension manages bgworkers that are consuming data using the `AWS SDK`_, and copying it into pipeline streams.

PipelineDB也支持集成Amazon Kinesis。这个功能由 **pipeline_kinesis** 插件提供。插件通过 `AWS SDK`_ 管理消费者，并将数据 **COPY** 到PipelineDB流中。

..  The repository for the extension is located `here <https://github.com/pipelinedb/pipeline_kinesis>`_. Instructions for building and installing the extension can be found in the :code:`README.md` file.

**pipeline_kinesis** 的 `github仓库 <https://github.com/pipelinedb/pipeline_kinesis>`_ 中的 :code:`README.md` 文件中包含了插件安装说明。

..  To enable the extension, it must be explicitly loaded:

使用插件前必须在客户端显式载入：

.. code-block:: psql

	postgres=# CREATE EXTENSION pipeline_kinesis;
	CREATE EXTENSION

..  To start ingestion, you must first tell pipeline where and how to get kinesis data by configuring an endpoint:

您必须先为管道配置好kinesis数据的endpoint：

**pipeline_kinesis.add_endpoint( name text, region text, credfile text := NULL, url text := NULL )**

    ..  **name** is a unique identifier for the endpoint. **region** is a string identifying the AWS region, e.g. :code:`us-east-1` or :code:`us-west-2`.

    **name** 是endpoint的唯一标识符。**region** 是AWS地区的字符形式的标识符，比如 :code:`us-east-1` 和 :code:`us-west-2`。

    ..  **credfile** is an optional parameter that allows overriding the default file location for AWS credentials.

    **credfile** 是一个可选参数，它可以覆盖AWS证书的默认文件路径。

    ..  **url** is an optional parameter that allows the use a different (non-AWS) kinesis server. This is mostly useful for testing with local kinesis servers such as `kinesalite`_.

    **url** 是一个可选参数，它可以指定其它的（非AWS的）kinesis服务。这在测试如 `kinesalite`_ 之类的本地kinesis服务时是非常有用的。

.. _`kinesalite`: https://github.com/mhart/kinesalite
.. _`AWS SDK`: https://github.com/aws/aws-sdk-cpp

..  Consuming Messages

消费kinesis
~~~~~~~~~~~~~~~~~~

**pipeline_kinesis.consume_begin ( endpoint text, stream text, relation text, format text := 'text', delimiter text := E'\\t', quote text := NULL, escape text := NULL, batchsize int := 1000, parallelism int := 1, start_offset int := NULL )**

    ..  Starts a logical consumer group that consumes kinesis messages from kinesis **stream** at **endpoint** and copies them into the pipeline stream **relation**.

    在 **endpoint** 生成一个kinesis逻辑消费者组来消费kinesis **流数据**，并将数据 **COPY** 到PipelineDB流中。

    ..  **parallelism** is used to specify the number of background worker processes that should be used per consumer to balance load. Note - this does not need to be set to the number of shards, since the extension is internally threaded. The default value of 1 is sufficient unless the consumer starts to fall behind.

    **parallelism** 用于指定每个消费者的后台worker进程数从而实现载入均衡。⚠️由于插件内部是串行的，所以不需要指定分片的数量。默认值1已经够用了，除非消费进度开始落后。

    ..  **format**, **delimiter**, **escape** and **quote** are optional parameters used to control the format of the copied rows, as in `PostgreSQL COPY`_.

    **format**, **delimiter**, **escape** 和 **quote** 是可选参数，用于控制 **COPY** 到PipelineDB中数据的格式。

    ..  **batchsize** is passed on to the AWS SDK and controls the :code:`Limit` parameter used in `Kinesis GetRecords`_.

    **batchsize** 会传入 `AWS SDK`_ 并控制 `Kinesis GetRecords`_ 中的 :code:`Limit` 参数。

    ..  **start_offset** is used to control the stream position that the extension starts reading from. -1 is used to start reading from the end of the stream, and -2 to read from the start. Internally, these map to :code:`TRIM_HORIZON` and :code:`LATEST`. See `Kinesis GetShardIterator`_ for more details.

    **start_offset** 是插件用于记录流消费起点的参数。-1表示从尾开始读，-2表示从头开始读。对应 :code:`TRIM_HORIZON` 和 :code:`LATEST`，详细信息见 `Kinesis GetShardIterator`_。

.. _`PostgreSQL COPY`: http://www.postgresql.org/docs/current/static/sql-copy.html
.. _`Kinesis GetRecords`: https://docs.aws.amazon.com/kinesis/latest/APIReference/API_GetRecords.html
.. _`Kinesis GetShardIterator`: https://docs.aws.amazon.com/kinesis/latest/APIReference/API_GetShardIterator.html

**pipeline_kinesis.consume_end (endpoint text, stream text, relation text)**

    ..  Terminates all background worker process for a particular consumer.

    停止指定消费者的所有worker进程。

**pipeline_kinesis.consume_begin()**

	.. Launches all previously created consumers.

    启动所有已创建的消费者。

**pipeline_kinesis.consume_end()**

    ..  Terminates all background worker processes for all previously started consumers.

    停止所有消费者的worker进程。


Metadata
~~~~~~~~

..  **pipeline_kinesis** uses several tables to durably keep track of its own state across system restarts:

**pipeline_kinesis** 使用多张表记录消费信息：

**pipeline_kinesis.endpoints**

	.. Stores the metadata for each endpoint that is created by **kinesis_add_endpoint**

    存储每个所有通过 **kinesis_add_endpoint** 创建的endpoint。

**pipeline_kinsesis.consumers**

	.. Stores the metadata for each consumer that is created by **kinesis_consume_begin**.

    存储每个通过 **kinesis_consume_begin** 创建的消费者的源信息。

**pipeline_kinsesis.seqnums**

	.. Stores the per-shard metadata for each consumer. Namely, seqnums.

    存储每个消费者的切片源数据，也就是血清（seqnums）。

-----------------------
