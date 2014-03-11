var PushNotification = function() {};

PushNotification.prototype = {
    register: function() {
        console.log("Calling cordova.exec");
        cordova.exec(null, null, 'PushNotification', 'register', [{ ecb: "foo", senderID: "12345" }]);
    }
}

var plugin = new PushNotification();

module.exports = plugin;
