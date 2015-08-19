.. _quickstart:

Quickstart
=======================

Initialize a data directory and start the PipelineDB server:

.. code-block:: bash

	pipeline-init -D <data directory>
	pipeline-server -D <data directory>

Wikipedia Traffic
-----------------

In this example we'll compute some basic statistics on a day's worth of `Wikipedia page view data`_. Each record in the dataset contains hourly page view statistics for every Wikipedia page. The record format is as follows:

.. _Wikipedia page view data: http://dumps.wikimedia.org/other/pagecounts-raw/

.. code-block:: bash

	hour | project | page title | view count | bytes served

First, let's create our continuous view using :code:`psql`:

.. code-block:: bash

	psql -h localhost -p 5432 -d pipeline -c "
	CREATE CONTINUOUS VIEW wiki_stats AS
	SELECT hour::timestamp, project::text,
		count(*) AS total_pages,
		sum(view_count::bigint) AS total_views,
		min(view_count) AS min_views,
		max(view_count) AS max_views,
		avg(view_count) AS avg_views,
		percentile_cont(0.99) WITHIN GROUP (ORDER BY view_count) AS p99_views,
		sum(size::bigint) AS total_bytes_served
	FROM wiki_stream
	GROUP BY hour, project;"

Now we'll decompress the dataset as a stream and write it to :code:`stdin`, which can be used as an input to :code:`COPY`:

.. code-block:: bash

		curl -sL http://pipelinedb.com/data/wiki-pagecounts | gunzip | \
			psql -h localhost -p 5432 -d pipeline -c "
			COPY wiki_stream (hour, project, title, view_count, size) FROM STDIN"

Note that this dataset is large, so the above command will run for quite a while (cancel it whenever you'd like). As it's running, select from the continuous view as it ingests data from the input stream:

.. code-block:: bash

	psql -h localhost -p 5432 -d pipeline -c "
	SELECT * FROM wiki_stats ORDER BY total_views DESC";
