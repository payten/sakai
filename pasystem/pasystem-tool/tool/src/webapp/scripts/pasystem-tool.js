$(function () {

    $('.pasystem-body .datepicker').each(function() {
        localDatePicker({
            input: '#' + $(this).attr('id'),
            useTime: 1
        });
    });

});
