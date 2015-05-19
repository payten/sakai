-- Popup screens

CREATE TABLE pasystem_popup_screens (
  uuid varchar2(255) PRIMARY KEY ,
  descriptor varchar2(255),
  start_time NUMBER,
  end_time NUMBER
);

CREATE INDEX popup_screen_descriptor on pasystem_popup_screens (descriptor);
CREATE INDEX popup_screen_start_time on pasystem_popup_screens (start_time);
CREATE INDEX popup_screen_end_time on pasystem_popup_screens (end_time);


CREATE TABLE pasystem_popup_content (
  uuid varchar2(255),
  template_content CLOB,
  CONSTRAINT popup_content_uuid_fk FOREIGN KEY (uuid) REFERENCES pasystem_popup_screens(uuid)
);


CREATE TABLE pasystem_popup_assign (
  uuid varchar2(255),
  user_eid varchar2(255) DEFAULT NULL,
  open_campaign number(1) DEFAULT NULL,
  CONSTRAINT popup_assign_uuid_fk FOREIGN KEY (uuid) REFERENCES pasystem_popup_screens(uuid)
);

CREATE INDEX popup_assign_lower_user_eid on pasystem_popup_assign (lower(user_eid));

CREATE TABLE pasystem_popup_dismissed (
  uuid varchar2(255),
  user_eid varchar2(255) DEFAULT NULL,
  state varchar2(50) DEFAULT NULL,
  dismiss_time NUMBER,
  CONSTRAINT popup_dismissed_uuid_fk FOREIGN KEY (uuid) REFERENCES pasystem_popup_screens(uuid),
  CONSTRAINT popup_dismissed_unique UNIQUE (user_eid, state, uuid)
);

CREATE INDEX popup_dismissed_lower_user_eid on pasystem_popup_dismissed (lower(user_eid));
CREATE INDEX popup_dismissed_state on pasystem_popup_dismissed (state);

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
