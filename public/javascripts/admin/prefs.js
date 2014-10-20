"use strict";

define(['jquery', 'app/client', 'mustache', 'routes', 'app/editing', 'levenshtein', 'bootstrap'],
    function($, client, mustache, jsroutes, editing, levenshtein) {
        function renderTag(template, tag) {
            tag.lower = tag.name.toLowerCase();
            var data = {
                tag: tag,
                urls: {
                    "delete": jsroutes.controllers.Admin.deleteTag(tag.id).url,
                    "view": jsroutes.controllers.Application.tag(tag.name).url
                }
            };
            return $(mustache.render(template, data));
        }

        function loadTagsListEntryTemplate() {
            return $.get("/assets/template/tagsListEntry.html");
        }

        function loadTags() {
            return client.Tag.loadAll({dataType: 'text'});
        }

        function renderTags(tags, template) {
            $('#tags-list')
                .text("")
                .append(tags.map(function(tag) {
                    return $('<div></div>').append(renderTag(template, tag));
                }));
        }

        function updateTagsVisibility($el) {
            var $list = $('#tags-list');
            var $controls = $('#tags-controls');
            var importantOnly = $controls.find("#tags-important-only").is(':checked');
            var infix = $controls.find("input[type=text]").val().toLowerCase();
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

        function addDelegatingEventHandler() {
            $('#tags-important-only').on('change', function() {
                updateTagsVisibility();
            });

            $('#tags-infix').on('input', function() {
                updateTagsVisibility();
            });

            $('input.setImportantTag').on('change', function(){
                var $input = $( this );
                var $checked = $input.is( ":checked" );
                var $id = $input.attr("id");

                $.ajax({
                    type: 'PUT',
                    url: '/admin/tag/' + $id,
                    data: {  important: $checked },
                    datatype: 'text',
                    cache: 'false',
                    error: function(){
                        $input.prop( "checked", !$checked );
                    }
                });
            });
        }

        function addDirectTagEventHandlers($root) {
            $root.find(".tag-edit").popover({
                html: true,
                placement: "top",
                container: "body",
                content: function() {
                    var id = $(this).data('tag-id');
                    var name = $(this).data('tag-name');
                    return '<form id="formEditTag'+id+'" class="ajax-submit" data-method="PUT" data-url="/admin/tag/'+id+'" data-action="reload" data-alert-id="alertEditTag'+id+'" role="form">' +
                        '<div class="form-group">' +
                        '<input type="text" name="name" class="form-control input-sm" size="25" value="'+name+'">' +
                        '</div>' +
                        '<button type="submit" class="btn-primary btn btn-sm">OK</button>' +
                        '</form>';
                }
            }).click(function(e) {
                e.preventDefault();
            });

            $root.find('.tag-edit').on('shown.bs.popover', function () {
                var id = $(this).data('tag-id');
                editing().form_ajax_submit($("#formEditTag" + id));
            });

            editing().attachDeleteHandler($root);
        }

        function removeDirectTagEventHandlers(root) {
            root
                .find('.tag-edit')
                .off('click')
                .off('shown.bs.popover')
                .popover('destroy');
            editing().removeDeleteHandler(root);
        }

        function findSimilarTags(tags, maxLevenshteinDistance) {
            function isSimilar(distance) { return distance <= maxLevenshteinDistance; }

            return tags.map(function(tag, idx) {
                var similarTagsWithMetadata = (tags
                ).slice(idx + 1
                ).filter(function(other) {
                    // Do some inexpensive pre-filtering, to avoid calculating
                    // the LD for obviously uninteresting pairs.
                    return Math.abs(tag.name.length - other.name.length) <= maxLevenshteinDistance;
                }).map(function(other) {
                    return [levenshtein.get(tag.name, other.name), other];
                }).filter(function(it) {
                    return isSimilar(it[0]);
                });
                var bestSimilarity = Math.min.apply(
                    [],
                    similarTagsWithMetadata.map(function(it) {
                        return it[0];
                    })
                );
                var similarTags = similarTagsWithMetadata.map(function(it) {
                    return it[1];
                });

                return [tag, bestSimilarity, similarTags];
            }).filter(function(it) {
                return isSimilar(it[1]);
            });
        }

        function installTagDuplicatesEventHandler(template, tags) {
            $('#tags-duplicates').find("form").submit(function(e) {
                e.preventDefault();
                var $message = $('#tags-duplicates-message');
                var $list = $('#tags-duplicates-list');
                var maxLevenshteinDistance = Number($('#tags-duplicates-max-levenshtein-distance').val());
                if (maxLevenshteinDistance < 0 || maxLevenshteinDistance > 5) {
                    $message.text(
                        'Ungültiger Wert: Muss Null oder größer und kleiner als 6 sein.');
                    $message.removeClass("alert-info").addClass("alert-danger");
                    $message.show();
                    $list.hide();
                    return;
                }

                var groups = findSimilarTags(tags, maxLevenshteinDistance);
                $message.text(
                    'Habe ' + groups.length + ' Gruppen an ähnlichen Tags gefunden.');
                $message.removeClass("alert-danger").addClass("alert-info");
                $list.hide();
                removeDirectTagEventHandlers($list);
                $list.html("");
                groups.sort(function(left, right) {
                    return right[1] - left[1];
                });
                $list.append(groups.map(function(group) {
                    return $("<div></div>")
                               .append(renderTag(template, group[0]))
                               .append($('<span class="inbetween-text-for-list"> ähnelt: </span>'))
                               .append(group[2].reduce(function(data, similar, idx) {
                                   if (idx > 0) {
                                       data.push($('<span class="inbetween-text-for-list"> und </span>'));
                                   }
                                   data.push(renderTag(template, similar));
                                   return data;
                               }, []));
                }));
                addDirectTagEventHandlers($list);
                $list.show();
            });
        }

    addDelegatingEventHandler();
    var tagsDeferred = loadTags();

        // Start loading tags and template ASAP,
        // but only render them when the DOM is ready.
        $.when(loadTagsListEntryTemplate(), tagsDeferred)
         .done(function(templateResponse, tagsResponse) {
             $(function() {
                 renderTags(tagsResponse[0], templateResponse[0]);
                 updateTagsVisibility();
                 addDirectTagEventHandlers($('#tags-list'));
             });
         });

        // Start loading tags and template ASAP,
        // but only render them when the DOM is ready.
        $.when(loadTagsListEntryTemplate(), tagsDeferred)
         .done(function(templateResponse, tagsResponse) {
             $(function() {
                 installTagDuplicatesEventHandler(
                     templateResponse[0], tagsResponse[0]);
             });
         });
    });
