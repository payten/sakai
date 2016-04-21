/**************************************************************************************
 *                    Gradebook Sorter Javascript                                      
 *************************************************************************************/
function GradebookSorter($container) {
  this.$container = $container;

  if ($container.hasClass("by-category")) {
    this.setupByCategorySorting();
  } else if ($container.hasClass("by-grade-item")) {
    this.setupByGradeItemSorting();
  }
};

GradebookSorter.prototype.setupByCategorySorting = function() {
  var self = this;

  $(".gb-sorter-category ul", this.$container).each(function() {
    $(this).sortable({
      axis: "y",
      placeholder: "gb-sorter-placeholder",
      forcePlaceholderSize: true,
      stop: self.updateHiddenInputValues
    });
  });
};

GradebookSorter.prototype.setupByGradeItemSorting = function() {
  var self = this;

  $("ul", this.$container).each(function() {
    $(this).sortable({
      axis: "y",
      placeholder: "gb-sorter-placeholder",
      forcePlaceholderSize: true,
      stop: self.updateHiddenInputValues
    });
  });
};

GradebookSorter.prototype.updateHiddenInputValues = function(event, ui) {
  var $ul = $(ui.item).closest("ul");

  $ul.find("li").each(function(i, li) {
    var $li = $(li);
    $li.find(":input[name$='[order]']").val(i);
  });
};
