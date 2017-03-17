.. _upgrades:


Upgrades
==============

.. versionadded:: 0.9.5

Most PipelineDB version upgrades will not require anything other than installing the newest version of PipelineDB and simply running the server on top of your data directory from the previous version.

However, some versions change the layout of the internal system catalogs, which requires migrating from the old catalog schema to the new one. One way to accomplish this is to simply dump_ your existing database from the old version, and restore_ it into a data directory created by the latest version.

For small databases, this is actually the most straightforward approach. For larger databases, it can be prohibitively slow. The fastest way to upgrade non-trivial deployments between PipelineDB versions is thus to use the :code:`pipeline-upgrade` tool. :code:`pipeline-upgrade` copies all catalog data from the previous to new PipelineDB version, and then simply copies all other database data at the operating system level.

.. _dump: http://docs.pipelinedb.com/backups.html#backups
.. _restore: http://docs.pipelinedb.com/backups.html#restoring-continuous-views

Its usage is as follows:

.. code-block:: pipeline

	Usage:
		pipeline-upgrade [OPTION]...

	Options:
		-b, --old-bindir=BINDIR       old cluster executable directory
		-B, --new-bindir=BINDIR       new cluster executable directory
		-c, --check                   check clusters only, don't change any data
		-d, --old-datadir=DATADIR     old cluster data directory
		-D, --new-datadir=DATADIR     new cluster data directory
		-j, --jobs                    number of simultaneous processes or threads to use
		-k, --link                    link instead of copying files to new cluster
		-o, --old-options=OPTIONS     old cluster options to pass to the server
		-O, --new-options=OPTIONS     new cluster options to pass to the server
		-p, --old-port=PORT           old cluster port number (default 50432)
		-P, --new-port=PORT           new cluster port number (default 50432)
		-r, --retain                  retain SQL and log files after success
		-U, --username=NAME           cluster superuser (default "derek")
		-v, --verbose                 enable verbose internal logging
		-V, --version                 display version information, then exit
		-?, --help                    show this help, then exit

	Before running pipeline-upgrade you must:
		create a new database cluster (using the new version of pipeline-init)
		shutdown the postmaster servicing the old cluster
		shutdown the postmaster servicing the new cluster

	When you run pipeline-upgrade, you must provide the following information:
		the data directory for the old cluster  (-d DATADIR)
		the data directory for the new cluster  (-D DATADIR)
		the "bin" directory for the old version (-b BINDIR)
		the "bin" directory for the new version (-B BINDIR)

	For example:
		pipeline-upgrade -d oldCluster/data -D newCluster/data \
		  -b oldCluster/bin -B newCluster/bin
	or
		$ export PGDATAOLD=oldCluster/data
		$ export PGDATANEW=newCluster/data
		$ export PGBINOLD=oldCluster/bin
		$ export PGBINNEW=newCluster/bin
		$ pipeline-upgrade

The idea is to point :code:`pipeline-upgrade` to both the previous version's data directory and binary directory as well as a fresh data directory created by the new version's :code:`pipeline-init`, and the binary directory of the new version.

.. note:: You'll want to back up (just :code:`cp` it somewhere) the previous version's installation directory (usually :code:`/usr/lib/pipelinedb`) so you can still reference its binaries when upgrading.

For example:

.. code-block:: bash

    # After installing the newset PipelineDB version...
    $ pipeline-init -D new_data_dir

    # new_data_dir is initialized...

    # Migrate old catalog data to new schema, and copy old database
    # files to the new data directory
    $ pipeline-upgrade -b old_version/bin -d old_data_dir \
      -B new_version/bin -D new_data_dir
