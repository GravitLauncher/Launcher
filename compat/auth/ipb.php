<?php
header("Content-Type: text/plain; charset=UTF-8");

// Verify login and password
$login = $_GET['login'];
$password = $_GET['password'];
if(empty($login) || empty($password)) {
	exit('Empty login or password');
}

// Load IPB core
define('IPB_THIS_SCRIPT', 'public');
require_once('initdata.php');
require_once(IPS_ROOT_PATH . 'sources/base/ipsRegistry.php');
$reg = ipsRegistry::instance();
$reg->init();

// Resolve member by login
$member = IPSMember::load(IPSText::parseCleanValue($login), 'all', 'username');
$member_id = $member['member_id'];
if (!$member_id) {
	exit('Incorrect login');
}

// Try authenticate
$success = IPSMember::authenticateMember($member_id, md5(IPSText::parseCleanValue($password)));
echo($success ? 'OK:' . $member['name'] : 'Incorrect login or password');
?>
