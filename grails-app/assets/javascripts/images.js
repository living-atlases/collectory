/**
 * Populate Images tab body with images via AJAX call to Biocache
 */
function loadImagesTab() {
    var wsBase = "/occurrences/search.json";
    var uiBase = "/occurrences/search";
    var imagesQueryUrl = "?facets=type_status&fq=multimedia%3AImage&pageSize=100&q=" + (SHOW_REC.isPipelinesCompatible? "collectionUid:" : "collection_uid:") + SHOW_REC.instanceUuid;

    $.ajax({
        url: SHOW_REC.biocacheServicesUrl + wsBase + imagesQueryUrl,
        dataType: 'jsonp',
        timeout: 20000,
        complete: function(jqXHR, textStatus) {
            if (textStatus == 'timeout') {
                noBiocacheData();
            }
            if (textStatus == 'error') {
                noBiocacheData();
            }
        },
        success: function(data) {
            // check for errors
            if (data.length == 0 || data.totalRecords == undefined || data.totalRecords == 0) {
                //noBiocacheData();
            } else {
                if(data.totalRecords > 0){
                    $('#imagesTabEl').css({display:'block'});
                    var description = ""
                    $('#imagesSpiel').html('<p><a href="' + SHOW_REC.biocacheWebappUrl + uiBase + imagesQueryUrl +'">' + jQuery.i18n.prop("images.available.count", data.totalRecords) + '</a> '
                    + jQuery.i18n.prop("images.available.count.available", SHOW_REC.instanceName) + '<br/> ' + description + '</p>');
                    $.each(data.occurrences, function(idx, item){
                        var imageText = item.scientificName;
                        if(item.typeStatus !== undefined){
                            imageText = item.typeStatus + " - " + imageText;
                        }
                        $('#imagesList').append('<div class="imgCon"><a href="' + SHOW_REC.biocacheWebappUrl + '/occurrences/' + item.uuid + '"><img src="' + item.smallImageUrl + '"/><br/>'+ imageText + '</a></div>');
                    })
                }
            }
        }
    });
}