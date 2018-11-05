<?php
header("Content-Type: text/plain; charset=UTF-8");

// Verify login and password
$login = $_GET['login'];
$password = $_GET['password'];
if(empty($login) || empty($password)) {
	exit('Введены неверные данные');
}

// Load IPB core
define('IPB_THIS_SCRIPT', 'public');
require('initdata.php'); // not once!!!
require_once(IPS_ROOT_PATH . 'sources/base/ipsRegistry.php');
$reg = ipsRegistry::instance();
$reg->init();

// Resolve member by login
$member = IPSMember::load(IPSText::parseCleanValue($login), 'all', 'username');
$member_id = $member['member_id'];
if (!$member_id) {
	exit('Введены неверные данные');
}

// Try authenticate
$success = IPSMember::authenticateMember($member_id, md5(IPSText::parseCleanValue($password)));
echo($success ? 'OK:' . $member['name'] : 'Ошибка при авторизации');
?>
