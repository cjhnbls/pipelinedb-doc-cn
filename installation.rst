.. _installation:

Installation
==============

rpm
-----------

To install the PipelineDB RPM package, run:

.. code-block:: pipeline

	sudo -ivh pipelinedb-<version>.rpm

This will install PipelineDB at :code:`/opt/pipelinedb`. To install at a prefix of your choosing, use the :code:`--prefix` argument:

.. code-block:: pipeline

	sudo -ivh --prefix=/path/to/pipelinedb pipelinedb-<version>.rpm


Initializing PipelineDB
------------------------

Once PipelineDB is installed, you can initialize a database directory. This is where PipelineDB will store all the files and data associated with a database. To initialize a data directory, run:

.. code-block:: pipeline

	pipeline-init -D <data directory>

where :code:`<data directory>` is a nonexistent directory. Once this directory has been successfully initialized, you can run a PipelineDB server.

Running PipelineDB
---------------------

To run the PipelineDB server in the background, use the :code:`pipeline-ctl` driver and point it to your newly initialized data directory:

.. code-block:: pipeline

	pipeline-ctl -D <data directory> -l pipelinedb.log start

The :code:`-l` option specifies the path of a logfile to log to. The :code:`pipeline-ctl` driver can also be used to stop running servers:

.. code-block:: pipeline

	pipeline-ctl -D <data directory> stop

Run :code:`pipeline-ctl --help` to see other available functionality. Finally, the PipelineDB server can also be run in the foreground directly:

.. code-block:: pipeline

	pipeline-server -D <data directory>

To connect to a running server using the default database "pipeline", the :code:`pipeline` command can be used:

.. code-block:: pipeline

	pipeline pipeline

`PostgreSQL's`_ standard client, :code:`psql`, can also be used to connect to PipelineDB. Note that PipelineDB's default port is :code:`6543`:

.. _`PostgreSQL's`:  http://www.postgresql.org/download/

.. code-block:: pipeline

	psql -p 6543 -h localhost pipeline

-------------

Now you're ready to put PipelineDB to work! Check out the :ref:`continuous-views` section to get started.

