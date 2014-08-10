==============
Importing Data
==============

Currently, large amounts of data can only be imported from Google spread sheets. There is an `open issue <https://github.com/stheophil/wahlversprechen/issues/24>`_ on github to fix this. 

- Go to `localhost:9001/admin/import <https://localhost:9001/admin/import>`_ 
- Login when necessary. On a default installation use user name ``test@test.net`` and password ``secret``
- Import some test data from a publicly shared Google Sheet. You can use e.g. ``0AuS3i7YOiV-wdHM1dHhTY3lfTUJTa1VBNzJ0eDdpekE`` which is `this sheet <https://docs.google.com/spreadsheet/pub?key=0AuS3i7YOiV-wdHM1dHhTY3lfTUJTa1VBNzJ0eDdpekE&output=html>`_

The Data Model 
==============

You can import arbitrary *statements*, e.g., campaign promises, that you want to track. A *statement* has an *author* who made this statement or promise. There is a very simple hierarchy among *authors*. An *author* is a *top level* author or it is not. This is used to represent campaign promises in coalition governments, where several parties competed in the elections based on their own programs and then form a coalition, often with a formalized coalition treaty. The coalition treaty would be the *top level author* and the individual party programs would be second level authors. 

A statement from a second level author can reference a single statement from a top level author, i.e., a campaign promise from party A could refer to the statement in the coalition treaty it is most closely related to. 

It is currently impossible, to edit the list of authors in the `admin preferences <https://localhost:9001/admin/prefs>`_. The list of authors is set when the application is started for the first time. See ``app/Global.scala``. Again, there is an `open issue <https://github.com/stheophil/wahlversprechen/issues/25>`_ on github.

Importing from Google Spreadsheets
==================================

If you want to import your own data, your Google spreadsheet needs to have the same column names as the test data:

- `titel` - the title of your campaign promise
- `zitat` - the exact quote that you want to track in Markdown format. 
- `quelle` - the source of the quote in Markdown format. 
- `ressort` - the promise's category, e.g. "Foreign Affairs", "Economics" etc
- `tags` - a comma separated list of tags
- `links` - when importing statements for a *top-level author*, a comma separated list of statement ids of second level authors. When the list includes more than one statement id for a single second level author, only a single id will be imported. 

Yes, there is also an `open issue <https://github.com/stheophil/wahlversprechen/issues/20>`_ to internationalize all texts in the application, including these column names.

You can import the same table multiple times. Previously imported statements are identified based on their title. Thus, you cannot change these after the first import. All other fields of already existing statements are updated. 