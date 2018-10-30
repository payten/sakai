$(function() {
    // Setup jQueryUI sortable lists
    $( ".col1 ul, .col2 ul" ).sortable({
        connectWith: ".lessonsreorderlist",
        tolerance: "pointer",
        placeholder: "layoutReorderer-dropMarker",
    }).disableSelection();
});

var recalculate = function(){
    var keepList = '';
    var removeList = '';
    jQuery('.col1 .layoutReorderer-module').each(function(i){
        i > 0 ? keepList = keepList + ' ' + $(this).find('.reorderSeq').text() : keepList = $(this).find('.reorderSeq').text();
    });
    jQuery('.col2 .layoutReorderer-module').each(function(i){
        i > 0 ? removeList = removeList + ' ' + $(this).find('.reorderSeq').text() : removeList = $(this).find('.reorderSeq').text();
    });
    
    keepList=keepList + ' --- ';
    keepList=keepList.replace('  ',' ');
    removeList=removeList.replace('  ',' ');
    jQuery('input[id=order]').val(keepList + removeList);
    
    if (jQuery('.col2 .layoutReorderer-module').length===0){
        jQuery('.col2 #deleteListHead').attr('class','deleteListMessageEmpty');
    }
    else {
        jQuery('.col2 #deleteListHead').attr('class','deleteListMessage');
    }
    $('.layoutReorderer-module').find('.marker').closest('.layoutReorderer-module').remove();
};


$(document).ready(function(){
    $('.layoutReorderer-module').find('.marker').closest('.layoutReorderer-module').remove();
    recalculate();

/*
    jQuery('.col1 .layoutReorderer-module').each(function(i){
        i > 0 ? ids = ids + ' ' + $(this).find('.reorderSeq').text() : ids = $(this).find('.reorderSeq').text();
    });
    
    ids=ids + ' --- '
    ids=ids.replace('  ',' ')
    jQuery('input[id=order]').val(ids);

*/    
    $('#save').click(function(e){
	    recalculate();
	    return true;
	});

    $('.deleteAnswerLink').click(function(e){
        e.preventDefault();
        $(this).closest('.layoutReorderer-module').addClass('highlightEl').appendTo('#reorderCol2 ul').removeClass('highlightEl', {duration:1000});
        
        recalculate();
    });
});
