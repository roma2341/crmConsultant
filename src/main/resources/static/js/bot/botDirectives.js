'use strict';
angular.module('springChat.directives').directive('botContainer', function($compile, $parse) {
	    return {
        controller: 'ChatViewItemController',
        link: function(scope, element, attr, ctrl) {
            scope.$watch(attr.content, function() {

                element.html($parse(attr.content)(scope));
                if (typeof attr.callback != 'undefined') {
                    var callBackFunction = new Function("return " + attr.callback)();
                    if (typeof callBackFunction != 'undefined')
                        callBackFunction(element);
                }
                scope.content = $parse(attr.content)(scope);
                $compile(element.contents())(scope);
            }, true);
        }
    }
});

/*

angular.module('springChat.directives').directive('botContainer', function($compile, $parse) {
    return {
        controller: 'ChatViewItemController',
        link: function(scope, element, attr, ctrl) {
            scope.$watch(attr.content, function() {

                var html = "<span>fsdfsdfsdfds</span>           <div class=\"btn-group\" role=\"group\" aria-label=\"...\">  <button type=\"button\" class=\"btn btn-default\">Left</button>  <button type=\"button\" class=\"btn btn-default\">Middle</button>  <button type=\"button\" class=\"btn btn-default\">Right</button> </div>                <span>fsdfsdfsdfds3</span>                <span>fsdfsdfsdfds4</span>"
                var parser = new DOMParser();
                // doc = parser.parseFromString(html, "text/xml");
                var div = document.createElement('div');
                div.innerHTML = html;
                var elements = div.children;

                var head = "<ul class='list-group'>";
                var footer = "</ul>";
                var result = head;

                for (var i = 0; i < elements.length; i++) {
                	result += "<li class=\"list-group-item toggle animation\">" + elements[i].outerHTML + "</li>";
                }

                element.html(result);

                if (typeof attr.callback != 'undefined') {
                    var callBackFunction = new Function("return " + attr.callback)();
                    if (typeof callBackFunction != 'undefined')
                        callBackFunction(element);
                }
                scope.content = $parse(attr.content)(scope);
                $compile(element.contents())(scope);
            }, true);
        }
    }
});

*/

angular.module('springChat.directives').directive('botList', function($compile, $parse) {
    return {
        controller: 'ChatViewItemController',
        link: function(scope, element, attr, ctrl) {
            scope.$watch(attr.content, function() {
                var elements = element[0].children;

                var head = "<ul class='list-group'>";
                var footer = "</ul>";
                var result = head;

                for (var i = 0; i < elements.length; i++) {
                	result += "<li class=\"list-group-item toggle animation\">" + elements[i].outerHTML + "</li>";
                }

                element.html(result);

                if (typeof attr.callback != 'undefined') {
                    var callBackFunction = new Function("return " + attr.callback)();
                    if (typeof callBackFunction != 'undefined')
                        callBackFunction(element);
                }
                scope.content = $parse(attr.content)(scope);
                $compile(element.contents())(scope);
            }, true);
        }
    }
});
/*
attributes list: 
content,
linkindex, //unique index of link for sending to server
ispost,//if false - redirect to link href, true - make post request
href, //link to other page or address for post request 
classes; // list of classes like: "btn btn-large"
*/
angular.module('springChat.directives').directive('botlink', function($compile, $parse,$http) {
        return {
        controller: 'ChatViewItemController',
        link: function(scope, element, attr, ctrl) {
            scope.$watch(attr.content, function() {
                var body = attr.content.escapeHtml();
                console.log("body:"+body);
                var usePost = attr.ispost  === 'true';
               
                var ngclickFunction = '';
                if (usePost && (typeof attr.href !== 'undefined') && 
                    (typeof attr.linkindex !== 'undefined')&& 
                    attr.href.length>0 && attr.linkindex.length>0){
                    ngclickFunction = "sendPostToUrl({0},{1})".format(attr.href,attr.linkindex)
                }
                var linkHref = '#';
                if (!usePost){
                    linkHref = attr.href;
                }

                var prefix = '<a class="{0}" ng-click="{1}" href="{2}">'.format(attr.classes,ngclickFunction,linkHref);
                var suffix = '</a>';
                var elementValue = prefix + body + suffix;

                element.html(elementValue);
     
                scope.content = elementValue;
                $compile(element.contents())(scope);
            }, true);
        }
    }
});
