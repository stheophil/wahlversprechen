"use strict";

define(['jquery',
        'routes',
        'app/client',
        'moment',
        'mustache',
        'pikaday',
        'rangeslider',
        'text!template/relatedUrlGroup.html'
    ],
    function($, jsroutes, client, Moment, mustache, pikaday, rangeslider, template) {
        Moment.locale('de');

        function onButtonClick(from, to, confidence, mapStatements) {
            var div = $("#relatedurl");

            client.RelatedURLs.load(from, to).
            	done( function(relatedurls) {
                    div.children().remove();

                    var relatedUrlsByStmt = {};

                    relatedurls.forEach( function(relatedurl) {
                        relatedurl.favicon = client.Util.faviconURL(relatedurl.url);

                        if(relatedUrlsByStmt[ relatedurl.stmt_id ] === undefined) {
                            relatedUrlsByStmt[ relatedurl.stmt_id ] = [];
                        }
                        relatedUrlsByStmt[ relatedurl.stmt_id ].push(relatedurl);
                    });

                    var relatedUrlGroups = [];
                    for(var stmt_id in relatedUrlsByStmt) {
                        var arrRelatedUrlGroup = relatedUrlsByStmt[stmt_id];
                        arrRelatedUrlGroup.sort( client.Util.reverseCompareBy(function(r) { return r.confidence; }) );

                        var time = Moment.max(
                                arrRelatedUrlGroup.map( function(a) { return Moment(a.lastseen); })
                            );
                        var stmt = mapStatements[stmt_id];
                        var visible = confidence <= arrRelatedUrlGroup[0].confidence;

                        relatedUrlGroups.push({
                            stmt_id: stmt.id,
                            stmt_url: stmt.url,
                            stmt_title: stmt.title,
                            stmt_category: stmt.category,
                            lastSeen: time.format("L"),
                            visible: visible,
                            confidence: arrRelatedUrlGroup[0].confidence,
                            articles: arrRelatedUrlGroup
                        });
                    }
                    relatedUrlGroups.sort( client.Util.reverseCompareBy( function(r) {
                        return r.confidence;
                    }));

                    $(mustache.render(
                        template,
                        {
                            relatedUrlGroups: relatedUrlGroups
                        }
                    )).appendTo(div);

                    $("ul.relatedurl").each(function() {
                        var ul = $(this);
                        var visible = false;
                        ul.children("li").each(function() {
                            var li = $(this);
                            if(li.data("confidence")<confidence) {
                                li.hide();
                            } else {
                                visible = true;
                            }
                        });

                        if(!visible) {
                            ul.parents(".stmt-list").hide();
                        }
                    });
                }).
                fail(function(jqXHR, textStatus, errorThrown) {
                    console.log("Error: " + errorThrown)
                });
        }

        function createDatePicker(id) {
            return new pikaday({
                field: document.getElementById(id),
                firstDay: 1,
                minDate: new Date('2014-01-01'),
                maxDate: new Date('2018-12-31'),
                yearRange: [2014, 2018]
            });
        }

        $(document).ready(function() {
            var picker_from = createDatePicker('datepicker_from');
            var picker_to = createDatePicker('datepicker_to');

            var slider = $('input[type="range"]');
            slider.rangeslider();

            // Load all statements and setup event handlers
            client.Statement.loadAll().done( function(stmts) {
                console.log("Loaded " + stmts.length + " statements.");
                var mapStatements = client.Util.toMap(stmts, 'id');

                $("#gobutton").click(function(evt) {
                    evt.preventDefault();

                    var from = picker_from.getDate();
                    var to = picker_to.getDate();
                    var confidence = slider.val();

                    onButtonClick(from, to, confidence, mapStatements);
                });
            });
        });
});