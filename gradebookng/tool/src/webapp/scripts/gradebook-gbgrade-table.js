GbGradeTable = {};

GbGradeTable.unpack = function (s, rowCount, columnCount) {
  var blob = atob(s);

  // Our result will be an array of Float64Array rows
  var result = [];

  // The byte from our blob we're currently working on
  var readIndex = 0;

  for (var row = 0; row < rowCount; row++) {
    var writeIndex = 0;
    var currentRow = [];

    for (var column = 0; column < columnCount; column++) {
      if (blob[readIndex].charCodeAt() == 127) {
        // This is a sentinel value meaning "null"
        currentRow[writeIndex] = "";
	readIndex += 1;
      } else if (blob[readIndex].charCodeAt() & 128) {
        // If the top bit is set, we're reading a two byte integer
        currentRow[writeIndex] = (((blob[readIndex].charCodeAt() & 63) << 8) | blob[readIndex + 1].charCodeAt());

        // If the second-from-left bit is set, there's a fraction too
        if (blob[readIndex].charCodeAt() & 64) {
	  // third byte is a fraction
	  var fraction = blob[readIndex + 2].charCodeAt();
	  currentRow[writeIndex] += (fraction / Math.pow(10, Math.ceil(Math.log10(fraction))));
	  readIndex += 1;
        }

        readIndex += 2;
      } else {
        // a one byte integer and no fraction
        currentRow[writeIndex] = blob[readIndex].charCodeAt();
        readIndex += 1;
      }

      writeIndex += 1;
    };

    result.push(currentRow);
  }

  return result;
};

$(document).ready(function() {
  // need TrimPath to load before parsing templates
  GbGradeTable.templates = {
    cell: TrimPath.parseTemplate(
        $("#cellTemplate").html().trim().toString()),
    courseGradeCell: TrimPath.parseTemplate(
        $("#courseGradeCellTemplate").html().trim().toString()),
    courseGradeHeader: TrimPath.parseTemplate(
        $("#courseGradeHeaderTemplate").html().trim().toString()),
    assignmentHeader: TrimPath.parseTemplate(
        $("#assignmentHeaderTemplate").html().trim().toString()),
    categoryScoreHeader: TrimPath.parseTemplate(
        $("#categoryScoreHeaderTemplate").html().trim().toString()),
    studentHeader: TrimPath.parseTemplate(
        $("#studentHeaderTemplate").html().trim().toString()),
    studentCell: TrimPath.parseTemplate(
        $("#studentCellTemplate").html().trim().toString())
  };

});

GbGradeTable.courseGradeRenderer = function (instance, td, row, col, prop, value, cellProperties) {

  var $td = $(td);
  var cellKey = (row + ',' + col + ',' + value);
  var wasInitialised = $td.data('cell-initialised');

  if (wasInitialised === cellKey) {
    return;
  }

  if (!wasInitialised) {
    var html = GbGradeTable.templates.courseGradeCell.process({
      value: value
    });

    td.innerHTML = html;
  } else if (wasInitialised != cellKey) {
    $td.find(".gb-value").html(value);
  }

  var student = instance.getDataAtCell(row, 0);

  $td.data('studentid', student.userId);
  $td.data('cell-initialised', cellKey);
};

