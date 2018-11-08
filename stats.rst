.. _stats:

Statistics
==============

PipelineDB includes a number of statistics-gathering views that provide insight into how the system is behaving. Statistics can be broken down by process, continuous view, stream, or viewed globally across the entire installation.

Each statistics view is described below.

pipelinedb.proc_stats
----------------------

Statistics broken down by worker and combiner processes. These statistics only last for the duration of the underlying processes.

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

Continuous view-level statistics (views and transforms).

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

Stream-level statistics.

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

Database-wide statistics.

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