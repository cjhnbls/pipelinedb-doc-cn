.. _installation:

PipelineDB Installation
===========================

Install PostgreSQL
---------------------------

Since PipelineDB runs as an extension to PostreSQL, begin by `installing PostgreSQL`_.

.. note:: PipelineDB currently supports PostgreSQL versions 10.1, 10.2, 10.3, 10.4 and 10.5.

.. _`installing PostgreSQL`: https://www.postgresql.org/download/

Once you have PostgreSQL installed on your system, you just need to install the PipelineDB binaries and then create the PipelineDB extension within your PostgreSQL database. You can install binaries from our **apt** or **yum** repositories or you can download packages from our `release archives`_ and install them directly.

.. _`release archives`: https://github.com/pipelinedb/pipelinedb/releases

apt
------------

First, add our **apt** repository to your system:

.. code-block:: sh

	curl -s http://download.pipelinedb.com/apt.sh | sudo bash

Now simply install the latest PipelineDB package:

.. code-block:: sh

	apt-get install pipelinedb-postgresql-10

yum
---------------

Add our **yum** repository to your system:

.. code-block:: sh

	curl -s http://download.pipelinedb.com/yum.sh | sudo bash

Install the latest PipelineDB package:

.. code-block:: sh

 yum install pipelinedb-postgresql-10

.. note:: **apt** and **yum** repositories only need to be added to your system a single time. Once you've added them, you don't need to run these scripts again. You need only run the installation commands to get new versions of PipelineDB.

-------------------------

You may also download binary packages from our `release <https://github.com/pipelinedb/pipelinedb/releases>`_ archives and install them directly.

RPM Packages
--------------------

To install the PipelineDB RPM package, run:

.. code-block:: sh

	sudo rpm -ivh pipelinedb-postgresql-<pg version>_<pipelindb version>.rpm

Debian Packages
---------------------

To install the PipelineDB Debian package, run:

.. code-block:: sh

	sudo dpkg -i pipelinedb-postgresql-<pg version>_<pipelindb version>.deb

OS X
----

Just double-click the :code:`pipelinedb-<version>.pkg` file to launch the OS X Installer. For older versions of OS X, you might need to install a few packages that PipelineDB depends on:

.. code-block:: sh

  brew install json-c freexl

Initializing PipelineDB
------------------------

Once both PostgreSQL and PipelineDB are installed, you can initialize a PostgreSQL database directory:

.. code-block:: sh

	initdb -D <data directory>

where :code:`<data directory>` is a nonexistent directory. Once this directory has been successfully initialized, you can run PostgreSQL.

Creating the PipelineDB Extension
------------------------------------------

In order for PipelineDB to run, the :code:`shared_preload_libraries` configuration parameter must be set in :code:`postgresql.conf`, which can be found underneath your data directory. It's also a good idea to set :code:`max_worker_processes` to something reasonably high to give PipelineDB worker processes plenty of capacity:

.. code-block:: sh

	# At the bottom of <data directory>/postgresql.conf
	shared_preload_libraries = 'pipelinedb'
	max_worker_processes = 128
	
Running PostgreSQL
---------------------

To run the PipelineDB server in the background, use the :code:`pipeline-ctl` driver and point it to your newly initialized data directory:

.. code-block:: sh

	pg_ctl -D <data directory> -l postgresql.log start

To connect to a running server using the default database, use PostgreSQL's standard client, `psql`_. Since PipelineDB is an extension of PostgreSQL, you'll need to create the PipelineDB extension:

.. code-block:: sh

	psql -c "CREATE EXTENSION pipelinedb"

Once the PipelineDB extension has been created, you're ready to start using PipelineDB!

.. _`psql`:  https://www.postgresql.org/docs/current/static/app-psql.html

You can check out the :ref:`quickstart` section to start streaming data into PipelineDB right now.

Configuration
---------------------

By default, PostgreSQL is not configured to allow incoming connections from remote hosts. To enable incoming connections, first set the following line in :code:`postgresql.conf`:

.. code-block:: sh

    listen_addresses = '*'

And in :code:`pg_hba.conf`, add a line such as the following to allow incoming connections:

.. code-block:: sh

    host    all             all             <ip address>/<subnet>            md5


For example, to allow incoming connections from any host:

.. code-block:: sh

    host    all             all             0.0.0.0/0            md5

-------------

Now you're ready to put PipelineDB to work! Check out the :ref:`continuous-views` or :ref:`quickstart` sections to get started.

Docker
---------------------

A PipelineDB Docker image is also available (thanks to Josh Berkus). It can be run with:

.. code-block:: sh

  docker run -v /dev/shm:/dev/shm pipelinedb/pipelinedb-postgresql-10

This image exposes port :code:`5432` for interaction with PipelineDB; credentials are user :code:`pipeline`, password :code:`pipeline`.

The database gets installed to :code:`/mnt/pipelinedb`, so if you want to put that on real storage, or modify the configuration files, then simply mount that as a volume before starting the image for the first time.

.. note:: The configuration which installs with the image is appropriate for testing on your laptop. If you deploy this to production, you will want to edit pipelinedb.conf and substantially increase resource limits for most things.
