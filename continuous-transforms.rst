.. _continuous-transforms:

Continuous Transforms
========================

Continuous transforms can be used to continuously transform incoming time-series data without storing it. Since no data is stored, continuous transforms don't support aggregations. The result of the transformation can be piped into another stream or written to an external data store.

Creating Continuous Transforms
------------------------------------

Transforms are defined as PostgreSQL views with the :code:`action` parameter set to :code:`transform`. Here's the syntax for creating a continuous transform:

.. code-block:: sql

	CREATE VIEW name (WITH action=transform [, outputfunc=function_name( arguments ) ]) AS query 

**query** is a subset of a PostgreSQL :code:`SELECT` statement:

.. code-block:: sql

  SELECT expression [ [ AS ] output_name ] [, ...]
      [ FROM from_item [, ...] ]
      [ WHERE condition ]
      [ GROUP BY expression [, ...] ]

  where any expression in the SELECT statement can't contain an aggregate and
  from_item can be one of:

      stream_name [ [ AS ] alias [ ( column_alias [, ...] ) ] ]
      table_name [ [ AS ] alias [ ( column_alias [, ...] ) ] ]
      from_item [ NATURAL ] join_type from_item [ ON join_condition ]

**function_name** is an optional user-supplied function that is declared as taking no arguments and returning type :code:`trigger`, which is executed for every single row that is output by the continuous transform.

**arguments** is an optional comma-separated list of arguments to be provided to the function when the trigger is executed. Arguments can only be literal string constants.

.. note:: You can think of continuous transforms as being `triggers <http://www.postgresql.org/docs/9.1/static/sql-createtrigger.html>`_ on top of incoming streaming data where the trigger function is executed for each new row output by the continuous transform. Internally the function is executed as an :code:`AFTER INSERT FOR EACH ROW` trigger so there is no :code:`OLD` row and the :code:`NEW` row contains the row output by the continuous tranform.

Dropping Continuous Transforms
------------------------------------

To :code:`DROP` a continuous transform from the system, use the :code:`DROP VIEW` command. Its syntax is simple:

.. code-block:: sql

	DROP VIEW continuous_transform;

This will remove the continuous transform from the system along with all of its associated resources.

Viewing Continuous Transforms
-----------------------------------

To view the continuous transforms and their definitions currently in the system, you can run the following query:

.. code-block:: sql

	SELECT * FROM pipelinedb.transforms;

.. _ct-output-streams:

Continuous Transform Output Streams
---------------------------------------

All continuous transforms have :ref:`output-streams` associated with them, making it easy for other transforms or continuous views to read from them. A continuous transform's output stream simply contains whatever rows the transform selects.

For example, here's a simple transform that joins incoming rows with a table:

.. code-block:: sql

  CREATE VIEW t WITH (action=transform) AS
    SELECT t.y FROM some_stream s JOIN some_table t ON s.x = t.x;

This transform now writes values from the joined table out to its output stream, which can be read using :code:`output_of`:

.. code-block:: sql

  CREATE VIEW v WITH (action=materialize) AS
    SELECT sum(y) FROM output_of('t');

Built-in Transform Output Functions
-------------------------------------------

In order to provide more flexibility over a continuous transform's output than their built-in output streams provide, PipelineDB exposes an interface to receive a transform's rows using a trigger function. Trigger functions attached to tranforms can then do whatever you'd like with the rows they receive, including write out to other streams.

Currently, PipelineDB provides only one built-in trigger function, :code:`pipelinedb.insert_into_stream`, that can be used with continuous transforms. It inserts the output of the continuous transform into all the streams that are provided as the string literal arguments. For example:

.. code-block:: sql

  CREATE VIEW t WITH (action=transform, outputfunc=pipelinedb.insert_into_stream('even_stream)) AS
    SELECT x, y FROM stream WHERE mod(x, 2) = 0;

This continuous transform will insert all values of :code:`(x, y)` into :code:`even_stream` where :code:`x` is even.

.. important:: All arguments to :code:`pipelinedb.insert_into_stream` must be valid names of streams that already exist in the system, otherwise an error will be thrown.

Creating Your Own Output Function
--------------------------------------

You can also create your own output function that can be used with continuous transforms. For example, if you want to insert the output into a table, you could do something like:

.. code-block:: sql

  CREATE TABLE t (user text, value int);

  CREATE OR REPLACE FUNCTION insert_into_t()
    RETURNS trigger AS
    $$
    BEGIN
      INSERT INTO t (user, value) VALUES (NEW.user, NEW.value);
      RETURN NEW;
    END;
    $$
    LANGUAGE plpgsql;

  CREATE VIEW ct WITH (action=transform, outputfunc=insert_into_t) AS
    SELECT user::text, value::int FROM stream WHERE value > 100;
