.. _replication:

..  Replication

主从复制集
=======================

..  Setting up replication for PipelineDB is identical to how you would set up replication on regular PostgreSQL. If you've already done so in the past, all of this might sound extremely familiar. If not, then it might be worth a read because setting up replication on PostgreSQL has a lot of quirks, mostly as a result of how replication has evolved overtime. `The history of replication in PostgreSQL <http://peter.eisentraut.org/blog/2015/03/03/the-history-of-replication-in-postgresql/>`_ by Peter Eisentraut is a fun read if you want to learn more about this evolution.

PipelineDB配置复制集的方式跟普通的PostgreSQL一摸一样。如果您以前搭建过PostgreSQL的复制集，后面的所有操作都是及其相似的。如果没有这类经验的话，本章节是值得一读的，因为PostgreSQL复制集的搭建存在许多特性，这些特性大多是伴随着技术的演进形成的。这是由Peter Eisentraut撰写的 `PostgreSQL发展历史 <http://peter.eisentraut.org/blog/2015/03/03/the-history-of-replication-in-postgresql/>`_ ，里面记录了技术的演进过程。

..  We're not going to look at old replication methods such as `Log-Shipping Standby Servers <http://www.postgresql.org/docs/9.3/static/warm-standby.html#WARM-STANDBY>`_ since they're overly complex and not very robust. The only reason to use them would be if you had an old PostgreSQL version running, but since we're built into the PostgreSQL 9.5 core, we can leverage all the latest and greatest features PostgreSQL has to offer.

我们不会涉及如 `Log-Shipping Standby Servers <http://www.postgresql.org/docs/9.3/static/warm-standby.html#WARM-STANDBY>`_ 之类的的老的复制技术，因为这些技术复杂且不稳定。唯一的阅读理由可能就是您正在使用老版本的PostgreSQL，但自PostgreSQL 9.5版本之后版本已经最新的强大特性。

..  Streaming Replication

流复制
---------------------

..  PipelineDB supports PostgreSQL's `streaming replication <http://www.postgresql.org/docs/9.3/static/warm-standby.html#STREAMING-REPLICATION>`_ (both asynchronous and synchronous variants) out of the box. Using streaming replication, we can create a hot-standby node which keeps up to date with the primary by *tailing* the write-ahead log and can serve read-only requests. In case the primary fails, the hot-standby can be promoted to be the new primary.

PostgreSQL的 `流复制 <http://www.postgresql.org/docs/9.3/static/warm-standby.html#STREAMING-REPLICATION>`_ （同步和异步模式）对PipelineDB是开箱即用的，我们可以创建一个通过 *tailing* 预写日志（write-ahead log，WAL）与主节点保持同步并且提供只读服务的热备节点。主节点宕机时，热备节点可以升级为新的主节点。

..  Let's say we already have a PipelineDB instance running on :code:`localhost:5432`. The first thing we need to do is create a role on the primary with :code:`REPLICATION` previledges. This role will be used by the standby to connect to the primary and stream the WAL.

假定我们已经装好了PipelineDB并运行在 :code:`localhost:5432`。首先，我们需要在主节点上创建一个具有 :code:`REPLICATION` 权限的 **role**。这个 **role** 可用于从节点流式读取主节点的WAL。

.. code-block:: bash

  $ psql -h localhost -p 5432 -d postgres -c \
  "CREATE ROLE replicator WITH LOGIN REPLICATION;"

  CREATE ROLE

..  You can also create the role with a :code:`PASSWORD` option, in case your primary is on the open network (free tip: it never should be).

如果您的主节点在开放的网络环境下（这种情况是不应该存在的），您可以在创建 **role** 时设置 :code:`PASSWORD`。

..  Next we need to add an entry for the standby to the `pg_hba.conf <http://www.postgresql.org/docs/current/static/auth-pg-hba-conf.html>`_ file. You can find it in the data directory of the primary. The :code:`pg_hba.conf` file handles all client authentication details for PipelineDB. For our example, we'll append the following entry to it, but for any real-world setup it will almost always be different.

接下来需要将从节点的网络信息添加到主节点的 `pg_hba.conf <http://www.postgresql.org/docs/current/static/auth-pg-hba-conf.html>`_ 文件。您可以在主节点的数据目录下找到该文件。:code:`pg_hba.conf` 文件包含所有的客户端认证连接信息。比如，我们添加如下配置，可以根据实际情况进行调整：

.. code-block:: bash

  host replication replicator 0.0.0.0/0 trust

