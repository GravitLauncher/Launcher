-- WARNING! Usage of this generation method is NOT recommended

DELIMITER //
CREATE TRIGGER setUUID BEFORE INSERT ON users
FOR EACH ROW BEGIN
	IF NEW.uuid IS NULL THEN
		SET @md5 = MD5(CONCAT("OfflinePlayer:", NEW.username));
		SET NEW.uuid = CONCAT_WS('-', LEFT(@md5, 8), MID(@md5, 9, 4), MID(@md5, 13, 4), MID(@md5, 17, 4), RIGHT(@md5, 12));
	END IF;
END; //
DELIMITER ;
