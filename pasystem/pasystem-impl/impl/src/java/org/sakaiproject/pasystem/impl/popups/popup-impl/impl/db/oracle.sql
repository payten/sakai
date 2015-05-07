CREATE TABLE nyu_t_splash_screens (
  campaign varchar2(255) PRIMARY KEY ,
  template_name varchar2(255) DEFAULT NULL,
  start_time NUMBER,
  end_time NUMBER
);

CREATE INDEX splash_screen_start_time on nyu_t_splash_screens (start_time);
CREATE INDEX splash_screen_end_time on nyu_t_splash_screens (end_time);


CREATE TABLE nyu_t_splash_assign (
  netid varchar2(255) DEFAULT NULL,
  campaign varchar2(255) DEFAULT NULL,
  open_campaign number(1) DEFAULT NULL,
  CONSTRAINT splash_assign_campaign_fk FOREIGN KEY (campaign) REFERENCES nyu_t_splash_screens(campaign)
);

CREATE INDEX splash_assign_lower_netid on nyu_t_splash_assign (lower(netid));

CREATE TABLE nyu_t_splash_dismissed (
  netid varchar2(255) DEFAULT NULL,
  state varchar2(50) DEFAULT NULL,
  dismiss_time NUMBER,
  campaign varchar2(255) DEFAULT NULL,
  CONSTRAINT splash_dismissed_campaign_fk FOREIGN KEY (campaign) REFERENCES nyu_t_splash_screens(campaign),
  CONSTRAINT splash_dismissed_unique UNIQUE (netid, state, campaign)
);

CREATE INDEX splash_dismissed_lower_netid on nyu_t_splash_dismissed (lower(netid));
CREATE INDEX splash_dismissed_state on nyu_t_splash_dismissed (state);

-- Examples

-- Insert a new splash screen that will be shown starting now and
-- finishing 2 weeks from now.
--
-- insert into nyu_t_splash_screens (campaign, template_name, start_time, end_time)
--   values ('goat-warning', 'goatwarning.vm',
--           ((sysdate - to_date('1-1-1970 00:00:00','MM-DD-YYYY HH24:Mi:SS'))*24*3600*1000),
--           (((sysdate + 14) - to_date('1-1-1970 00:00:00','MM-DD-YYYY HH24:Mi:SS'))*24*3600*1000));
--
--
-- Make it available to everyone
--
-- insert into nyu_t_splash_assign (campaign, open_campaign) values ('goat-warning', 1);
