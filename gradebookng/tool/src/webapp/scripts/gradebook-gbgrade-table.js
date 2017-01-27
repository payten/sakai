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
      value: value[0]
    });

    td.innerHTML = html;
  } else if (wasInitialised != cellKey) {
    $td.find(".gb-value").html(value[0]);
  }

  var student = instance.getDataAtCell(row, 0);

  $td.data('studentid', student.userId);
  $td.data('cell-initialised', cellKey);
};

GbGradeTable.replaceContents = function (elt, newContents) {
  // empty it
  while (elt.firstChild) {
    elt.removeChild(elt.firstChild);
  }

  if ($.isArray(newContents)) {
    for (var i in newContents) {
      elt.appendChild(newContents[i]);
    }
  } else {
    elt.appendChild(newContents);
  }

  return elt;
};

// This function is called a *lot*, so avoid doing anything too expensive here.
GbGradeTable.cellRenderer = function (instance, td, row, col, prop, value, cellProperties) {

  var $td = $(td);
  var index = col - 2;
  var student = instance.getDataAtCell(row, 0);
  var column = instance.view.settings.columns[col]._data_;

  // key needs to contain all values the cell requires for render
  // otherwise it won't rerender when those values change
  var hasComment = column.type === "assignment" ? GbGradeTable.hasComment(student, column.assignmentId) : false;
  var scoreState = column.type === "assignment" ? GbGradeTable.getScoreState(student.userId, column.assignmentId) : false;
  var keyValues = [row, index, value, student.eid, hasComment, column.type, scoreState];
  var cellKey = keyValues.join(",");

  var wasInitialised = $.data(td, 'cell-initialised');

  if (!GbGradeTable.forceRedraw && wasInitialised === cellKey) {
    // Nothing to do
    return;
  }

  var student = instance.getDataAtCell(row, 0);

  var valueCell;

  if (!wasInitialised) {
    // First time we've initialised this cell.
    var html = GbGradeTable.templates.cell.process({
      value: value
    });

    td.innerHTML = html;
  } else if (wasInitialised != cellKey) {
    valueCell = td.getElementsByClassName('gb-value')[0];

    // This cell was previously holding a different value.  Just patch it.
    GbGradeTable.replaceContents(valueCell, document.createTextNode(value));
  }

  if (!valueCell) {
    valueCell = td.getElementsByClassName('gb-value')[0];
  }

  $.data(td, "studentid", student.userId);
  if (column.type === "assignment") {
    $.data(td, "assignmentid", column.assignmentId);
    $.removeData(td, "categoryId");
  } else if (column.type === "category") {
    $.data(td, "categoryId", column.categoryId);
    $.removeData(td, "assignmentid");
    if (value != null && (value+"").length > 0) {
      GbGradeTable.replaceContents(valueCell, document.createTextNode('' + value + '%'));
    } else {
      GbGradeTable.replaceContents(valueCell, document.createTextNode('-'));
    }
  } else {
    throw "column.type not supported: " + column.type;
  }

  // comment notification
  var commentNotification = td.getElementsByClassName("gb-comment-notification")[0];
  if (commentNotification) {
    if (hasComment) {
      commentNotification.style.display = 'block';
    } else {
      commentNotification.style.display = 'none';
    }
  }

  // other notifications
  var gbNotification = td.getElementsByClassName('gb-notification')[0];
  var cellDiv = td.getElementsByClassName('relative')[0];

  cellDiv.className = 'relative';
  var $cellDiv = $(cellDiv);

  if (column.externallyMaintained) {
    $cellDiv.addClass("gb-read-only");
  } else if (scoreState == "saved") {
    $cellDiv.addClass("gb-save-success");

    setTimeout(function() {
      GbGradeTable.setScoreState(false, student.userId, column.assignmentId);
      $cellDiv.removeClass("gb-save-success", 2000);
    }, 2000);
  } else if (scoreState == "error") {
    $cellDiv.addClass("gb-save-error");
  } else if (scoreState == "invalid") {
    $cellDiv.addClass("gb-save-invalid");
  }
  if (parseFloat(value) > parseFloat(column.points)) {
    $cellDiv.addClass("gb-extra-credit");
    $(gbNotification).addClass("gb-flag-extra-credit");
  } else {
    $(gbNotification).removeClass("gb-flag-extra-credit");
    $cellDiv.removeClass("gb-extra-credit");
  }

  $.data(td, 'cell-initialised', cellKey);
};


