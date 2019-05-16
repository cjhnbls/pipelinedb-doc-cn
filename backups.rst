.. _backups:

..  Backups

备份
==============

..  Since PipelineDB objects are represented by standard PostgreSQL objects, backups can be taken using PostgreSQL's `pg_dump`_ and `pg_dumpall`_ tools. Other PostgreSQL backup and restore tooling will work as well, since a PipelineDB database is just a regular PostgreSQL database.

因为PipelineDB就是一个常规的PostgreSQL数据库，所以可以直接使用PostgreSQL的 `pg_dump`_ 和 `pg_dumpall`_ 工具或者其它PostgreSQL备份和恢复工具进行备份。

..  Exporting Specific Continuous Views

导出流视图
-----------------------------------------

..  To export a single continuous view, both the continuous view and its associated materialization table must be explicitly dumped, like so:

导出流视图的时候，流视图及其关联的数据表必须同时显式导出：

.. code-block:: bash

  pipeline-dump -t <CV name> -t <CV name>_mrel # <-- Note the "_mrel" suffix

..  Restoring Continuous Views

恢复流视图
-------------------------------

..  To restore a backup taken with `pg_dump`_, simply pass its output to the :code:`psql` client:

可以通过 `pg_dump`_ 恢复备份，将其输出通过 :code:`psql` 客户端导入即可：

.. code-block:: bash

  pg_dump > backup.sql
  psql -f backup.sql

.. _pg_dump: http://www.postgresql.org/docs/current/static/app-pgdump.html
.. _pg_dumpall: http://www.postgresql.org/docs/current/static/app-pg-dumpall.html
