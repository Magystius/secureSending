/**
 * Created by timdekarz on 28.03.17.
 */

$(document).ready(function() {

	$('select').material_select();

	$('.modal').modal();

	$('#send').click(function () {
		var form = $('#secureMail');
		//TODO: check if theres still some invalid fields (prefilled)
		if (form[0].checkValidity()) {
			$('#confirmText').hide();
			$('#pendingForm').show();
		} else {
			$('#confirm').modal('close');
			$('#confirmText').show();
			$('#pendingForm').hide();
		}
	});

	$('#secureMail').find(':input').each(function () {
		if($(this).val() != "") {
			if($(this)[0].checkValidity()) {
				$(this).addClass("valid");
			} else {
				$(this).addClass("invalid");
			}
		}
	});

	$('.error').each(function () {
		var fieldName = $(this).attr("data-field");
		$('#' + fieldName).addClass("invalid").removeClass("valid");
	});
	if ($('.error')[0]){
		//scroll the window to the first element in the DOM that has a class name ending with "EditError"
		//TODO: fix this
		jQuery(window).scrollTop(jQuery('[class$="invalid"]:first').position().top);
	}

	Materialize.updateTextFields();


	if($('#sms').attr('checked')) {
		$('#telephone').prop('disabled', false);
	}
	$('#sms').change(
		function(){
			if (this.checked) {
				$('#telephone').prop('disabled', false);
			} else {
				$('#telephone').prop('disabled', true);
			}
		}
	);

	tinymce.init({
		selector: '#wysiwyg'
	});
});
