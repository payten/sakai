/************************************************************************************
 * PASystemBannerAlerts - for handle banner things!
 */
function PASystemBannerAlerts(json) {
  this.json = json;
  this.setupAlertBannerToggle();
  this.renderBannerAlerts();
}

PASystemBannerAlerts.prototype.getBannerAlerts = function() {
  return $(".pasystem-banner-alert");
};

PASystemBannerAlerts.prototype.clearBannerAlerts = function() {
  this.getBannerAlerts().remove();
  this.$toggle.slideUp();
};

PASystemBannerAlerts.prototype.setupAlertBannerToggle = function() {
  var self = this;

  self.$toggle = $($("#pasystemBannerAlertsToggleTemplate").html().trim());
  self.$toggle.hide();
  $("#loginLinks").prepend(self.$toggle);

  self.$toggle.click(function(event) {
    event.preventDefault();

    self.showAllAlerts();
    self.$toggle.slideUp();

    return false;
  });
};

PASystemBannerAlerts.prototype.showAllAlerts = function() {
  $.cookie("pasystem-banner-alert-dismissed", null, { path: "/" });
  this.renderBannerAlerts();
};

PASystemBannerAlerts.prototype.hasAlertBeenDismissed = function(alertId) {
  var dismissedIds = [];
  if ($.cookie("pasystem-banner-alert-dismissed") != null) {
    dismissedIds = $.cookie("pasystem-banner-alert-dismissed").split(",");
  }
  return $.inArray(alertId, dismissedIds) >= 0;
};

PASystemBannerAlerts.prototype.markAlertAsDismissed = function(alertId) {
  if ($.cookie("pasystem-banner-alert-dismissed") != null) {
    var ids = $.cookie("pasystem-banner-alert-dismissed");
    $.cookie("pasystem-banner-alert-dismissed", ids + "," + alertId, { path: "/" });
  } else {
    $.cookie("pasystem-banner-alert-dismissed", alertId, { path: "/" });
  };
};

PASystemBannerAlerts.prototype.handleBannerAlertClose = function($alert) {
  var self = this;

  self.markAlertAsDismissed($alert.attr("id"));
  $alert.slideUp(function() {
    $alert.remove();
    self.$toggle.slideDown();
  });
};

PASystemBannerAlerts.prototype.renderBannerAlerts = function() {
  var self = this;

  if (self.json.length == 0) {
    return self.clearBannerAlerts();
  }

  if (typeof self.$container == "undefined") {
    self.$container = $("<div>").addClass("pasystem-banner-alerts");
    $(document.body).prepend(self.$container);
  }

  var dismissedAlertIds = [];
  var activeAlertIds = [];

  // ensure all active alerts are rendered
  $.each(self.json, function(i, alert) {
    var alertId = "bannerAlert_"+alert.id;

    activeAlertIds.push(alertId);

    // if alert is not in the DOM.. add it.
    var $alert = $("#"+alertId);
    if ($alert.length == 0) {
        $alert = $($("#pasystemBannerAlertsTemplate").html().trim()).attr("id", alertId);
        $alert.find(".pasystem-banner-alert-message").html(alert.message);
        if (!alert.dismissible) {
          $alert.find(".pasystem-banner-alert-close").remove();
        }
        if (alert.cssClass) {
          $alert.addClass(alert.cssClass);
        }
        $alert.hide();
        self.$container.append($alert);

        $alert.find(".pasystem-banner-alert-close").click(function() {
          self.handleBannerAlertClose($(this).closest(".pasystem-banner-alert"));
          return false;
        });

    }

    if (self.hasAlertBeenDismissed(alertId)) {
      self.$toggle.show().slideDown();
    } else {
      $alert.slideDown();
    }
  });

  // remove any alerts that are now inactive
  self.getBannerAlerts().each(function() {
    var $alert = $(this);
    if ($.inArray($alert.attr("id"), activeAlertIds) < 0) {
      $alert.remove();
    }
  });
};

