var PushNotification = function() {};

PushNotification.prototype = {
    register: function(senderID) {
        console.log("Calling cordova.exec");
        cordova.exec(null, null, 'PushNotification', 'register', [{ sender_id: "" + senderID }]);
    }
}

var plugin = new PushNotification();

module.exports = plugin;
