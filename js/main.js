//import {Tabulator} from 'tabulator-tables';
//$(document).ready(function() {

		function checkConnection() {
			var s = document.forms["connform"]["server"].value;
			var p = document.forms["connform"]["port"].value;
			var ep = document.forms["connform"]["endpoint"].value;
			var user = document.forms["connform"]["userID"].value;
			var topic = document.forms["connform"]["topic"].value;
			//usa l'url specificato in back-end per richiedere la connessione al server sul giusto canale
			var url = "http://"+s+":"+p+"/"+ep+"?userID="+user+"&topic="+topic; 
			//tramite l'oggetto EventSource si mette in ascolto di event Emitters che gli faranno pervenire gli eventi
			var eventSource = new EventSource(url); 
			
			/* //non fa visualizzare in console il messaggio
			eventSource.onMessage = function(evt) {
				console.log(evt.data);
			}*/

			//'open' e 'error' sono evnti predefiniti, non vanno definiti nel back-end
			eventSource.addEventListener("open", (event) => {
				console.log('connection is live');
				$("#status").html("Connected");
				$("#status").css("background-color", "green");
			});

			//gestisce gli eventi dell'emitter chiamati 'latestNews' sulla parte di Back-End, il secondo parametro è una funzione di call-back 
			//che prende e gestisce il messaggio arrivato dall'evento del server, questo mi permette di poter gestire diversamente eventi di tipo diverso arrivati dal server
			eventSource.addEventListener("latestNews", function(event){
				console.log(event.lastEventId);
				var articleData = JSON.parse(event.data);
				
				//var t = document.createTextNode(articleData.title);
				//var listTop = document.createElement('li');
				//listTop.appendChild(t);
				//document.getElementById('subList').appendChild(listTop);
				addBlock(articleData.title, articleData.device, articleData.operation, articleData.fps, articleData.resolution);
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

			/* START */

			/*var tabledata = [
			    {id:1, name:"Oli Bob", progress:12, gender:"male", rating:1, col:"red", dob:"19/02/1984", car:1},
			    {id:2, name:"Mary May", progress:1, gender:"female", rating:2, col:"blue", dob:"14/05/1982", car:true},
			    {id:3, name:"Christine Lobowski", progress:42, gender:"female", rating:0, col:"green", dob:"22/05/1982", car:"true"},
			    {id:4, name:"Brendon Philips", progress:100, gender:"male", rating:1, col:"orange", dob:"01/08/1980"},
			    {id:5, name:"Margret Marmajuke", progress:16, gender:"female", rating:5, col:"yellow", dob:"31/01/1999"},
			    {id:6, name:"Frank Harbours", progress:38, gender:"male", rating:4, col:"red", dob:"12/05/1966", car:1},
			];
		
			var table = new Tabulator("#example-table", {
			    data:tabledata,           //load row data from array
			    layout:"fitColumns",      //fit columns to width of table
			    responsiveLayout:"hide",  //hide columns that dont fit on the table
			    tooltips:true,            //show tool tips on cells
			    addRowPos:"top",          //when adding a new row, add it to the top of the table
			    history:true,             //allow undo and redo actions on the table
			    pagination:"local",       //paginate the data
			    paginationSize:7,         //allow 7 rows per page of data
			    paginationCounter:"rows", //display count of paginated rows in footer
			    movableColumns:true,      //allow column order to be changed
			    initialSort:[             //set the initial sort order of the data
			        {column:"name", dir:"asc"},
			    ],
			    columns:[                 //define the table columns
			        {title:"Name", field:"name", editor:"input"},
			        {title:"Task Progress", field:"progress", hozAlign:"left", formatter:"progress", editor:true},
			        {title:"Gender", field:"gender", width:95, editor:"select", editorParams:{values:["male", "female"]}},
			        {title:"Rating", field:"rating", formatter:"star", hozAlign:"center", width:100, editor:true},
			        {title:"Color", field:"col", width:130, editor:"input"},
			        {title:"Date Of Birth", field:"dob", width:130, sorter:"date", hozAlign:"center"},
			        {title:"Driver", field:"car", width:90,  hozAlign:"center", formatter:"tickCross", sorter:"boolean", editor:true},
			    ],
			});*/

			/* FINISH */

			return false;
		}
		
		
	//});

	window.onBeforeunload = function() {
		eventSource.close();
	}

	function addBlock(title,device,operation,fps,resolution) {
		var a = document.createElement('article');
		var h = document.createElement('h3');
		var t = document.createTextNode(title);
		h.appendChild(t);
		var dev = document.createElement('span');
		dev.innerHTML = device;
		var op = document.createElement('span');
		op.innerHTML = operation;
		var fPs = document.createElement('span');
		fPs.innerHTML = fps;
		var res = document.createElement('span');
		res.innerHTML = resolution;
		a.appendChild(h);
		a.appendChild(dev);
		a.appendChild(op);
		a.appendChild(fPs);
		a.appendChild(res);
		document.getElementById('pack').appendChild(a);
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
/*
	
*/
	