PASystemBannerAlerts.prototype.addBannerAlert = function(id, message, dismissible, cssClass) {
  this.json.push({
    id: id,
    message: message,
    dismissible: dismissible,
    cssClass: cssClass
  });

  this.renderBannerAlerts();
};


/************************************************************************************
 * PASystemPopup - for handle popup things!
 */
function PASystemPopup(uuid, csrf_token) {
  this.permanentlyAcknowledged = false;
  this.uuid = uuid;
  this.csrf_token = csrf_token;

  this.$popupContent = $('#popup-container-content');

  if (this.uuid) {
    this.showPopup();
  }
};


PASystemPopup.prototype.showPopup = function() {
  var self = this;

  var popupHTML = (self.$popupContent.html() + $('#popup-container-footer').html()).trim();

  $.featherlight(popupHTML,
                 {
                   afterClose : function (event) {
                     var acknowledgement = self.permanentlyAcknowledged ? 'permanent' : 'temporary';
                     self.acknowledge(acknowledgement);
                   },
                   afterContent : function (event) {
                     $('#popup-acknowledged-button').click(function () {
                       self.permanentlyAcknowledged = true;
                       $.featherlight.current().close();
                     });

                     $('#popup-later-button').click(function () {
                       self.permanentlyAcknowledged = false;
                       $.featherlight.current().close();
                     });
                   }
                 });
};


PASystemPopup.prototype.acknowledge = function(acknowledgement) {
  if (this.uuid !== 'preview') {
    $.ajax({
      method: 'POST',
      url: '/pasystem-tool/popups/acknowledge',
      data: {
        uuid: this.uuid,
        acknowledgement: acknowledgement,
        sakai_csrf_token: this.csrf_token
      },
    });
  }
};


/************************************************************************************
 * PASystemTimezoneChecker - for checking the user's timezone and things!
 */
function PASystemTimezoneChecker() {
  this.tz = jstz.determine();
  this.checkTimezone();
};

