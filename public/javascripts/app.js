"use strict";

require.config({
    baseUrl: "/assets/javascripts/lib",
    shim: {
        "routes": {
            exports: "jsroutes"
        },
        'typeahead': {
            deps: ['jquery']
        },
        'bootstrap': {
            deps: ['jquery']
        }
    },
    paths: {
        "app": "./..",
        "template": "./../../template",
        "routes": "/routes"
    }
});

// TODO: Currently, title, detailView and editing.js are always included instead of making
// them requirejs entry points. However, the size of our site javascript is very small compared
// to the libraries we include. So it may be better to load on 300K js file (unzipped) instead of 
// three different ones. 
// See https://github.com/jrburke/requirejs/wiki/Patterns-for-separating-config-from-the-main-module

require(['jquery', 'routes', 'moment', 'typeahead', 'bootstrap', 'app/title', 'app/detailView', 'app/editing'],
    function($, jsroutes, moment, client) {

        // setup typeahead search box
        {
            // constructs the suggestion engine
            var tags = new Bloodhound({
                datumTokenizer: Bloodhound.tokenizers.obj.whitespace('name'),
                queryTokenizer: Bloodhound.tokenizers.whitespace,
                prefetch: {
                    url: jsroutes.controllers.Application.tagsAsJSON().url
                }
            });

            tags.initialize();

            var categories = new Bloodhound({
                datumTokenizer: Bloodhound.tokenizers.obj.whitespace('name'),
                queryTokenizer: Bloodhound.tokenizers.whitespace,
                prefetch: {
                    url: jsroutes.controllers.Application.categoriesAsJSON().url
                }
            });

            categories.initialize();

            $('.typeahead').typeahead({
                hint: true,
                highlight: true,
                minLength: 1
            }, {
                name: 'tags',
                displayKey: 'name',
                source: tags.ttAdapter(),
                templates: {
                    header: '<h3 class="tt-header">Stichwörter</h3>'
                }
            }, {
                name: 'categories',
                displayKey: 'name',
                source: categories.ttAdapter(),
                templates: {
                    header: '<h3 class="tt-header">Ressorts</h3>'
                }
            });

            $('.typeahead').on("typeahead:selected typeahead:autocompleted", function(evt, suggestion, dataset) {
                if (dataset === "tags") {
                    window.location = jsroutes.controllers.Application.tag(suggestion.name).url;
                } else if (dataset === "categories") {
                    window.location = jsroutes.controllers.Application.category(suggestion.name).url;
                }
            });

            $('.typeahead').on("change", function(e) {
                window.location = jsroutes.controllers.Application.search(e.target.value).url;
            });
        }

        // Setup consistent time rendering  
        moment.locale('de', {
            relativeTime: {
                future: "in %s",
                past: "%s",
                s: "< 1 min",
                m: "1 min",
                mm: "%d min",
                h: "1 h",
                hh: "%d h",
                d: "1 Tag",
                dd: "%d Tage",
                M: "1 Monat",
                MM: "%d Monate",
                y: "1 Jahr",
                yy: "%d Jahre"
            }
        });
    });