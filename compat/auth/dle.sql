SELECT name FROM dle_users WHERE (email=? OR name=?) AND password=MD5(MD5(?)) LIMIT 1
