"use strict";

define(['jquery',
        'routes',
        'app/client',
        'moment',
        'mustache',
        'text!template/relatedUrlListItem.html'
    ],
    function($, jsroutes, client, moment, mustache, listItemTemplate) {

        function relatedUrlForTemplate(relatedurl, min_confidence) {
            var style = 'list-style-image: url(' + client.Util.faviconURL(relatedurl.url) + ');';
            if (relatedurl.confidence < min_confidence) {
                style += " display:none;";
            }

            return {
                style: style,
                time: moment(relatedurl.lastseen).fromNow(),
                relatedurl: relatedurl
            };
        }

        // elements with attribute "data-fill-relatedurls"
        // TODO: Pass template to render
        function loadMoreRelatedUrls(element, button, stmt_id, cNewItems) {
            cNewItems = typeof cNewItems !== 'undefined' ? cNewItems : 5;

            var itemCount = element.first().children().length;

            var cLoadItems = Math.max(10, 2 * cNewItems);
            var data = {};
            data['limit'] = cLoadItems;
            data['offset'] = itemCount;

            $.ajax({
                url: jsroutes.controllers.DetailViewController.relatedUrlsAsJSON(stmt_id).url,
                data: data
            }).done(function(relatedurls, textStatus, jqXHR) {
                var min_confidence = 8;
                var relatedurlsForTemplate = relatedurls.map(function(r) {
                    return relatedUrlForTemplate(r, min_confidence);
                });

                var cVisibleItems = 0;
                for (var i = 0; i < relatedurlsForTemplate.length && cVisibleItems < cNewItems; ++i) {
                    $(mustache.render(listItemTemplate, relatedurlsForTemplate[i])).appendTo(element);
                    if (min_confidence <= relatedurlsForTemplate[i].relatedurl.confidence) {
                        ++cVisibleItems;
                    }
                }
                if (relatedurls.length < cLoadItems) {
                    button.prop("disabled", true);
                } else if (cVisibleItems < cNewItems) {
                    loadMoreRelatedUrls(element, button, stmt_id, cNewItems - cVisibleItems);
                }
            })
        }

        $(document).ready(function() {
            $("[data-fill-relatedurls]").each(function() {
                var container = $(this);
                var stmt_id = container.data("stmt");
                var count = container.data("count");
                var loadMoreButtonID = container.data("load");
                var button = $(loadMoreButtonID);

                loadMoreRelatedUrls(container, button, stmt_id, count);
                button.click(function(e) {
                    loadMoreRelatedUrls(container, button, stmt_id, count);
                    e.preventDefault();
                });
            });
        });
    }
);