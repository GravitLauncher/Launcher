// ====== SERVERS CONFIG ====== //
var serversConfig = {
    defaults: {
        // Лозунг сервера
        description: "Мир в котором возможно все"
    },

    getServerProperty: function(profile, property){
        if(serversConfig[profile]==null || serversConfig[profile][property]==null){
          return serversConfig.defaults[property];
        }
        return serversConfig[profile][property];
    }
};
