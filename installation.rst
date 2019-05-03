.. _installation:

..  PipelineDB Installation

PipelineDB安装
===========================

..  Install PostgreSQL

安装PostgreSQL
---------------------------

..	Since PipelineDB runs as an extension to PostreSQL, begin by `installing PostgreSQL`_.

PipelineDB是以PostgreSQL插件运行的，先从 `安装PostgreSQL`_ 开始。

.. note::
	..	PipelineDB currently supports PostgreSQL versions 10.1, 10.2, 10.3, 10.4, 10.5, and 11.0 on **64-bit architectures**.

	PipelineDB当前支持64位架构下的PostgreSQL版本：10.1，10.2，10.3，10.4，10.5，10.6和11.0。

.. _`installing PostgreSQL`: https://www.postgresql.org/download/
.. _`安装PostgreSQL`: https://www.postgresql.org/download/

..	Once you have PostgreSQL installed on your system, you just need to install the PipelineDB binaries and then create the PipelineDB extension within your PostgreSQL database. You can install binaries from our **apt** or **yum** repositories or you can download packages from our `release archives`_ and install them directly.

安装好PostgreSQL后，您只需安装PipelineDB二进制包并且在PostgreSQL数据库中创建PipelineDB插件。您可以通过 **apt**、**yum** 以及直接通过 `安装包`_ 进行安装。

.. _`release archives`: https://github.com/pipelinedb/pipelinedb/releases
.. _`安装包`: https://github.com/pipelinedb/pipelinedb/releases

apt
------------

..	First, add our **apt** repository to your system (`inspect apt.sh`_):

首先，添加 **apt源** 到系统源中（`apt.sh安装脚本`_）：

.. _`inspect apt.sh`: http://download.pipelinedb.com/apt.sh
.. _`apt.sh安装脚本`: http://download.pipelinedb.com/apt.sh

.. code-block:: sh

	curl -s http://download.pipelinedb.com/apt.sh | sudo bash

..	Now simply install the latest PipelineDB package:

安装最新的PipelineDB包：

.. code-block:: sh

  # PostgreSQL 10
  sudo apt-get install pipelinedb-postgresql-10

  # PostgreSQL 11
  sudo apt-get install pipelinedb-postgresql-11

yum
---------------

..	Add our **yum** repository to your system (`inspect yum.sh`_):

添加 **yum源** 到系统源中（`yum.sh安装脚本`_）：

.. _`inspect yum.sh`: http://download.pipelinedb.com/yum.sh
.. _`yum.sh安装脚本`: http://download.pipelinedb.com/yum.sh

.. code-block:: sh

	curl -s http://download.pipelinedb.com/yum.sh | sudo bash

..	Install the latest PipelineDB package:

安装最新的PipelineDB包：

.. code-block:: sh

 # PostgreSQL 10
 sudo yum install pipelinedb-postgresql-10

 # PostgreSQL 11
 sudo yum install pipelinedb-postgresql-11

.. note::
	..	**apt** and **yum** repositories only need to be added to your system a single time. Once you've added them, you don't need to run these scripts again. You need only run the installation commands to get new versions of PipelineDB.

	一旦将 **apt源** 和 **yum源** 添加到系统，后面无需再次运行这些脚本，只需要运行安装指令获取最新PipelineDB包。

-------------------------

..	You may also download binary packages from our `release <https://github.com/pipelinedb/pipelinedb/releases>`_ archives and install them directly.

您可以直接从我们的 `github仓库 <https://github.com/pipelinedb/pipelinedb/releases>`_ 中直接下载二进制包。

..	RPM Packages

RPM包安装
--------------------

..	To install the PipelineDB RPM package, run:

运行下面的指令以.rpm形式安装：

.. code-block:: sh

	sudo rpm -ivh pipelinedb-postgresql-<pg version>_<pipelindb version>.rpm

..	Debian Packages

Debian包安装
---------------------

..	To install the PipelineDB Debian package, run:

运行下面的指令以.deb形式安装：

.. code-block:: sh

	sudo dpkg -i pipelinedb-postgresql-<pg version>_<pipelindb version>.deb

.. _creating-extension:

..	Creating the PipelineDB Extension

创建PipelineDB插件
------------------------------------------

..	In order for PipelineDB to run, the :code:`shared_preload_libraries` configuration parameter must be set in :code:`postgresql.conf`, which can be found underneath your data directory. It's also a good idea to set :code:`max_worker_processes` to something reasonably high to give PipelineDB worker processes plenty of capacity:

为了PipelineDB能够运行，必须在Postgres数据目录下的 :code:`postgresql.conf` 文件中设置好 :code:`shared_preload_libraries` 参数，同时 :code:`max_worker_processes` 也必须被设到一个合理并且足够大的值来保证PipelineDB的计算性能。

