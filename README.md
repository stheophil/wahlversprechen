[![Build Status](https://travis-ci.org/stheophil/wahlversprechen.svg?branch=master)](https://travis-ci.org/stheophil/wahlversprechen)

# Tracking Election Promises 

This is the code behind http://www.wahlversprechen2013.de, a web application that tracks the promises of the current German government. More precisely, it tracks the promises both governing parties made in their campaigns as well as the promises made in the coalition treaty. 

More details in English are available [in a blog post describing the project](http://theophil.net/2014/01/27/tracking-election-promises-with-scala-and-play/)


# Setup Guide

**Prerequisites:**
- Install a [JDK >= 1.6](http://www.oracle.com/technetwork/java/javase/downloads/index.html?ssSourceSiteId=otnjp) and [PostgresSQL >= 9.3](http://www.postgresql.org). I use [postgresapp.com](http://postgresapp.com) on the Mac. 
- Download [Play Framework 2.2](http://www.playframework.com/download) and unzip it to a folder of your choice.

**Configuring the database**
- Start the PostgresSQL console psql and create a new database: `CREATE DATABASE your_database_name;`

**Configuring the wahlversprechen web application**

- Clone the repository
- Copy `conf/application.conf.template` to `conf/application.conf`. In `conf/application.conf` edit the following settings:
- Set `db.default.url="jdbc:postgresql://localhost/your_database_name"` _(if your database is running on another machine, replace localhost with that machine's name, obviously)_
- Set `db.default.user` to your user name _(or the database user if you have configured one)_
- Set `application.secret`


**Start Play**
- From the terminal, `cd` to the folder you've cloned the wahlversprechen repository into. 
- Start `play` from this folder by typing your equivalent of `pathtoplay2.2/play`
- In the play console that should appear, type `run -Dhttp.port=9000 -Dhttps.port=9001`
- In the browser, go to [localhost:9000](http://localhost:9000) and you should see a message "Database 'default' needs evolution", press "Apply this script now". This will create the necessary database tables.
- Now you should see a completely empty version of the app.

**Importing Test Data**
- Open [localhost:9001/login](http://localhost:9001/login) and login with user name "test@test.net" and password "secret".
- Now go to [localhost:9001/admin/import](https://localhost:9001/admin/import) and import some test data from a publicly shared Google Sheet. You can use e.g. `0AuS3i7YOiV-wdHM1dHhTY3lfTUJTa1VBNzJ0eDdpekE` which is [this sheet](https://docs.google.com/spreadsheet/pub?key=0AuS3i7YOiV-wdHM1dHhTY3lfTUJTa1VBNzJ0eDdpekE&output=html). Or you can create your own test data as long as the table has the exact same column names. 
- You can make some changes to the site using the Admin preferences at [localhost:9001/admin/prefs](https://localhost:9001/admin/prefs)

That's it. 

# Contributing

Please do! All contributions are welcome, but several areas have to be improved

- The application needs to be internationalized to be usable in other countries.
- There should be a simple setup page that appearing when the site is started for the first time without any data.
- I would like to improve the JSON API so people can visualize the data. 
- The site needs fancy visualizations for the data that is accumulated, e.g., a timeline of the fulfilled campaign promises.

# License

wahlversprechen is licensed under the MIT License.
