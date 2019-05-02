.. _quickstart:

..
    Quickstart
快速开始
=======================
..
    First, complete the :ref:`installation` process.
首先，请完成 :ref:`installation` 步骤

..
    Wikipedia Traffic
维基百科流量统计
-----------------

In this example we'll compute some basic statistics on a day's worth of `Wikipedia page view data`_. Each record in the dataset contains hourly page view statistics for every Wikipedia page. The record format is as follows:

.. _Wikipedia page view data: http://dumps.wikimedia.org/other/pagecounts-raw/

.. code-block:: bash

	hour | project | page title | view count | bytes served

First, let's create our continuous view using :code:`psql`:

.. code-block:: bash

	psql -c "
	CREATE FOREIGN TABLE wiki_stream (
		hour timestamp,
		project text,
		title text,
		view_count bigint,
		size bigint)
	SERVER pipelinedb;
	CREATE VIEW wiki_stats WITH (action=materialize) AS
	SELECT hour, project,
		count(*) AS total_pages,
		sum(view_count) AS total_views,
		min(view_count) AS min_views,
		max(view_count) AS max_views,
		avg(view_count) AS avg_views,
		percentile_cont(0.99) WITHIN GROUP (ORDER BY view_count) AS p99_views,
		sum(size) AS total_bytes_served
	FROM wiki_stream
	GROUP BY hour, project;"

Now we'll decompress the dataset as a stream and write it to :code:`stdin`, which can be used as an input to :code:`COPY`:

.. code-block:: bash

		curl -sL http://pipelinedb.com/data/wiki-pagecounts | gunzip | \
			psql -c "
			COPY wiki_stream (hour, project, title, view_count, size) FROM STDIN"

Note that this dataset is large, so the above command will run for quite a while (cancel it whenever you'd like). As it's running, select from the continuous view as it ingests data from the input stream:

.. code-block:: bash

	psql -c "
	SELECT * FROM wiki_stats ORDER BY total_views DESC";
