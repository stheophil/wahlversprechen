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
                            stmt_url: "http://www.wahlversprechen2013.de/item/"+stmt.id,
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
                    updateVisibility(confidence, false);
                }).
                fail(function(jqXHR, textStatus, errorThrown) {
                    console.log("Error: " + errorThrown)
                });
        }

        function showOrFadeIn(elem, fade) {
            if(fade) {
                elem.fadeIn();
            } else {
                elem.show();
            }
        }

        function hideOrFadeOut(elem, fade) {
            if(fade) {
                elem.fadeOut();
            } else {
                elem.hide();
            }
        }

        function updateVisibility(confidence, fade) {
            $("ul.relatedurl").each(function() {
                var ul = $(this);
                var visible = false;
                ul.children("li").each(function() {
                    var li = $(this);
                    if(li.data("confidence")<confidence) {
                        hideOrFadeOut(li, fade);
                    } else {
                        showOrFadeIn(li, fade);
                        visible = true;
                    }
                });

                var parent = ul.parents(".stmt-list");
                if(visible) {
                    showOrFadeIn(parent, fade);
                } else {
                    hideOrFadeOut(parent, fade);
                }
            });
        }

        function createDatePicker(id) {
            return new pikaday({
                field: document.getElementById(id),
                firstDay: 1,
                minDate: new Date('2014-01-01'),
                maxDate: new Date('2018-12-31'),
                yearRange: [2014, 2018],
                format: 'L',
                i18n: {
                    previousMonth : 'Voriger Monat',
                    nextMonth     : 'Nächster Monat',
                    months        : ['Januar','Februar','März','April','Mai','Juni','Juli','August','September','Oktober','November','Dezember'],
                    weekdays      : ['Sonntag','Montag','Dienstag','Mittwoch','Donnerstag','Freitag','Samstag'],
                    weekdaysShort : ['So','Mo','Di','Mi','Do','Fr','Sa']
                }
            });
        }

        $(document).ready(function() {
            var picker_from = createDatePicker('datepicker_from');
            var picker_to = createDatePicker('datepicker_to');

            var slider = $('input[type="range"]');
            slider.rangeslider();

            slider.change(function(evt) {
                updateVisibility(slider.val(), true);
            });

            // Load all statements and setup event handlers
            client.Statement.loadAll().done( function(stmts) {
                console.log("Loaded " + stmts.length + " statements.");
                var mapStatements = client.Util.toMap(stmts, 'id');

                var button = $("#gobutton");
                button.click(function(evt) {
                    evt.preventDefault();

                    var from = picker_from.getDate();
                    var to = picker_to.getDate();
                    var confidence = slider.val();

                    onButtonClick(from, to, confidence, mapStatements);
                });

                button.removeClass("disabled");
            });
        });
});