.. _activation-deactivation:

Activation and deactivation
============================

Because :ref:`continuous-views` are continuously processing input streams, it is useful to have a notion of starting and stopping that processing without having to completely shutdown PipelineDB. This level of control is accomplished by the :code:`ACTIVATE` and :code:`DEACTIVATE` commands, which are synonymous with "play" and "pause". When a :code:`CONTINUOUS VIEW` is *active*, it is actively reading from its input streams and incrementally updating its result accordingly. Conversely, an *inactive* :code:`CONTINUOUS VIEW` is not reading its input streams and is not updating its result.

.. important:: If a :code:`CONTINUOUS VIEW` is inactive, any events written to its input streams will never be read by that :code:`CONTINUOUS VIEW`, even after it is activated again.

ACTIVATE
-----------

.. code-block:: pipeline

	ACTIVATE { name [, ...] | WHERE condition } [ WITH ( parameter = value [, ...] ) ]

The syntax for :code:`DEACTIVATE` is similar, it just excludes the :code:`WITH` clause:

DEACTIVATE
-----------

.. code-block:: pipeline

	DEACTIVATE { name [, ...] | WHERE condition }

**name**
	The name of the :code:`CONTINUOUS VIEW`

**condition**
	An expression involving the columns of the :code:`pipeline_query` catalog table. See :ref:`pipeline-query` for more information about :code:`pipeline_query`. All :code:`CONTINUOUS VIEW` s for which this condition evaluates to :code:`true` will be activated or deactivated, depending on which command was issued.

**parameter**
	One of: **batchsize**, **emptysleepms**, **maxwaitms**, **parallelism** (explained in the :ref:`parameters` section)

**value**
	An integral value


--------------------

:code:`ACTIVATE` and :code:`DEACTIVATE` both return the number of :code:`CONTINUOUS VIEW` s that were affected by the command.

Examples
-----------

Let's take a look at a few examples of what it would look like to :code:`ACTIVATE` and :code:`DEACTIVATE` :code:`CONTINUOUS VIEW` s from an interactive :code:`psql` session. Assume that we've already created the following :code:`CONTINUOUS VIEW` s:

- :code:`view0`
- :code:`view1`
- :code:`continuous_view0`
- :code:`continuous_view2`

This will :code:`ACTIVATE` all 4 of them:

.. code-block:: pipeline

	pipeline=# ACTIVATE;
	4

Calling :code:`ACTIVATE` on an active :code:`CONTINUOUS VIEW` s is a noop:

.. code-block:: pipeline

	pipeline=# ACTIVATE;
	4
	pipeline=# ACTIVATE;
	0

:code:`ACTIVATE` :code:`view0` and :code:`view1`:

.. code-block:: pipeline

	pipeline=# ACTIVATE view0, view1;
	2

:code:`ACTIVATE` :code:`continuous_view0` and :code:`continuous_view1`:

.. code-block:: pipeline

	pipeline=# ACTIVATE WHERE name LIKE '%continuous%';
	2

:code:`DEACTIVATE` :code:`continuous_view0` and :code:`view0`:

.. code-block:: pipeline

	pipeline=# ACTIVATE WHERE name LIKE '%view0%';
	2


.. _parameters:

Parameters
-------------

It is possible to supply performance tuning parameters to :code:`CONTINUOUS VIEW` s. The interface for this is given by the optional :code:`WITH` clause of the :code:`ACTIVATE` command. The available tuning parameters are described below.

**batchsize**
	Number of events to accumulate before executing a continuous query plan on them. A higher value usually yields less frequent :code:`CONTINUOUS VIEW` updates.

	*Defaults to 1000*

**emptysleepms**
	Number of milliseconds for a continuous query processes to wait before going to sleep if it hasn't received any new data, which prevents it from needlessly consuming CPU cycles. A higher value may cause a  continuous query worker process to waste CPU cycles but it will sleep less often.

	*Defaults to 2*

**maxwaitms**
	Number of milliseconds to wait for **batchsize** events to accumulate before forcing the continuous query plan to execute on however many events are available. A higher value usually yields less frequent :code:`CONTINUOUS VIEW` updates.

	*Defaults to 2*

**parallelism**
	Number of parallel continuous query worker processes to use for the :code:`CONTINUOUS VIEW`. A higher value will increase throughput but consume more CPU cycles.

	*Defaults to 1*

Here is an example of an :code:`ACTIVATE` commands using these parameters:

.. code-block:: pipeline

	ACTIVATE name WITH ( batchsize = 100000, parallelism = 2);

.. note:: Usually it won't be necessary to set any of these yourself. If you do decide to change these parameters, **batchsize** and **parallelism** are likely the only ones that will be useful to you.

---------------------