GbGradeTable.cellRenderer = function (instance, td, row, col, prop, value, cellProperties) {

  var $td = $(td);
  var index = col - 2;
  var student = instance.getDataAtCell(row, 0);
  var column = instance.view.settings.columns[col]._data_;

  // key needs to contain all values the cell requires for render
  // otherwise it won't rerender when those values change
  var hasComment = column.type === "assignment" ? GbGradeTable.hasComment(student, column.assignmentId) : false;
  var keyValues = [row, index, value, student.eid, hasComment, column.type];
  var cellKey = keyValues.join(",");

  var wasInitialised = $td.data('cell-initialised');

  if (!GbGradeTable.forceRedraw && wasInitialised === cellKey) {
    // Nothing to do
    return;
  }

  var student = instance.getDataAtCell(row, 0);

  // THINKME: All of this was here because patching the DOM was faster than
  // replacing innerHTML on every scroll event.  Can we do the same sort of
  // thing?
  if (!wasInitialised) {
    // First time we've initialised this cell.
    var html = GbGradeTable.templates.cell.process({
      value: value
    });

    td.innerHTML = html;
  } else if (wasInitialised != cellKey) {
    // This cell was previously holding a different value.  Just patch it.
    $td.find(".gb-value").html(value);
  }

  $td.data("studentid", student.userId);
  if (column.type === "assignment") {
    $td.data("assignmentid", column.assignmentId);
    $td.removeData("categoryId");
  } else if (column.type === "category") {
    $td.data("categoryId", column.categoryId);
    $td.removeData("assignmentid");
    if (value != null && (value+"").length > 0) {
      $td.find(".gb-value").append("%");
    } else {
      $td.find(".gb-value").html("-");
    }
  } else {
    throw "column.type not supported: " + column.type;
  }


  if (hasComment) {
    $td.find(".gb-comment-notification").show();
  } else {
    $td.find(".gb-comment-notification").hide();
  }


  $td.data('cell-initialised', cellKey);
};


GbGradeTable.headerRenderer = function (col, column) {
  if (col == 0) {
    return GbGradeTable.templates.studentHeader.process({col: col});
  } else if (col == 1) {
    return GbGradeTable.templates.courseGradeHeader.process({col: col});
  }

  if (column.type === "assignment") {
    return GbGradeTable.templates.assignmentHeader.process($.extend({col: col}, column));
  } else if (column.type === "category") {
    return GbGradeTable.templates.categoryScoreHeader.process($.extend({col: col}, column));
  } else {
    return "Unknown column type for column: " + col + " (" + column.type+ ")";
  }
};

GbGradeTable.studentCellRenderer = function(instance, td, row, col, prop, value, cellProperties) {
  var $td = $(td);

  $td.attr("scope", "row").attr("role", "rowHeader");

  var html = GbGradeTable.templates.studentCell.process(value);
  $td.html(html);
}


GbGradeTable.mergeColumns = function (data, fixedColumns) {
  var result = [];

  for (var row = 0; row < data.length; row++) {
    var updatedRow = []

    for (var i=0; i < fixedColumns.length; i++) {
      updatedRow.push(fixedColumns[i][row]);
    }

    for (var col = 0; col < data[row].length; col++) {
      updatedRow.push(data[row][col]);
    }

    result.push(updatedRow)
  }

  return result;
}

var nextRequestId = 0;
GbGradeTable.ajaxCallbacks = {}

GbGradeTable.ajaxComplete = function (requestId, status, data) {
  GbGradeTable.ajaxCallbacks[requestId](status, data);
};

GbGradeTable.ajax = function (params, callback) {
  params['_requestId'] = nextRequestId++;

  GbGradeTable.ajaxCallbacks[params['_requestId']] = callback || $.noop;;

  GbGradeTable.domElement.trigger("gbgradetable.action", params);
};

