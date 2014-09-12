function form_ajax_submit(form) {
  var submit = form.find("button[type='submit']");
  if (0 === submit.length) {
    var id = form.attr("id");
    submit = $("button[type='submit'][form='" + id + "']");
  }

  submit.click(function(e) {
    submit.prop("disabled", true);
    var alert = form.data("alert-id");

    $('#' + alert).remove();

    $.ajax({
      type: form.data("method"),
      url: form.data("url"),
      data: form.serialize()
    })
      .done(function(data, textStatus, jqXHR) {
        if (form.data("action") == "reload") {
          location.reload(true);
        } else if (form.data("action") == "message") {
          if (0 < data.length) {
            $('<div class="alert alert-success" id=' + alert + '>' + data + '</div>').insertAfter(form);
          }
        }
      })
      .fail(function(jqXHR, textStatus, errorThrown) {
        $('<div class="alert alert-danger" id=' + alert + '>' + jqXHR.responseText + '</div>').insertAfter(form);
      })
      .always(function() {
        submit.prop("disabled", false);
      }); // End Ajax

    e.preventDefault();
  });
}

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

function showErrorMessageForOneSecondAfterElement(element) {
  var errorMessage = "Speichern fehlgeschlagen!";
  $('<div class="alert alert-danger">' + errorMessage + '</div>')
    .insertAfter(element)
    .delay(1000)
    .fadeOut();
}

function handleEntryUpdateCancel(entryId) {
  var $renderedEntry = $(".entry-content[data-id=" + entryId + "]");
  var $textarea = $renderedEntry.prevAll('.form-group')
                                .children('textarea');

  // Restore original text
  var originalText = $renderedEntry.data("original")
  var originalHtml = marked(originalText);
  $renderedEntry.html(originalHtml);

  // Remove notice
  $notice = $renderedEntry.prevAll(".alert-warning");
  fadeOutAndRemove($notice);

  // Remove textarea
  fadeOutAndRemove($textarea.parent());

  // Remove buttons
  fadeOutAndRemove($renderedEntry.next(".form-group"));

  // Hide delete link
  $deleteLink = $renderedEntry.prevAll('.entry-buttons')
                              .children('.ajax-delete');
  $deleteLink.fadeOut();

  // Show edit link again
  $editLink = $deleteLink.siblings('.ajax-edit');
  $editLink.fadeIn();
}

// Update the entry with the given id an reload the page on success
function handleEntryUpdateById(entryId) {
  var $textarea = $("textarea").filter(
    function() { return $(this).data("id") === entryId }
  );

  var data = {};
  var updatedText = $textarea.val();
  data.content = updatedText;

  var url = "/entry/" + entryId.toString();

  $.ajax({
    type: "PUT",
    url: url,
    data: data,
    datatype: 'text',
    cache: 'false',
    success: function() {
      location.href += "#" + entryId;
      location.reload();
    },
    error: function() {
      var $renderedEntry = $(".entry-content[data-id=" + entryId + "]");
      showErrorMessageForOneSecondAfterElement($renderedEntry);
    }
  }); // End Ajax
}

// Helper to fadeOut an element and remove it afterwards
function fadeOutAndRemove(element) {
  element.fadeOut(function() {
    $(this).remove();
  });
}

// Helper to enable edit mode with received markdown from server
function createTextareaWithButtons(entryId, originalText) {
  var $renderedEntry = $(".entry-content[data-id=" + entryId + "]");

  // Store original text for recovery when cancelling
  $renderedEntry.data("original", originalText)

  // Hidden form-group blueprint
  var $formGroup = $("<div />").addClass("form-group")
                               .hide();

  // Create textarea
  var $textarea = $("<textarea />").data("id", entryId)
                                   .attr("rows", 15)
                                   .addClass("form-control")
                                   .addClass("live-markdown")
                                   .text(originalText);

  // Add textarea to site
  $formGroup.clone()
            .append($textarea)
            .insertBefore($renderedEntry)
            .fadeIn();

  // Small button blueprint
  var $buttonSmall = $('<button />').addClass('btn')
                                    .addClass('btn-sm')
                                    .attr('type', 'button')

  // Create save button
  var $buttonSave = $buttonSmall.clone()
                                .addClass('btn-primary')
                                .addClass('pull-right')
                                .text('Speichern')
                                .click(function(e) {
                                  e.preventDefault();
                                  handleEntryUpdateById(entryId);
                                });

  // Create cancel button
  var $buttonCancel = $buttonSmall.clone()
                                  .addClass('btn-default')
                                  .text('Abbrechen')
                                  .click(function(e) {
                                    e.preventDefault();
                                    handleEntryUpdateCancel(entryId);
                                  })

  // Add buttons to site including an hr
  $formGroup.clone()
            .append($buttonCancel)
            .append($buttonSave)
            .append('<hr />') // just looks a little better
            .insertAfter($renderedEntry)
            .fadeIn();

  // Create a small notice to make things more clear
  var noticeText = 'Vorschau! Der Eintrag wird erst beim Speichern wirklich geändert.'
  var $notice = $('<p />').addClass('alert')
                          .addClass('alert-warning')
                          .text(noticeText)
                          .hide()
                          .fadeIn();

  // Add notice to site
  $notice.insertBefore($renderedEntry);
}

