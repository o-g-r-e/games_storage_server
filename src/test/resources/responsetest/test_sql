DROP DATABASE IF EXISTS epsilon_test;

CREATE DATABASE epsilon_test;

USE epsilon_test;

CREATE TABLE `games` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `name` varchar(45) DEFAULT NULL,
    `package` varchar(45) DEFAULT 'default',
    `owner_id` int(11) DEFAULT NULL,
    `api_key` varchar(45) NOT NULL,
    `api_secret` varchar(45) DEFAULT NULL,
    `type` varchar(45) NOT NULL DEFAULT 'default',
    `prefix` varchar(45) DEFAULT NULL,
    `hash` varchar(45) DEFAULT NULL,
    `player_id_field` varchar(45) NOT NULL DEFAULT 'playerId',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `owners` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `email` varchar(45) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `owners` (`id`, `email`) VALUES (1, 'owner@example.com');

CREATE TABLE `special_requests` (
    `game_id` int(11) NOT NULL,
    `request_name` varchar(15) NOT NULL,
    `query_table` varchar(30) NOT NULL,
    `fields` varchar(100) NOT NULL,
    PRIMARY KEY (`game_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;