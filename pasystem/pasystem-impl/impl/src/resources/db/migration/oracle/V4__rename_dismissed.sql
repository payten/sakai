CREATE TABLE pasystem_banner_dismissed (
  uuid varchar2(255),
  user_eid varchar2(255) DEFAULT NULL,
  state varchar2(50) DEFAULT NULL,
  dismiss_time NUMBER,
  CONSTRAINT banner_dismissed_uuid_fk FOREIGN KEY (uuid) REFERENCES pasystem_banner_alert(uuid),
  CONSTRAINT banner_dismissed_unique UNIQUE (user_eid, state, uuid)
);

CREATE INDEX banner_dismissed_lower_user_eid on pasystem_banner_dismissed (lower(user_eid));
CREATE INDEX banner_dismissed_state on pasystem_banner_dismissed (state);
