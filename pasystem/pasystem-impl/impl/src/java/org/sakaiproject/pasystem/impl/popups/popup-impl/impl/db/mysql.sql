CREATE TABLE `nyu_t_splash_screens` (
  `campaign` varchar(255) PRIMARY KEY ,
  `template_name` varchar(255) DEFAULT NULL,
  `start_time` BIGINT,
  `end_time` BIGINT,
  INDEX `start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `nyu_t_splash_assign` (
  `netid` varchar(255) DEFAULT NULL,
  `campaign` varchar(255) DEFAULT NULL,
  `open_campaign` int(11) DEFAULT NULL,
   FOREIGN KEY (campaign) REFERENCES nyu_t_splash_screens(campaign),
   INDEX `netid` (`netid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `nyu_t_splash_dismissed` (
  `netid` varchar(255) DEFAULT NULL,
  `state` varchar(50) DEFAULT NULL,
  `dismiss_time` BIGINT,
  `campaign` varchar(255) DEFAULT NULL,
   UNIQUE KEY `unique_splash_dismissed` (`netid`,`state`, `campaign`),
   FOREIGN KEY (campaign) REFERENCES nyu_t_splash_screens(campaign),
   INDEX `netid` (`netid`),
   INDEX `state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- Examples
-- insert into nyu_t_splash_screens (campaign, template_name, start_time, end_time) values ('goat-warning', 'goatwarning.vm', 0, 9999999999999);
-- insert into nyu_t_splash_assign (campaign, open_campaign) values ('goat-warning', 1);
