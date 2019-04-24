<?php

namespace controllers;
use \Account;
use \app;
use \AccountException;
/**
 * Description of minecraftController
 *
 * @author gravit
 */
const SECRET_AUTH_KEY = "8k37Jm4l33jQw88eRo9LV";
class minecraftController extends \Controller {
    //put your code here
    public function request($args) {
        \helpers\ajaxHelper::returnStatus(400);
    }
    
    public function getAction($args) {
        api\userAction::getuserAction($args);
    }
    public function checkAuthAction($args)
    {
        if($args['access_key'] !== SECRET_AUTH_KEY)
        {
            echo 'server access key incorrect';
            app::stop();
        }
        try {
            $acc = new Account();
            $acc->auth($args['login'],$args['pass']);
            echo 'OK:'.$acc->login.'';
            app::stop();
        } catch (AccountException $e) {
            $msg = $e->getMessage();
            if ($msg == AccountException::AuthError) {
                echo 'Login or password is incorrect';
                app::stop();
            }
            if ($msg == AccountException::NoLoginError) {
                echo 'This account is not allowed to sign in';
                app::stop();
            }
            if ($msg == AccountException::FatalBanError) {
                echo 'You are permanently banned';
                app::stop();
            }
        }
    }
}
