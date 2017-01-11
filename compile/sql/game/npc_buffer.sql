-- ----------------------------
-- Table structure for npc_buffer
-- ----------------------------
DROP TABLE IF EXISTS `npc_buffer`;
CREATE TABLE `npc_buffer` (
  `id` int(6) NOT NULL AUTO_INCREMENT,
  `skill_id` int(6) NOT NULL,
  `skill_level` int(6) NOT NULL DEFAULT 1,
  `skill_fee_id` int(6) NOT NULL DEFAULT 0,
  `skill_fee_amount` int(6) NOT NULL DEFAULT 0,
  `buff_group` int(6) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`,`skill_id`,`buff_group`)
);

-- ----------------------------
-- Records 
-- ----------------------------
INSERT INTO npc_buffer VALUES
(1, 264, 1, 0, 0, 264),
(2, 265, 1, 0, 0, 265),
(3, 266, 1, 0, 0, 266),
(4, 267, 1, 0, 0, 267),
(5, 268, 1, 0, 0, 268),
(6, 269, 1, 0, 0, 269),
(7, 270, 1, 0, 0, 270),
(8, 271, 1, 0, 0, 271),
(9, 272, 1, 0, 0, 272),
(10, 273, 1, 0, 0, 273),
(11, 274, 1, 0, 0, 274),
(12, 275, 1, 0, 0, 275),
(13, 276, 1, 0, 0, 276),
(14, 277, 1, 0, 0, 277),
(15, 304, 1, 0, 0, 304),
(16, 305, 1, 0, 0, 305),
(17, 306, 1, 0, 0, 306),
(18, 307, 1, 0, 0, 307),
(19, 308, 1, 0, 0, 308),
(20, 309, 1, 0, 0, 309),
(21, 310, 1, 0, 0, 310),
(22, 311, 1, 0, 0, 311),
(23, 349, 1, 0, 0, 349),
(24, 363, 1, 0, 0, 363),
(25, 364, 1, 0, 0, 364),
(26, 366, 1, 0, 0, 366),
(27, 367, 1, 0, 0, 367),
(28, 1032, 1, 0, 0, 1032),
(29, 1033, 1, 0, 0, 1033),
(30, 1035, 1, 0, 0, 1035),
(31, 1036, 1, 0, 0, 1036),
(32, 1040, 1, 0, 0, 1040),
(33, 1043, 1, 0, 0, 1043),
(34, 1044, 1, 0, 0, 1044),
(35, 1045, 1, 0, 0, 1045),
(36, 1048, 1, 0, 0, 1048),
(37, 1059, 1, 0, 0, 1059),
(38, 1062, 1, 0, 0, 1062),
(39, 1068, 1, 0, 0, 1068),
(40, 1077, 1, 0, 0, 1077),
(41, 1078, 1, 0, 0, 1078),
(42, 1085, 1, 0, 0, 1085),
(43, 1086, 1, 0, 0, 1086),
(44, 1182, 1, 0, 0, 1182),
(45, 1189, 1, 0, 0, 1189),
(46, 1191, 1, 0, 0, 1191),
(47, 1204, 1, 0, 0, 1204),
(48, 1240, 1, 0, 0, 1240),
(49, 1242, 1, 0, 0, 1242),
(50, 1243, 1, 0, 0, 1243),
(51, 1303, 1, 0, 0, 1303);