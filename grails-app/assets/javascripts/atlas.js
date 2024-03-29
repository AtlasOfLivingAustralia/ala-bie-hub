
$(document).ready(function() {
	// Sticky footer
	var footerHeight = $(".site-footer").outerHeight();
	var imageId, attribution, recordUrl;
	$(".wrap").css("margin-bottom", -footerHeight);
	$(".push").height(footerHeight);

	// Tabs init
	var hash = window.location.hash;
	hash && $(".taxon-tabs a[href='" + hash + "']").tab("show");
	$(".taxon-tabs a").click(function (e) {
		window.location.hash = this.hash;
	});

	// Links to tabs
	$(".tab-link").click(function (e) {
		e.preventDefault();
		window.location.hash = this.hash;
		var tabID = $(this).attr("href");
		$(".taxon-tabs a[href='" + tabID + "']").tab("show");
	})

	// Lightbox
	$(document).delegate('*[data-toggle="lightbox"]', 'click', function(event) {
		event.preventDefault();
		switch (SHOW_CONF.imageDialog){
			case 'MODAL':
				$(this).ekkoLightbox();
				break;
			case 'LEAFLET':
				imageId = $(this).parent().attr('data-image-id');
				if (imageId == undefined) {
					imageId = $(this).attr('data-image-id');
					attribution = $(this).attr('data-footer');
					recordUrl = $(this).attr('data-record-url');
				} else {
					attribution = $(this).parent().attr('data-footer');
					recordUrl = $(this).parent().attr('data-record-url');
				}
				setDialogSize();
				$('#imageDialog').modal('show');
				break;
		}
	});

	// show image only after modal dialog is shown. otherwise, image position will be off the viewing area.
	$('#imageDialog').on('shown.bs.modal',function () {
		imgvwr.viewImage($("#viewerContainerId"), imageId, SHOW_CONF.scientificName, SHOW_CONF.guid, {
			imageServiceBaseUrl: SHOW_CONF.imageServiceBaseUrl,
			addSubImageToggle: false,
			addCalibration: false,
			addDrawer: false,
			addCloseButton: true,
			addAttribution: true,
			addLikeDislikeButton: false,
			addPreferenceButton: SHOW_CONF.addPreferenceButton,
			attribution: attribution,
			disableLikeDislikeButton: SHOW_CONF.disableLikeDislikeButton,
			likeUrl: SHOW_CONF.likeUrl + '?id=' + imageId,
			dislikeUrl: SHOW_CONF.dislikeUrl + '?id=' + imageId,
			userRatingUrl: SHOW_CONF.userRatingUrl + '?id=' + imageId,
			userRatingHelpText: SHOW_CONF.userRatingHelpText.replace('RECORD_URL', recordUrl),
			savePreferredSpeciesListUrl: SHOW_CONF.savePreferredSpeciesListUrl + '?id=' + imageId + '&scientificName=' + SHOW_CONF.scientificName + '&family=' + SHOW_CONF.family,
			getPreferredSpeciesListUrl: SHOW_CONF.getPreferredSpeciesListUrl,
			druid: SHOW_CONF.druid
		});
	});


	// set size of modal dialog during a resize
	$(window).on('resize', setDialogSize)
	function setDialogSize() {
		var height = $(window).height()
		height *= 0.8
		$("#viewerContainerId").height(height);
	}

	// Tooltips
    $("[data-toggle='tooltip']").tooltip();


	// Search: Refine results accordions
	$(".refine-box h2 a").click(function() {
		$(this).children(".glyphicon").toggleClass("glyphicon-chevron-down glyphicon-chevron-up");
	});
	$("a.expand-options").click(function() {
		$(this).text(function(i, text){
			return text.trim() === "More" ? "Less" : "More";
		})
		$(this).prev(".collapse").collapse("toggle");
	});

	$('#copy-al4r').on('click', function() {
		var input = document.querySelector('#al4rcode');
		if (navigator.clipboard && window.isSecureContext) {
			// navigator clipboard api method'
			navigator.clipboard.writeText(input.value)
				.then(() => {
					$('#copy-al4r').qtip({
						content: jQuery.i18n.prop('list.copylinks.tooltip.copied'),
						show: true,
						hide: { when: { event: 'mouseout'} }
					})})
				.catch((error) => { alert(jQuery.i18n.prop('list.copylinks.alert.failed') + error) })
		} else {
			alert("Copying to clipboard requires a secure HTTPS connection. Value copied to clipboard is: " + input.value);
		}

	});

	$('#copy-al4r').on('mouseleave', function() {
		$('#copy-al4r').qtip({
			content: jQuery.i18n.prop('list.copylinks.tooltip.copytoclipboard'),
			show: {when: {event: 'mouseover'}},
			style: {
				classes: 'ui-tooltip-rounded ui-tooltip-shadow'
			},
			position: {
				target: 'mouse',
				my: 'bottom center',
				adjust: {x: -6, y: -10}
			}
		})
	})
});
