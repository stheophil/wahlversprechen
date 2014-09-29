/*global define module require */
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
  // Make the library usable as a AMD module, as well as a globally
  // exported variable.
  // http://stackoverflow.com/questions/12558499/how-to-write-an-amd-module-for-use-in-pages-without-requirejs

  // Node support is untested!
  if (typeof module === 'object') {
    module.exports = definition(require('jquery'))
  } else if (typeof define == 'function' && define.amd) {
  // Don't name the module with AMD.
  // http://www.requirejs.org/docs/api.html#modulename
    define(['jquery'], definition)
  } else this[name] = definition()
  // name should be syntactically valid JavaScript property
})('wahlversprechenClient', function ($) {
  var urlBase = '/json/'

  function getURL(suffix) {
    return urlBase + suffix
  }

  return {
    tags: {
      /**
       * Queries all tags.
       *
       * Returns a jqXHR promise for a possibly empty array.
       */
      get: function() {
        return $.getJSON(this.url);
      },
      url: getURL('tags')
    }
  };
});