// FIXME: Hard-coded stuff here
GbGradeTable.renderTable = function (elementId, tableData) {
  GbGradeTable.domElement = $('#' + elementId);
  GbGradeTable.students = tableData.students;
  GbGradeTable.columns = tableData.columns;
  GbGradeTable.grades = GbGradeTable.mergeColumns(GbGradeTable.unpack(tableData.serializedGrades,
                                                                      tableData.rowCount,
                                                                      tableData.columnCount),
                                                  [
                                                    tableData.students,
                                                    tableData.courseGrades
                                                  ]);

  GbGradeTableEditor = Handsontable.editors.TextEditor.prototype.extend();

  GbGradeTableEditor.prototype.createElements = function () {
    Handsontable.editors.TextEditor.prototype.createElements.apply(this, arguments);
    var outOf = "<span class='out-of'></span>";
    $(this.TEXTAREA_PARENT).append(outOf);
  };

  GbGradeTableEditor.prototype.beginEditing = function() {
    Handsontable.editors.TextEditor.prototype.beginEditing.apply(this, arguments);

    var col = this.instance.getSelected()[1];
    var assignment = GbGradeTable.columns[col - 2];
    var points = assignment.points;
    $(this.TEXTAREA_PARENT).find(".out-of").html("/" + points);

    if ($(this.TEXTAREA).val().length > 0) {
      $(this.TEXTAREA).select();
    }
  };

  GbGradeTableEditor.prototype.saveValue = function() {
    var that = this;
    var row = this.row;

    var oldScore = this.originalValue;
    var newScore = $(this.TEXTAREA).val();
    var studentId = $(this.TD).data("studentid");
    var assignmentId = $(this.TD).data("assignmentid");

    // FIXME: We'll need to pass through the original comment text here.
    GbGradeTable.ajax({
      action: 'setScore',
      studentId: studentId,
      assignmentId: assignmentId,
      oldScore: oldScore,
      newScore: newScore,
      comment: ""
    }, function (status, data) {
      that.instance.setDataAtCell(row, 1, data.courseGrade);
    });

    Handsontable.editors.TextEditor.prototype.saveValue.apply(this, arguments);
  }


  GbGradeTable.container = $("#gradebookSpreadsheet");

  GbGradeTable.instance = new Handsontable(document.getElementById(elementId), {
    data: GbGradeTable.grades,
//    rowHeaderWidth: 220,
//    rowHeaders: GbGradeTable.studentCellRenderer,
    fixedColumnsLeft: 2,
    colHeaders: true,
    columns: GbGradeTable.getFilteredColumns(),
    colWidths: [240,140].concat(GbGradeTable.columns.map(function () { return 230 })),
    autoRowSize: false,
    autoColSize: false,
    height: $(window).height() * 0.4,
    width: $('#' + elementId).width() * 0.9,
    fillHandle: false,
    afterGetRowHeader: function(row,th) {
      $(th).
        attr("role", "rowheader").
        attr("scope", "row");
    },
    afterGetColHeader: function(col, th) {
      var $th = $(th);

      if (col < 2) {
        $th.html(GbGradeTable.headerRenderer(col));
      } else {
        $th.html(GbGradeTable.headerRenderer(col, this.view.settings.columns[col]._data_))
      }

      $th.
        attr("role", "columnheader").
        attr("scope", "col").
        addClass("gb-categorized"); /* TODO only if enabled */

      // assignment column
      if (col > 1) {
        var column = this.view.settings.columns[col]._data_;
        var columnName = column.title;
        $th.
          attr("role", "columnheader").
          attr("scope", "col").
          attr("abbr", columnName).
          attr("aria-label", columnName).
          css("borderTopColor", column.color || column.categoryColor);

        if (column.type == "assignment") {
          $th.data("assignmentid", column.assignmentId);
        }
        
        $th.find(".swatch").css("backgroundColor", column.color || column.categoryColor);
      }
    },
    beforeOnCellMouseDown: function(event, coords, td) {
      if (coords.row < 0 && coords.col >= 0) {
        event.stopImmediatePropagation();
        this.selectCell(0, coords.col);
      } else if (coords.col < 0) {
        event.stopImmediatePropagation();
        this.selectCell(coords.row, 0);
      }
    },
    currentRowClassName: 'currentRow',
    currentColClassName: 'currentCol',
    multiSelect: false
  });

  // append all dropdown menus to body to avoid overflows on table
  var $dropdownMenu;
  var $link;
  $(window).on('show.bs.dropdown', function (event) {
    $dropdownMenu = $(event.target).find('.dropdown-menu');
    $dropdownMenu.addClass("gb-dropdown-menu");

    $link = $(event.target);
    $dropdownMenu.data("cell", $link.closest("td, th"));

    $dropdownMenu.width($dropdownMenu.outerWidth());

    $('body').append($dropdownMenu.detach());

    var linkOffset = $link.offset();

    $dropdownMenu.css({
        'display': 'block',
        'top': linkOffset.top + $link.outerHeight(),
        'left': linkOffset.left - $dropdownMenu.outerWidth() + $link.outerWidth()
    });
  });
  $(window).on('hide.bs.dropdown', function (event) {
    $link.append($dropdownMenu.detach());
    $dropdownMenu.hide();
    $dropdownMenu = null;
  });
  $(".wtHolder").on('scroll', function (event) {
    if ($dropdownMenu && $dropdownMenu.length > 0) {
      var linkOffset = $link.offset();

      $dropdownMenu.css({
          'top': linkOffset.top + $link.outerHeight(),
          'left': linkOffset.left - $dropdownMenu.outerWidth() + $link.outerWidth()
      });
    }
  });


  var filterTimeout;
  $("#studentFilterInput").on("keyup", function(event) {
    clearTimeout(filterTimeout);
    filterTimeout = setTimeout(function() {
      GbGradeTable.redrawTable(true);
    }, 500);
  });

  // Setup menu event bindings
  // View Log
  $(document).on("click", ".gb-dropdown-menu .gb-view-log", function() {
    var $dropdown = $(this).closest(".gb-dropdown-menu");
    var $cell = $dropdown.data("cell");

    GbGradeTable.ajax({
      action: 'viewLog',
      studentId: $cell.data("studentid"),
      assignmentId: $cell.data("assignmentid")
    });
  }).
  // Edit Assignment
  on("click", ".gb-dropdown-menu .edit-assignment-details", function() {
    var $dropdown = $(this).closest(".gb-dropdown-menu");
    var $cell = $dropdown.data("cell");

    GbGradeTable.ajax({
      action: 'editAssignment',
      assignmentId: $cell.data("assignmentid")
    });
  }).
  // View Assignment Statistics
  on("click", ".gb-dropdown-menu .gb-view-statistics", function() {
    var $dropdown = $(this).closest(".gb-dropdown-menu");
    var $cell = $dropdown.data("cell");

    GbGradeTable.ajax({
      action: 'viewStatistics',
      assignmentId: $cell.data("assignmentid")
    });
  }).
  // Override Course Grade
  on("click", ".gb-dropdown-menu .gb-course-grade-override", function() {
    var $dropdown = $(this).closest(".gb-dropdown-menu");
    var $cell = $dropdown.data("cell");

    GbGradeTable.ajax({
      action: 'overrideCourseGrade',
      studentId: $cell.data("studentid")
    });
  }).
  // Edit Comment
  on("click", ".gb-dropdown-menu .gb-edit-comments", function() {
    var $dropdown = $(this).closest(".gb-dropdown-menu");
    var $cell = $dropdown.data("cell");

    GbGradeTable.ajax({
      action: 'editComment',
      assignmentId: $cell.data("assignmentid"),
      studentId: $cell.data("studentid")
    });
  });

  GbGradeTable.setupToggleGradeItems();
};

