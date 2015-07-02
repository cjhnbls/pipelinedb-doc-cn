.. _quickstart:

Quickstart
=======================

Initialize a data directory and start the PipelineDB server:

.. code-block:: bash

	pipeline-init -D <data directory>
	pipeline-server -D <data directory>

Wikipedia Traffic
-----------------

In this example we'll compute some basic statistics on a day's worth of `Wikipedia page view data`_. The format of each record in the dataset is:

.. _Wikipedia page view data: http://dumps.wikimedia.org/other/pagecounts-raw/

.. code-block:: bash

	hour | project | page title | view count | bytes served

First, let's create our continuous view using :code:`psql`:

.. code-block:: bash

	psql -h localhost -p 6543 -d pipeline -c "
	CREATE CONTINUOUS VIEW wiki_stats AS
	SELECT day::timestamp, project::text,
		count(*) AS total_pages,
		sum(count::bigint) AS total_views,
		min(count) AS min_views,
		max(count) AS max_views,
		avg(count) AS avg_views,
		sum(size::bigint) AS total_bytes_served
	FROM wiki_stream
	GROUP BY day, project;"

Now we'll decompress the dataset as a stream and write it to :code:`stdin`, which can be used as an input to :code:`COPY`:

.. code-block:: bash

		curl http://pipelinedb.com/quickstart/wiki | gunzip | \
			psql -h localhost -p 6543 -d pipeline -c "
			COPY wiki_stream (day, project, title, count, size) FROM STDIN"

Note that this dataset is large, so the above command will run for quite a while (cancel it whenever you'd like). As it's running, select from the continuous view as it ingests data from the input stream:

.. code-block:: bash
	
	psql -h localhost -p 6543 -d pipeline -c "SELECT * FROM wiki_stats";


