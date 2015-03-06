.. _activation-deactivation:

Activation and deactivation
============================

Because :ref:`continuous-views` are continuously processing input streams, it is useful to have a notion of starting and stopping that processing without having to completely shutdown PipelineDB. For example, if a continuous view incurs an unexpected amount of system load or begins throwing errors, it may be useful to stop that particular continuous view until the issue is resolved--without having to stop any other continuous views.

This level of control is provided by the :code:`ACTIVATE` and :code:`DEACTIVATE` commands, which are synonymous with "play" and "pause". When a continuous view is *active*, it is actively reading from its input streams and incrementally updating its result accordingly. Conversely, an *inactive* continuous view is not reading its input streams and is not updating its result.

.. important:: If a continuous view is inactive, any events written to its input streams while it's inactive will never be read by that continuous view, even after it is activated again. Other active continuous views reading from the same input streams will continue to read events regularly, of course.

ACTIVATE
-----------

Here's the syntax for the :code:`ACTIVATE` command:

.. code-block:: pipeline

	ACTIVATE { name [, ...] | WHERE condition } [ WITH ( parameter = value [, ...] ) ]

The syntax for :code:`DEACTIVATE` is similar, it just excludes the :code:`WITH` clause:

DEACTIVATE
-----------

.. code-block:: pipeline

	DEACTIVATE { name [, ...] | WHERE condition }

**name**
	The name of the continuous view

**condition**
	An expression involving the columns of the :code:`pipeline_query` catalog table. See :ref:`pipeline-query` for more information about :code:`pipeline_query`. All continuous views for which this condition evaluates to :code:`true` will be activated or deactivated, depending on which command was issued.

**parameter**
	One of: **batchsize**, **emptysleepms**, **maxwaitms**, **parallelism** (explained in the :ref:`parameters` section)

**value**
	An integral value


--------------------

:code:`ACTIVATE` and :code:`DEACTIVATE` both return the number of continuous views that were affected by the command.

Examples
-----------

Let's take a look at a few examples of what it would look like to :code:`ACTIVATE` and :code:`DEACTIVATE` continuous views from an interactive :code:`psql` session. Assume that we've already created the following continuous views:

- :code:`view0`
- :code:`view1`
- :code:`continuous_view0`
- :code:`continuous_view2`

This will :code:`ACTIVATE` all 4 of them:

.. code-block:: pipeline

	pipeline=# ACTIVATE;
	4
	pipeline=#

Calling :code:`ACTIVATE` on an active continuous view is a noop:

.. code-block:: pipeline

	pipeline=# ACTIVATE;
	4
	pipeline=# ACTIVATE;
	0
	pipeline=#

:code:`ACTIVATE` :code:`view0` and :code:`view1`:

.. code-block:: pipeline

	pipeline=# ACTIVATE view0, view1;
	2
	pipeline=#

:code:`ACTIVATE` :code:`continuous_view0` and :code:`continuous_view1`:

.. code-block:: pipeline

	pipeline=# ACTIVATE WHERE name LIKE '%continuous%';
	2
	pipeline=#

:code:`DEACTIVATE` :code:`continuous_view0` and :code:`view0`:

.. code-block:: pipeline

	pipeline=# ACTIVATE WHERE name LIKE '%view0%';
	2
	pipeline=#


.. _parameters:

Parameters
-------------

It is possible to supply performance tuning parameters to continuous views. The interface for this is given by the optional :code:`WITH` clause of the :code:`ACTIVATE` command. The available tuning parameters are described below.

**batchsize**
	Number of events to accumulate before executing a continuous query plan on them. A higher value usually yields less frequent continuous view updates.

	*Defaults to 1000*

**emptysleepms**
	Number of milliseconds for a continuous query processes to wait before going to sleep if it hasn't received any new data, which prevents it from needlessly consuming CPU cycles. A higher value may cause a  continuous query worker process to waste CPU cycles but it will sleep less often.

	*Defaults to 2*

**maxwaitms**
	Number of milliseconds to wait for **batchsize** events to accumulate before forcing the continuous query plan to execute on however many events are available. A higher value usually yields less frequent continuous view updates.

	*Defaults to 2*

**parallelism**
	Number of parallel continuous query worker processes to use for the continuous view. A higher value will increase throughput but consume more CPU cycles.

	*Defaults to 1*

Here is an example of an :code:`ACTIVATE` command using these parameters:

.. code-block:: pipeline

	ACTIVATE name WITH ( batchsize = 100000, parallelism = 2 );

.. note:: Usually it won't be necessary to set any of these yourself. If you do decide to change these parameters, **batchsize** and **parallelism** are likely the only ones that will be useful to you.

---------------------
