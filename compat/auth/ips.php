<?php
if ( !isset( $_GET['login'] ) OR !isset( $_GET['password'] ) )
{
	exit;
}
require_once 'init.php';
$member = \IPS\Member::load( $_GET['login'], 'name' );
if ( $member->member_id )
{
	if ( strcmp( $member->members_pass_hash, $member->encryptedPassword( $_GET['password'] ) ) === 0 )
	{
		echo 'OK:' . $member->name;
	}
	else
	{
		echo 'Incorrect login or password';
	}
}
else
{
	echo 'Incorrect login or password';
}
