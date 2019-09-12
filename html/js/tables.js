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
		var htmlTable = '<div class="row usr-table-block">';
		htmlTable += '<div class="col-md-12 col-sm-6">';
		htmlTable += '<h3 style="float:left">'+tableName+'</h3><button type="button">Edit '+tableName+'</button>';
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
		htmlTable += '<button id="add-row-to-'+tableName.toLowerCase()+'" type="button" class="btn btn-primary add-row-btn">Add row</button>';
		htmlTable += '<button id="sbm-new-rows-'+tableName.toLowerCase()+'" style="margin-left: 4px; display: none;" type="button" class="btn btn-success">Submit</button>';
		htmlTable += '</div>';

		return htmlTable;
	}

	$('#create-table').on('click', function() {
		$('.modal-table-creation').show();
	});

	$('.add-row-btn').on('click', function() {
		var tableId = /add-row-to-(.*)/.exec($(this).attr('id'))[1];
		var lastRow = $("#"+tableId+"-tb tbody tr:last");
		var newRow = '<tr class="nrow">';
		$(lastRow).children("td").each(function (index, value) {
			newRow += '<td><input type="text" name="val"></td>';
		});
		newRow += '</tr>';
		$(newRow).insertAfter(lastRow);
		$("#"+tableId+"-tb tbody tr:last td:last").append('<button id="remove-new-row" style="float: right;">Remove</button>');
		$("#sbm-new-rows-"+tableId).show();
	});

	$('#table-creation-modal-close-button1, #table-creation-modal-close-button2').on('click', function() {
		$('.modal-table-creation').hide();
		$('.modal-table-creation .additional-field').remove();
		$('.modal-table-creation input').val('');
	});

	$('#add-field').on('click', function() {
		var el = '<div class="input-group nfield additional-field">';
		el += '<label>Type:\n';
		el += '<select name="f1-type">';
		el += '<option>INTEGER</option>';
		el += '<option>STRING</option>';
		el += '<option>FLOAT</option>';
		el += '</select></label>\n';
		el += '<label>Name:\n';
		el += '<input type="text" name="f1-name" placeholder="Val"></label>';
		el += '&nbsp;<button class="rem-field" type="button">Remove</button>';
		el += '</div>';
		$(el).insertBefore('.add-field-group');
	});

	$('.modal-body').on('click', '.rem-field', function() {
		$(this).parent().remove();
	});

	$('.usr-table-block').on('click', 'div table tbody tr td #remove-new-row', function() {
		if($(this).closest("tbody").children("tr.nrow").length <= 1) {
			$(this).closest(".usr-table-block").children("[id^='sbm-new-rows-']").hide();
		}
		$(this).parent().parent().remove();
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