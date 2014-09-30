/*global define module require moment jsroutes */
/**
 * Provides access to public data from http://www.wahlversprechen2013.de/
 *
 * client.js is a library to consume the JSON API.
 *
 * jQuery needs to be available, either as AMD module 'jquery'
 * or globally as window.jQuery.
 *
 * MIT License
 */

!(function (name, definition) {
    // Make the library usable as an AMD module, as well as a globally
    // exported variable.
    // http://stackoverflow.com/questions/12558499/how-to-write-an-amd-module-for-use-in-pages-without-requirejs

    // Node support is untested!
    if (typeof module === 'object') {
        throw new Error("node support not yet possible: routes missing.")
        module.exports = definition(require('jquery') /* routes missing */)
    } else if (typeof define == 'function' && define.amd) {
        // Don't name the module with AMD.
        // http://www.requirejs.org/docs/api.html#modulename
        define(['jquery', 'routes'], definition)
    } else {
        throw new Error("Cannot yet export as global variable: routes missing.")
        this[name] = definition()
    }
    // name should be syntactically valid JavaScript property
})('wahlversprechenClient', function ($) {
    var urlBase = '/json/'

    function getURL(suffix) {
        return urlBase + suffix
    }
    var client = {
        Util : {
            parseURL :
            function(href) {
                var l = document.createElement("a");
                l.href = href;
                return l; // HTMLAnchorElement implements URLUtils interface
            },
            faviconURL :
            function(href) {
                var host = this.parseURL(href).hostname;
                return "//www.google.com/s2/favicons?domain="+host;
            }
        },
        Author : {
            loadAll : function() {
                return $.ajax({
                    url: jsroutes.controllers.Application.authorsAsJSON().url
                });
            }
        },
        Statement : {
            loadForAuthor : function(authorName) {
                return $.ajax({
                    url: jsroutes.controllers.Application.itemsByAuthorAsJSON(authorName).url
                });
            },
            loadAll : function() {
                return client.Author.loadAll().done( function( authors ) {
                    var deferred = authors.map( function( author ) {
                        return client.Statement.loadForAuthor(author.name);
                    });

                    $.when.apply($, deferred).done( function() {
                        var stmts = [];
                        for(var i=0; i<arguments.length; ++i) {
                            stmts.push.apply(stmts, arguments[i][0]);
                        }
                        return stmts;
                    });
                });
            }
        },
        RelatedURLs : {
            load : function(from, to) {
                if(from != null && (to == null || from < to)) {
                    var url;
                    var f = moment(from).format("YYYYMMDD")
                    if(to==null) {
                        url = jsroutes.controllers.Application.relatedUrlsAsJSON(f).url
                    } else {
                        url = jsroutes.controllers.Application.relatedUrlsAsJSON(f, moment(to).format("YYYYMMDD")).url
                    }

                    return $.ajax({ url: url });
                } else {
                    return $.Deferred().reject(null, "", "Invalid date range");
                }
            }
        },
        Tag : {
            /**
             * Queries all tags.
             *
             * Returns a jqXHR promise for a possibly empty array.
             */
            loadAll: function() {
                return $.getJSON(jsroutes.controllers.Application.tagsAsJSON().url);
            }
        }
    };

    return client;
});