define(['jquery', 'xwiki-meta'], function ($, xm) {

  var getCurrent = function () {
    return getApp(false, false);
  };

  var getApp = function (appIdInput, wiki) {

    var exports = {};
    var appId = appIdInput;

    var addCurrentPath = '';
    var addWikiPath = '';

    // Check if we look for the current application or a specified application.
    if(!appId) {
      var wikiName  = xm ? xm.wiki  : $('meta[name="wiki"]').attr('content');
      var space = xm ? xm.space : $('meta[name="space"]').attr('content');
      var page  = xm ? xm.page  : $('meta[name="page"]').attr('content');
      appId = wikiName + ':' + space + '.' + page;
      addCurrentPath = 'current/';
    }
    // Check if we look for an application in another wiki
    else if(wiki) {
      addWikiPath = 'wikis/'+encodeURI(wiki)+'/';
    }

    var getItems = exports.getItems = function(options, callback) {
      // getItems() should work with or without the "options" parameter. If "options" is not provided, the first arg is the callback.
      if(typeof callback === 'undefined' && typeof options === 'function') {
        callback = options;
        options = {};
      }
      $.ajax({
        url : '/xwiki/rest/'+addWikiPath+'applications/'+addCurrentPath + encodeURI(appId)+'/items',
        type: "GET",
        data: $.param(options)
      }).success(function(data){
        callback(null, data);
      }).error(function(xhr, status, err) {
        callback(err, null);
      });
    };

    var getSchema = exports.getSchema = function(callback) {
      $.ajax({
        url : '/xwiki/rest/'+addWikiPath+'applications/'+addCurrentPath + encodeURI(appId)+'/schema',
        type: "GET"
      }).success(function(data){
        callback(null, data);
      }).error(function(xhr, status, err) {
        callback(err, null);
      });
    };

    var getItem = exports.getItem = function(itemId, properties, callback) {
      // getItem() should work with or without the "properties" parameter. If "properties" is not provided,
      // the second arg is the callback.
      if(typeof callback === 'undefined' && typeof properties === 'function') {
        callback = properties;
        properties = '';
      }
      $.ajax({
        url : '/xwiki/rest/'+addWikiPath+'applications/'+addCurrentPath + encodeURI(appId)+'/items/'+encodeURI(itemId),
        data: 'properties='+properties,
        type: "GET"
      }).success(function(data){
        callback(null, data);
      }).error(function(xhr, status, err) {
        callback(err, null);
      });
    };

    var storeItem = exports.storeItem = function(itemId, itemData, callback) {
      $.ajax({
        url : '/xwiki/rest/'+addWikiPath+'applications/'+addCurrentPath + encodeURI(appId)+'/items/'+encodeURI(itemId),
        type: "PUT",
        contentType : "application/json",
        data: JSON.stringify(itemData)
      }).success(function(data){
        callback(null, data);
      }).error(function(xhr, status, err) {
        callback(err, null);
      });
    };

    var deleteItem = exports.deleteItem = function(itemId, callback) {
      $.ajax({
        url : '/xwiki/rest/'+addWikiPath+'applications/'+addCurrentPath + encodeURI(appId)+'/items/'+encodeURI(itemId),
        type: "DELETE"
      }).success(function(data){
        callback(null, data);
      }).error(function(xhr, status, err) {
        callback(err, null);
      });
    };

    var getItemDocument = exports.getItemDocument = function(itemId, properties, callback) {
      // getItemDocument() should work with or without the "properties" parameter. If "properties" is not provided,
      // the second arg is the callback.
      if(typeof callback === 'undefined' && typeof properties === 'function') {
        callback = properties;
        properties = '';
      }
      $.ajax({
        url : '/xwiki/rest/'+addWikiPath+'applications/'+addCurrentPath + encodeURI(appId)+'/items/'+encodeURI(itemId)+'/document',
        data: 'properties='+properties,
        type: "GET"
      }).success(function(data){
        callback(null, data);
      }).error(function(xhr, status, err) {
        callback(err, null);
      });
    };

    var storeItemDocument = exports.storeItemDocument = function(itemId, itemData, callback) {
      $.ajax({
        url : '/xwiki/rest/'+addWikiPath+'applications/'+addCurrentPath + encodeURI(appId)+'/items/'+encodeURI(itemId)+'/document',
        type: "PUT",
        contentType : "application/json",
        data: JSON.stringify(itemData)
      }).success(function(data){
        callback(null, data);
      }).error(function(xhr, status, err) {
        callback(err, null);
      });
    };

    return exports;
  };

  return {
    getCurrent : getCurrent,
    getApp : getApp
  };
});