springChatControllers.controller('ChatRouteController',['$routeParams','$rootScope','$scope', '$http', '$location', '$interval','$cookies','$timeout','toaster', 'ChatSocket', '$cookieStore','Scopes',function($routeParams,$rootScope,$scope, $http, $location, $interval,$cookies,$timeout, toaster, chatSocket, $cookieStore,Scopes) {

	function messageError(){
		toaster.pop('error', "Error","server request timeout",1000);
	}
	function messageError(mess){
		toaster.pop('error', "Error", mess, 1000);
	}
	
	Scopes.store('ChatRouteController', $scope);
	var chatControllerScope = Scopes.get('ChatController');
	var lastRoomBindings = [];

	$scope.messages     = [];
	$scope.participants = [];
	$scope.roomType = -1;
	$scope.ajaxRequestsForRoomLP     = [];
	$scope.newMessage   = '';
	$scope.uploadProgress = 0;
	$scope.message_busy = true;

	$rootScope.$on("login", function (event, chatUserId) {
		for(var index in $scope.participants) {
			if($scope.participants[index].chatUserId == chatUserId) {
				$scope.participants[index].online = true;
				break;
			}
		}
	});

	$rootScope.$on("logout", function (event, chatUserId) {
		for(var index in $scope.participants) {
			if($scope.participants[index].chatUserId == chatUserId) {
				$scope.participants[index].online = false;
				break;
			}
		}
	});


	$scope.goToDialog = function(roomId) {
		//console.log("roomName:"+roomName);
		if (chatControllerScope.currentRoom!==undefined && getRoomById($scope.rooms,chatControllerScope.currentRoom) !== undefined )
			getRoomById($scope.rooms,chatControllerScope.currentRoom.roomId).date = curentDateInJavaFromat();

		goToDialogEvn(roomId);
	};

	$scope.goToDialogById = function(roomId) {
		console.log("roomId:" + roomId);
		goToDialogEvn(roomId);

		if (chatControllerScope.currentRoom!==undefined && getRoomById($scope.rooms,chatControllerScope.currentRoom) !== undefined )
			getRoomById($scope.rooms,chatControllerScope.currentRoom.roomId).date = curentDateInJavaFromat();

		$scope.templateName = 'chatTemplate.html';
		$scope.dialogName = "private";


	};

	function goToDialogEvn(id)
	{
		console.log("goToDialogEvn("+id+")");
		chatControllerScope.currentRoom = {roomId:id};
		$scope.changeRoom();

		var room = getRoomById($scope.rooms,id);
		if (room!=undefined)
		{
			chatControllerScope.currentRoom = room;
			//$scope.$apply();
			room.nums = 0;
			$scope.dialogName = room.string;
		}

		if ($rootScope.socketSupport){
			chatSocket.send("/app/chat.go.to.dialog/{0}".format(chatControllerScope.currentRoom.roomId), {}, JSON.stringify({}));	
		}
		else
		{
			$http.post(serverPrefix + "/chat.go.to.dialog/{0}".format(chatControllerScope.currentRoom.roomId));
		}
	}


	/*************************************
	 * CHANGE ROOM
	 *************************************/
	$scope.changeRoom=function(){
		debugger;
		$scope.messages=[];
		console.log("roomId:"+chatControllerScope.currentRoom.roomId);
		room=chatControllerScope.currentRoom.roomId+'/';

		//debugger;
		if($rootScope.socketSupport == true)
		{
			lastRoomBindings.push(
					chatSocket.subscribe("/topic/{0}chat.message".format(room), function(message) 
							{
						$scope.messages.unshift(JSON.parse(message.body));

							}));

			lastRoomBindings.push(chatSocket.subscribe("/app/{0}chat.participants".format(room), function(message) 
					{
				var o = JSON.parse(message.body);
				loadSubscribeAndMessage(o);
					}));

			lastRoomBindings.push(chatSocket.subscribe("/topic/{0}chat.participants".format(room), function(message) 
					{
				var o = JSON.parse(message.body);
				console.log("!!!!!!!!!!!!!!!!!!!!!!!!!!!			" );
				console.log(o);
				$scope.participants = o["participants"];
					}));
		}
		else
		{
			console.log('subscribeMessageAndParticipants');
			subscribeMessageLP();//@LP@
			subscribeParticipantsLP();
			loadSubscribeAndMessageLP();
		}	

		lastRoomBindings.push(
				chatSocket.subscribe("/topic/{0}chat.typing".format(room), function(message) {
					var parsed = JSON.parse(message.body);
					if(parsed.username == $scope.chatUserId) return;
					//$scope.participants[parsed.username].typing = parsed.typing;
					for(var index in $scope.participants) {
						var participant = $scope.participants[index];

						if(participant.chatUserId == parsed.username) {
							$scope.participants[index].typing = parsed.typing;
							//break;
						}
					} 
				}));


		//chatSocket.send("/topic/{0}chat.participants".format(room), {}, JSON.stringify({}));
	}

	$scope.addUserToRoom=function(){
		chatControllerScope.userAddedToRoom = false;
		room=$scope.currentRoom.roomId+'/';
		if($rootScope.socketSupport === true)
		{
			chatSocket.send("/app/chat/rooms.{0}/user.add.{1}".format($scope.currentRoom.roomId,$scope.searchInputValue.email), {}, JSON.stringify({}));
			var myFunc = function(){
				if (angular.isDefined(addingUserToRoom))
				{
					$timeout.cancel(addingUserToRoom);
					addingUserToRoom=undefined;
				}
				if (chatControllerScope.userAddedToRoom) return;
				toaster.pop('error', "Error","server request timeout",1000);
				chatControllerScope.userAddedToRoom = true;

			};
			addingUserToRoom = $timeout(myFunc,6000);
		}
		else
		{
			console.log("$scope.searchInputValue:"+$scope.searchInputValue);
			$http.post(serverPrefix + "/chat/rooms.{0}/user.add.{1}".format($scope.currentRoom.roomId,$scope.searchInputValue.email), {}).
			success(function(data, status, headers, config) {
				console.log("ADD USER OK " + data);
				chatControllerScope.userAddedToRoom = true;
			}).
			error(function(data, status, headers, config) {
				chatControllerScope.userAddedToRoom = true;
			});
		}
		$scope.searchInputValue.email = '';
	}
	/*************************************
	 * UPDATE MESSAGE LP
	 *************************************/
	function subscribeMessageLP(){
		var currentUrl = serverPrefix + "/{0}/chat/message/update".format($scope.currentRoom.roomId);
		console.log("subscribeMessageLP()");
		$scope.ajaxRequestsForRoomLP.push(
				$.ajax({
					type: "POST",
					url: currentUrl,
					success: function(data){
						var parsedData = JSON.parse(data);
						for(var index=0; index < parsedData.length; index++) { 
							if(parsedData[index].hasOwnProperty("message")){
								$scope.messages.unshift(parsedData[index])
								console.log("subscribeMessageLP success:"+parsedData[index]);
							}
						}
						$scope.$apply();
						subscribeMessageLP();			
					},
					error: function(xhr, text_status, error_thrown){
						//if (text_status == "abort")return;
						if (xhr.status === 0 || xhr.readyState === 0) {
							return;}
						subscribeMessageLP();    	
					}
				}));
	}

	function subscribeParticipantsLP(){
		var currentUrl = serverPrefix + "/{0}/chat/participants/update".format($scope.currentRoom.roomId)
		$scope.ajaxRequestsForRoomLP.push(
				$.ajax({
					type: "POST",
					url: currentUrl,
					success: function(data){
						console.log("subscribeParticipantsLP:"+data)
						subscribeParticipantsLP();
						var parsedData = JSON.parse(data);
						if(parsedData.hasOwnProperty("participants"))
							$scope.participants = parsedData["participants"];
						$scope.$apply();

					},
					error: function(xhr, text_status, error_thrown){
						if (xhr.status === 0 || xhr.readyState === 0) return;
						subscribeParticipantsLP();

					}


				}));
	};

	/*************************************
	 * SEND MESSAGE
	 *************************************/
	$scope.sendMessage = function(message,attaches) {
		var textOfMessage;
		if (message===undefined)textOfMessage = $scope.newMessage;
		else textOfMessage = message;

		var destination = "/app/{0}/chat.message".format(chatControllerScope.currentRoom.roomId);
		chatControllerScope.messageSended = false;
		if($rootScope.socketSupport == true)
		{
			if($scope.sendTo != "everyone") {
				destination = "/app/{0}chat.private.".format(chatControllerScope.currentRoom.roomId) + $scope.sendTo;
				$scope.messages.unshift({message: textOfMessage, username: 'you', priv: true, to: $scope.sendTo});
			}
			chatSocket.send(destination, {}, JSON.stringify({message: textOfMessage, username:$scope.chatUserNickname,attachedFiles:attaches}));


			var myFunc = function(){
				if (angular.isDefined(sendingMessage))
				{
					$timeout.cancel(sendingMessage);
					sendingMessage=undefined;
				}
				if (chatControllerScope.messageSended) return;
				messageError();
				chatControllerScope.messageSended = true;

			};
			sendingMessage = $timeout(myFunc,2000);
		}
		else
		{

			$http.post(serverPrefix + "/{0}/chat/message".format(chatControllerScope.currentRoom.roomId), {message: textOfMessage, username:$scope.chatUserNickname}).
			success(function(data, status, headers, config) {
				console.log("MESSAGE SEND OK " + data);
				chatControllerScope.messageSended = true;
			}).
			error(function(data, status, headers, config) {
				messageError();
				chatControllerScope.messageSended = true;
			});
		};
		if (message===undefined)
			$scope.newMessage = '';

	}
	/*************************************
	 * LOAD MESSAGE LP
	 *************************************/

	function loadSubscribeAndMessage(message)
	{
		$scope.participants = message["participants"];
		$scope.roomType = message["type"];
		for (var i=0; i< message["messages"].length;i++){
			$scope.messages.unshift(message["messages"][i]);
			//$scope.messages.unshift(JSON.parse(o["messages"][i].text));
		}
		$scope.message_busy = false;
	}

	function loadSubscribesOnly(message)
	{
		$scope.participants = message["participants"];
		$scope.roomType = message["type"];
	}
	function loadMessagesOnly(message)
	{
		$scope.roomType = message["type"];
		for (var i=0; i< message["messages"].length;i++){
			$scope.messages.unshift(message["messages"][i]);
			//$scope.messages.unshift(JSON.parse(o["messages"][i].text));
		}
	}

	function loadSubscribeAndMessageLP(){
		$http.post(serverPrefix + "/{0}/chat/participants_and_messages".format(chatControllerScope.currentRoom.roomId), {}).
		success(function(data, status, headers, config) {
			console.log("MESSAGE SEND OK " + data);
			loadSubscribeAndMessage(data);
		}).
		error(function(data, status, headers, config) {

		});
	}
	$scope.loadOtherMessages = function () {
		$scope.message_busy = true;
		console.log("TRY " + $scope.messages.length);
		$http.post(serverPrefix + "/{0}/chat/loadOtherMessage".format(chatControllerScope.currentRoom.roomId), $scope.messages[$scope.messages.length - 1]).
		success(function(data, status, headers, config) {
			console.log("MESSAGE onLOAD OK " + data);
			if(data == "")
				return;
			
			for(var index=0; index < data.length; index++) { 
				if(data[index].hasOwnProperty("message")){
					$scope.messages.push(data[index]);
				}
			}
			$scope.message_busy = false;
		}).
		error(function(data, status, headers, config) {
			
			//messageError("no other message");
		});
	}

	// file upload button click reaction
	angular.element( document.querySelector( '#upload_file_form' ) ).context.onsubmit = function() {
		var input = this.elements.myfile;
		var files =[];
		for( var i = 0 ; i < input.files.length;i++) files.push(input.files[i]);
		debugger;
		if (files) {
			uploadXhr(files,"upload_file/"+$scope.currentRoom.roomId,
					function successCallback(data){    
				$scope.uploadProgress = 0;
				$scope.sendMessage("я отправил вам файл", JSON.parse(data));
				$scope.$apply();
			},
			function(xhr) {
				$scope.uploadProgress = 0;
				$scope.$apply();
				alert("SEND FAILD");
			},
			function(event, loaded) {
				console.log(event.loaded + ' / ' + event.totalSize);
				$scope.uploadProgress = Math.floor((event.loaded/event.totalSize)*100);
				$scope.$apply();

			}) ;
		}
		return false;
	}
	$scope.getNameFromUrl=function getNameFromUrl(url){
		var fileNameSignaturePrefix = "file_name=";
		var startPos = url.lastIndexOf(fileNameSignaturePrefix) + fileNameSignaturePrefix.length;
		var endPos= url.length - DEFAULT_FILE_PREFIX_LENGTH;
		return url.substring(startPos,endPos);
	}

	$scope.checkUserAdditionPermission = function(){
		if (typeof chatControllerScope.currentRoom === "undefined")return false;
		var resultOfChecking = ($scope.roomType == 0) && (chatControllerScope.chatUserId==chatControllerScope.currentRoom.roomAuthorId) && chatControllerScope.isMyRoom;
		return resultOfChecking;
	}
	/*
	 * 
	 */

	if(isInited == true)
		$scope.goToDialog($routeParams.roomId);

	chatControllerScope.currentRoom.roomId = $routeParams.roomId;

	$scope.pageClass = 'page-about';

	/*
	 * close event
	 */
	$scope.$on('$locationChangeStart', function( event ) {
		var isLastRoomBindingsEmpty = lastRoomBindings==undefined || lastRoomBindings.length == 0;
		if ( !isLastRoomBindingsEmpty ) {

			while (lastRoomBindings.length>0)
			{
				var subscription = lastRoomBindings.pop();
				//if (subscription!=undefined)
				subscription.unsubscribe();
			}
		}


		while ($scope.ajaxRequestsForRoomLP.length>0)
		{

			var subscription = $scope.ajaxRequestsForRoomLP.pop();
			console.log("cancel ajaxRequestsForRoomLP:"+subscription);
			subscription.abort();
		}
		/* var answer = confirm("Are you sure you want to leave this page?")
	    if (!answer) {
	        event.preventDefault();
	    }*/
	});

}]);