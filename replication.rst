.. _replication:

Replication
=======================

Setting up replication for PipelineDB is identical to how you would set up replication on regular PostgreSQL. If you've already done so in the past, all of this might sound extremely familiar. If not, then it might be worth a read because setting up replication on PostgreSQL has a lot of quirks, mostly as a result of how replication has evolved overtime. `The history of replication in PostgreSQL <http://peter.eisentraut.org/blog/2015/03/03/the-history-of-replication-in-postgresql/>`_ by Peter Eisentraut is a fun read if you want to learn more about this evolution.

We're not going to look at old replication methods such as `Log-Shipping Standby Servers <http://www.postgresql.org/docs/9.3/static/warm-standby.html#WARM-STANDBY>`_ since they're overly complex and not very robust. The only reason to use them would be if you had an old PostgreSQL version running, but since we're built into the PostgreSQL 9.4 core, we can leverage all the latest and greatest features PostgreSQL has to offer.

Streaming Replication
---------------------

PipelineDB supports PostgreSQL's `streaming replication <http://www.postgresql.org/docs/9.3/static/warm-standby.html#STREAMING-REPLICATION>`_ (both asynchronous and synchronous variants) out of the box. Using streaming replication, we can create a hot-standby
node which keeps up to date with the primary by *tailing* the write-ahead log and can serve read-only requests. In case the primary fails, the hot-standby can be promoted to be the new primary.

Let's say we already have a PipelineDB instance running on :code:`localhost:5432`. The first thing we need to do is create a role on the primary with :code:`REPLICATION` previledges. This role will be used by the standby to connect to the primary and stream the WAL.

.. code-block:: bash

  $ psql -h localhost -p 5432 -d pipeline -c \
  "CREATE ROLE replicator WITH LOGIN REPLICATION;"

  CREATE ROLE

You can also create the role with a :code:`PASSWORD` option, in case your primary is on the open network (free tip: it never should be).

Next we need to add an entry for the standby to the `pg_hba.conf <http://www.postgresql.org/docs/9.4/static/auth-pg-hba-conf.html>`_ file. You can find it in the data directory of the primary. The :code:`pg_hba.conf` file handles all client authentication details for PipelineDB. For our example, we'll append the following entry to it, but for any real-world setup it will almost always be different.

.. code-block:: bash

  host replication replicator 0.0.0.0/0 trust

Next, we need to set a few configuration parameters on the primary by either updating the :code:`pipelinedb.conf` file or passing them as :code:`-c` flags when starting up :code:`pipeline-server`. Set `wal_level <http://www.postgresql.org/docs/9.4/static/runtime-config-wal.html#GUC-WAL-LEVEL>`_ to :code:`hot_standby`, `hot_standby <http://www.postgresql.org/docs/9.0/static/runtime-config-wal.html#GUC-HOT-STANDBY>`_ to :code:`on`, `max_replication_slots <http://www.postgresql.org/docs/9.4/static/runtime-config-replication.html#GUC-MAX-REPLICATION-SLOTS>`_ to 1, and `max_wal_senders <http://www.postgresql.org/docs/9.4/static/runtime-config-replication.html#GUC-MAX-WAL-SENDERS>`_ to 2. Even though one standby node only needs one sender connection, 2 are needed while bootstrapping (not necessarily, but at least in the method documented below). You will need to restart the primary after updating these parameters.

Last we will create a `replication slot <http://www.postgresql.org/docs/9.4/static/warm-standby.html#STREAMING-REPLICATION-SLOTS>`_ for the standby. Replication slots are a means for the standby to *register* with the primary, so that it is always aware of what WAL segments need to be kept around. Once a standby has consumed a WAL segment, it updates the :code:`restart_lsn` column in the `pg_replication_slots <http://www.postgresql.org/docs/9.4/static/catalog-pg-replication-slots.html>`_ catalog so that the primary knows it can now garbage collect that WAL segment.

.. code-block:: bash

  $ psql -h localhost -p 5432 -d pipeline -c \
  "SELECT * FROM pg_create_physical_replication_slot('replicator_slot');"

      slot_name    | xlog_position
  -----------------+---------------
   replicator_slot |
  (1 row)


This is all the setup work we need to do on the primary. Let's move on to the standby now. The first thing we need to do on the standby is to take a base backup of the primary. You can think of this as :code:`rsync`\-ing the primary's data directory which the standby will use as its starting point. For this we use the :code:`pipeline-basebackup` utility (analagous to `pg_basebackup <http://www.postgresql.org/docs/9.4/static/app-pgbasebackup.html>`_). You can also use :code:`rsync`\—it tends to be a little faster, but at the added complexity of dealing with authentication setups yourself. :code:`pipeline-basebackup` uses a normal PostgreSQL connection to ship all the base files so you don't have to worry about auth details.

.. code-block:: bash

  $ pipeline-basebackup -X stream -D /path/to/standby_datadir -h localhost -p 5432 -U replicator

This :code:`-X stream` argument is what requires the second slot when taking a base backup. Essentially what this does is stream the WAL for changes that take place while the base backup is happening, so we don't need to manually configure the `wal_keep_segments <http://www.postgresql.org/docs/9.4/static/runtime-config-replication.html#GUC-WAL-KEEP-SEGMENTS>`_ parameter.

The final thing we need to do is write a `recovery.conf <http://www.postgresql.org/docs/9.4/static/standby-settings.html>`_ in the standby's data directory which tells the PipelineDB instance that it needs to operate under standby mode and how to connect to the primary node. For us it will look like:

.. code-block:: bash

  standby_mode = 'on'
  primary_slot_name = 'replicator_slot'
  primary_conninfo = 'user=replicator host=localhost port=5432'
  recovery_target_timeline = 'latest'

We're all set now. Let's fire off the hot standby on post :code:`6544`.

.. code-block:: bash

  pipeline-ctl start -D /path/to/standby_datadir -o "-p 6544"

You should see something like the following in the standby's log file:

.. code-block:: bash

  LOG:  entering standby mode
  LOG:  redo starts at 0/5000028
  LOG:  consistent recovery state reached at 0/50000F0
  LOG:  database system is ready to accept read only connections
  LOG:  started streaming WAL from primary at 0/6000000 on timeline 1

Just to make sure, connect to the standby and confirm it's in recovery mode.

.. code-block:: bash

  $ psql -h localhost -p 6544 -d pipeline -c \
  "SELECT pg_is_in_recovery();"

   pg_is_in_recovery
  -------------------
   t
  (1 row)

High Availability
-----------------

PostgreSQL doesn't come with high availability options out of the box. Most deployments will rely on manually promoting the hot standby in case of a primary failure. `Failover <http://www.postgresql.org/docs/9.4/static/warm-standby-failover.html>`_ can be triggered by :code:`pipeline-ctl promote` or touching a trigger file is there is a :code:`trigger_file` setting in the :code:`recovery.conf` file. `Compose.io <https://www.compose.io>`_ has a good `blog post <https://www.compose.io/articles/high-availability-for-postgresql-batteries-not-included/>`_ about how they designed their HA solution. You could potentially reuse their `Governor <https://github.com/compose/governor>`_ system; make sure to change the PostgreSQL binaries referenced in the code to their PipelineDB equivalent ones though.

Please get in touch if all of this seems inadequte and we'll help you figure something out!

Logical Decoding
----------------

While we haven't yet played around with logical decoding, there is no reason to believe that it wouldn't work without any hiccups as well. If you are a user of logical decoding and find that PipelineDB is incompatible with it, please let us know!
