var PushNotification = function() {};

PushNotification.prototype = {
    register: function(options) {
        var _options = {
            sender_id: "",
            on_success: null,
            on_failure: null
        };
        options = this.merge(_options, options);

        cordova.exec(null, null, 'PushNotification', 'register', [options]);
    },

    merge: function(base, incoming) {
        var output = base;
        for (var key in base) {
            if (incoming[key] !== undefined) {
                output[key] = incoming[key];
            }
        }

        return output;
    }
}

var plugin = new PushNotification();

module.exports = plugin;
