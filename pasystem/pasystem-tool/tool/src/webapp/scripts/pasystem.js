/************************************************************************************
 * PASystemBannerAlerts - for handle banner things!
 */
function PASystemBannerAlerts(json) {
  this.json = json;
  this.setupAlertBannerToggle();
  this.renderBannerAlerts();
  this.setupEvents();
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

  self.$toggle.on("click", function(event) {
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
        $alert.hide();
        self.$container.append($alert);
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

PASystemBannerAlerts.prototype.setupEvents = function() {
  var self = this;

  $(document).on("click", ".pasystem-banner-alert-close", function() {
    self.handleBannerAlertClose($(this).closest(".pasystem-banner-alert"));
    return false;
  });
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
                     $('#popup-acknowledged-button').on('click', function () {
                       self.permanentlyAcknowledged = true;
                       $.featherlight.current().close();
                     });

                     $('#popup-later-button').on('click', function () {
                       self.permanentlyAcknowledged = false;
                       $.featherlight.current().close();
                     });
                   }
                 });
};


PASystemPopup.prototype.acknowledge = function(acknowledgement) {
  $.ajax({
      method: 'POST',
      url: '/pasystem-tool/popups/acknowledge',
      data: {
          uuid: this.uuid,
          acknowledgement: acknowledgement,
          sakai_csrf_token: this.csrf_token
      },
  });
};