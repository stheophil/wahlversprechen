======================
Exporting Data in JSON
======================

JSON Endpoints
==============

- ``/json/tags`` 					a list of all existing tags::

	[
  		{"id":937,"name":"EU","important":false}, 
  		...
	]

- ``/json/categories`` 			an list of all existing categories::

	[
		{"id":2,"name":"Education","ordering":2},
		...
	]

- ``/json/authors`` 				a list of all authors (e.g. "Government", "Election Program 2012")::
	
	[
		{"id":1,"name":"Coalition Treaty","ordering":1,"top_level":true,"color":"#ffffff","background":"#999999"},
		...
	]

- ``/json/items/{author.name}`` 		get the list of all promises made by ``author`` with the given name::

	[
		{
			"id":680,
			"title":"This statement's title",
			"quote":"\"Whatever he said\"",
			"quote_src":"[Here's where he or she said it](http://cnn.com)",
			"author":"Coalition Treaty",
			"category":"Education",
			"tags":["EU","Research","Foreign Languages"],
			"ratings":[
				{"rating":"InTheWorks","date":"2014-01-01T01:00Z"}
				{"rating":"PromiseKept","date":"2014-01-23T17:12Z"}
			],
			"linked_to":""
		},
		...
	]

- ``/json/item/{id}`` 			promise with ``id``, also returns the text *entries* posted on the site::
	
	{
		"id":680,
		"title":"This statement's title",
		"quote":"\"Whatever he said\"",
		"quote_src":"[Here's where he or she said it](http://cnn.com)",
		"author":"Coalition Treaty",
		"category":"Education",
		"tags":["EU","Research","Foreign Languages"],
		"ratings":[
			{"rating":"InTheWorks","date":"2014-01-01T01:00Z"}
			{"rating":"PromiseKept","date":"2014-01-23T17:12Z"}
		],
		"entries":[
			{
				"id":77,
				"content":"# The text entry in Markdown formatting.\n May contain raw HTML too if the site is so configured.",
				"date":"2014-01-31T11:09Z",
				"user":"Sebastian"
			}
		],
		"linked_to":""
	}

- ``/json/item/{id}/relatedurls?[limit=x&offset=y]``	get the list of web pages related to promise ``id``, returns at most ``limit`` results starting at result ``offset`` in chronologically backwards order. Both ``limit`` and ``offset`` are optional.::
 
	[
		{
			"id":488,
			"stmt_id":682,
			"title":"Shocker: Many Europeans speak several languages",
			"url":"http://cnn.com/blabla",
			"confidence":3.03125,
			"lastseen":"2014-05-29T08:46Z"
		},
		...
	]
