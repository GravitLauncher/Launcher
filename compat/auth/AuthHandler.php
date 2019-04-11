<?php

//Секретный ключ. Внимание! должен совпадать с ключем в лаунчсервере. Пожалуйста, смените его, иначе это ставит под угрозу проект.
$secretkey = '12345678';
//Настройки связи с базой данных
$link = mysqli_connect(
    'localhost', // Хост
	'root', // Пользователь
	'', // Пароль
	'test' // База данных
);

// Настройка таблицы
$settings = [
    'table' => "dle_users", // Название таблицы
	'usernameColumn' => "name", // Столбец с именами пользователей
	'uuidColumn' => "uuid", // Столбец с uuid
	'accessTokenColumn' => "accessToken", // Столбец с accessToken
	'ServerIDColumn' => "serverID" // Столбец с serverID
];

// Не трогать
// Можно повредить скрипт
$AuthHandler = [
	'type' => filter_input(INPUT_GET, 'type', FILTER_SANITIZE_STRING),
	'username' => filter_input(INPUT_GET, 'username', FILTER_SANITIZE_STRING),
	'uuid' => filter_input(INPUT_GET, 'uuid', FILTER_SANITIZE_STRING),
	'accessToken' => filter_input(INPUT_GET, 'accessToken', FILTER_SANITIZE_STRING),
	'ServerID' => filter_input(INPUT_GET, 'ServerID', FILTER_SANITIZE_STRING),
	'secretKey' => filter_input(INPUT_GET, 'secretKey', FILTER_SANITIZE_STRING)
];

if (!isset($AuthHandler['secretKey'])) {
	die('Не указан ключ!');
}

if ($secretkey != $AuthHandler['secretKey']) {
	die('Неверный ключ!');
}

if(!$link) {
	die('Ошибка подключения к базе данных');
}

if(isset($AuthHandler['type'])) {
  if($AuthHandler['type'] == "FetchByUUID") {
	if(isset($AuthHandler['uuid'])) {
	  $result = mysqli_query($link, 'SELECT '.$settings['usernameColumn'].','.$settings['accessTokenColumn'].','.$settings['serverID'].' FROM '.$settings['table'].' WHERE '.$settings['uuidColumn'].'="'.$AuthHandler['uuid'].'" LIMIT 1') or die($link->error);
	  $row = $result->fetch_assoc();
	  mysqli_free_result($result);
	  mysqli_close($link);
	  die($row[$settings['usernameColumn']] + ':' + $row[$settings['accessTokenColumn']] + ':' + $row[$settings['serverID']]);
  }
   if($AuthHandler['type'] == "FetchByUsername") {
	if(isset($AuthHandler['uuid'])) {
	  $result = mysqli_query($link, 'SELECT '.$settings['uuidColumn'].','.$settings['accessTokenColumn'].','.$settings['serverID'].' FROM '.$settings['table'].' WHERE '.$settings['usernameColumn'].'="'.$AuthHandler['username'].'" LIMIT 1') or die($link->error);
	  $row = $result->fetch_assoc();
	  mysqli_free_result($result);
	  mysqli_close($link);
	  die($row[$settings['uuidColumn']] + ':' + $row[$settings['accessTokenColumn']] + ':' + $row[$settings['serverID']]);
  }
  
  // Обновление строк
  
  if($AuthHandler['type'] == "SetAccessTokenAndUUID") {
	  $result = mysqli_query($link, 'UPDATE '.$settings['table'].' SET '.$settings['accessTokenColumn'].'="'.$AuthHandler['accessToken'].'" WHERE '.$settings['usernameColumn'].'="'.$AuthHandler['username'].'"') or die($link->error);
	  $result1 = mysqli_query($link, 'UPDATE '.$settings['table'].' SET '.$settings['uuidColumn'].'="'.$AuthHandler['uuid'].'" WHERE '.$settings['usernameColumn'].'="'.$AuthHandler['username'].'"') or die($link->error);
	  mysqli_close($link);
	  die('OK');
  }
  if($AuthHandler['type'] == "SetServerID") {
	  $result = mysqli_query($link, 'UPDATE '.$settings['table'].' SET '.$settings['ServerIDColumn'].'="'.$AuthHandler['serverID'].'" WHERE '.$settings['uuidColumn'].'="'.$AuthHandler['uuid'].'"') or die($link->error);
	  mysqli_close($link);
	  die('OK');
  }
  die('FAIL!');
} else { 
  die('Type not set!');
}
?>
