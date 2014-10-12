/*global define */
define(['jquery', 'routes', 'app/completion'], function($, jsroutes, completion) {
    function setupTagCompletion() {
        var $inputbox = $('li.tags .typeahead-tags');

        $inputbox.typeahead({
            hint: true,
            highlight: true,
            minLength: 1
        },
            completion.datasets.getTags()
        );
    }

    $(function() {
        setupTagCompletion();
    });
});