/*! jstz - v1.0.4 - 2012-12-18 */
(function(e){var t=function(){"use strict";var e="s",n=function(e){var t=-e.getTimezoneOffset();return t!==null?t:0},r=function(e,t,n){var r=new Date;return e!==undefined&&r.setFullYear(e),r.setDate(n),r.setMonth(t),r},i=function(e){return n(r(e,0,2))},s=function(e){return n(r(e,5,2))},o=function(e){var t=e.getMonth()>7?s(e.getFullYear()):i(e.getFullYear()),r=n(e);return t-r!==0},u=function(){var t=i(),n=s(),r=i()-s();return r<0?t+",1":r>0?n+",1,"+e:t+",0"},a=function(){var e=u();return new t.TimeZone(t.olson.timezones[e])},f=function(e){var t=new Date(2010,6,15,1,0,0,0),n={"America/Denver":new Date(2011,2,13,3,0,0,0),"America/Mazatlan":new Date(2011,3,3,3,0,0,0),"America/Chicago":new Date(2011,2,13,3,0,0,0),"America/Mexico_City":new Date(2011,3,3,3,0,0,0),"America/Asuncion":new Date(2012,9,7,3,0,0,0),"America/Santiago":new Date(2012,9,3,3,0,0,0),"America/Campo_Grande":new Date(2012,9,21,5,0,0,0),"America/Montevideo":new Date(2011,9,2,3,0,0,0),"America/Sao_Paulo":new Date(2011,9,16,5,0,0,0),"America/Los_Angeles":new Date(2011,2,13,8,0,0,0),"America/Santa_Isabel":new Date(2011,3,5,8,0,0,0),"America/Havana":new Date(2012,2,10,2,0,0,0),"America/New_York":new Date(2012,2,10,7,0,0,0),"Asia/Beirut":new Date(2011,2,27,1,0,0,0),"Europe/Helsinki":new Date(2011,2,27,4,0,0,0),"Europe/Istanbul":new Date(2011,2,28,5,0,0,0),"Asia/Damascus":new Date(2011,3,1,2,0,0,0),"Asia/Jerusalem":new Date(2011,3,1,6,0,0,0),"Asia/Gaza":new Date(2009,2,28,0,30,0,0),"Africa/Cairo":new Date(2009,3,25,0,30,0,0),"Pacific/Auckland":new Date(2011,8,26,7,0,0,0),"Pacific/Fiji":new Date(2010,11,29,23,0,0,0),"America/Halifax":new Date(2011,2,13,6,0,0,0),"America/Goose_Bay":new Date(2011,2,13,2,1,0,0),"America/Miquelon":new Date(2011,2,13,5,0,0,0),"America/Godthab":new Date(2011,2,27,1,0,0,0),"Europe/Moscow":t,"Asia/Yekaterinburg":t,"Asia/Omsk":t,"Asia/Krasnoyarsk":t,"Asia/Irkutsk":t,"Asia/Yakutsk":t,"Asia/Vladivostok":t,"Asia/Kamchatka":t,"Europe/Minsk":t,"Australia/Perth":new Date(2008,10,1,1,0,0,0)};return n[e]};return{determine:a,date_is_dst:o,dst_start_for:f}}();t.TimeZone=function(e){"use strict";var n={"America/Denver":["America/Denver","America/Mazatlan"],"America/Chicago":["America/Chicago","America/Mexico_City"],"America/Santiago":["America/Santiago","America/Asuncion","America/Campo_Grande"],"America/Montevideo":["America/Montevideo","America/Sao_Paulo"],"Asia/Beirut":["Asia/Beirut","Europe/Helsinki","Europe/Istanbul","Asia/Damascus","Asia/Jerusalem","Asia/Gaza"],"Pacific/Auckland":["Pacific/Auckland","Pacific/Fiji"],"America/Los_Angeles":["America/Los_Angeles","America/Santa_Isabel"],"America/New_York":["America/Havana","America/New_York"],"America/Halifax":["America/Goose_Bay","America/Halifax"],"America/Godthab":["America/Miquelon","America/Godthab"],"Asia/Dubai":["Europe/Moscow"],"Asia/Dhaka":["Asia/Yekaterinburg"],"Asia/Jakarta":["Asia/Omsk"],"Asia/Shanghai":["Asia/Krasnoyarsk","Australia/Perth"],"Asia/Tokyo":["Asia/Irkutsk"],"Australia/Brisbane":["Asia/Yakutsk"],"Pacific/Noumea":["Asia/Vladivostok"],"Pacific/Tarawa":["Asia/Kamchatka"],"Africa/Johannesburg":["Asia/Gaza","Africa/Cairo"],"Asia/Baghdad":["Europe/Minsk"]},r=e,i=function(){var e=n[r],i=e.length,s=0,o=e[0];for(;s<i;s+=1){o=e[s];if(t.date_is_dst(t.dst_start_for(o))){r=o;return}}},s=function(){return typeof n[r]!="undefined"};return s()&&i(),{name:function(){return r}}},t.olson={},t.olson.timezones={"-720,0":"Etc/GMT+12","-660,0":"Pacific/Pago_Pago","-600,1":"America/Adak","-600,0":"Pacific/Honolulu","-570,0":"Pacific/Marquesas","-540,0":"Pacific/Gambier","-540,1":"America/Anchorage","-480,1":"America/Los_Angeles","-480,0":"Pacific/Pitcairn","-420,0":"America/Phoenix","-420,1":"America/Denver","-360,0":"America/Guatemala","-360,1":"America/Chicago","-360,1,s":"Pacific/Easter","-300,0":"America/Bogota","-300,1":"America/New_York","-270,0":"America/Caracas","-240,1":"America/Halifax","-240,0":"America/Santo_Domingo","-240,1,s":"America/Santiago","-210,1":"America/St_Johns","-180,1":"America/Godthab","-180,0":"America/Argentina/Buenos_Aires","-180,1,s":"America/Montevideo","-120,0":"Etc/GMT+2","-120,1":"Etc/GMT+2","-60,1":"Atlantic/Azores","-60,0":"Atlantic/Cape_Verde","0,0":"Etc/UTC","0,1":"Europe/London","60,1":"Europe/Berlin","60,0":"Africa/Lagos","60,1,s":"Africa/Windhoek","120,1":"Asia/Beirut","120,0":"Africa/Johannesburg","180,0":"Asia/Baghdad","180,1":"Europe/Moscow","210,1":"Asia/Tehran","240,0":"Asia/Dubai","240,1":"Asia/Baku","270,0":"Asia/Kabul","300,1":"Asia/Yekaterinburg","300,0":"Asia/Karachi","330,0":"Asia/Kolkata","345,0":"Asia/Kathmandu","360,0":"Asia/Dhaka","360,1":"Asia/Omsk","390,0":"Asia/Rangoon","420,1":"Asia/Krasnoyarsk","420,0":"Asia/Jakarta","480,0":"Asia/Shanghai","480,1":"Asia/Irkutsk","525,0":"Australia/Eucla","525,1,s":"Australia/Eucla","540,1":"Asia/Yakutsk","540,0":"Asia/Tokyo","570,0":"Australia/Darwin","570,1,s":"Australia/Adelaide","600,0":"Australia/Brisbane","600,1":"Asia/Vladivostok","600,1,s":"Australia/Sydney","630,1,s":"Australia/Lord_Howe","660,1":"Asia/Kamchatka","660,0":"Pacific/Noumea","690,0":"Pacific/Norfolk","720,1,s":"Pacific/Auckland","720,0":"Pacific/Tarawa","765,1,s":"Pacific/Chatham","780,0":"Pacific/Tongatapu","780,1,s":"Pacific/Apia","840,0":"Pacific/Kiritimati"},typeof exports!="undefined"?exports.jstz=t:e.jstz=t})(this);

