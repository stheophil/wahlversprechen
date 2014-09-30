"use strict";

define(['jquery'], function($) {
    // Show the semi-transparent fade-out effect if 
    // the content reaches the maximum allowed height
    $(document).ready($('.read-more').each(function() {
        var height = $(this).parent().css("height");
        var maxheight = $(this).parent().css("max-height");
        if (height === maxheight) {
            $(this).show();
        }
    }));
});