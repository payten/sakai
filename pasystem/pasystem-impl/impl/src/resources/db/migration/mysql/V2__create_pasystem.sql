-- Splash screens

CREATE TABLE `pasystem_splash_screens` (
  `campaign` varchar(255) PRIMARY KEY ,
  `template_name` varchar(255) DEFAULT NULL,
  `start_time` BIGINT,
  `end_time` BIGINT,
  INDEX `start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `pasystem_splash_assign` (
  `user_eid` varchar(255) DEFAULT NULL,
  `campaign` varchar(255) DEFAULT NULL,
  `open_campaign` int(11) DEFAULT NULL,
   FOREIGN KEY (campaign) REFERENCES pasystem_splash_screens(campaign),
   INDEX `user_eid` (`user_eid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `pasystem_splash_dismissed` (
  `user_eid` varchar(255) DEFAULT NULL,
  `state` varchar(50) DEFAULT NULL,
  `dismiss_time` BIGINT,
  `campaign` varchar(255) DEFAULT NULL,
   UNIQUE KEY `unique_splash_dismissed` (`user_eid`,`state`, `campaign`),
   FOREIGN KEY (campaign) REFERENCES pasystem_splash_screens(campaign),
   INDEX `user_eid` (`user_eid`),
   INDEX `state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- Banners
CREATE TABLE pasystem_banner_alert
( `uuid` VARCHAR(255) PRIMARY KEY,
  `message` VARCHAR(4000) NOT NULL,
  `hosts` VARCHAR(512) DEFAULT NULL,
  `active` INT(1) NOT NULL DEFAULT 0,
  `dismissible` INT(1) NOT NULL DEFAULT 1,
  `active_from` BIGINT DEFAULT NULL,
  `active_until` BIGINT DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
