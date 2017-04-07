/**
 * Created by timdekarz on 28.03.17.
 */

$(document).ready(function() {

	$('select').material_select();

	$('.modal').modal();

	$('#send').click(function (event) {
		var form = $('#secureMail');
		//TODO: check if theres still some invalid fields (prefilled)
		if (form[0].checkValidity()) {
			$('#confirmText').hide();
			$('#pendingForm').show();
			event.preventDefault();
			tinymce.activeEditor.uploadImages(function(success) {
				form[0].submit();
			});
			form[0].submit();
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

	tinymce.init({
		selector: '#wysiwyg',
		language: 'de',
		height : 300,
		theme: 'modern',
		browser_spellcheck: true,
		contextmenu: false,
		plugins: [
			'advlist autolink lists link image imagetools charmap print preview anchor',
			'searchreplace visualblocks code fullscreen',
			'insertdatetime media table contextmenu paste code wordcount'
		],
		paste_data_images: true,
		automatic_uploads: true,
		images_upload_url: '/saveimage',
		images_upload_base_path: '/getimage?image=',
		file_picker_types: 'image',
		file_picker_callback: function(cb, value, meta) {
			var input = document.createElement('input');
			input.setAttribute('type', 'file');
			input.setAttribute('accept', 'image/*');

			// Note: In modern browsers input[type="file"] is functional without
			// even adding it to the DOM, but that might not be the case in some older
			// or quirky browsers like IE, so you might want to add it to the DOM
			// just in case, and visually hide it. And do not forget do remove it
			// once you do not need it anymore.

			input.onchange = function() {
				var file = this.files[0];

				// Note: Now we need to register the blob in TinyMCEs image blob
				// registry. In the next release this part hopefully won't be
				// necessary, as we are looking to handle it internally.
				var id = 'blobid' + (new Date()).getTime();
				var blobCache = tinymce.activeEditor.editorUpload.blobCache;
				var blobInfo = blobCache.create(id, file);
				blobCache.add(blobInfo);

				// call the callback and populate the Title field with the file name
				cb(blobInfo.blobUri(), { title: file.name });
			};

			input.click();
		},
		toolbar: 'undo redo | insert | styleselect | bold italic | alignleft aligncenter alignright alignjustify | ' +
		'bullist numlist outdent indent | link image'
	});
});