..  Next, we need to set a few configuration parameters on the primary by either updating the :code:`postgresql.conf` file or passing them as :code:`-c` flags when starting up :code:`postgresql`. Set `wal_level <http://www.postgresql.org/docs/9.4/static/runtime-config-wal.html#GUC-WAL-LEVEL>`_ to :code:`hot_standby`, `hot_standby <http://www.postgresql.org/docs/9.0/static/runtime-config-wal.html#GUC-HOT-STANDBY>`_ to :code:`on`, `max_replication_slots <http://www.postgresql.org/docs/9.4/static/runtime-config-replication.html#GUC-MAX-REPLICATION-SLOTS>`_ to 1, and `max_wal_senders <http://www.postgresql.org/docs/9.4/static/runtime-config-replication.html#GUC-MAX-WAL-SENDERS>`_ to 2. Even though one standby node only needs one sender connection, 2 are needed while bootstrapping (not necessarily, but at least in the method documented below). You will need to restart the primary after updating these parameters.

然后，我们需要更新 :code:`postgresql.conf` 文件或在执行 :code:`postgresql` 时通过 :code:`-c` 将其传递进去。将 Set `wal_level <http://www.postgresql.org/docs/9.4/static/runtime-config-wal.html#GUC-WAL-LEVEL>`_ 设置为 :code:`hot_standby`，`hot_standby <http://www.postgresql.org/docs/9.0/static/runtime-config-wal.html#GUC-HOT-STANDBY>`_ 设置为 :code:`on`，`max_replication_slots <http://www.postgresql.org/docs/9.4/static/runtime-config-replication.html#GUC-MAX-REPLICATION-SLOTS>`_ 设置为1，`max_wal_senders <http://www.postgresql.org/docs/9.4/static/runtime-config-replication.html#GUC-MAX-WAL-SENDERS>`_ 设置为2。即使一个从节点只需要建立一条发送的连接，但在启动时需要建立两条（不是必须的，但在下面的文档中是必要的）。修改完这些参数后需要重启主节点。

..  Last we will create a `replication slot <http://www.postgresql.org/docs/9.4/static/warm-standby.html#STREAMING-REPLICATION-SLOTS>`_ for the standby. Replication slots are a means for the standby to *register* with the primary, so that it is always aware of what WAL segments need to be kept around. Once a standby has consumed a WAL segment, it updates the :code:`restart_lsn` column in the `pg_replication_slots <http://www.postgresql.org/docs/9.4/static/catalog-pg-replication-slots.html>`_ catalog so that the primary knows it can now garbage collect that WAL segment.

最后，为从节点创建一个 `replication slot <http://www.postgresql.org/docs/9.4/static/warm-standby.html#STREAMING-REPLICATION-SLOTS>`_。Replication slots用于从节点在主节点上*注册* 工具，它使主节点能获悉需要保留的WAL片段。一旦从节点消费了一个WAL片段，它就会更新 `pg_replication_slots <http://www.postgresql.org/docs/9.4/static/catalog-pg-replication-slots.html>`_ 目录中的 :code:`restart_lsn` 列，以便主节点知悉当前可回收的WAL片段。

.. code-block:: bash

  $ psql -h localhost -p 5432 -d postgres -c \
  "SELECT * FROM pg_create_physical_replication_slot('replicator_slot');"

      slot_name    | xlog_position
  -----------------+---------------
   replicator_slot |
  (1 row)


..  This is all the setup work we need to do on the primary. Let's move on to the standby now. The first thing we need to do on the standby is to take a base backup of the primary. You can think of this as :code:`rsync`\-ing the primary's data directory which the standby will use as its starting point. For this we use the :code:`pipeline-basebackup` utility (analagous to `pg_basebackup <http://www.postgresql.org/docs/9.4/static/app-pgbasebackup.html>`_). You can also use :code:`rsync`\—it tends to be a little faster, but at the added complexity of dealing with authentication setups yourself. :code:`pipeline-basebackup` uses a normal PostgreSQL connection to ship all the base files so you don't have to worry about auth details.

以上是我们需要在主节点上执行的所有操作，下面开始配置从节点。第一步，选取主节点的基础备份，您可以将主节点的数据目录 :code:`rsync` 到从节点中 ———— 从节点需要基于这些数据来启动。为此，我们使用 :code:`pipeline-basebackup` 工具（类似于 `pg_basebackup <http://www.postgresql.org/docs/9.4/static/app-pgbasebackup.html>`_）。您也可以使用 :code:`rsync` ————这通常更快一点，但会使自身的鉴权设置变得复杂。:code:`pipeline-basebackup` 使用普通的PostgreSQL连接来传输基础文件，所以您不用担心认证细节。

