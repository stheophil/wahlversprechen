/*global define Bloodhound */

/**
 * Support typing completion.
 *
 * Provide typeahead datasets and Bloodhounds.
 *
 * https://github.com/twitter/typeahead.js
 */
define(['routes', 'typeahead'], function(jsroutes) {

    function createTagsBloodhound() {
        var tags = new Bloodhound({
            datumTokenizer: Bloodhound.tokenizers.obj.whitespace('name'),
            queryTokenizer: Bloodhound.tokenizers.whitespace,
            prefetch: {
                url: jsroutes.controllers.Application.tagsAsJSON().url
            }
        });

        tags.initialize();

        return tags;
    }

    function createCategoriesBloodhound() {
        var categories = new Bloodhound({
            datumTokenizer: Bloodhound.tokenizers.obj.whitespace('name'),
            queryTokenizer: Bloodhound.tokenizers.whitespace,
            prefetch: {
                url: jsroutes.controllers.Application.categoriesAsJSON().url
            }
        });

        categories.initialize();

        return categories;
    }

    return {
        datasets: {
            getCategories: function() {
                return {
                    name: 'categories',
                    displayKey: 'name',
                    source: createCategoriesBloodhound().ttAdapter(),
                    templates: {
                        header: '<h3 class="tt-header">Ressorts</h3>'
                    }
                };
            },
            getTags: function() {
                return  {
                    name: 'tags',
                    displayKey: 'name',
                    source: createTagsBloodhound().ttAdapter(),
                    templates: {
                        header: '<h3 class="tt-header">Stichwörter</h3>'
                    }
                };
            }
        }
    };
});
