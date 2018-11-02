.. _conf:

Configuration
==============

**pipelinedb.stream_insert_level**

  Determines when a client :code:`INSERT` operation will return. Its options are:

    * **async**: return as soon as the inserts have been loaded into the server's memory
    * **sync_receive** (default): return as soon as the inserts have been received by a worker process
    * **sync_commit**: return only when the downstream combiner has committed all changes resulting from the inserted rows

.. note:: :code:`sync_commit` is primarly used for testing purposes and is not meant for production workloads due to significantly increased write latency.

**pipelinedb.num_combiners**

  Sets the number of parallel continuous query combiner processes to use for each database. A higher number will utilize multiple cores and increase throughput until we're I/O bound. *Default: 1.*

**pipelinedb.commit_interval**

  Sets the number of milliseconds that combiners will keep combining in memory before committing the result. A longer commit interval will increase performance at the expense of less frequent continuous view updates and more potential data loss. *Default: 50ms.*

**pipelinedb.num_workers**

  Sets the number of parallel continuous query worker processes to use for each database. A higher number will utilize multiple cores and increase throughput until we're CPU bound. *Default: 1.*

**pipelinedb.num_queues**

  Sets the number of parallel continuous query queue processes to use for each database. Queues are used when workers and combiners are writing out results to streams, necessitating an IPC queue to prevent stalls. *Default: 1.*

**pipelinedb.num_reapers**

  Sets the number of parallel reaper processes to use for each database. Reaper processes handle :ref:`ttl-expiration`. *Default: 1.*

**pipelinedb.ttl_expiration_batch_size**

  Sets the maximum number of rows that a reaper will delete from a continuous view at a time. This is designed to minimize long-running transactions. A value of **0** means an unlimited number of rows can be deleted in a given transaction.  *Default: 10000.*

**pipelinedb.ttl_expiration_threshold**

  Sets the percentage of a TTL that must have elapsed since a reaper last deleted rows from a continuous view before attempting to delete from it again. A lower percentage will yield more aggressive expiration at the expensive of more delete transactions on the continuous view. *Default: 5%.*

**pipelinedb.batch_size**

  Sets the maximum number of events to accumulate before executing a continuous query plan on them. A higher value usually yields less frequent continuous view updates, but adversely affects latency and can cause more data loss in case of process crashes. *Default: 10000.*

**pipelinedb.combiner_work_mem**

  Sets the maximum memory to be used for combining partial results for continuous queries. This much memory can be used by each combiner processes's internal sort operation and hash table before switching to temporary disk files. *Default: 256mb.*

**pipelinedb.anonymous_update_checks**

  Toggles whether PipelineDB should anonymous check if a new version is available. *Default: true.*

**pipelinedb.matrels_writable**

  Toggles whether changes can be directly made to materialization tables. *Default: false.*

**pipelinedb.ipc_hwm**

  Sets the high watermark for IPC messages between worker and combiner processes. *Default: 10.*

**pipelinedb.max_wait**

  Sets the time a continuous query process will wait for a batch to accumulate. A higher value usually yields less frequent continuous view updates, but adversely affects latency and can cause more data loss in case of process crashes. *Default: 10ms.*

**pipelinedb.fillfactor**

  Sets the default fillfactor to use for materialization tables. *Default: 50.*

**pipelinedb.sliding_window_step_factor**

  Sets the default step size for a sliding window query as a percentage of the window size. A higher number will improve performance but tradeoff refresh interval. *Default: 5.*

