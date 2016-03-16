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


SAMPLE_CELL = '<div role="gridcell" tabindex="0" class="gb-grade-item-cell" data-assignmentid="%{ASSIGNMENT_ID}" data-studentuuid="%{STUDENT_UUID}" aria-readonly="false"><div><input type="text" tabindex="-1" class="gb-editable-grade" name="table:body:rows:1:cells:4:cell:editableGrade" id="editableGradef" value="%{GRADE}"><!-- dropdown menu --><div class="btn-group"> <a class="btn btn-sm btn-default dropdown-toggle" title="%{TITLE}" data-toggle="dropdown" href="#" role="button" aria-haspopup="true" "> <span class="caret"></span> </a> <ul class="dropdown-menu dropdown-menu-right" role="menu"> <li><a href="javascript:;" role="menuitem" id="viewGradeLog11">Grade Log</a></li><li><a href="javascript:;" role="menuitem" class="gb-edit-comments" id="editGradeComment12"><span>Add Comment</span></a></li> </ul> </div></div></div>';

GbGradeTable.cellRenderer = function (instance, td, row, col, prop, value, cellProperties) {
  var wasInitialised = td.getAttribute('data-cell-initialised');

  if (!wasInitialised) {
    td.setAttribute('data-cell-initialised', '1');

    var $td = $(td);
    // First time we've initialised this cell.
    var html = SAMPLE_CELL;
    html = html.replace('%{ASSIGNMENT_ID}', '27');
    html = html.replace('%{STUDENT_UUID}', 'dbf3ef87-4aa4-48cf-b900-3e7bcef79d8f');
    html = html.replace('%{GRADE}', ('' + GbGradeTable.grades[row][col]));
    html = html.replace('%{TITLE}', "Open menu for student (whoever) and assignment (whatever) cell");

    $td.html(html);
  }
};

// FIXME: Hard-coded stuff here
GbGradeTable.renderTable = function (elementId, assignmentList, studentList, data) {
  GbGradeTable.grades = data;
  GbGradeTable.instance = new Handsontable(document.getElementById(elementId), {
    data: data,
    rowHeaders: studentList,
    rowHeaderWidth: 120,
    colHeaders: assignmentList,
    columns: assignmentList.map(function () { return {renderer: GbGradeTable.cellRenderer} }),
    colWidths: assignmentList.map(function () { return 200 }),
    autoRowSize: false,
    autoColSize: false,
    height: 600,
    width: 1200,
  });

};
