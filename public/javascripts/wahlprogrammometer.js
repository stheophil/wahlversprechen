
$(document).ready(function() {  
	var textexpand = $('textarea.expand');
	if(textexpand) {
		var height = textexpand.height();
		var targetheight = "200px"; // TODO
		var showonclick = textexpand.parents("form").children(".showonclick");

		textexpand.click(function() {
			textexpand.animate({height: targetheight}, 500);
			showonclick.fadeIn()
		});

		var Collapse = function() {
			textexpand.animate({height: height + "px"}, 500);
			showonclick.fadeOut();
		};

		textexpand.focusout(function() {
			if(textexpand.val()=="") {
				Collapse();
			}	
		})

		textexpand.parents("form").children(".cancel").click(function() {
			textexpand.val("");
			Collapse();
		});
	}
});