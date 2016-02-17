.. _stats:

Statistics
==============

PipelineDB includes a number of statistics-gathering views that provide insight into how the system is behaving. Statistics can be broken down by process, continuous view, stream, or viewed globally across the entire installation.

Each statistics view's schema is as follows:

pipeline_proc_stats
----------------------

.. code-block:: pipeline

					View "pg_catalog.pipeline_proc_stats"
			Column     |           Type           | Modifiers
	---------------+--------------------------+-----------
	 type          | text                     |
	 pid           | integer                  |
	 start_time    | timestamp with time zone |
	 input_rows    | bigint                   |
	 output_rows   | bigint                   |
	 updates       | bigint                   |
	 input_bytes   | bigint                   |
	 output_bytes  | bigint                   |
	 updated_bytes | bigint                   |
	 executions    | bigint                   |
	 errors        | bigint                   |


pipeline_query_stats
----------------------

.. code-block:: pipeline

					View "pg_catalog.pipeline_query_stats"
			Column     |  Type  | Modifiers
	---------------+--------+-----------
	 name          | text   |
	 type          | text   |
	 input_rows    | bigint |
	 output_rows   | bigint |
	 updates       | bigint |
	 input_bytes   | bigint |
	 output_bytes  | bigint |
	 updated_bytes | bigint |
	 errors        | bigint |


pipeline_stream_stats
----------------------

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

.. code-block:: pipeline

					View "pg_catalog.pipeline_stats"
			Column     |           Type           | Modifiers
	---------------+--------------------------+-----------
	 type          | text                     |
	 start_time    | timestamp with time zone |
	 input_rows    | bigint                   |
	 output_rows   | bigint                   |
	 updates       | bigint                   |
	 input_bytes   | bigint                   |
	 output_bytes  | bigint                   |
	 updated_bytes | bigint                   |
	 executions    | bigint                   |
	 errors        | bigint                   |
	 cv_create     | bigint                   |
	 cv_drop       | bigint                   |

