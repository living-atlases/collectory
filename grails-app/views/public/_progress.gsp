<asset:script type="text/javascript">

  var queryUrl = CHARTS_CONFIG.biocacheServicesUrl + "/occurrences/search?pageSize=0&q=${facet}:${instance.uid}";
  $.ajax({
    url: queryUrl,
    dataType: 'json',
    timeout: 30000,
    complete: function(jqXHR, textStatus) {
    },
    success: function(data) {
       setPercentAgeNumbers(data.totalRecords, ${instance.numRecords});
    }
  });

/************************************************************\
 * Set total and percent biocache record numbers
 \************************************************************/
function setPercentAgeNumbers(totalBiocacheRecords, totalRecords) {
    var recordsClause = "";
    switch (totalBiocacheRecords) {
        case 0: recordsClause = "No records"; break;
        case 1: recordsClause = "1 record"; break;
        default: recordsClause = addCommas(totalBiocacheRecords) + " records";
    }
    $('#numBiocacheRecords').html(recordsClause);

    if (totalRecords > 0) {

        var percent = totalBiocacheRecords/totalRecords * 100;
        if (percent > 100 && false) {
            // don't show greater than 100 if the mapping is not exact as the estimated num records may be correct
            percent = 100;
        }
        setProgress(percent);
    } else {
        // the progress bar doesn't make sense if there is no estimated speciemens count
        $('#progressBarItem').hide();

        // change the display text for the lack of metadata
        $('#speedoCaption').hide();
        $('#speedoCaptionMissingMetadata').show();
    }
}

/************************************************************\
 * Draw % digitised bar (progress bar)
 \************************************************************/

function setProgress(percentage){
    var captionText = "";
    var noun = jQuery.i18n.prop('public.show.setprogress.specimens');
    if (noun == null)
        noun = 'specimens';
    if (percentage == 0) {
        captionText = jQuery.i18n.prop('public.show.setprogress.02', orgNameLong);
    } else {
        var displayPercent = percentage.toFixed(1);
        if (percentage < 0.1) {displayPercent = percentage.toFixed(2)}
        if (percentage > 20) {displayPercent = percentage.toFixed(0)}
        if (percentage > 100) {displayPercent = jQuery.i18n.prop('public.show.percent.over')}
        captionText = jQuery.i18n.prop('public.show.percentrecords.01', displayPercent, noun, orgNameLong);
    }
    $('#speedoCaption').html(captionText);
    if (percentage > 100) {
        $('#progressBar').removeClass('percentImage1');
        $('#progressBar').addClass('percentImage4');
        percentage = 101;
    }
    //var newProgress = eval(initial)+eval(percentageWidth)+'px';
    $('#progressBar').css('width', percentage +'%');
}

</asset:script>
