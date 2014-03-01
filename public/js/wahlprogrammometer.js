function form_ajax_submit(form) {
    var submit = form.find("button[type='submit']")
    if(0==submit.length) {
    	var id = form.attr("id")
    	submit = $("button[type='submit'][form='"+id+"']")
    }

    submit.click(function(e) {             	
        submit.prop("disabled", true);
        var alert = form.attr("data-alert-id")

        $('#'+alert).remove()    

        $.ajax({
            type: form.attr("data-method"),
            url: form.attr("data-url"),
            data: form.serialize()
        })
        .done(function(data, textStatus, jqXHR) {
        	if(form.attr("data-action")=="reload") {
        		location.reload(true)
        	} else if(form.attr("data-action")=="message") {
        		if(0<data.length) {
        			$('<div class="alert alert-success" id='+alert+'>' + data + '</div>').insertAfter(form);
        		}
            }
        })
        .fail(function(jqXHR, textStatus, errorThrown) {
            $('<div class="alert alert-danger" id='+alert+'>' + jqXHR.responseText + '</div>').insertAfter(form);
        })
        .always(function() { 
            submit.prop("disabled", false); 
        }); // End Ajax  
         
        e.preventDefault()
    })
}

function showAndHideProgressGlyphs() {
    $(".progress-bar").each( function() {
        var bar = $(this)
        $(this).find(".glyphicon").each( function() {
            var glyph = $(this)
            if(bar.width() < glyph.width()) {
                glyph.hide()
            } else {
                glyph.show()
            }
        })
    })
}

$(document).ready(showAndHideProgressGlyphs)
$(window).resize(showAndHideProgressGlyphs)

$(document).ready(function() {  
	$('textarea.expand').each( function( i ) {		
		var height = $(this).height();
		var targetheight = "200px"; // TODO
		var showonclick = $(this).parents("form").children(".showonclick");

		$(this).click(function() {
			$(this).animate({height: targetheight}, 500);
			showonclick.fadeIn()
		});

		var Collapse = function() {
			$(this).animate({height: height + "px"}, 500);
			showonclick.fadeOut();
		};

		$(this).focusout(function() {
			if($(this).val()=="") {
				Collapse();
			}	
		})

		$(this).parents("form").children(".cancel").click(function() {
			$(this).val("");
			Collapse();
		});
	});

	$('.read-more').each( function(i) {
		var height = $(this).parent().css("height");
		var maxheight = $(this).parent().css("max-height");
		if(height==maxheight) {
			$(this).show();
		}
	});
          
    $('.ajax-update').each(function () {
    	// Store the current value on focus and on change        	
    	var element = $( this );
        var elementDOM = this
        var bSelect = this.nodeName.toLowerCase()=="select"        
    	var method = element.attr("method")
    	if(!method) {
    		method = "PUT"
    	}

    	var prevValue = bSelect ? element.val() : elementDOM.outerText;
        var errorHandler = bSelect 
            ? function() { 
                element.val( prevValue ); 
            }
            : function() {
                $('<div class="alert alert-danger">Speichern fehlgeschlagen!</div>')
                    .insertAfter(element)
                    .delay(1000)
                    .fadeOut()
            }

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
        }

        if(bSelect) {
            element.on("change input", saveHandler)     
        } else {
            element.attr("contenteditable", true)

            $('<button type="button" class="btn btn-default btn-sm">Abbrechen</button>')
                .insertAfter(element)
                .click(function() {
                    element.nextAll("button").fadeOut()
                    element.text(prevValue)
                })
                .hide(); 

            $('<br/><button type="button" class="btn btn-primary btn-sm">Speichern</button>&nbsp;')
                .insertAfter(element)
                .click(saveHandler)
                .hide();

            element.focusin( function() { 
                element.nextAll("button").fadeIn()
            })

            element.blur( function() { 
                element.nextAll("button").fadeOut()
            })
        }
	  })

	  $('.ajax-delete').click(function (e) {     	
    	var element = $( this );
    	var target = element.attr("target-url")

        $.ajax({
            type: 'DELETE',
            url: element.attr("url"),
            data: {},
            datatype: 'text',
            cache: 'false',
            success: function() {
            	if(target) {
            		location.href = target
            	} else {
            		location.reload()
            	}            	
            },
            error: function(){

            }
        }); // End Ajax  
        e.preventDefault()
	  })

      $("form.ajax-submit").each(function() {
      	form_ajax_submit($(this))
  	   })
});