GbGradeTable.selectCell = function(assignmentId, studentId) {
  var row = 0;
  if (studentId != null){
    row = GbGradeTable.rowForStudent(studentId);
  }

  var col = 0;
  if (assignmentId != null) {
    col = GbGradeTable.colForAssignment(assignmentId);
  }

  return GbGradeTable.instance.selectCell(row, col);
};

GbGradeTable.selectCourseGradeCell = function(studentId) {
  var row = 0;
  if (studentId != null){
    row = GbGradeTable.rowForStudent(studentId);
  }

  return GbGradeTable.instance.selectCell(row, 1);
};

GbGradeTable.rowForStudent = function(studentId) {
  return GbGradeTable.instance.view.settings.data.findIndex(function(row, index, array) {
           return row[0].userId === studentId;
         });
};

GbGradeTable.modelForStudent = function(studentId) {
  for (var i=0; i<GbGradeTable.students.length; i++) {
    var student = GbGradeTable.students[i];
    if (student.userId === studentId) {
      return student;
    }
  }

  throw "modelForStudent: model not found for " + studentId;
};

GbGradeTable.colForAssignment = function(assignmentId) {
  return GbGradeTable.instance.view.settings.columns.findIndex(function(column, index, array) {
           return column._data_ && column._data_.assignmentId === parseInt(assignmentId);
         });
};

