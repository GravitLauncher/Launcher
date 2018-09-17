<?php
header("Content-Type: text/plain; charset=UTF-8");

// Verify login and password
$login = $_GET['login'];
$password = $_GET['password'];
if(empty($login) || empty($password)) {
	exit('Empty login or password');
}

// Load XenForo core
$dir = dirname(__FILE__);
$libraryDir = $dir . '/library';
require_once($dir . '/library/XenForo/Autoloader.php');
XenForo_Autoloader::getInstance()->setupAutoloader($libraryDir);
XenForo_Application::initialize($libraryDir, $dir);
XenForo_Application::set('page_start_time', microtime(true));
$db = XenForo_Application::get('db');

// Resolve user_id by login
$result = $db->fetchRow('SELECT user_id, username FROM xf_user WHERE username=' . $db->quote($login) . ' OR email=' . $db->quote($login));
if(!count($result)) {
	exit('Incorrect login');
}
$user_id = $result['user_id'];
$username = $result['username'];

// Get user data
$result = $db->fetchCol('SELECT data FROM xf_user_authenticate WHERE user_id=' . $db->quote($user_id));
if(!count($result)) {
	exit('Unable to get user data: ' . $user_id);
}
$data = $result[0];

// Select authentication core
$auth = NULL;
if(class_exists('XenForo_Authentication_Core12')) {
	$auth = new XenForo_Authentication_Core12;
} else if(class_exists('XenForo_Authentication_Core')) {
	$auth = new XenForo_Authentication_Core;
} else exit('Unable to select authentication core');

// Try authenticate
$auth->setData($data);
$success = $auth->authenticate($user_id, $password);
echo($success ? 'OK:' . $username : 'Incorrect login or password');
?>
