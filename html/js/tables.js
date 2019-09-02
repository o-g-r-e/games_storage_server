$(function() {
	$.ajax({
    url: 'http://localhost:3636/get_usr_tables?key=vjsYeNp4ZsGtPcHLz4AfghqMkTPwCjA4',
	crossDomain: true,
	xhrFields: {
        withCredentials: true
    },
    complete: function(jqXHR, textStatus) {
        if (textStatus == 'success') {
            
        }
        if (textStatus == 'error') {
            
        }
    }, 
	success: function(data){
		var data = JSON.parse(data);
		/*buildTableRows($("#boosts"), data.boosts);
		buildTableRows($("#game_owners"), data.game_owners);
		buildTableRows($("#games"), data.games);
		buildTableRows($("#players"), data.players);
		buildTableRows($("#saves"), data.saves);*/
		
    }
	});

	var htmlTable = buildTable("T1", {head:["field1", "field2", "field3"], body:[["value1", "value2", "value3"],["value1", "value2", "value3"]]});
	$(htmlTable).appendTo('.container');

	function buildTable(tableName, tableData) {
		var htmlTable = '<div class="row">';
		htmlTable += '<div class="col-md-12 col-sm-6">';
		htmlTable += '<h3>'+tableName+'</h3>';
		htmlTable += '<table id="'+tableName.toLowerCase()+'-tb" class="table">';
		htmlTable += '<thead>';
		htmlTable += '<tr>';
		
		$.each(tableData.head, function( index, value ) {
			htmlTable += '<th>'+value+'</th>';
		});

		htmlTable += '</tr>';
		htmlTable += '</thead>';
		htmlTable += '<tbody>';

		$.each(tableData.body, function( index, value ) {
			htmlTable += '<tr>';
			$.each(value, function( index, value ) {
				htmlTable += '<td>'+value+'</td>';
			});
			htmlTable += '</tr>';
		});
		
		htmlTable += '</tbody>';
		htmlTable += '</table>';
		htmlTable += '</div>';
		htmlTable += '<button id="add-row-to-'+tableName.toLowerCase()+'" type="button" class="btn btn-primary">Add row</button>';
		htmlTable += '</div>';

		return htmlTable;
	}

	$('#create-table').on('click', function() {
		$('.modal-table-creation').show();
	});

	$('#table-creation-modal-close-button1, #table-creation-modal-close-button2').on('click', function() {
		$('.modal-table-creation').hide();
	});

	$('#add-field').on('click', function() {
		var el = '<div class="input-group">';
		el += '<label for="sel1">Type:</label>';
		el += '<select class="form-control" id="sel1">';
		el += '<option>INTEGER</option>';
		el += '<option>STRING</option>';
		el += '<option>FLOAT</option>';
		el += '</select>';
		el += '<input type="text" class="form-control" name="val" placeholder="Val">';
		el += '</div>';
		$(el).insertBefore('.add-field-group');
	});
	
	function buildTableRows(tableElement, table) {
		
		var htmlRow = "";
		$.each(table, function( index, value ) {
			htmlRow += "<tr>";
			$.each(value, function( index, value ) {
				if(index == 0) {
					htmlRow += "<th scope=\"row\">"+value+"</th>";
				} else {
					htmlRow += "<td>"+value+"</td>";
				}
			});
			htmlRow += "</tr>";
		});
		
		tableElement.find("tbody").append(htmlRow);
	}
});