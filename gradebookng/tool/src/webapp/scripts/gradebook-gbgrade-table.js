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
    item.setAttribute("title", title);
  }

  td.setAttribute('data-cell-initialised', row + ',' + col);
};

// FIXME: Hard-coded stuff here
GbGradeTable.renderTable = function (elementId, assignmentList, studentList, data) {

  GbGradeTable.students = studentList;
  GbGradeTable.assignments = assignmentList;
  GbGradeTable.grades = data;

  GbGradeTable.instance = new Handsontable(document.getElementById(elementId), {
    data: data,
    rowHeaders: studentList,
    rowHeaderWidth: 120,
    colHeaders: assignmentList.map(function (id) { return "Assignment #<" + id + ">"}),
    columns: assignmentList.map(function () { return {renderer: GbGradeTable.cellRenderer} }),
    colWidths: assignmentList.map(function () { return 230 }),
    autoRowSize: false,
    autoColSize: false,
    height: 600,
    width: $('#' + elementId).width() * 0.9,
  });

};
