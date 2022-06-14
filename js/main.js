//import {Tabulator} from 'tabulator-tables';

var table;

	/* START TABULATOR*/

$(document).ready(function() {
	var tabledata = [
	    //{id:1, title:"floud/Dev001", device:"Dev001", operation:" ", fps:"60", resolution:"120x60"},
	];

    table = new Tabulator("#example-table", {
    data:tabledata,           //load row data from array
    layout:"fitColumns",      //fit columns to width of table
    //responsiveLayout:"hide",  //hide columns that dont fit on the table
    //tooltips:true,            //show tool tips on cells
    addRowPos:"top",          //when adding a new row, add it to the top of the table
    //history:true,             //allow undo and redo actions on the table
    pagination:"local",       //paginate the data
    paginationSize:10,         //allow 7 rows per page of data
    //paginationCounter:"rows", //display count of paginated rows in footer
    //movableColumns:true,      //allow column order to be changed
    
    /*initialSort:[             //set the initial sort order of the data
        {column:"id", dir:"asc"},
    ],*/
    columns:[                 //define the table columns
        {title:"Topic", field:"title"},
        {title:"Device", field:"device"},
        {title:"Operation", field:"operation"},
        {title:"FPS", field:"fps"},
        {title:"Resolution", field:"resolution"},
        {title:"Sec since last", field:"tslm"},
    ],
	});

});

	/* FINISH */

	function checkConnection() {
		//var s = document.forms["connform"]["server"].value;
		var s = "localhost";
		//var p = document.forms["connform"]["port"].value;
		var p = "6033";
		//var ep = document.forms["connform"]["endpoint"].value;
		var ep = "subscribe";
		var user = document.forms["connform"]["userID"].value;
		var topic = document.forms["connform"]["topic"].value;
		
		if(topic.includes("#"))
	 		topic = topic.replace('#','*'); //perchè Eventsource non riconosce il char '#' nell'url che usa per la chiamata
		console.log(topic);

		//usa l'url specificato in back-end per richiedere la connessione al server sul giusto canale
		var url = "http://"+s+":"+p+"/"+ep+"?userID="+user+"&topic="+topic; 
		//tramite l'oggetto EventSource si mette in ascolto di event Emitters che gli faranno pervenire gli eventi
		var eventSource = new EventSource(url);

		eventSource.addEventListener("open", (event) => {
			console.log('connection is live');
			$("#status").html("Connected");
			$("#status").css("background-color", "green");
		});

		//gestisce gli eventi dell'emitter chiamati 'latestNews' sulla parte di Back-End, il secondo parametro è una funzione di call-back 
		//che prende e gestisce il messaggio arrivato dall'evento del server, questo mi permette di poter gestire diversamente eventi di tipo diverso arrivati dal server
		eventSource.addEventListener("diagnosys", function(event){
			console.log(event.lastEventId);
			table.addData(event.data, true); //aggiunge la nuova riga sulla tabella, true indica che la aggiunge in testa alla tabella
		});

		eventSource.addEventListener("INIT", (event) => {
			var jsonParsedData = JSON.parse(event.data);
			var t = document.createTextNode(jsonParsedData.topic);
			var listTop = document.createElement('li');
			listTop.id = 'id-'+jsonParsedData.topic;
			listTop.className = 'subscription';
			listTop.appendChild(t);
			document.getElementById('subList').appendChild(listTop);
		});

		//‘error’ event will be called whenever there is a network error 
		//and also when the server closes the connection by calling a 'complete’ or ‘completeWithError’ method on the emitter.
		eventSource.addEventListener("error", function(event){
			console.log("Error: " + event.currentTarget.readyState);
			if (event.currentTarget.readyState == EventSource.CLOSED) {
				console.log('eventSource.CLOSED');
			}
			else{
				$("#status").html("Disconnected");
				$("#status").css("background-color", "red");
				const topics = document.querySelectorAll('.subscription');
				topics.forEach(topic => {
				  topic.remove();
				});

				eventSource.close();
			}
		});

		return false;
	}

window.onBeforeunload = function() {
	eventSource.close();
}

const unsubscribeTopic = async () => {
	var user = document.forms["connform"]["userID"].value;
	var topic = document.forms["connform"]["topic"].value;
	//TODO Togliere dalla lista delle iscrizioni il topic 
	//console.log("id-"+topic)
	const topicToRemove = document.getElementById("id-"+topic);
	topicToRemove.remove();
	const response = await fetch('http://localhost:6033/unsubscribe', {
	    method: 'POST',
	    body: 
	    '{user: ' + user + ', topic : ' + topic + '}', 
	    headers: {
	      'Content-Type': 'application/json'
	    }
	  });
  	//const myJson = await response.json(); //extract JSON from the http response
  	// do something with myJson
  	if (response.status != 200)
  		console.log(error);
}
