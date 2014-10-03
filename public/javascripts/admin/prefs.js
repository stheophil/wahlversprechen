/*global define */
define(['jquery', 'app/client', 'mustache', 'routes', 'app/editing'],
    function($, client, mustache, jsroutes, editing) {
        function renderTagTo(template, tag, $tagsList) {
            var data = {
                tag: tag,
                urls: {
                    "delete": jsroutes.controllers.Admin.deleteTag(tag.id).url,
                    "view": jsroutes.controllers.Application.tag(tag.name).url
                }
            }
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

            $('input.setImportantTag').change(function(){
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
                }); // End Ajax
            }); // End onclick

            var $list = $('#tags-list');
            $list.find(".tag-edit").popover({
                html: true,
                placement: "top",
                container: "body",
                content: function() {
                    var id = $(this).data('tag-id')
                    var name = $(this).data('tag-name')
                    return '<form id="formEditTag'+id+'" class="ajax-submit" data-method="PUT" data-url="/admin/tag/'+id+'" data-action="reload" data-alert-id="alertEditTag'+id+'" role="form">' +
                        '<div class="form-group">' +
                        '<input type="text" name="name" class="form-control input-sm" size="25" value="'+name+'">' +
                        '</div>' +
                        '<button type="submit" class="btn-primary btn btn-sm">OK</button>' +
                        '</form>';
                }
            }).click(function(e) {
                e.preventDefault()
            })

            $list.find('.tag-edit').on('shown.bs.popover', function () {
                var id = $(this).data('tag-id')
                editing().form_ajax_submit($("#formEditTag" + id))
            })

            editing().attachDeleteHandler($list);
        }

        // Start loading tags and template ASAP,
        // but only render them when the DOM is ready.
        $.when(loadTagsListEntryTemplate(), loadTags())
         .done(function(templateResponse, tagsResponse) {
             $(function() {
                 renderTags(tagsResponse[0], templateResponse[0]);
                 updateTagsVisibility();
                 installEventHandler();
             });
         });
    });