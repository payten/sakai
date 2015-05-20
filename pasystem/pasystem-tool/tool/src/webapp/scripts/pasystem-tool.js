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
            $datepicker.siblings().find("button").click(function() {
                $datepicker.focus();
            });
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
    addFormHandlers();

});
