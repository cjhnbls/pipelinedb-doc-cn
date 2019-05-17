.. _conf:

..  Configuration

配置
==============

**pipelinedb.stream_insert_level**

  ..    Determines when a client :code:`INSERT` operation will return. Its options are:

  :code:`INSERT` 操作有以下三种模式：

    .. * **async**: return as soon as the inserts have been loaded into the server's memory
    .. * **sync_receive** (default): return as soon as the inserts have been received by a worker process
    .. * **sync_commit**: return only when the downstream combiner has committed all changes resulting from the inserted rows

    * **async**: 异步模式，数据载入数据库内存后返回。
    * **sync_receive** (默认): 同步接收，数据被worker进程接受后返回。
    * **sync_commit**: 同步提交，下游combiner完成所有计算，并更新完变化的数据后返回。

.. note::
    ..  :code:`sync_commit` is primarly used for testing purposes and is not meant for production workloads due to significantly increased write latency.

    :code:`sync_commit` 主要用于测试，并且由于其会极大增大写入载荷，所以不适合上生产环境。


**pipelinedb.num_combiners**

  ..    Sets the number of parallel continuous query combiner processes to use for each database. A higher number will utilize multiple cores and increase throughput until we're I/O bound. *Default: 1.*

  设置并行的combiner进程数，调大后可更好地利用多核性能，并在达到IO瓶颈前增大吞吐量。（默认为1）

**pipelinedb.commit_interval**

  ..    Sets the number of milliseconds that combiners will keep combining in memory before committing the result. A longer commit interval will increase performance at the expense of less frequent continuous view updates and more potential data loss. *Default: 50ms.*

  设置combiner提交结果的毫秒时间间隔，更大的间隔会减小数据更新频率和数据丢失的风险，从而提高性能。

**pipelinedb.num_workers**

  ..    Sets the number of parallel continuous query worker processes to use for each database. A higher number will utilize multiple cores and increase throughput until we're CPU bound. *Default: 1.*

  设置并行的worker进程数，调大后可更好地利用多核性能，并在达到CPU瓶颈前增大吞吐量。（默认为1）

**pipelinedb.num_queues**

  ..    Sets the number of parallel continuous query queue processes to use for each database. Queues are used when workers and combiners are writing out results to streams, necessitating an IPC queue to prevent stalls. *Default: 1.*

  设置并行的queue进程数，**queue** 在worker和combiner向流中写入数据时使用，需要一个IPC队列来防止宕机。（默认为1）

**pipelinedb.num_reapers**

  ..    Sets the number of parallel reaper processes to use for each database. Reaper processes handle :ref:`ttl-expiration`. *Default: 1.*

  设置并行的reaper进程数，reaper进程用于处理 :ref:`数据存活-过期<ttl-expiration>` 。

**pipelinedb.ttl_expiration_batch_size**

  ..    Sets the maximum number of rows that a reaper will delete from a continuous view per transaction. This is designed to minimize long-running transactions. A value of **0** means an unlimited number of rows can be deleted in a given transaction.  *Default: 10000.*

  设置reaper在流视图的每次转换中可删除的最大记录条数，用于减小数据转换过程中的空间载荷，设置为 **0** 可以让每次转换操作清除的记录数变为 **无限大**。（默认10000）

  .. note::
      译者注：对于数据量非常大的流视图，建议将此值设为0，否则过期数据的清理会很慢，磁盘空间占用大。

**pipelinedb.ttl_expiration_threshold**

  ..    Sets the percentage of a TTL that must have elapsed since a reaper last deleted rows from a continuous view before attempting to delete from it again. A lower percentage will yield more aggressive expiration at the expensive of more delete transactions on the continuous view. *Default: 5%.*

  设置每次数据回收相对TTL的延时比例，这个值设置得越低，产生的删除转换就会更多，数据回收的开销就会越大。（默认5%）

  .. note::

      译者注：比如一个流视图的ttl为20小时，ttl_expiration_batch_size为默认值10000，数据每次回收的间隔为20*5%=1小时，每次删除10000条过期数据

**pipelinedb.batch_size**

  ..    Sets the maximum number of events to accumulate before executing a continuous query plan on them. A higher value usually yields less frequent continuous view updates, but adversely affects latency and can cause more data loss in case of process crashes. *Default: 10000.*

  设置流视图每个批次处理的最大event数。这个值越大，流视图数据更新的频率就越低，但会增加数据延时，以及在宕机时造成更多的数据丢失。（默认10000）

**pipelinedb.combiner_work_mem**

  ..    Sets the maximum memory to be used for combining partial results for continuous queries. This much memory can be used by each combiner processes's internal sort operation and hash table before switching to temporary disk files. *Default: 256mb.*

  设置可用于组合结果数据的最大内存。在combiner进程写入磁盘前，这部分内存可用于其内部的排序操作和哈希表。（默认256mb）

**pipelinedb.anonymous_update_checks**

  ..    Toggles whether PipelineDB should anonymous check if a new version is available. *Default: true.*

  PipelineDB新版本检测开关。（默认为true）

**pipelinedb.matrels_writable**

  ..    Toggles whether changes can be directly made to materialization tables. *Default: false.*

  物化视图可修改开关（默认为false）

  .. note::

  	译者注：若为false，则流视图中的数据不可被delete，update。

**pipelinedb.ipc_hwm**

  ..    Sets the high watermark for IPC messages between worker and combiner processes. *Default: 10.*

  设置worker和combiner进程间IPC消息允许的最大延时。

  .. note::

  	译者注：可参考此 `issue <https://github.com/pipelinedb/pipelinedb/issues/2018>`_ 中作者关于ipc_hwm的说明。


**pipelinedb.max_wait**

  ..    Sets the time a continuous query process will wait for a batch to accumulate. A higher value usually yields less frequent continuous view updates, but adversely affects latency and can cause more data loss in case of process crashes. *Default: 10ms.*

  设置进程处理一个batch的最大等待时间。这个值越大，流视图数据更新的频率就越低，但会增加数据延时，以及在宕机时造成更多的数据丢失。（默认10ms）

**pipelinedb.fillfactor**

  ..    Sets the default fillfactor to use for materialization tables. *Default: 50.*

  设置物化视图的填充因子（空间使用率）。（默认50）

**pipelinedb.sliding_window_step_factor**

  ..    Sets the default step size for a sliding window query as a percentage of the window size. A higher number will improve performance but tradeoff refresh interval. *Default: 5.*

  设置滑动窗口步长相对窗口长度的比例。更大的步长会提升性能，但会降低刷新率。（默认5）
