require.config({
        baseUrl : "/assets/javascripts",
        shim : {
            "jquery" : {
                exports : "$"
            }
            /*,
            "jsRoutes" : {
                exports : "jsRoutes"
            }*/
        },
        paths : {
            "jquery" : "lib/jquery",
            "bootstrap" : "lib/bootstrap",
            "typeahead" : "lib/typeahead",
            "moment"  : "lib/moment-with-langs",
            "marked"  : "lib/marked"
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
require(['jquery', 'typeahead', 'moment', 'editing', 'bootstrap'],
function  ($) {
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
      url: "/json/item/" + stmt_id + "/relatedurls",
      data: data
    }).done(function(relatedurls, textStatus, jqXHR) {
      var cVisibleItems = 0;
      for( i = 0; i < relatedurls.length && cVisibleItems<cNewItems; ++i ) {
        var relatedurl = relatedurls[i];
        var host = getLocation(relatedurl.url).hostname;

        var style = 'list-style-image: url(//www.google.com/s2/favicons?domain='+host+');';
        if(relatedurl.confidence<8) {
          style += " display:none;";
        } else {
          cVisibleItems += 1;
        }

        var time = moment(relatedurl.lastseen);

        $('<li data-confidence="' + relatedurl.confidence + '" style="' + style + '"">' +
          '<a href="' + relatedurl.url + '">' + relatedurl.title + '</a>' +
          '&nbsp;<small>' + time.fromNow() + '</small>' +
          '</li>'
        ).appendTo(element);
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
        url: '/json/tags'
      }
    });

    tags.initialize();

    var categories = new Bloodhound({
      datumTokenizer: Bloodhound.tokenizers.obj.whitespace('name'),
      queryTokenizer: Bloodhound.tokenizers.whitespace,
      prefetch: {
        url: '/json/categories'
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
        window.location = "/tag/" + suggestion.name;
      } else if (dataset === "categories") {
        window.location = "/category/" + suggestion.name;
      }
    });

    $('.typeahead').on("change", function(e) {
      window.location = "/search?query=" + e.target.value;
    });
  }
  
  // Setup consistent time rendering
  $(document).ready(function() {
    moment.lang("de", {
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

