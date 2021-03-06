

function toArray(object) {
    return angular.isArray(object) ? object : Object.keys(object).map(function(key) {
        return object[key];
    });
}
MAX_UPLOAD_FILE_SIZE_BYTES = 100 * 1000 * 1024;
TIME_FOR_WAITING_ANSWER_FROM_TENANT = 30000;
var daysName = {},
    hoursName = {},
    minutesName = {};
var dayName = {},
    hourName = {},
    minuteName = {};
var daysNameShort = {},
    hoursNameShort = {},
    minutesNameShort = {};
var dayNameShort = {},
    hourNameShort = {},
    minuteNameShort = {};
var endName = {};

daysNameShort['ua'] = 'днів';
dayNameShort['ua'] = 'день';
daysNameShort['en'] = 'day';
dayNameShort['en'] = 'days';
daysNameShort['ru'] = 'дней';
dayNameShort['ru'] = 'день';

hoursNameShort['ua'] = 'годин';
hourNameShort['ua'] = 'годину';
hoursNameShort['en'] = 'hour';
hourNameShort['en'] = 'hours';
hoursNameShort['ru'] = 'часов';
hourNameShort['ru'] = 'час';

minutesNameShort['ua'] = ' хвилин ';
minuteNameShort['ua'] = ' хвилину ';
minutesNameShort['en'] = ' minutes ';
minuteNameShort['en'] = ' minute ';
minutesNameShort['ru'] = ' минут ';
minuteNameShort['ru'] = ' минуту ';

daysNameShort['ua'] = 'дн.';
dayNameShort['ua'] = 'дн.';
daysNameShort['en'] = 'd.';
dayNameShort['en'] = 'd.';
daysNameShort['ru'] = 'дн.';
dayNameShort['ru'] = 'дн.';

hoursNameShort['ua'] = 'год.';
hourNameShort['ua'] = 'год.';
hoursNameShort['en'] = 'h';
hourNameShort['en'] = 'h';
hoursNameShort['ru'] = 'час.';
hourNameShort['ru'] = 'час';

minutesNameShort['ua'] = ' хв. ';
minuteNameShort['ua'] = ' хв. ';
minutesNameShort['en'] = ' min ';
minuteNameShort['en'] = ' min ';
minutesNameShort['ru'] = ' мин. ';
minuteNameShort['ru'] = ' мин. ';

endName['ua'] = "тому";
endName['en'] = "ago";
endName['ru'] = "спустя";

var ROOM_PERMISSIONS = {
    ADD_USER: 1,
    REMOVE_USER: 2
};

var checkIfToday = function(inputDateLong) {
    var inputDate = new Date(inputDateLong);
    var todaysDate = new Date();
    return inputDate.setHours(0, 0, 0, 0) == todaysDate.setHours(0, 0, 0, 0);
}
var checkIfYesterday = function(inputDateLong) {
    var inputDate = new Date(inputDateLong);
    var yesterdayDate = new Date();
    yesterdayDate.setDate(yesterdayDate.getDate() - 1);
    return inputDate.setHours(0, 0, 0, 0) == yesterdayDate.setHours(0, 0, 0, 0);
}
var getNameFromUrl = function(url) {
    var fileNameSignaturePrefix = "file_name=";
    var startPos = url.lastIndexOf(fileNameSignaturePrefix) + fileNameSignaturePrefix.length;
    var endPos = url.length - DEFAULT_FILE_PREFIX_LENGTH;
    return url.substring(startPos, endPos);
}
var firstLetter = function(name) {
    if (undefined != name)
        return name.toUpperCase().charAt(0);
}

var formatDateWithLast = function(date, short) {
    if(short == undefined)
        short = false;
    
    if (date == null || date == undefined)
        return "";

    // need translate and move to global to config map
    var dateObj = new Date(date);

    if (dateObj == null || dateObj == undefined || isNaN(dateObj))
        return "";

    var delta = new Date().getTime() - dateObj.getTime();
    if (delta > 60000 * 59)
        return formatDate(date, short);
    else
    if (Math.round(delta / 60000) == 0)
        return "щойно";

    var minutesStr = Math.round(delta / 60000);
    if (short) {
        if (minutesStr > 1)
            return minutesStr + minutesNameShort[globalConfig.lang];
        else
            return minutesStr + minuteNameShort[globalConfig.lang];
    } else {
        if (minutesStr > 1)
            return minutesStr + minutesNameShort[globalConfig.lang] + endName[globalConfig.lang];
        else
            return minutesStr + minuteNameShort[globalConfig.lang] + endName[globalConfig.lang];
    }


}

