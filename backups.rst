.. _backups:

Backups
==============

Since PipelineDB objects are represented by standard PostgreSQL objects, backups can be taken using PostgreSQL's `pg_dump`_ and `pg_dumpall`_ tools. Other PostgreSQL backup and restore tooling will work as well, since a PipelineDB database is just a regular PostgreSQL database.

Exporting Specific Continuous Views
-----------------------------------------

To export a single continuous view, both the continuous view and its associated materialization table must be explicitly dumped, like so:

.. code-block:: bash

  pipeline-dump -t <CV name> -t <CV name>_mrel # <-- Note the "_mrel" suffix
  
Restoring Continuous Views
-------------------------------
  
To restore a backup taken with `pg_dump`_, simply pass its output to the :code:`psql` client:

.. code-block:: bash

  pg_dump > backup.sql
  psql -f backup.sql
  
.. _pg_dump: http://www.postgresql.org/docs/current/static/app-pgdump.html
.. _pg_dumpall: http://www.postgresql.org/docs/current/static/app-pg-dumpall.html