.. code-block:: bash

  $ pg_basebackup -X stream -D /path/to/standby_datadir -h localhost -p 5432 -U replicator

..  This :code:`-X stream` argument is what requires the second slot when taking a base backup. Essentially what this does is stream the WAL for changes that take place while the base backup is happening, so we don't need to manually configure the `wal_keep_segments <http://www.postgresql.org/docs/9.4/static/runtime-config-replication.html#GUC-WAL-KEEP-SEGMENTS>`_ parameter.

:code:`-X stream` 选项就是配置第二个slot的原因。本质上来说，这个操作就是在基础备份发生变化时，将WAL的变化也实时传输到从节点，所以我们不必手动配置 `wal_keep_segments <http://www.postgresql.org/docs/9.4/static/runtime-config-replication.html#GUC-WAL-KEEP-SEGMENTS>`_ 参数。

..  The final thing we need to do is write a `recovery.conf <http://www.postgresql.org/docs/9.4/static/standby-settings.html>`_ in the standby's data directory which tells the PipelineDB instance that it needs to operate under standby mode and how to connect to the primary node. For us it will look like:

最后一步，在从节点的数据目录中添加一个 `recovery.conf <http://www.postgresql.org/docs/9.4/static/standby-settings.html>`_ 文件来通知PipelineDB实例切换到standby模式以及如何连接主节点：

.. code-block:: bash

  standby_mode = 'on'
  primary_slot_name = 'replicator_slot'
  primary_conninfo = 'user=replicator host=localhost port=5432'
  recovery_target_timeline = 'latest'

..  We're all set now. Let's fire off the hot standby on post :code:`6544`.

设置完毕，在 :code:`6544` 端口上启动热备节点。

.. code-block:: bash

  pg_ctl start -D /path/to/standby_datadir -o "-p 6544"

..   You should see something like the following in the standby's log file:

启动后，您应该会看到如下所示的日式：

.. code-block:: bash

  LOG:  entering standby mode
  LOG:  redo starts at 0/5000028
  LOG:  consistent recovery state reached at 0/50000F0
  LOG:  database system is ready to accept read only connections
  LOG:  started streaming WAL from primary at 0/6000000 on timeline 1

..  Just to make sure, connect to the standby and confirm it's in recovery mode.

确认从节点以recovery模式运行：

.. code-block:: bash

  $ psql -h localhost -p 6544 -d postgres -c \
  "SELECT pg_is_in_recovery();"

   pg_is_in_recovery
  -------------------
   t
  (1 row)

..  High Availability

高可用
-----------------

PostgreSQL doesn't come with high availability options out of the box. Most deployments will rely on manually promoting the hot standby in case of a primary failure. `Failover <http://www.postgresql.org/docs/9.4/static/warm-standby-failover.html>`_ can be triggered by :code:`pg_ctl promote` or touching a trigger file is there is a :code:`trigger_file` setting in the :code:`recovery.conf` file. `Compose.io <https://www.compose.io>`_ has a good `blog post <https://www.compose.io/articles/high-availability-for-postgresql-batteries-not-included/>`_ about how they designed their HA solution. You could potentially reuse their `Governor <https://github.com/compose/governor>`_ system; make sure to change the PostgreSQL binaries referenced in the code to their PipelineDB equivalent ones though.

PostgreSQL没有开箱即用的高可用方案。大多数部署都是依赖于在主节点宕机时手动切换到热备节点。`Failover <http://www.postgresql.org/docs/9.4/static/warm-standby-failover.html>`_ 可以被 :code:`pg_ctl promote` 触发，也可以通过在 :code:`recovery.conf` 文件中配置 :code:`trigger_file` 来实现。`Compose.io <https://www.compose.io>`_ 中有一篇讲述如何设计高可用的 `博客 <https://www.compose.io/articles/high-availability-for-postgresql-batteries-not-included/>`_，您可以复用他们的 `Governor <https://github.com/compose/governor>`_ 系统，确保将代码中引用的PostgreSQL二进制文件切换到PipelineDB。

..  Please get in touch if all of this seems inadequte and we'll help you figure something out!
如果以上内容对您来说不够丰富，请联系我们，我们将提供一些指导！
