CREATE TABLE `%stest_table` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `playerId` varchar(17) NOT NULL,
    `test_int` int(11) NOT NULL,
    `test_float` DECIMAL(5,2) NOT NULL,
    `test_string` varchar(30) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
		
INSERT INTO `%slevels` (`id`, `playerId`, `level`, `score`, `stars`) VALUES
(1, 'emfvv9zi-4wwb3gjc', 1, 0, 0),
(2, 'lshheyeq-iacaqsvc', 1, 650, 2),
(3, 'lshheyeq-iacaqsvc', 2, 0, 0),
(4, 'yh6cgoqh-2sstxb6r', 1, 730, 2),
(5, 'yh6cgoqh-2sstxb6r', 2, 0, 0),
(6, 'mgw8vrhu-bp3lu9yk', 1, 730, 2),
(7, 'mgw8vrhu-bp3lu9yk', 2, 0, 0),
(8, 'weoh98bf-vjyziipb', 1, 920, 2),
(9, 'weoh98bf-vjyziipb', 2, 0, 0),
(10, 'qig0qa8a-w4m3spwa', 1, 680, 2),
(11, 'qig0qa8a-w4m3spwa', 2, 0, 0),
(12, 'emfvv9zi-4wwb3gjc', 2, 222, 222);

INSERT INTO `%splayers` (`playerId`, `facebookId`) VALUES 
('emfvv9zi-4wwb3gjc', '3294251159'),
('lshheyeq-iacaqsvc', '6186682720'),
('yh6cgoqh-2sstxb6r', '3006020705'),
('mgw8vrhu-bp3lu9yk', '3998823149'),
('weoh98bf-vjyziipb', '4979193465'),
('qig0qa8a-w4m3spwa', '1464368749');