//var longPollApp = angular.module('springChat.controllers', ['toaster','ngRoute','ngResource']);
var serverPrefix = "/crmChat";
springChat.controller('PollingController', ['$scope', '$http', '$interval','$timeout',
	function ($scope, $http, $interval, $timeout) {
        function updateRooms(data)
        {
            console.log("update rooms");
            $scope.rooms = data;
            $scope.roomsArray = Object.keys($scope.rooms)
            .map(function(key) {
                return $scope.rooms[key];
            });

            $scope.roomsCount = Object.keys($scope.rooms).length;
            //console.log($scope.rooms);
        }

		function poll(){
			console.log("poll()");
			 $http.post(serverPrefix+"/chat/rooms/user.login")
    .success(function(data, status, headers, config) {
        updateRooms(data);
        //console.log("resposnse data received:"+response.data);
        poll();
    }).error(function errorHandler(data, status, headers, config) {
    	//console.log("error during http request");
    	//$scope.topics = ["error"];
         //console.log("resposnse data error:"+response.data);
         poll();
    });
		
    }
console.log("test");
//$scope.topics=[112,'2313','3131'];
poll();


	}]);

