.. _continuous-transforms:

.. Continuous Transforms

流转换
========================

..	Continuous transforms can be used to continuously transform incoming time-series data without storing it. Since no data is stored, continuous transforms don't support aggregations. The result of the transformation can be piped into another stream or written to an external data store.

流转换可以在不存储时序的情况下对其进行实时转换。由于数据不存储数据，所以流转换不支持聚合操作。转换后的数据既可以作为另一个流的输入，也可以写入到外部数据存储中。

..	Creating Continuous Transforms

创建流转换
------------------------------------

..	Transforms are defined as PostgreSQL views with the :code:`action` parameter set to :code:`transform`. Here's the syntax for creating a continuous transform:

通过将 :code:`action` 赋值为 :code:`transform` 来声明流转换，下面是创建流转换的语法：

.. code-block:: sql

	CREATE VIEW name (WITH action=transform [, outputfunc=function_name( arguments ) ]) AS query

..	**query** is a subset of a PostgreSQL :code:`SELECT` statement:

**query** 是一个PostgreSQL :code:`SELECT` 报表。

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

..	**function_name** is an optional user-supplied function that is declared as taking no arguments and returning type :code:`trigger`, which is executed for every single row that is output by the continuous transform.

**function_name** 是一个用户传入的函数，它的返回类型为 :code:`trigger`，并且会作用到流转换的每一行输出上。

..	**arguments** is an optional comma-separated list of arguments to be provided to the function when the trigger is executed. Arguments can only be literal string constants.

**arguments** 是一系列逗号分隔的参数，在触发器执行时传给函数，只能为字符串常量。

.. note::
	..	You can think of continuous transforms as being `triggers <http://www.postgresql.org/docs/9.1/static/sql-createtrigger.html>`_ on top of incoming streaming data where the trigger function is executed for each new row output by the continuous transform. Internally the function is executed as an :code:`AFTER INSERT FOR EACH ROW` trigger so there is no :code:`OLD` row and the :code:`NEW` row contains the row output by the continuous tranform.

	您可以将流转换视为作用在流上每一行输出上的 `触发器 <http://www.postgresql.org/docs/9.1/static/sql-createtrigger.html>`_ 函数，它会在内部以 :code:`AFTER INSERT FOR EACH ROW` 的形式执行，所以不存在流转换中不会包含行输出的 :code:`OLD` 和 :code:`NEW` 行数据。

..	Dropping Continuous Transforms

删除流转换
------------------------------------

..	To :code:`DROP` a continuous transform from the system, use the :code:`DROP VIEW` command. Its syntax is simple:

您可以使用 :code:`DROP VIEW` 指令来来删除流转换，语法如下：

.. code-block:: sql

	DROP VIEW continuous_transform;

..	This will remove the continuous transform from the system along with all of its associated resources.

这将移除所有与转换流相关资源。

..	Viewing Continuous Transforms

查看流转换
-----------------------------------

..	To view the continuous transforms and their definitions currently in the system, you can run the following query:

您可以通过如下查询来查看当前系统中所有的流转换任务：

.. code-block:: sql

	SELECT * FROM pipelinedb.transforms;

.. _ct-output-streams:

..	Continuous Transform Output Streams

流转换输出到流
---------------------------------------

..	All continuous transforms have :ref:`output-streams` associated with them, making it easy for other transforms or continuous views to read from them. A continuous transform's output stream simply contains whatever rows the transform selects.

所有流转换都有与其对应的 :ref:`输出流<output-streams>`，这使得可以很容易地被其它流转换或流视图读到。流转换的输出流只包含被select的内容。

..		For example, here's a simple transform that joins incoming rows with a table:

下面是将流转换同数据表进行join的例子：

.. code-block:: sql

  CREATE VIEW t WITH (action=transform) AS
    SELECT t.y FROM some_stream s JOIN some_table t ON s.x = t.x;

..	This transform now writes values from the joined table out to its output stream, which can be read using :code:`output_of`:

这个流转换将join后的结果写入到其对应的输出流中，可以通过 :code:`output_of` 读到输出流中的数据：


.. code-block:: sql

  CREATE VIEW v WITH (action=materialize) AS
    SELECT sum(y) FROM output_of('t');

..	Built-in Transform Output Functions

内置流转换输出函数
-------------------------------------------

..	In order to provide more flexibility over a continuous transform's output than their built-in output streams provide, PipelineDB exposes an interface to receive a transform's rows using a trigger function. Trigger functions attached to tranforms can then do whatever you'd like with the rows they receive, including write out to other streams.

为了给流转换输出提供比输出流更高的灵活性，PipelineDB提供了一个基于触发器函数，用于处理转换流数据的接口。这个接口可以对接受的数据执行任意操作，包括写入到其它流中。

..	Currently, PipelineDB provides only one built-in trigger function, :code:`pipelinedb.insert_into_stream`, that can be used with continuous transforms. It inserts the output of the continuous transform into all the streams that are provided as the string literal arguments. For example:

目前为止，PipelineDB只提供了1个内置触发器函数 :code:`pipelinedb.insert_into_stream`，它可以同流转换一起使用，将流转换的输出写入到参数列表中的各个流。用法如下：

.. code-block:: sql

  CREATE VIEW t WITH (action=transform, outputfunc=pipelinedb.insert_into_stream('even_stream)) AS
    SELECT x, y FROM stream WHERE mod(x, 2) = 0;

..	This continuous transform will insert all values of :code:`(x, y)` into :code:`even_stream` where :code:`x` is even.

流转换将 :code:`x` 为偶数的 :code:`(x, y)` 的结果写入到 :code:`even_stream` 中。

.. important::
	..	All arguments to :code:`pipelinedb.insert_into_stream` must be valid names of streams that already exist in the system, otherwise an error will be thrown.

	传入 :code:`pipelinedb.insert_into_stream` 的参数必须是系统中已存在的流（foreign table）。

..	Creating Your Own Output Function

创建自定义转换流输出函数
--------------------------------------

..	You can also create your own output function that can be used with continuous transforms. For example, if you want to insert the output into a table, you could do something like:

您也可以创建作用于流转换的自定义输出函数。比如您想将输出写入数据表中，可以通过如下操作实现：

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