$(document).ready(showAndHideProgressGlyphs);
$(window).resize(showAndHideProgressGlyphs);

$(document).ready(function() {
  $('textarea.expand').each(function(i) {
    var height = $(this).height();
    var targetheight = "200px"; // TODO
    var showonclick = $(this).parents("form").find(".showonclick");

    $(this).click(function() {
      $(this).animate({
        height: targetheight
      }, 500);
      showonclick.fadeIn();
    });

    var Collapse = function() {
      $(this).animate({
        height: height + "px"
      }, 500);
      showonclick.fadeOut();
    };

    $(this).focusout(function() {
      if ($(this).val() === "") {
        Collapse();
      }
    });

    $(this).parents("form").children(".cancel").click(function() {
      $(this).val("");
      Collapse();
    });
  });

  $('.read-more').each(function(i) {
    var height = $(this).parent().css("height");
    var maxheight = $(this).parent().css("max-height");
    if (height == maxheight) {
      $(this).show();
    }
  });

  // TODO: Cleanup
  $('.ajax-update').each(function() {
    // Store the current value on focus and on change
    var element = $(this);
    var elementDOM = this;
    var bSelect = this.nodeName.toLowerCase() == "select";
    var method = element.attr("method");
    if (!method) {
      method = "PUT";
    }

    var prevValue = bSelect ? element.val() : elementDOM.outerText;
    var errorHandler = bSelect ? function() {
      element.val(prevValue);
    } : function() {
      showErrorMessageForOneSecondAfterElement(element);
    };

    var saveHandler = function() { 
      var value = bSelect ? element.val() : elementDOM.outerText;

      var data = {};
      data[element.attr("name")] = value;

      $.ajax({
        type: method,
        url: element.attr("url"),
        data: data,
        datatype: 'text',
        cache: 'false',
        success: function() {
          prevValue = value;
        },
        error: errorHandler
      }); // End Ajax
    };

    if (bSelect) {
      element.on("change input", saveHandler);
    } else { // TODO: Check if needed anywhere else, if not remove this whole else block
      element.attr("contenteditable", true);

      $('<button type="button" class="btn btn-default btn-sm">Abbrechen</button>')
        .insertAfter(element)
        .click(function() {
          element.nextAll("button").fadeOut();
          element.text(prevValue);
        })
        .hide();

      $('<br/><button type="button" class="btn btn-primary btn-sm">Speichern</button>&nbsp;')
        .insertAfter(element)
        .click(saveHandler)
        .hide();

      element.focusin(function() { 
        element.nextAll("button").fadeIn();
      });

      element.blur(function() { 
        element.nextAll("button").fadeOut();
      });
    }
  });

  // Listens for edit clicks and gets content from server
  $(".ajax-edit").click(function(e) {
    e.preventDefault();

    $editLink = $(this);
    $deleteLink = $editLink.siblings(".ajax-delete");

    var id = $(this).data("id");
    var url = "/entry/" + id.toString();

    $.ajax({
      type: "GET",
      url: url,
      datatype: "text",
      cache: "false",
      success: function(apiResult) {
        $editLink.fadeOut();
        createTextareaWithButtons(id, apiResult);
        $deleteLink.fadeIn();
      }
    });
  });

  // Listens for changes and renders them after every keystroke
  $(document).on("keyup", "textarea.live-markdown", function(e) {
    var entryId = $(this).data("id");
    var $renderedEntry = $(".entry-content[data-id=" + entryId + "]");

    var changedText = $(this).val();
    var newHtml = marked(changedText);
    $renderedEntry.html(newHtml);
  });

  $('.ajax-delete').click(function(e) {
    e.preventDefault();
    
    var really = confirm("Willst du diesen Eintrag wirklich löschen?");

    if (really) {
      var element = $(this);
      var target = element.attr("target-url");

      $.ajax({
        type: 'DELETE',
        url: element.attr("url"),
        data: {},
        datatype: 'text',
        cache: 'false',
        success: function() {
          if (target) {
            location.href = target;
          } else {
            location.reload();
          }
        },
        error: function() {

        }
      }); // End Ajax
    }
  });

  $("form.ajax-submit").each(function() {
    form_ajax_submit($(this));
  });

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
    if (dataset == "tags") {
      window.location = "/tag/" + suggestion.name;
    } else if (dataset == "categories") {
      window.location = "/category/" + suggestion.name;
    }
  });

  $('.typeahead').on("change", function(e) {
    window.location = "/search?query=" + e.target.value;
  });
});
