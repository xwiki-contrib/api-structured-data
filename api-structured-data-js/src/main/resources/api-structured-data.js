define(['jquery'], function ($) {

  var getApp = function (appId) {


    var getItems = function(options, callback) {
      // getItems() should work with or without the "options" parameter. If "options" is not provided, the first arg is the callback.
      if(typeof callback === 'undefined' && typeof options === 'function') { 
        callback = options;
        options = {};
      }
      $.ajax({
        url : '/xwiki/rest/applications/'+encodeURIComponent(appId)+'/items',
        type: "GET",
        data: $.param(options)
      }).success(function(data){
        callback(null, data);
      }).error(function(xhr, status, err) {
        callback(err, null);
      });
    };

    var getAppSchema = function(callback) {
      $.ajax({
        url : '/xwiki/rest/applications/'+encodeURIComponent(appId)+'/schema',
        type: "GET"
      }).success(function(data){
        callback(null, data);
      }).error(function(xhr, status, err) {
        callback(err, null);
      });
    };

    var getItem = function(itemId, callback) {
      $.ajax({
        url : '/xwiki/rest/applications/'+encodeURIComponent(appId)+'/items/'+encodeURIComponent(itemId),
        type: "GET"
      }).success(function(data){
        callback(null, data);
      }).error(function(xhr, status, err) {
        callback(err, null);
      });
    };

    var storeItem = function(itemId, itemData, callback) {
      $.ajax({
        url : '/xwiki/rest/applications/'+encodeURIComponent(appId)+'/items/'+encodeURIComponent(itemId),
        type: "PUT",
        contentType : "application/json",
        data: JSON.stringify(itemData)
      }).success(function(data){
        callback(null, data);
      }).error(function(xhr, status, err) {
        callback(err, null);
      });
    };

    var deleteItem = function(itemId, callback) {
      $.ajax({
        url : '/xwiki/rest/applications/'+encodeURIComponent(appId)+'/items/'+encodeURIComponent(itemId),
        type: "DELETE"
      }).success(function(data){
        callback(null, data);
      }).error(function(xhr, status, err) {
        callback(err, null);
      });
    };

    return {
      getItem : getItem,
      getItems : getItems,
      getAppSchema : getAppSchema,
      storeItem : storeItem,
      deleteItem : deleteItem
    };
  };

  return {
    getApp : getApp
  };
});