GbGradeTable.colModelForAssignment = function(assignmentId) {
  for (var i=0; i<GbGradeTable.columns.length; i++) {
    var column = GbGradeTable.columns[i];
    if (column.type == "assignment") {
      if (column.assignmentId === parseInt(assignmentId)) {
        return column;
      }
    }
  }
  
  throw "colModelForAssignment: column not found for " + assignmentId;
};

GbGradeTable.hasComment = function(student, assignmentId) {
  var assignmentIndex = $.inArray(GbGradeTable.colModelForAssignment(assignmentId), GbGradeTable.columns);
  return student.hasComments[assignmentIndex] === "1";
};


GbGradeTable.updateHasComment = function(student, assignmentId, comment) {
  var hasComments = student.hasComments;
  var flag = (comment == null || comment == "") ? '0' : '1';

  var assignmentIndex = $.inArray(GbGradeTable.colModelForAssignment(assignmentId), GbGradeTable.columns);

  student.hasComments = hasComments.substr(0, assignmentIndex) + flag + hasComments.substr(assignmentIndex+1);
}


GbGradeTable.colModelForCategoryScore = function(categoryName) {
  for (var i=0; i<GbGradeTable.columns.length; i++) {
    var column = GbGradeTable.columns[i];
    if (column.type == "category") {
      if (column.title === categoryName) {
        return column;
      }
    }
  }
  
  throw "colModelForCategoryScore: column not found for " + categoryName;
};


GbGradeTable.selectStudentCell = function(studentId) {
  var row = 0;
  if (studentId != null){
    row = GbGradeTable.rowForStudent(studentId);
  }

  return GbGradeTable.instance.selectCell(row, 0);
};

GbGradeTable.updateComment = function(assignmentId, studentId, comment) {
  var student = GbGradeTable.modelForStudent(studentId);

  var hasComments = student.hasComments;
  var flag = (comment == null || comment == "") ? '0' : '1';

  var assignmentIndex = $.inArray(GbGradeTable.colModelForAssignment(assignmentId), GbGradeTable.columns);

  student.hasComments = hasComments.substr(0, assignmentIndex) + flag + hasComments.substr(assignmentIndex+1);

  var row = GbGradeTable.rowForStudent(studentId);
  var col = GbGradeTable.colForAssignment(assignmentId);

  GbGradeTable.instance.setDataAtCell(row, 0, student);
  GbGradeTable.redrawCell(row, col);
};

GbGradeTable.redrawCell = function(row, col) {
  var $cell = $(GbGradeTable.instance.getCell(row, col));
  $cell.removeData('cell-initialised');

  GbGradeTable.instance.render();
};

GbGradeTable._redrawTableTimeout;
GbGradeTable.redrawTable = function(force) {
  clearTimeout(GbGradeTable._redrawTableTimeout);

  GbGradeTable._redrawTableTimeout = setTimeout(function() {
    GbGradeTable.forceRedraw = force || false;
    GbGradeTable.instance.loadData(GbGradeTable.getFilteredData());
    GbGradeTable.instance.updateSettings({
      columns: GbGradeTable.getFilteredColumns()
    });
    GbGradeTable.forceRedraw = false;
  }, 100);
};

