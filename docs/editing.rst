==================================
Using wahlversprechen as an Editor
==================================


Markdown Formatting
===================

When writing updates on campaign promises, you can use Markdown to format your texts. Markdown is a very simple text format that remains legible, even when it is not rendered as HTML. `See the reference guide for more information <http://daringfireball.net/projects/markdown/>`_. Here is an example:

::

    # This would be a top level heading

    And this is a short first paragraph with *italic* text and **bold** text.

    ## This is a second-level heading

    Links, e.g., [CNN](http://www.cnn.com) are quick to write as well. 

    - When you need lists, 
    - just write a list.

Rendered as HTML it looks like this:

This would be a top level heading
.................................

And this is a short first paragraph with *italic* text and **bold** text.

This is a second-level heading
------------------------------

Links, e.g., `CNN <http://www.cnn.com>`_ are quick to write as well. 

 - When you need lists, 
 - just write a list.

Embedding Charts
================

While most text formatting can and should be done with Markdown, HTML can be used to embed charts e.g. `Datawrapper.de <http://www.datawrapper.de>`_ can be used to create embeddedable, interactive charts to visualize important statistics. 

Once you've created a chart with `datawrapper <http://www.datawrapper.de>`_ or most other charting websites, the chart can be embedded using HTML code similar to the following:

::

	<iframe src="http://cf.datawrapper.de/VLnBD/1/" 
	... 
	width="600" height="400">
	</iframe>

where ``http://cf.datawrapper.de/VLnBD/1/`` is the address of the chart you've just created. For best results, this should be changed slightly to

::

	<iframe src="http://cf.datawrapper.de/VLnBD/1/" 
	... 
	width="100%" height="400">

	Please <a href="http://cf.datawrapper.de/VLnBD/1/">click here to see the chart</a>.

	</iframe>

Note that ``width="600"`` was changed to ``width="100%"`` which allows the chart to always occupy 100% of the available screen width on a large computer screen as well as on a small phone screen. `See here for an example <http://www.wahlversprechen2013.de/item/794>`_. The height must be a fixed value. 300 or 400 work fine. 

The additional line ``Please <a href="http://cf.datawrapper.de/VLnBD/1/">click here to see the chart</a>.`` is shown in environments that will not display the embedded charts, for example most RSS feed readers. It refers the reader to the website where he can see the chart.

