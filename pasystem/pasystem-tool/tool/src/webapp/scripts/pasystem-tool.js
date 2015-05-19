$(function () {

    $('.pasystem-body .datepicker').each(function() {
        var $datepicker = $(this);

        // setup date-time picker
        localDatePicker({
            input: '#' + $datepicker.attr('id'),
            useTime: 1,
            icon: 0
        });

        // setup input button to trigger date-time picker
        $datepicker.siblings().find("button").click(function() {
          $datepicker.focus();
        });
    });

});
