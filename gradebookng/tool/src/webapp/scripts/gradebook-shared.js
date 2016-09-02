/**************************************************************************************
 *                    Gradebook Shared Javascript                                      
 *************************************************************************************/

/**************************************************************************************
 * Add global ajax failure handling 
 */
$(function() {
  Wicket.Event.subscribe('/ajax/call/failure', function(jqEvent, attributes, jqXHR, errorThrown, textStatus) {
    var $iframe = $('<iframe class="portletMainIframe" height="475" width="100%" frameborder="0" marginwidth="0" marginheight="0" scrolling="auto" allowfullscreen="allowfullscreen">');
    
    var idMatch = jqXHR.responseText.match(/setMainFrameHeight\(\'([a-zA-Z0-9]+)\'\)/);
    $iframe.attr("id", idMatch[idMatch.length - 1]);
    $iframe.attr("name", $iframe.attr("id"));

    $(".Mrphs-container.Mrphs-sakai-gradebookng").html($iframe);

    setTimeout( function() {
        var doc = $iframe[0].contentWindow.document;
        var $body = $('body', doc);
        $body.html(jqXHR.responseText);
    }, 1 );
  });
});