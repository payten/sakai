GbGradeTable = {};

GbGradeTable.unpack = function (s, rowCount, columnCount) {
  var blob = atob(s);

  // Our result will be an array of Float64Array rows
  var result = [];

  // The byte from our blob we're currently working on
  var readIndex = 0;

  for (var row = 0; row < rowCount; row++) {
    var writeIndex = 0;
    var currentRow = new Float64Array(columnCount);

    for (var column = 0; column < columnCount; column++) {
      if (blob[readIndex].charCodeAt() & 128) {
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


SAMPLE_CELL = '<div role="gridcell" tabindex="0" class="gb-grade-item-cell" data-assignmentid="%{ASSIGNMENT_ID}" data-studentuuid="%{STUDENT_UUID}" aria-readonly="false"><input type="text" tabindex="-1" class="gb-editable-grade"  value="%{GRADE}"><a class="btn btn-sm btn-default dropdown-toggle" title="%{TITLE}" data-toggle="dropdown" href="#" role="button" aria-haspopup="true" "> <span class="caret"></span> </a></div>';

GbGradeTable.cellRenderer = function (instance, td, row, col, prop, value, cellProperties) {
  var wasInitialised = td.getAttribute('data-cell-initialised');

  if (wasInitialised === (row + ',' + col)) {
    // Nothing to do
    return;
  }

  var assignmentId = GbGradeTable.assignments[col];
  var studentId = GbGradeTable.students[row];
  var grade = ('' + GbGradeTable.grades[row][col]);
  var title = "Open menu for student " + GbGradeTable.students[row] + " and assignment " + GbGradeTable.assignments[col] + " cell";

  $(td).data("assignmentId", assignmentId);
  $(td).data("studentId", studentId);
  td.innerHTML = value;
  return;

  if (!wasInitialised) {
    // First time we've initialised this cell.
    var html = SAMPLE_CELL;
    html = html.replace('%{ASSIGNMENT_ID}', assignmentId);
    html = html.replace('%{STUDENT_UUID}', studentId);
    html = html.replace('%{GRADE}', grade);
    html = html.replace('%{TITLE}', title);

    td.innerHTML = html;
  } else if (wasInitialised != (row + ',' + col)) {
    // This cell was previously holding a different value.  Just patch it.
    var item = td.getElementsByClassName("gb-grade-item-cell")[0];
    item.setAttribute("data-assignmentid", assignmentId);
    item.setAttribute("data-studentuuid", studentId);

    var input = td.getElementsByClassName("gb-editable-grade")[0];
    input.value = grade;
    input.setAttribute("value", grade);

    var dropdown = td.getElementsByClassName("dropdown-toggle")[0];
    dropdown.setAttribute("title", title);
  }

  td.setAttribute('data-cell-initialised', row + ',' + col);
};


SAMPLE_HEADER_CELL = '<div class="gb-title">%{ASSIGNMENT_NAME}</div><div class="gb-grade-section">Total: <span class="gb-total-points" data-outof-label="/10">10</span></div><div class="gb-due-date">Due: <span>-</span></div><div class="gb-grade-item-flags"><span class="gb-flag-is-released"></span><span class="gb-flag-not-counted"</span></div><div class="btn-group"><a class="btn btn-sm btn-default dropdown-toggle" data-toggle="dropdown" href="#" role="button" aria-haspopup="true" title="Open menu for %{ASSIGNMENT_NAME}"><span class="caret"></span></a></div></div>';

GbGradeTable.headerRenderer = function (col) {
  var html = SAMPLE_HEADER_CELL;
  html = html.replace(/\%\{ASSIGNMENT_NAME\}/g, "Assignment #<" + GbGradeTable.assignments[col] + ">");

  return html;
};


// FIXME: Hard-coded stuff here
GbGradeTable.renderTable = function (elementId, assignmentList, studentList, data) {


  GbGradeTableEditor = Handsontable.editors.TextEditor.prototype.extend();

  GbGradeTableEditor.prototype.createElements = function () {
    Handsontable.editors.TextEditor.prototype.createElements.apply(this, arguments);
    // add 'out-of' label
    var outOf = "<span class='out-of'>/10</span>";
    $(this.TEXTAREA_PARENT).append(outOf);
  };

  GbGradeTableEditor.prototype.beginEditing = function() {
    Handsontable.editors.TextEditor.prototype.beginEditing.apply(this, arguments);
    if ($(this.TEXTAREA).val().length > 0) {
      $(this.TEXTAREA).select();
    }
  };

  GbGradeTableEditor.prototype.saveValue = function() {
    Handsontable.editors.TextEditor.prototype.saveValue.apply(this, arguments);
    console.log("-- SAVING --");
    console.log("value: " + $(this.TEXTAREA).val());
    console.log("studentId: " + $(this.TD).data("studentId"));
    console.log("assignmentId: " + $(this.TD).data("assignmentId"));
    // TODO ajax post and add notifications to this.TD for success/error
  }

  GbGradeTable.students = studentList;
  GbGradeTable.assignments = assignmentList;
  GbGradeTable.grades = data;

  GbGradeTable.instance = new Handsontable(document.getElementById(elementId), {
    data: data,
    rowHeaders: studentList,
    rowHeaderWidth: 120,
    rowHeaders: studentList,
    colHeaders: GbGradeTable.headerRenderer,
    columns: assignmentList.map(function () {
      return {
        renderer: GbGradeTable.cellRenderer,
        editor: GbGradeTableEditor
      };
    }),
    colWidths: assignmentList.map(function () { return 230 }),
    autoRowSize: false,
    autoColSize: false,
    height: 600,
    width: $('#' + elementId).width() * 0.9,
    fillHandle: false,
    afterGetRowHeader: function(row,th) {
      $(th).
        attr("role", "rowheader").
        attr("scope", "row");
    },
    afterGetColHeader: function(col, th) {
      var name = "Assignment #<" + col + ">"
      $(th).
        attr("role", "columnheader").
        attr("scope", "col").
        attr("abbr", name).
        attr("aria-label", name);
      
    }
  });

};