"use strict";

define(['jquery', 'routes'], function($, jsroutes) {
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
        }
    };

    return client;
});