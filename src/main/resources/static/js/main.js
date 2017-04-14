/**
 * Custom JavaScript
 */
$(document).ready(function() {

	//init select fields
	$('select').material_select();

	//init modals
	$('.modal').modal();

	//fix for prefilled fields
	Materialize.updateTextFields();

	//init and configure tinymce
	tinymce.init({
		selector: '#wysiwyg',
		language: 'de', //localization only works if js is present
		//STYLE
		height : 300,
		theme: 'modern',
		browser_spellcheck: true,
		contextmenu: false,
		toolbar: 'undo redo | insert | styleselect | bold italic | alignleft aligncenter alignright alignjustify | ' +
		'bullist numlist outdent indent | link image',
		plugins: [
			'advlist autolink lists link image imagetools charmap print preview anchor',
			'searchreplace visualblocks code fullscreen',
			'insertdatetime media table contextmenu paste code wordcount'
		],
		//IMAGE UPLOAD FUNCTION
		paste_data_images: true,
		automatic_uploads: true,
		images_upload_url: '/image',
		images_upload_base_path: '/image?id=',
		file_picker_types: 'image',
		file_picker_callback: function(cb, value, meta) {
			var input = document.createElement('input');
			input.setAttribute('type', 'file');
			input.setAttribute('accept', 'image/*');
			input.onchange = function() {
				var file = this.files[0];
				var id = 'blobid' + (new Date()).getTime();
				var blobCache = tinymce.activeEditor.editorUpload.blobCache;
				var blobInfo = blobCache.create(id, file);
				blobCache.add(blobInfo);
				cb(blobInfo.blobUri(), { title: file.name });
			};
			input.click();
		}
	});

	//handle submit
	$('#send').click(function (event) {
		var form = $('#secureMail');
		//TODO: check if theres still some invalid fields (prefilled)
		//check if fields are correct
		if (form[0].checkValidity()) {
			$('#confirmText').hide(); //hide modal text
			$('#pendingForm').show(); //show loading bar
			//first upload all remain images
			event.preventDefault();
			tinymce.activeEditor.uploadImages(function(success) {
				form[0].submit();
			});
			form[0].submit(); //finally submit
		} else {
			//theres an error -> close dialog and go back to form
			$('#confirm').modal('close');
			$('#confirmText').show();
			$('#pendingForm').hide();
		}
	});

	//revalidate all fields if prefilled
	$('#secureMail').find(':input').each(function () {
		if($(this).val() != "") {
			if($(this)[0].checkValidity()) {
				$(this).addClass("valid");
			} else {
				$(this).addClass("invalid");
			}
		}
	});

	//apply errors to all fields according to error list
	$('.error').each(function () {
		var fieldName = $(this).attr("data-field");
		$('#' + fieldName).addClass("invalid").removeClass("valid");
	});
	if ($('.error')[0]){
		//scroll the window to the first element in the DOM that has a class name ending with "EditError"
		//TODO: fix this
		jQuery(window).scrollTop(jQuery('[class$="invalid"]:first').position().top);
	}
});
