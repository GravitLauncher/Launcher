<?php
header("Content-Type: text/plain; charset=UTF-8");

// Verify login and password
$login = $_GET['login'];
$password = $_GET['password'];
if(empty($login) || empty($password)) {
	exit('Empty login or password');
}

// Try authenticate
require_once('wp-load.php');
$user = wp_authenticate($login, $password);
echo($user instanceof WP_User ? 'OK:' . $user->user_login : 'Incorrect login or password');
?>