var differenceInSecondsBetweenDates = function(t1,t2){
var dif = t1.getTime() - t2.getTime();
var Seconds_from_T1_to_T2 = dif / 1000;
var Seconds_Between_Dates = Math.abs(Seconds_from_T1_to_T2);
return Seconds_Between_Dates;
}
var formatDate = function(date, short) {
    // need translate and move to global to config map
    if(short == undefined)
        short = false;

    var monthNames = {};
    monthNames['ua'] = [
        "Січеня", "Лютого", "Березеня ",
        "Квітня", "Травня ", "Червня ", "Липеня",
        "Серпня", "Вересеня", "Жовтеня",
        "Листопада", "Груденя"
    ];
    monthNames['en'] = [
        "January", "February", "March",
        "April", "May", "June", "July",
        "August", "September", "October",
        "November", "December"
    ];
    monthNames['ru'] = [
        "Января", "Февраля", "Марта",
        "Апреля", "Мая", "Июня", "Июля",
        "Августа", "Сентября", "Октября",
        "Ноября", "Декабря"
    ];
    var dateObj = new Date(date);
    var day = dateObj.getDate();
    var monthIndex = dateObj.getMonth();
    var year = dateObj.getFullYear();
    var minutes = dateObj.getMinutes();
    if (minutes < 10)
        minutes = '0' + minutes;
    if(short)
    {
        if(monthIndex + 1 < 10)
            return day + "." + monthIndex + 1;
        else
            return day + "." + monthIndex + 1;
    }

    return day + " " + monthNames[globalConfig.lang][monthIndex] + " " + dateObj.getHours() + ":" + minutes;
}

function getCurrentTime() {
    var currentdate = new Date();
    var h = currentdate.getHours();
    var m = currentdate.getMinutes();
    var s = currentdate.getSeconds();
    return h + ":" + m + ":" + s;
}

function formatDateToTime(date) {
    var h = date.getHours();
    var m = date.getMinutes();
    var s = date.getSeconds();
    return h + ":" + m + ":" + s;
}



function getPropertyByValue(obj, value) {
    for (var prop in obj) {
        if (obj.hasOwnProperty(prop)) {
            if (obj[prop] === value)
                return prop;
        }
    }
}

var curentDateInJavaFromat = function() {
    var currentdate = new Date();
    var day = currentdate.getDate();
    if (day < "10")
        day = "0" + day;

    var mouth = (currentdate.getMonth() + 1);
    if (mouth < "10")
        mouth = "0" + mouth;

    var datetime = currentdate.getFullYear() + "-" + mouth + "-" +
        day + " " + currentdate.getHours() + ":" + currentdate.getMinutes() + ":" + currentdate.getSeconds() + ".0";
    //console.log("------------------ " + datetime)
    return datetime;
};

function getIdInArrayFromObjectsMap(roomNameMap, propertyName, valueToFind) {

    for (var item in roomNameMap)
        if (roomNameMap[item][propertyName] == valueToFind) return item;
    return undefined;
}

function getRoomById(rooms, id) {

    for (var i = 0; i < rooms.length; i++) {
        if (rooms[i].roomId == id) return rooms[i];
    }
    return undefined;
}


/*
 * FILE UPLOAD
 */
function uploadXhr(files, urlpath, successCallback, errorCallback, onProgress) {

    var xhr = getXmlHttp();

    //  обработчик для закачки
    xhr.upload.onprogress = function(event) {
        //console.log(event.loaded + ' / ' + event.total);
        onProgress(event, xhr.upload.loaded);
    }

    //  обработчики успеха и ошибки
    //  если status == 200, то это успех, иначе ошибка
    xhr.onload = xhr.onerror = function() {
        if (this.status == 200) {
            console.log("SUCCESS:" + xhr.responseText);
            successCallback(xhr.responseText);
        } else {
            console.log("error " + this.status);
            errorCallback(xhr);
        }
    };

    xhr.open("POST", urlpath);
    var boundary = String(Math.random()).slice(2);
    //  xhr.setRequestHeader('Content-Type', 'multipart/form-data; boundary=' + boundary);
    var formData = new FormData();

    for (var i = 0; i < files.length; i++) {
        formData.append("file" + i, files[i]);
    }
    xhr.send(formData);

}

