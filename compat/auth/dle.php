<?php

$auth = [

	'logged' => false,

	'login' => filter_input(INPUT_GET, 'login', FILTER_SANITIZE_STRING),
	'password' => filter_input(INPUT_GET, 'password', FILTER_SANITIZE_STRING)

];

if( isset( $auth['login'] ) AND isset( $auth['password'] ) ) {
	
	define( 'DATALIFEENGINE', true );
	require(  __DIR__ . '/engine/classes/mysql.php' );
	require_once(  __DIR__ . '/engine/data/dbconfig.php' );

	$auth['login'] = $db->safesql( $auth['login'] );
	$auth['password'] = $db->safesql( $auth['password'] );

	if( strlen($auth['password']) > 72 ) $auth['password'] = substr($auth['password'], 0, 72);

	$member_id = $db->super_query( "SELECT name, email, password, hash  FROM dle_users WHERE name='{$auth['login']}' OR email='{$auth['login']}'" );

	if( !$member_id['name'] AND !$member_id['email'] ) {
		
		exit('Введены неверные данные');
	}
		
	if( strlen($member_id['password']) == 32 && ctype_xdigit($member_id['password']) ) {
		
		if( $member_id['password'] == md5(md5($auth['password'])) ) {
			$auth['logged'] = true;
		}
		
	} else {
		
		if( password_verify($auth['password'], $member_id['password']) ) {
			$auth['logged'] = true;
		}
		
	}

	if( $auth['logged'] ) {
		
		session_regenerate_id();

		if ( password_needs_rehash($member_id['password'], PASSWORD_DEFAULT) ) {

			$member_id['password'] = password_hash($auth['password'], PASSWORD_DEFAULT);
			
			$new_pass_hash = 'password='.$db->safesql($member_id['password']).', ';
				
		} else $new_pass_hash = '';

		if( function_exists('openssl_random_pseudo_bytes') ) {

			$stronghash = md5(openssl_random_pseudo_bytes(15));

		} else $stronghash = md5(uniqid( mt_rand(), TRUE ));

		$salt = sha1( str_shuffle('abcdefghjkmnpqrstuvwxyz0123456789') . $stronghash );
		$hash = '';

		for($i = 0; $i < 9; $i ++) {
			$hash .= $salt{mt_rand( 0, 39 )};
		}

		$hash = md5( $hash );
		$member_id['hash'] = $hash;
		
		$db->query( "UPDATE LOW_PRIORITY dle_users SET {$new_pass_hash}hash='{$hash}' WHERE name='{$member_id['name']}'" );

		exit('OK:'.$member_id['name'].'');

	} else {
		
		exit('Ошибка при авторизации');
	}
} else {
	exit('Введены неверные данные');
}
?>
