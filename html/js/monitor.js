$(function() {
	$.ajax({
    url: 'http://localhost:3636/monitor_data?key=vjsYeNp4ZsGtPcHLz4AfghqMkTPwCjA4',
	crossDomain: true,
	xhrFields: {
        withCredentials: true
    },
    complete: function(jqXHR, textStatus) {
        if (textStatus == 'success') {
            //console.log('Успешно.');
        }
        if (textStatus == 'error') {
            //console.log('Ошибка.');
        }
    }, 
	success: function(data){
        //console.log( "Прибыли данные: " + data );
		var data = JSON.parse(data);
		buildTable($("#boosts"), data.boosts);
		buildTable($("#game_owners"), data.game_owners);
		buildTable($("#games"), data.games);
		buildTable($("#players"), data.players);
		buildTable($("#saves"), data.saves);
    }
	});
	
	function buildTable(tableElement, table) {
		
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