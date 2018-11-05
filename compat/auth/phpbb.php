<?php
header("Content-Type: text/plain; charset=UTF-8");

// Verify login and password
$login = $_GET['login'];
$password = $_GET['password'];
if(empty($login) || empty($password)) {
	exit('Введены неверные данные');
}

// Load PHPBB Core
define('IN_PHPBB', true);
$phpbb_root_path = (defined('PHPBB_ROOT_PATH')) ? PHPBB_ROOT_PATH : './';
$phpEx = substr(strrchr(__FILE__, '.'), 1);
include($phpbb_root_path . 'common.' . $phpEx);

// Try authenticate
$result = $auth->login($login, $password, false, false, false);
echo($result['status'] === LOGIN_SUCCESS ? 'OK:' . $result['user_row']['username'] : 'Ошибка при авторизации');
?>
