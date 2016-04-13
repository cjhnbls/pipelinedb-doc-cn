.. _continuous-triggers:

Continuous Triggers
===================

.. versionadded:: 0.9.1

Continuous Triggers are fundamental to the real-time alerting functionality in PipelineDB. They allow clients to receive changes that occur to a continuous view as soon as they happen.

Here you'll find information about how to define these triggers, and how to subscribe to the real-time alerts arising from them.

.. warning:: Continuous triggers are a relatively new addition to PipelineDB and might still be unstable.

Configuration
-------------

Before we get started, we need to make sure that pipelinedb is configured correctly to support continuous triggers.

Continuous triggers are built upon the `logical decoding <http://www.postgresql.org/docs/9.5/static/logicaldecoding-explanation.html>`_ feature of Postgres, which in turn is based on replication of the Write Ahead Log (WAL).

Logical Decoding allows us to decode the contents of the WAL, which contains all of the metadata about changes to the database. The continuous trigger process consumes these changes via a replication slot, and then decodes them to evaluate triggers.

To enable continuous triggers, set the following configuration options in :code:`pipelinedb.conf`:

.. code-block:: pipeline

    continuous_triggers_enabled = on
    wal_level = logical
    max_wal_senders = 1
    max_replication_slots = 1

If you have more than one database with continuous triggers, :code:`max_wal_senders` and :code:`max_replication_slots` need to be increased to match the number of databases that you have.

Creating Triggers
-----------------

The syntax for creating a continuous trigger is very similar to creating a regular trigger on a table:

.. code-block:: pipeline

    CREATE TRIGGER name AFTER {INSERT|UPDATE|DELETE} ON cv_name FOR EACH ROW
    [WHEN ( condition )]
    EXECUTE PROCEDURE function_name ( arguments );

:code:`condition` is a boolean expression that determines whether the trigger function will be executed.

**condition**

Any valid expression that evaluates to a result of type boolean. If :code:`WHEN` is specified, the procedure will only be called if the condition returns true. The  :code:`WHEN` condition can refer to old or new rows by using  :code:`OLD.column_name` / :code:`NEW.column_name`. :code:`INSERT` triggers cannot refer to :code:`OLD`. :code:`DELETE` triggers cannot refer to :code:`NEW`.

:code:`DELETE` triggers are only valid on sliding window continuous views.

**function_name**

A user-supplied function that is declared as taking no arguments and returning type :code:`trigger`, which is executed for every time the trigger is fired.

**arguments**

Optional comma-separated list of arguments to be provided to the function when the trigger is executed. Arguments can only be string literals.

Here's an example:

.. code-block:: pipeline

    CREATE CONTINUOUS VIEW v AS SELECT x::int,COUNT(*) FROM s GROUP BY x;

    CREATE TRIGGER t AFTER UPDATE OR INSERT ON v FOR ROW
    WHEN (NEW.count > 100)
    EXECUTE PROCEDURE pipeline_send_alert_new_row();

**Limitations**

- :code:`WHEN` expressions cannot contain subqueries.
- Only row-level :code:`AFTER` triggers are allowed.

Dropping Triggers
-----------------

.. code-block:: pipeline

    DROP TRIGGER name ON view_name

This will remove the continuous trigger from the system along with all of its associated resources.

Viewing Continuous Triggers Definitions
---------------------------------------

To see the triggers currently defined on a continuous view, connect to an instance using the pipeline client:

.. code-block:: pipeline

    pipeline=# \d+ v

Any defined triggers will be listed after the view definition.

Example output:

.. code-block:: pipeline

                  Continuous view "public.v"
     Column |  Type   | Modifiers | Storage | Description
    --------+---------+-----------+---------+-------------
     x      | integer |           | plain   |
     count  | bigint  |           | plain   |
    View definition:
     SELECT x::integer,
        count(*) AS count
       FROM ONLY s
      GROUP BY x::integer;
    Triggers:
        t AFTER INSERT OR UPDATE ON v FOR EACH ROW
        WHEN (NEW.count > 100)
        EXECUTE PROCEDURE pipeline_send_alert_new_row()

Receiving Alerts
----------------

PipelineDB comes with a push server that can send real-time alerts to clients. To receive alerts for a trigger, use the built-in trigger function called :code:`pipeline_send_alert_new_row` when creating the trigger. This trigger function will sends the :code:`NEW` row to any clients connected to the push server whenever the trigger’s :code:`WHEN` condition evaluates to :code:`true`.

To connect to the push server, use the :code:`pipeline-recv-alerts` tool. You can't use :code:`psql` or :code:`pipeline` with the push server.

.. code-block:: pipeline

    pipeline-recv-alerts is the PipelineDB tool for receiving alerts.

    Usage:
      pipeline-recv-alerts -a alert_name [OPTION]...

    General options:
      -a, --alert=ALERTNAME   alert name to subscribe to (view_name.trigger_name)

    Connection options:
      -d, --dbname=DBNAME     database name to connect to (default: "pipeline")
      -h, --host=HOSTNAME     database server host (default: "local socket")
      -p, --port=PORT         database server port (default: "5432")
      -U, --username=USERNAME database user name (default: "username")
      -w, --no-password       never prompt for password
      -W, --password          force password prompt

:code:`pipeline-recv-alerts` takes similar command line options to :code:`psql` and :code:`pipeline`.

**Limitations**

Only one alert may be subscribed to per alert client.

**Examples**

Subscribe to an alert arising from a trigger name t defined on view v:

.. code-block:: pipeline

    pipeline-recv-alerts -a v.t -h clusternode.example.com

The client will produce rows of data to stdout in postgres :code:`COPY` format. That is, tab separated escaped strings, terminated with a newline.

The output is a stream of tab-separated row data:

.. code-block:: pipeline

    195	101
    80	101
    190	101
    179	102

Examples
--------

Here are some examples of the types of triggers you may wish to define.
For brevity's sake, only the when condition is shown.

To get changes happening to a particular row:

.. code-block:: pipeline

    WHEN (NEW.x = 3)

To get changes to all rows (in this example, the WHEN clause could have been elided):

.. code-block:: pipeline

    WHEN (true)

To get changes to rows meeting a certain criteria, e.g. when the count field is greater than 100:

.. code-block:: pipeline

    WHEN (NEW.count > 100)

To get changes when a threshold is crossed:

.. code-block:: pipeline

    WHEN (OLD.count < 100 and NEW.count > 100)