PASystemTimezoneChecker.prototype.isCheckRequired = function() {
  return (document.cookie.indexOf("pasystem_timezone_warning_dismissed=true") < 0) &&
           (document.cookie.indexOf("pasystem_timezone_ok=true") < 0)
};


PASystemTimezoneChecker.prototype.checkTimezone = function() {
  var self = this;

  if (self.isCheckRequired()) {
    $.ajax({
      url: '/direct/pasystem/checkTimeZone',
      data: {timezone: self.tz.name()},
      type: "GET",
      dataType: 'json',
      success: function(data) {
        if (data.status == 'MISMATCH' && data.setTimezoneUrl) {
          // Add banner for Timezone check message
          pasystem.banners.addBannerAlert("tz", self.getTimezoneBannerContent(data), true, "pasystem-timezone-banner-alert");
        } else {
          self.doNotCheckAgainForAWhile();
        }
      }
    });
  }
};


PASystemTimezoneChecker.prototype.getTimezoneBannerContent = function(msg) {
  var template = $("#timezoneBannerTemplate").html().trim();
  var trimPathTemplate = TrimPath.parseTemplate(template, "timezoneBannerTemplate");

  return $(trimPathTemplate.process({
    setTimezoneUrl: msg.setTimezoneUrl,
    reportedTimezone: msg.reportedTimezone,
    prefsTimezone: msg.prefsTimezone
  }));
};


PASystemTimezoneChecker.prototype.doNotCheckAgainForAWhile = function() {
  var expire_time = new Date();
  expire_time.setTime(expire_time.getTime() + (1 * 60 * 60 * 1000));
  document.cookie = "pasystem_timezone_ok=true; path=/; expires=" + expire_time.toGMTString() + ";";
};