.. code-block:: sh

	# At the bottom of <data directory>/postgresql.conf
	shared_preload_libraries = 'pipelinedb'
	max_worker_processes = 128

..	Running PostgreSQL

运行PostgreSQL
---------------------

..	To run the PostgreSQL server in the background, use the :code:`pg_ctl` driver and point it to your newly initialized data directory:

通过 :code:`pg_ctl` 指令并指向新创建的数据目录，使PostgreSQL服务在后台运行：

.. code-block:: sh

	pg_ctl -D <data directory> -l postgresql.log start

..	To connect to a running server using the default database, use PostgreSQL's standard client, `psql`_, which can be used to create the PipelineDB extension:

使用PostgreSQL的标准客户端 `psql`_ 连接运行中服务的默认database，执行以下指令创建PipelineDB插件：

.. code-block:: sh

	psql -c "CREATE EXTENSION pipelinedb"

..	Once the PipelineDB extension has been created, you're ready to start using PipelineDB!

PipelineDB插件创建完毕后，您就可以开始使用PipelineDB了！

.. _`psql`:  https://www.postgresql.org/docs/current/static/app-psql.html

..	You can check out the :ref:`quickstart` section to start streaming data into PipelineDB right now.

现在您可以参考 :ref:`快速开始<quickstart>` 部分来将数据流式写入PipelineDB。

..	Configuration

配置项
---------------------

..	By default, PostgreSQL is not configured to allow incoming connections from remote hosts. To enable incoming connections, first set the following line in :code:`postgresql.conf`:

默认情况下，PostgreSQL禁止远程主机连接。为了开放连接，需要修改 :code:`postgresql.conf` 中配置作如下修改：

.. code-block:: sh

    listen_addresses = '*'

..	And in :code:`pg_hba.conf`, add a line such as the following to allow incoming connections:

同时在 :code:`pg_hba.conf` 中添加一行配置以开放连接：

.. code-block:: sh

    host    all             all             <ip address>/<subnet>            md5


..	For example, to allow incoming connections from any host:

比如，开放任意主机连接：

.. code-block:: sh

    host    all             all             0.0.0.0/0            md5

-------------

Docker
---------------------

..	PipelineDB is available as a `Docker image`_, making it very easy to run on platforms that don't currently have official packages. The PipelineDB extension will automatically be created upon database initialization, so :ref:`creating-extension` is is not necessary with the Docker image.

可直接获取PipelineDB的 `Docker镜像`_，这使其可以很简单地在基于Unix架构的系统中运行，镜像中包含完整的依赖，无需额外安装即可直接运行。

..	You can run a PipelineDB Docker container via :code:`docker run`:

您可以基于镜像，通过 :code:`docker run` 启动PipelineDB实例：

.. code-block:: sh

  docker run pipelinedb/pipelinedb-postgresql-{postgresql version}

..	The PipelineDB Docker image uses the `PostgreSQL image`_ as its parent, so all configuration and customization can be done via the interface that the `PostgreSQL image`_ provides.

PipelineDB Docker镜像基于 `PostgreSQL image`_ 构建，所有配置项及个性化参数均可以通过 `PostgreSQL镜像`_ 提供的接口进行设置。

.. _`Docker image`: https://hub.docker.com/r/pipelinedb/pipelinedb-postgresql-11
.. _`Docker镜像`: https://hub.docker.com/r/pipelinedb/pipelinedb-postgresql-11
.. _`PostgreSQL image`: https://hub.docker.com/_/postgres/
.. _`PostgreSQL镜像`: https://hub.docker.com/_/postgres/

-----------------

..	Now you're ready to put PipelineDB to work! Check out the :ref:`continuous-views` or :ref:`quickstart` sections to get started.

此时PipelineDB已能正常工作！跳转到 :ref:`流视图<continuous-views>` 或 :ref:`快速开始<quickstart>` 部分来开始使用PipelineDB。

macOS/OSX
---------------------

..	Since there is no standard PostgreSQL installation location on macOS/OSX, we provide a generic tarball package for these platforms from which you may install the PipelineDB binaries against an existing PostgreSQL installation.

鉴于没有macOS/OSX平台下的标准PostgreSQL安装包，我们为这类平台提供了一通用的二进制安装包，您可以在PostgreSQL安装完毕后直接进行安装。

..	After downloading the latest release tarball from our `release archives`_, you just need to run the simple :code:`install.sh` script:

下载好最新的 `安装包`_ 后，您只需运行 :code:`install.sh` 即可：

.. code-block:: sh

  $ tar -xvf pipelinedb-postgresql-<pg version>-<pipelinedb version>.osx.tar.gz
  $ cd pipelinedb-postgresql-<pg version>-<pipelinedb version>
  $ sudo ./install.sh
  PipelineDB installation complete.

..	From here you may proceed by :ref:`creating-extension`.

跳转到 :ref:`创建PipelineDB插件<creating-extension>`。
