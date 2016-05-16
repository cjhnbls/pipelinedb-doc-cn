.. _stats:

Statistics
==============

PipelineDB includes a number of statistics-gathering views that provide insight into how the system is behaving. Statistics can be broken down by process, continuous view, stream, or viewed globally across the entire installation.

Each statistics view is described below.

pipeline_proc_stats
----------------------

Statistics broken down by worker and combiner processes. These statistics only last for the duration of the underlying processes.

.. code-block:: pipeline

					View "pg_catalog.pipeline_proc_stats"
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
	 tuples_ps     | bigint                   |
	 bytes_ps      | bigint                   |
	 time_pb       | bigint                   |
	 tuples_pb     | bigint                   |
	 memory        | bigint                   |
	 errors        | bigint                   |


pipeline_query_stats
----------------------

Continuous view-level statistics.

.. code-block:: pipeline

					View "pg_catalog.pipeline_query_stats"
			Column     |  Type  | Modifiers
	---------------+--------+-----------
	 name          | text   |
	 type          | text   |
	 input_rows    | bigint |
	 output_rows   | bigint |
	 updated_rows  | bigint |
	 input_bytes   | bigint |
	 output_bytes  | bigint |
	 updated_bytes | bigint |
	 tuples_ps     | bigint |
	 bytes_ps      | bigint |
	 time_pb       | bigint |
	 tuples_pb     | bigint |
	 errors        | bigint |


pipeline_stream_stats
----------------------

Stream-level statistics.

.. code-block:: pipeline

					View "pg_catalog.pipeline_stream_stats"
			Column     |  Type  | Modifiers
	---------------+--------+-----------
	 schema        | text   |
	 name          | text   |
	 input_rows    | bigint |
	 input_batches | bigint |
	 input_bytes   | bigint |

pipeline_stats
---------------

Installation-wide statistics.

.. code-block:: pipeline

					View "pg_catalog.pipeline_stats"
			Column     |           Type           | Modifiers
	---------------+--------------------------+-----------
	 type          | text                     |
	 start_time    | timestamp with time zone |
	 input_rows    | bigint                   |
	 output_rows   | bigint                   |
	 updated_rows  | bigint                   |
	 input_bytes   | bigint                   |
	 output_bytes  | bigint                   |
	 updated_bytes | bigint                   |
	 executions    | bigint                   |
	 errors        | bigint                   |
	 cv_create     | bigint                   |
	 cv_drop       | bigint                   |
