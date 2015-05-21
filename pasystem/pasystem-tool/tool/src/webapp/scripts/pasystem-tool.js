$(function () {

    var initDatePickers = function () {
        $('.pasystem-body .datepicker').each(function() {
            var $datepicker = $(this);

            var initDate = $(this).data('initDate');

            // setup date-time picker
            localDatePicker({
                input: '#' + $datepicker.attr('id'),
                useTime: 1,
                icon: 0,
                val: (initDate && initDate > 0) ? new Date(initDate) : undefined,
                ashidden: { iso8601: $datepicker.attr('id') + '_selected_datetime' },
            });

            // setup input button to trigger date-time picker
            $datepicker.siblings().find(".invoke-datepicker-btn").click(function() {
                $datepicker.focus();
            });

            // add clear action if present
            $datepicker.siblings().find(".clear-datepicker-btn").click(function() {
              $datepicker.val("");
              $("[name='" + $datepicker.attr('id') + "_selected_datetime']").val("");
            });
        });
    };


    var initDeleteConfirmation = function() {
      $(".pasystem-delete-btn").on("click", function(event) {
        event.preventDefault();
        event.stopPropagation();

        var template = $("#pasystemDeleteConfirmationModalTemplate").html().trim().toString();
        var trimPathTemplate = TrimPath.parseTemplate(template, "pasystemDeleteConfirmationModalTemplate");

        var $modal = $(trimPathTemplate.process({
                      recordType: $(this).data("record-type"),
                      deleteURL: $(this).prop("href")
                     }));

        $(document.body).append($modal);

        $modal.modal();
      });
    };


    var addFormHandlers = function () {
        var openCampaignRadio = $('#open-campaign-radio');

        if (openCampaignRadio.length == 0) {
            return;
        }

        var distribution = $('#distribution');

        $('.campaign-visibility').on('change', function () {
            if ($(this).attr('id') == openCampaignRadio.attr('id')) {
                distribution.prop('disabled', true);
            } else {
                distribution.prop('disabled', false);
            }
        });
    };


    initDatePickers();
    initDeleteConfirmation();
    addFormHandlers();

});
