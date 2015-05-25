CREATE TABLE `pasystem_banner_dismissed` (
  `uuid` varchar(255),
  `user_eid` varchar(255) DEFAULT NULL,
  `state` varchar(50) DEFAULT NULL,
  `dismiss_time` BIGINT,
   UNIQUE KEY `unique_banner_dismissed` (`user_eid`,`state`, `uuid`),
   FOREIGN KEY (uuid) REFERENCES pasystem_banner_alert(uuid),
   INDEX `user_eid` (`user_eid`),
   INDEX `state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
