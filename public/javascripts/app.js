require.config({
        baseUrl : "/assets/javascripts/lib",
        shim : {
             "routes" : {
                 exports: "jsroutes"
             }
        },
        paths : {
                "app" : "./..",
                "template" : "./../../template",
                "routes" : "/routes"
            }
        });

function getLocation(href) {
    var l = document.createElement("a");
    l.href = href;
    return l;
}

// TODO: Currently, editing.js is always included instead of only including
// it on pages that need it
// See https://github.com/muuki88/playframework-requirejs-multipage for
// an example project
require(['jquery',
        'routes',
        'moment',
        'mustache',
        'text!template/relatedUrlListItem.html',
        'typeahead',
        'app/editing',
        'bootstrap'],
function  ($, jsroutes, moment, mustache, template) {

  // In a progress bar, hide glyphs the are wider than the bar
  function showAndHideProgressGlyphs() {
    $(".progress-bar").each(function() {
      var bar = $(this);
      $(this).find(".glyphicon").each(function() {
        var glyph = $(this);
        if (bar.width() < glyph.width()) {
          glyph.hide();
        } else {
          glyph.show();
        }

      });
    });
  }
  $(document).ready(showAndHideProgressGlyphs);
  $(window).resize(showAndHideProgressGlyphs);

  // Show the semi-transparent fade-out effect if
  // the content reaches the maximum allowed height
  $(document).ready($('.read-more').each(function() {
      var height = $(this).parent().css("height");
      var maxheight = $(this).parent().css("max-height");
      if (height === maxheight) {
        $(this).show();
      }
  }));

  function relatedUrlForTemplate(relatedurl, min_confidence) {
      var host = getLocation(relatedurl.url).hostname;

      var style = 'list-style-image: url(//www.google.com/s2/favicons?domain='+host+');';
      if(relatedurl.confidence<min_confidence) {
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
      var relatedurlsForTemplate = relatedurls.map(function(r) { return relatedUrlForTemplate(r, min_confidence); });

      var cVisibleItems = 0;
      for( var i = 0; i < relatedurlsForTemplate.length && cVisibleItems<cNewItems; ++i ) {
        $( mustache.render(template, relatedurlsForTemplate[i]) ).appendTo(element);
        if(min_confidence<=relatedurlsForTemplate[i].relatedurl.confidence) {
          ++cVisibleItems;
        }
      }
      if(relatedurls.length < cLoadItems) {
        button.prop("disabled", true);
      } else if(cVisibleItems < cNewItems) {
        loadMoreRelatedUrls(element, button, stmt_id, cNewItems - cVisibleItems);
      }
    })
  }

  $(document).ready(function() {
    $("[data-fill-relatedurls]").each( function() {
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
  $(document).ready(function() {
    moment.locale('de', {
      relativeTime : {
        future: "in %s",
        past:   "%s",
        s:  "< 1 min",
        m:  "1 min",
        mm: "%d min",
        h:  "1 h",
        hh: "%d h",
        d:  "1 Tag",
        dd: "%d Tage",
        M:  "1 Monat",
        MM: "%d Monate",
        y:  "1 Jahr",
        yy: "%d Jahre"
      }
    })
  });

});
