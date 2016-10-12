springChatControllers.controller('ChatRouteController', ['$routeParams', '$rootScope', '$scope', '$http', '$location', '$interval', '$cookies', '$timeout', 'toaster', 'ChatSocket', '$cookieStore', 'Scopes', '$q', '$controller', function($routeParams, $rootScope, $scope, $http, $location, $interval, $cookies, $timeout, toaster, chatSocket, $cookieStore, Scopes, $q, $controller) {
    angular.extend(this, $controller('ChatRouteInterface', { $scope: $scope }));
    /*
     * 
     */
    $scope.controllerName = "ChatRouteController";
    var chatControllerScope = Scopes.get('ChatController');

    $rootScope.$watch('isInited', function() {
        console.log("try " + chatControllerScope.currentRoom);
        if ($rootScope.isInited == true) {

            var room = getRoomById($scope.rooms, $routeParams.roomId);

            if (room != null && room.type == 2 && $scope.controllerName != "ConsultationController") //redirect to consultation
            {
                $http.post(serverPrefix + "/chat/consultation/fromRoom/" + room.roomId)
                    .success(function(data, status, headers, config) {
                        if (data == "" || data == undefined)
                            $rootScope.goToAuthorize(); //not found => go out
                        else
                            $location.path("consultation_view/" + data);
                    }).error(function errorHandler(data, status, headers, config) {
                        $rootScope.goToAuthorize(); //not found => go out
                    });
                return;
            }

            $scope.goToDialog($routeParams.roomId).then(function(data) {
                if (data != undefined && data != null) {
                    chatControllerScope.currentRoom = data.data;
                    $scope.dialogName = chatControllerScope.currentRoom.string;
                }
                $scope.pageClass = 'scale-fade-in';
            }, function() {
                $rootScope.goToAuthorize();
                toaster.pop('warning', errorMsgTitleNotFound, errorMsgContentNotFound, 5000);
                // location.reload();
            });


            $http.post(serverPrefix + "/bot_operations/tenant/did_am_wait_tenant/{0}".format(chatControllerScope.currentRoom.roomId)).
            success(function(data, status, headers, config) {
                if (data == true)
                    $scope.showToasterWaitFreeTenant();
            }).
            error(function(data, status, headers, config) {
                alert("did_am_wait_tenant: server error")
            });
        }

    });
}]);