GbGradeTable.getFilteredColumns = function() {
  return [{
    renderer: GbGradeTable.studentCellRenderer,
    editor: false,
  },
  {
    renderer: GbGradeTable.courseGradeRenderer,
    editor: false,
  }].concat(GbGradeTable.columns.filter(function(col) {
    return !col.hidden;
  }).map(function (column) {
    if (column.type === 'category') {
      return {
        renderer: GbGradeTable.cellRenderer,
        editor: false,
        _data_: column
      };
    } else {
      return {
        renderer: GbGradeTable.cellRenderer,
        editor: GbGradeTableEditor,
        _data_: column
      };
    }
  }));
};

GbGradeTable.getFilteredColHeaders = function() {
  return GbGradeTable.getFilteredColumns().map(function() {
    return GbGradeTable.headerRenderer;
  });
};

GbGradeTable.getFilteredData = function() {
  var data = GbGradeTable.grades.slice(0);

  data = GbGradeTable.applyStudentFilter(data);
  data = GbGradeTable.applyColumnFilter(data);

  return data;
};

GbGradeTable.applyColumnFilter = function(data) {
  for (var i=GbGradeTable.columns.length-1; i>=0; i--) {
    var column = GbGradeTable.columns[i];
    if (column.hidden) {
      for(var row=0; row<data.length; row++) {
        data[row] = data[row].slice(0,i+2).concat(data[row].slice(i+3))
      }
    }
  } 

  return data;
};

GbGradeTable.applyStudentFilter = function(data) {
  var query = $("#studentFilterInput").val();

  if (query == "") {
    return data;
  } else {
    var queryStrings = query.split(" ");
    var filteredData = data.filter(function(row) {
      var match = true;

      var student = row[0];
      var searchableFields = [student.firstName, student.lastName, student.eid];
      var studentSearchString = searchableFields.join(";")

      for (var i=0; i<queryStrings.length; i++) {
        var queryString = queryStrings[i];

        if (studentSearchString.match(new RegExp(queryString, "i")) == null) {
          return false;
        }
      }
      return match;
    });

    return filteredData;
  }
};

