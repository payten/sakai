-- Splash screens

CREATE TABLE pasystem_splash_screens (
  campaign varchar2(255) PRIMARY KEY ,
  template_name varchar2(255) DEFAULT NULL,
  start_time NUMBER,
  end_time NUMBER
);

CREATE INDEX splash_screen_start_time on pasystem_splash_screens (start_time);
CREATE INDEX splash_screen_end_time on pasystem_splash_screens (end_time);


CREATE TABLE pasystem_splash_assign (
  user_eid varchar2(255) DEFAULT NULL,
  campaign varchar2(255) DEFAULT NULL,
  open_campaign number(1) DEFAULT NULL,
  CONSTRAINT splash_assign_campaign_fk FOREIGN KEY (campaign) REFERENCES pasystem_splash_screens(campaign)
);

CREATE INDEX splash_assign_lower_user_eid on pasystem_splash_assign (lower(user_eid));

CREATE TABLE pasystem_splash_dismissed (
  user_eid varchar2(255) DEFAULT NULL,
  state varchar2(50) DEFAULT NULL,
  dismiss_time NUMBER,
  campaign varchar2(255) DEFAULT NULL,
  CONSTRAINT splash_dismissed_campaign_fk FOREIGN KEY (campaign) REFERENCES pasystem_splash_screens(campaign),
  CONSTRAINT splash_dismissed_unique UNIQUE (user_eid, state, campaign)
);

CREATE INDEX splash_dismissed_lower_user_eid on pasystem_splash_dismissed (lower(user_eid));
CREATE INDEX splash_dismissed_state on pasystem_splash_dismissed (state);

-- Banners

CREATE TABLE pasystem_banner_alert
( uuid VARCHAR2(255) NOT NULL PRIMARY KEY,
  message VARCHAR2(4000) NOT NULL,
  hosts VARCHAR2(512),
  active NUMBER(1,0) DEFAULT 0 NOT NULL,
  dismissible NUMBER(1,0) DEFAULT 1 NOT NULL,
  active_from NUMBER,
  active_until NUMBER
);