GbGradeTable.headerRenderer = function (col, column) {
  if (col == 0) {
    return GbGradeTable.templates.studentHeader.process({col: col});
  } else if (col == 1) {
    return GbGradeTable.templates.courseGradeHeader.process({col: col});
  }

  var templateData = $.extend({
    col: col,
    settings: GbGradeTable.settings
  }, column);

  if (column.type === "assignment") {
    return GbGradeTable.templates.assignmentHeader.process(templateData);
  } else if (column.type === "category") {
    return GbGradeTable.templates.categoryScoreHeader.process(templateData);
  } else {
    return "Unknown column type for column: " + col + " (" + column.type+ ")";
  }
};

GbGradeTable.studentCellRenderer = function(instance, td, row, col, prop, value, cellProperties) {
  var $td = $(td);

  $td.attr("scope", "row").attr("role", "rowHeader");

  var html = GbGradeTable.templates.studentCell.process(value);
  $td.html(html);

  $td.data("studentid", value.userId);

  // If this cell gets reused for a score display, it'll need to be fully
  // reinitialised before use.
  $.removeData(td, "cell-initialised");
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
  GbGradeTable.settings = tableData.settings;
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

  // If an entered score is invalid, we keep track of the last good value here
  var lastValidGrades = {};

  GbGradeTableEditor.prototype.saveValue = function() {
    var that = this;
    var row = this.row;

    var $td = $(this.TD);

    var oldScore = this.originalValue;
    var newScore = $(this.TEXTAREA).val();
    var studentId = $td.data("studentid");
    var assignmentId = $td.data("assignmentid");

    var assignment = GbGradeTable.colModelForAssignment(assignmentId);

    if (!lastValidGrades[studentId]) {
      lastValidGrades[studentId] = {};
    }

    var postData = {
      action: 'setScore',
      studentId: studentId,
      assignmentId: assignmentId,
      oldScore: (lastValidGrades[studentId][assignmentId] || oldScore),
      newScore: newScore,
      comment: ""
    };

    if (assignment.categoryId != null) {
      postData['postData']= assignment.categoryId;
    }

    // FIXME: We'll need to pass through the original comment text here.
    GbGradeTable.ajax(postData, function (status, data) {
      if (status == "OK") {
        GbGradeTable.setScoreState("saved", studentId, assignmentId);
        delete lastValidGrades[studentId][assignmentId];

        if ($.isEmptyObject(lastValidGrades[studentId])) {
          delete(lastValidGrades[studentId])
        }
      } else if (status == "error") {
        GbGradeTable.setScoreState("error", studentId, assignmentId);

        if (!lastValidGrades[studentId][assignmentId]) {
          lastValidGrades[studentId][assignmentId] = oldScore;
        }
      } else if (status == "invalid") {
        GbGradeTable.setScoreState("invalid", studentId, assignmentId);

        if (!lastValidGrades[studentId][assignmentId]) {
          lastValidGrades[studentId][assignmentId] = oldScore;
        }
      } else if (status == "nochange") {
        // nothing to do!
      } else {
        console.log("Unhandled saveValue response: " + status);
      }

      // update the course grade cell
      if (data.courseGrade) {
        that.instance.setDataAtCell(row, 1, data.courseGrade);
      }

      // update the category average cell
      if (assignment.categoryId) {
        var categoryScoreCol = GbGradeTable.colForCategoryScore(assignment.categoryId);
        that.instance.setDataAtCell(row, categoryScoreCol, data.categoryScore);
      }
    });

    Handsontable.editors.TextEditor.prototype.saveValue.apply(this, arguments);
  }


  GbGradeTable.container = $("#gradebookSpreadsheet");

  GbGradeTable.columnDOMNodeCache = {};

  GbGradeTable.instance = new Handsontable(document.getElementById(elementId), {
    data: GbGradeTable.grades,
//    rowHeaderWidth: 220,
//    rowHeaders: GbGradeTable.studentCellRenderer,
    fixedColumnsLeft: 2,
    colHeaders: true,
    columns: GbGradeTable.getFilteredColumns(),
    colWidths: GbGradeTable.getColumnWidths(),
    autoRowSize: false,
    autoColSize: false,
    height: $(window).height() * 0.5,
    width: $("#gradebookSpreadsheet").width(),
    fillHandle: false,
    afterGetRowHeader: function(row,th) {
      $(th).
        attr("role", "rowheader").
        attr("scope", "row");
    },

    // This function is another hotspot.  Efficiency is paramount!
    afterGetColHeader: function(col, th) {
      var $th = $(th);

      // Calculate the HTML that we need to show
      var html = '';
      if (col < 2) {
        html = GbGradeTable.headerRenderer(col);
      } else {
        html = GbGradeTable.headerRenderer(col, this.view.settings.columns[col]._data_);
      }

      // If we haven't got a cached parse of it, do that now
      if (!GbGradeTable.columnDOMNodeCache[col] || GbGradeTable.columnDOMNodeCache[col].html !== html) {
        GbGradeTable.columnDOMNodeCache[col] = {
          html: html,
          dom: $(html).toArray()
        };
      }

      GbGradeTable.replaceContents(th, GbGradeTable.columnDOMNodeCache[col].dom);

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
          attr("aria-label", columnName);

        if (column.type == "assignment") {
          $.data(th, "assignmentid", column.assignmentId);
        }

        if (GbGradeTable.settings.isCategoriesEnabled) {
          var color = column.color || column.categoryColor;
          $th.css("borderTopColor", color);
          $th.find(".swatch").css("backgroundColor", color);
        }
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


  // resize the table on window resize
  var resizeTimeout;
  $(window).on("resize", function() {
    clearTimeout(resizeTimeout);
    resizeTimeout = setTimeout(function() {
      GbGradeTable.instance.updateSettings({
        height: $(window).height() * 0.5,
        width: $("#gradebookSpreadsheet").width()
      });
    }, 200);
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
  }).
  // View Grade Summary
  on("click", ".gb-dropdown-menu .gb-view-grade-summary", function() {
    var $dropdown = $(this).closest(".gb-dropdown-menu");
    var $cell = $dropdown.data("cell");

    GbGradeTable.viewGradeSummary($cell.data("studentid"));
  }).
  // Set Zero Score for Empty Cells
  on("click", ".gb-dropdown-menu .gb-set-zero-score", function() {
    GbGradeTable.ajax({
      action: 'setZeroScore'
    });
  }).
  // View Course Grade Override Log
  on("click", ".gb-dropdown-menu .gb-course-grade-override-log", function() {
    var $dropdown = $(this).closest(".gb-dropdown-menu");
    var $cell = $dropdown.data("cell");

    GbGradeTable.ajax({
      action: 'viewCourseGradeLog',
      studentId: $cell.data("studentid")
    });
  }).
  // Delete Grade Item
  on("click", ".gb-dropdown-menu .gb-delete-item", function() {
    var $dropdown = $(this).closest(".gb-dropdown-menu");
    var $cell = $dropdown.data("cell");

    GbGradeTable.ajax({
      action: 'deleteAssignment',
      assignmentId: $cell.data("assignmentid")
    });
  }).
  // Set ungraded values for assignment
  on("click", ".gb-dropdown-menu .gb-set-ungraded", function() {
    var $dropdown = $(this).closest(".gb-dropdown-menu");
    var $cell = $dropdown.data("cell");

    GbGradeTable.ajax({
      action: 'setUngraded',
      assignmentId: $cell.data("assignmentid")
    });
  });

  GbGradeTable.setupToggleGradeItems();
  GbGradeTable.setupColumnSorting();

  // Patch HandsonTable getWorkspaceWidth for improved scroll performance on big tables
  var origGetWorkspaceWidth = WalkontableViewport.prototype.getWorkspaceWidth;

  (function () {
    var cachedWidth = undefined;
    WalkontableViewport.prototype.getWorkspaceWidth = function () {
      var self = this;
      if (!cachedWidth) {
        cachedWidth = origGetWorkspaceWidth.bind(self)();
      }

      return cachedWidth;
    }
  }());
};


GbGradeTable.viewGradeSummary = function(studentId) {
  GbGradeTable.ajax({
    action: 'viewGradeSummary',
    studentId: studentId
  });
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

GbGradeTable.colForCategoryScore = function(categoryId) {
  return GbGradeTable.instance.view.settings.columns.findIndex(function(column, index, array) {
           return column._data_ && column._data_.categoryId === parseInt(categoryId);
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
      var readonly = column.externallyMaintained;

      return {
        renderer: GbGradeTable.cellRenderer,
        editor: readonly ? false : GbGradeTableEditor,
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

GbGradeTable.getColumnWidths = function() {
  var studentColumnWidth = 240;
  var courseGradeColumnWidth = 140;

  // if showing course grade letter, percentage and points
  // make column a touch wider
  if (GbGradeTable.settings.isCourseLetterGradeDisplayed
        && GbGradeTable.settings.isCoursePointsDisplayed
        && GbGradeTable.settings.isCourseAverageDisplayed) {
    courseGradeColumnWidth = 220;
  }

  return [studentColumnWidth, courseGradeColumnWidth].
            concat(GbGradeTable.columns.map(function () { return 230 }));
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

GbGradeTable.setupColumnSorting = function() {
  var $table = $(GbGradeTable.instance.rootElement);

  $table.on("click", ".gb-title", function() {
    var $handle = $(this);

    var colIndex = $handle.closest("th").index();

    var direction = $handle.data("sortOrder");

    // remove all sort icons
    $table.find(".gb-title").each(function() {
      $(this).removeClass("gb-sorted-asc").removeClass("gb-sorted-desc");
      $(this).data("sortOrder", null);
    });

    if (direction == null) {
      direction = "desc";
    } else if (direction == "desc") {
      direction = "asc";
    } else {
      direction = null;
    }

    $handle.data("sortOrder", direction);
    if (direction != null) {
      $handle.addClass("gb-sorted-"+direction);
    }

    GbGradeTable.sort(colIndex, direction);
  });
};


GbGradeTable.sort = function(colIndex, direction) {
  if (direction == null) {
    // reset the table data to default order
    GbGradeTable.instance.loadData(GbGradeTable.grades);
    return;
  }

  var clone = GbGradeTable.grades.slice(0);

  clone.sort(function(row_a, row_b) {
    var a = row_a[colIndex];
    var b = row_b[colIndex];

    // sort by students
    if (colIndex == 0) {
      if (a.eid > b.eid) {
        return 1;
      }
      if (a.eid < b.eid) {
        return -1;
      }
    // sort by course grade
    } else if (colIndex == 1) {
      var a_points = parseFloat(a[1]);
      var b_points = parseFloat(b[1]);

      if (a_points > b_points) {
        return 1;
      }
      if (a_points < b_points) {
        return -1;
      }
    } else {
      if (a == null || a == "") {
        return -1;
      }
      if (b == null || b == "") {
        return 1;
      }
      if (a > b) {
        return 1;
      }
      if (a < b) {
        return -1;
      }
    }

    return 0;
  });

  if (direction == "desc") {
    clone.reverse();
  }

  GbGradeTable.instance.loadData(clone);
};

GbGradeTable.setScoreState = function(state, studentId, assignmentId) {
  var student = GbGradeTable.modelForStudent(studentId);

  if (!student.hasOwnProperty('scoreStatus')) {
    student.scoreStatus = {};
  }

  student.scoreStatus[assignmentId] = state;
};

GbGradeTable.getScoreState = function(studentId, assignmentId) {
  var student = GbGradeTable.modelForStudent(studentId);

  if (student.hasOwnProperty('scoreStatus')) {
    return student.scoreStatus[assignmentId];
  } else {
    return false;
  }
};
