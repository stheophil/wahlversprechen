# Tracking Election Promises

This is the code behind http://www.wahlversprechen2013.de, a web application that tracks the promises of the current German government. More precisely, it tracks the promises both governing parties made in their campaigns as well as the promises made in the coalition treaty. 

More details in English are available [in a blog post describing the project](http://theophil.net/2014/01/27/tracking-election-promises-with-scala-and-play/)

# TODOs

The application is up and running, but several areas need work. 

- The application needs to be internationalized to be generally useful.
- It needs a simple setup process to get new sites up and running. 
- I would like to improve the JSON API so people can visualize the data. 
- There is no real documentation and no real tests
- The database interface is still based on play.anorm although it has already outgrown anorm's capabilities and is turning into a mess

Other than that it is a great project :-)

# License

wahlversprechen is licensed under the MIT License.
