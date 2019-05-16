.. _stats:

..  Statistics

内置统计视图
==============

..	PipelineDB includes a number of statistics-gathering views that provide insight into how the system is behaving. Statistics can be broken down by process, continuous view, stream, or viewed globally across the entire installation.

PipelineDB提供一系列包含系统运行情况的统计-收集视图，这些统计会被进程、流视图、以及全局操作打断。

..	Each statistics view is described below.

每个统计视图如下所示：

pipelinedb.proc_stats
----------------------

..	Statistics broken down by worker and combiner processes. These statistics only last for the duration of the underlying processes.

统计会被worker和combiner进程打算，只适用于一些基础流程。

.. code-block:: psql

		View "pipelinedb.proc_stats"
            Column     |           Type           | Modifiers
	---------------+--------------------------+-----------
	 type          | text                     |
	 pid           | integer                  |
	 start_time    | timestamp with time zone |
	 input_rows    | bigint                   |
	 output_rows   | bigint                   |
	 updated_rows  | bigint                   |
	 input_bytes   | bigint                   |
	 output_bytes  | bigint                   |
	 updated_bytes | bigint                   |
	 executions    | bigint                   |
	 errors        | bigint                   |
	 exec_ms       | bigint                   |


pipelinedb.query_stats
----------------------

..	Continuous view-level statistics (views and transforms).

流视图和流转换统计信息

.. code-block:: psql

		View "pipelinedb.query_stats"
            Column        |           Type           | Modifiers
	------------------+--------------------------+-----------
	 type             | text                     |
	 namespace        | text                     |
	 continuous_query | text                     |
	 input_rows       | bigint                   |
	 output_rows      | bigint                   |
	 updated_rows     | bigint                   |
	 input_bytes      | bigint                   |
	 output_bytes     | bigint                   |
	 updated_bytes    | bigint                   |
	 executions       | bigint                   |
	 errors           | bigint                   |
	 exec_ms          | bigint                   |


pipelinedb.stream_stats
-----------------------------

..	Stream-level statistics.

流（foreign table）统计信息

.. code-block:: psql

		View "pipelinedb.stream_stats"
            Column       |           Type           | Modifiers
	-----------------+--------------------------+-----------
	 namespace       | text                     |
	 stream          | text                     |
	 input_rows      | bigint                   |
	 input_batches   | bigint                   |
	 input_bytes     | bigint                   |


pipelinedb.db_stats
------------------------

..	Database-wide statistics.

库（database）统计信息

.. code-block:: psql

		View "pipelinedb.db_stats"
            Column        |           Type           | Modifiers
	------------------+--------------------------+-----------
	 type             | text                     |
	 input_rows       | bigint                   |
	 output_rows      | bigint                   |
	 updated_rows     | bigint                   |
	 input_bytes      | bigint                   |
	 output_bytes     | bigint                   |
	 updated_bytes    | bigint                   |
	 executions       | bigint                   |
	 errors           | bigint                   |
	 exec_ms          | bigint                   |