GbGradeTable.setupToggleGradeItems = function() {
  var $panel = $("<div>").addClass("gb-toggle-grade-items-panel").hide();
  var $button = $("#toggleGradeItemsToolbarItem");
  $button.after($panel);

  // move the Wicket generated panel into this menu dropdown
  $panel.append($("#gradeItemsTogglePanel").show());

  function repositionPanel() {
    //TODO $panel.css("right",  - ($button.position().left + $button.outerWidth()));
  };

  var updateCategoryFilterState = function($itemFilter) {
    var $group = $itemFilter.closest(".gb-item-filter-group");
    var $label = $group.find(".gb-item-category-filter label");
    var $input = $group.find(".gb-item-category-filter input");

    var checkedItemFilters = $group.find(".gb-item-filter :input:checked, .gb-item-category-score-filter :input:checked").length;
    var itemFilters = $group.find(".gb-item-filter :input, .gb-item-category-score-filter :input").length;

    $label.
      removeClass("partial").
      removeClass("off").
      find(".gb-filter-partial-signal").remove();

    if (checkedItemFilters == 0) {
      $input.prop("checked", false);
      $label.addClass("off");
    } else if (checkedItemFilters == itemFilters) {
      $input.prop("checked", true);
    } else {
      $input.prop("checked", false);
      $label.addClass("partial");
      $label.find(".gb-item-filter-signal").
        append($("<span>").addClass("gb-filter-partial-signal"));
    }
  };


  function handleCategoryFilterStateChange(event) {
    var $input = $(event.target);
    var $label = $input.closest("label");
    var $filter = $input.closest(".gb-item-category-filter");

    // toggle all columns in this category
    if ($input.is(":checked")) {
      $filter.removeClass("off");
      // show all
      $input.closest(".gb-item-filter-group").find(".gb-item-filter :input:not(:checked), .gb-item-category-score-filter :input:not(:checked)").trigger("click");
    } else {
      $filter.addClass("off");
      // hide all
      $input.closest(".gb-item-filter-group").find(".gb-item-filter :input:checked, .gb-item-category-score-filter :input:checked").trigger("click");
    }

    updateCategoryFilterState($input);
  };


  function handleGradeItemFilterStateChange(event) {
    var $input = $(event.target);
    var $label = $input.closest("label");
    var $filter = $input.closest(".gb-item-filter");

    var assignmentId = $input.val();

    var column = GbGradeTable.colModelForAssignment(assignmentId);

    if ($input.is(":checked")) {
      $filter.removeClass("off");
      //self.gradebookSpreadsheet.showGradeItemColumn(assignmentId);
      // TODO
      column.hidden = false;
    } else {
      $filter.addClass("off");
      //self.gradebookSpreadsheet.hideGradeItemColumn(assignmentId);
      // TODO
      column.hidden = true;
    }

    updateCategoryFilterState($input);
    GbGradeTable.redrawTable(true);
  };


  function handleCategoryScoreFilterStateChange(event) {
    var $input = $(event.target);
    var $label = $input.closest("label");
    var $filter = $input.closest(".gb-item-category-score-filter");

    var category = $input.val();

    var column = GbGradeTable.colModelForCategoryScore(category);

    if ($input.is(":checked")) {
      //self.gradebookSpreadsheet.showCategoryScoreColumn(category);
      // TODO
      $filter.removeClass("off");
      column.hidden = false;
    } else {
      //self.gradebookSpreadsheet.hideCategoryScoreColumn(category);
      // TODO
      $filter.addClass("off");
      column.hidden = true;
    }

    updateCategoryFilterState($input);
    GbGradeTable.redrawTable(true);
  }


  function handleShowAll() {
    $panel.find(".gb-item-category-filter :input:not(:checked)").trigger("click");
  };


  function handleHideAll() {
    $panel.find(".gb-item-category-filter :input:checked").trigger("click");
  };


  function handleShowOnlyThisCategory($filter) {
    var $input = $filter.find(":input");
    var $label = $filter.find("label");

    $panel.
        find(".gb-item-category-filter :input:checked:not([value="+$input.val()+"])").
        trigger("click");

    if ($input.is(":not(:checked)")) {
      $label.trigger("click");
    } else {
      $input.closest(".gb-item-filter-group").find(".gb-item-filter :input:not(:checked), .gb-item-category-score-filter :input:not(:checked)").trigger("click");
    }
  };


  function handleShowOnlyThisItem($filter) {
    var $input = $filter.find(":input");
    var $label = $filter.find("label");

    $panel.
        find(".gb-item-filter :input:checked:not(#"+$input.attr("id")+"), .gb-item-category-score-filter :input:checked").
        trigger("click");

    if ($input.is(":not(:checked)")) {
      $label.trigger("click");
    }
  };


  function handleShowOnlyThisCategoryScore($filter) {
    var $input = $filter.find(":input");
    var $label = $filter.find("label");

    $panel.
        find(".gb-item-filter :input:checked, .gb-item-category-score-filter :input:checked:not(#"+$input.attr("id")+")").
        trigger("click");

    if ($input.is(":not(:checked)")) {
      $label.trigger("click");
    }
  };


  $button.on("click", function(event) {
    event.preventDefault();

    $button.toggleClass("on");

    if ($button.hasClass("on")) {
      repositionPanel();
      $button.attr("aria-expanded", "true");
      $panel.show().attr("aria-hidden", "false");
    } else {
      $button.attr("aria-expanded", "false");
      $panel.hide().attr("aria-hidden", "true");
    }

    // Support click outside menu panel to close panel
    function hidePanelOnOuterClick(mouseDownEvent) {
      if ($(mouseDownEvent.target).closest(".gb-toggle-grade-items-panel, #toggleGradeItemsToolbarItem").length == 0) {
        $button.removeClass("on");
        $button.attr("aria-expanded", "false");
        $panel.hide().attr("aria-hidden", "true");
        $(document).off("mousedown", hidePanelOnOuterClick);
      }
    };
    $(document).on("mousedown", hidePanelOnOuterClick);

    return false;
  });

  $button.on("keydown", function(event) {
    // up arrow hides menu
    if (event.keyCode == 38) {
      if ($panel.is(":visible")) {
        $(this).trigger("click");
        return false;
      }
    // down arrow shows menu or focuses first item in menu
    } else if (event.keyCode == 40) {
      if ($panel.is(":not(:visible)")) {
        $(this).trigger("click");
      } else {
        $panel.find("a:first").focus();
      }
      return false;
    }
  });

  $panel.
        on("click", "#showAllGradeItems", function() {
          handleShowAll();
          $(this).focus();
        }).
        on("click", "#hideAllGradeItems", function() {
          handleHideAll();
          $(this).focus();
        }).
        on("click", ".gb-show-only-this-category", function(event) {
          event.preventDefault();

          var $filter = $(event.target).closest(".gb-item-category-filter");
          handleShowOnlyThisCategory($filter);
          $(this).focus();
        }).
        on("click", ".gb-show-only-this-item", function(event) {
          event.preventDefault();

          var $filter = $(event.target).closest(".gb-item-filter");
          handleShowOnlyThisItem($filter);
          $(this).focus();
        }).
        on("click", ".gb-show-only-this-category-score", function(event) {
          event.preventDefault();

          var $filter = $(event.target).closest(".gb-item-category-score-filter");
          handleShowOnlyThisCategoryScore($filter);
          $(this).focus();
        }).
        on("click", ".gb-toggle-this-category", function(event) {
          event.preventDefault();

          var $filter = $(event.target).closest(".gb-item-category-filter");
          $filter.find(":input").trigger("click");
          $(this).focus();
        }).
        on("click", ".gb-toggle-this-item", function(event) {
          event.preventDefault();

          var $filter = $(event.target).closest(".gb-item-filter");
          $filter.find(":input").trigger("click");
          $(this).focus();
        }).
        on("click", ".gb-toggle-this-category-score", function(event) {
          event.preventDefault();

          var $filter = $(event.target).closest(".gb-item-category-score-filter");
          $filter.find(":input").trigger("click");
          $(this).focus();
        });

  // any labels or action links will be included in the arrow navigation
  // we won't include dropdown toggles for this.. can get to those with tab keys
  var $menuItems = $panel.find("#hideAllGradeItems, #showAllGradeItems, label[role='menuitem']");
  $menuItems.on("keydown", function(event) {
    var $this = $(this);
    var currentIndex = $menuItems.index($this);

    // up arrow navigates up or back to button
    if (event.keyCode == 38) {
      if (currentIndex == 0) {
        $button.focus();
      } else {
        $menuItems[currentIndex-1].focus();
      }
      return false;
    // down arrow navigates down list
    } else if (event.keyCode == 40) {
      if (currentIndex + 1 < $menuItems.length) {
        $menuItems[currentIndex+1].focus();
        return false;
      }

    // if return then treat as click
    } else if (event.keyCode == 13) {
      $this.trigger("click");
      return false;
    }

    return true;
  });

  $panel.find(".gb-item-category-filter :input").on("change", handleCategoryFilterStateChange);
  $panel.find(".gb-item-filter :input").on("change", handleGradeItemFilterStateChange);
  $panel.find(".gb-item-category-score-filter :input").on("change", handleCategoryScoreFilterStateChange);

  $panel.find(":input:not(:checked)").trigger("change");
};