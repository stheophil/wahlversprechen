/*global define */
define(['jquery', 'app/client', 'mustache'],
  function($, client, mustache) {
    function renderTagTo(template, tag, $tagsList) {
      var data = {
        tag: tag,
        urls: { "view": "", "delete": "" }
      } // TODO add urls
      $(mustache.render(template, data)).appendTo($tagsList);
    }

    function loadTagsListEntryTemplate() {
      return $.get("/assets/template/tagsListEntry.html");
    }

    function loadTags() {
      return client.Tag.loadAll({dataType: 'text'});
    }

    function renderTags(tags, template) {
      var $tagsList = $('#tags-list');
      $tagsList.text("");
      tags.forEach(function(tag) {
        renderTagTo(template, tag, $tagsList);
      });
    }

    function updateTagsVisibility($el) {
      var $list = $('#tags-list');
      var $controls = $('#tags-controls');
      var importantOnly = $controls.find("#tags-important-only").is(':checked');
      var infix = $controls.find("input[type=text]").val()
      var toShow = $list.children().filter(function() {
        return (
          (!importantOnly || $(this).find(":checked").size() > 0)
               && (infix === "" || $(this).find('[data-tag-name*="' + infix + '"]').size() > 0)
        );
      });
      toShow.show();
      $list.children().not(toShow).hide();
      toShow.slice(30).hide();
    }

    function installEventHandler() {
      $('#tags-important-only').change(function() {
        updateTagsVisibility();
      });

      $('#tags-infix').on('input', function() {
        updateTagsVisibility(); // HTML5 only
      });
    }

    // Start loading tags and template ASAP,
    // but only render them when the DOM is ready.
    $.when(loadTagsListEntryTemplate(), loadTags())
     .done(function(templateResponse, tagsResponse) {
       $(function() {
         renderTags(tagsResponse[0], templateResponse[0]);
         updateTagsVisibility();
       });
     });

    $(installEventHandler);
  });