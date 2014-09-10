==============
Setup Guide
==============

Prerequisites
==============

- Install a `JDK >= 1.6 <http://www.oracle.com/technetwork/java/javase/downloads/index.html?ssSourceSiteId=otnjp>`_ and `PostgresSQL >= 9.3 <http://www.postgresql.org>`_. I use `postgresapp.com <http://postgresapp.com>`_ on the Mac.
- Download `Play Framework 2.2 <http://www.playframework.com/download>`_ and unzip it to a folder of your choice.

Database Configuration
=========================

- Start the PostgresSQL console ``psql`` and create a new database: ``CREATE DATABASE wahlversprechen;``

wahlversprechen Configuration
==============================

- Clone the repository including submodules `git clone https://github.com/stheophil/wahlversprechen.git --recursive`
- In `conf/application.conf` search for `db.default.user` and set it to your database username
- *(optional) if your database is running on another machine, search for `db.default.url` and replace `localhost` with that machine's hostname*
- *(optional) if your database user has only password protected access, set it at `db.default.password`*

Start Play
===========

- From the terminal, ``cd`` to the folder you've cloned the wahlversprechen repository into.
- Start ``play`` from this folder by typing your equivalent of ``pathtoplay2.2/play``
- In the play console that should appear, type ``run -Dhttp.port=9000 -Dhttps.port=9001``
- In the browser, go to `localhost:9000 <http://localhost:9000>`_ and you should see a message *"Database 'default' needs evolution", press "Apply this script now"*. This will create the necessary database tables.
- Now you should see a completely empty version of the app.

What to do next?
================

- See :doc:`dataimport` on how to import data
- Edit the Admin preferences at `localhost:9001/admin/prefs <https://localhost:9001/admin/prefs>`_

That's it.
