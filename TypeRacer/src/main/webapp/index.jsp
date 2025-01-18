<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Room Manager</title>
    <link rel="stylesheet" href="styles2.css">
    <style>
        body {
             font-family: Arial, sans-serif;
            margin: 0;
            padding: 0;
            color: #333;
            background: url('https://i.pinimg.com/1200x/cb/9b/82/cb9b8203a3b7d327a909dc6336e90f27.jpg') no-repeat center center fixed; /* Replace 'background-image.jpg' with your image path */
            background-size: cover;
        }

        #add-btn {
            display: inline-block;
            padding: 10px 20px;
            margin: 20px auto;
            border: none;
            border-radius: 5px;
            background-color: #4caf50;
            color: white;
            font-size: 16px;
            cursor: pointer;
        }

        #add-btn:hover {
            background-color: #45a049;
        }

        #rooms {
            display: flex;
            flex-wrap: wrap;
            justify-content: center;
            gap: 20px;
            margin: 20px;
        }

        .room {
            padding: 20px;
            border-radius: 10px;
            background-color: #ffffff;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            cursor: pointer;
            transition: transform 0.2s, box-shadow 0.2s;
            text-align: center;
            font-weight: bold;
            font-size: 18px;
            min-width: 150px;
        }

        .room:hover {
            transform: scale(1.05);
            box-shadow: 0 8px 12px rgba(0, 0, 0, 0.2);
        }

        .room:active {
            transform: scale(1);
        }
    </style>
    </head>
<body>
	<div style="text-align: center; margin-top: 20px;">
        <h1>Welcome</h1>
    </div>
    <div style="text-align: center;">
        <button id="add-btn">Add Room</button>
    </div>
    <div id="rooms"></div>


	<script type="text/javascript">
		
		/*It will loaded on the page loaded*/
		document.addEventListener('DOMContentLoaded', function(){
			
			const loggedInUser = localStorage.getItem('loggedInUser')
			
			if(!loggedInUser){
				windows.location.href = 'playRoom.html';
			}
			else{
				loadRooms();
			}
			
		});
		
		/*
		When click the 'Add Room' button it will ask for the room id and password
		and store it in the browser's local storage
		*/
		document.getElementById('add-btn').addEventListener('click', function(){
			
			let roomName = prompt('Enter the Room Name');
			
			if(roomName){
				let roomPass = prompt('Enter the Password')
				
				if(roomPass){
					let rooms = JSON.parse(localStorage.getItem('rooms')) || []
					rooms.push({name: roomName ,password:roomPass})
					localStorage.setItem('rooms',JSON.stringify(rooms))
					displayRooms(rooms)
				}
			}
			
		});
		
		function loadRooms(){
			const rooms = JSON.parse(localStorage.getItem('rooms')) || []
			displayRooms(rooms)
		}
		
		/* 
		It will display the all room and when enters the password it will 
		check it
		*/
		function displayRooms(rooms){
			const roomContainer = document.getElementById('rooms');
			
			 // Clear the container before displaying rooms
		    roomContainer.innerHTML = '';
			
			rooms.forEach(room => {
				let roomDiv = document.createElement('div')
				roomDiv.classList.add('room')
				roomDiv.textContent = room.name
				roomDiv.dataset.password = room.password
				
				roomDiv.addEventListener('click', function(){
					let enteredPassword = prompt('Enter the password for '+room.name+' : ')
					 console.log(enteredPassword)
					if(enteredPassword===room.password){
						localStorage.setItem('currentRoom',room.name)
						window.location.href = 'home.html'
					}
					else{
						alert('Incorrect Password!')
					}
					
				});
				
				roomContainer.appendChild(roomDiv);
			});
		}
	
	
	</script>


</body>
</html>