function upload($http, files, urlpath) {
    var formData = new FormData();
    for (var i = 0; i < files.length; i++) {
        formData.append("file" + i, files[i]);
    }

    return $http.post(urlpath, formData, {
        transformRequest: function(data, headersGetterFunction) {
            return data;
        },
        headers: { 'Content-Type': undefined }
    }).error(function(data, status) {
        console.log("Error ... " + status);
    });
}


function getXmlHttp() {
    var xmlhttp;
    try {
        xmlhttp = new ActiveXObject("Msxml2.XMLHTTP");
    } catch (e) {
        try {
            xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
        } catch (E) {
            xmlhttp = false;
        }
    }
    if ((!xmlhttp || !xmlhttp.upload) && typeof XMLHttpRequest != 'undefined') {
        xmlhttp = new XMLHttpRequest();
    }
    return xmlhttp;
}

function replacer(str, offset, s) {
    return "{{call(" + str.substring(2, str.length - 4) + ")}}";
}

function call(str) {
    return "{{" + str + "}}";
}

/*function upload(file,urlpath) {

var xhr = new XMLHttpRequest();

// обработчик для закачки
xhr.upload.onprogress = function(event) {
  log(event.loaded + ' / ' + event.total);
}

// обработчики успеха и ошибки
// если status == 200, то это успех, иначе ошибка
xhr.onload = xhr.onerror = function() {
  if (this.status == 200) {
    log("success");
  } else {
    log("error " + this.status);
  }
};

xhr.open("POST", urlpath, true);
xhr.setRequestHeader('Content-Type', 'multipart/form-data')
xhr.send(file);

}*/

/*
 * CONST
 */
var Operations = Object.freeze({
    "send_message_to_all": "SEND_MESSAGE_TO_ALL",
    "send_message_to_user": "SEND_MESSAGE_TO_USER",
    "add_user_to_room": "ADD_USER_TO_ROOM",
    "add_room": "ADD_ROOM",
    "add_room_from_tenant": "ADD_ROOM_FROM_TENANT",
    "add_room_on_login": "ADD_ROOM_ON_LOGIN"
});

var serverPrefix = "/crmChat";
var DEFAULT_FILE_PREFIX_LENGTH = 15;

var substringMatcher = function(strs) {
    return function findMatches(q, cb) {
        var matches, substringRegex;

        // an array that will be populated with substring matches
        matches = [];

        // regex used to determine if a string contains the substring `q`
        substrRegex = new RegExp(q, 'i');

        // iterate through the pool of strings and for any string that
        // contains the substring `q`, add it to the `matches` array
        $.each(strs, function(i, str) {
            if (substrRegex.test(str)) {
                matches.push(str);
            }
        });
        cb(matches);
    };
};

function getKeyByValue(value, object) {
    for (var prop in object) {
        if (object.hasOwnProperty(prop)) {
            if (object[prop] === value)
                return prop;
        }
    }
}

var isDate = function(date) {
    return (date instanceof Date); //&& (!Number.isInteger(parseInt(date)) && ((new Date(date) !== "Invalid Date" && !isNaN(new Date(date)) )));
}

function getType(value) {
    if (value === true || value === false || value == 'true' || value == 'false')
        return "bool";

    if (Array.isArray(value))
        return "array";

    if (isDate(value))
        return "date";

    return "string";
}

function parseBoolean(value) {
    if (value == "true")
        return true;
    else
        return false;
}

function Color(val) {
    this.val = val;
}
    function send(destination, data, ok_funk, err_funk) {
        var xhr = getXmlHttp();
        xhr.open("POST", serverPrefix + destination, true);
        xhr.onreadystatechange = function() {
            if (xhr.readyState == 4 || xhr.readyState == "complete") {
                ok_funk();
            } else
                err_funk();

        }
        xhr.send(data);
    }

function htmlEscape(str) {
    return str
        .replace(/&/g, '&amp;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
}

// I needed the opposite function today, so adding here too:
function htmlUnescape(str){
    return str
        .replace(/&quot;/g, '"')
        .replace(/&#39;/g, "'")
        .replace(/&lt;/g, '<')
        .replace(/&gt;/g, '>')
        .replace(/&amp;/g, '&');
}