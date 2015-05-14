-- Splash screens

CREATE TABLE `pasystem_splash_screens` (
  `uuid` VARCHAR(255) PRIMARY KEY,
  `descriptor` VARCHAR(255),
  `start_time` BIGINT,
  `end_time` BIGINT,
  INDEX `start_time` (`start_time`),
  INDEX `descriptor` (`descriptor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `pasystem_splash_content` (
  `uuid` varchar(255) PRIMARY KEY,
  `template_content` MEDIUMTEXT,
  FOREIGN KEY (uuid) REFERENCES pasystem_splash_screens(uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `pasystem_splash_assign` (
  `uuid` varchar(255),
  `user_eid` varchar(255) DEFAULT NULL,
  `open_campaign` int(11) DEFAULT NULL,
   FOREIGN KEY (uuid) REFERENCES pasystem_splash_screens(uuid),
   INDEX `user_eid` (`user_eid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `pasystem_splash_dismissed` (
  `uuid` varchar(255),
  `user_eid` varchar(255) DEFAULT NULL,
  `state` varchar(50) DEFAULT NULL,
  `dismiss_time` BIGINT,
   UNIQUE KEY `unique_splash_dismissed` (`user_eid`,`state`, `uuid`),
   FOREIGN KEY (uuid) REFERENCES pasystem_splash_screens(uuid),
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
