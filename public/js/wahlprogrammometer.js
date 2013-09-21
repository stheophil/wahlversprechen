
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
});