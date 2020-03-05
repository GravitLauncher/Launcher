<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\DB;

class LauncherAuthController extends Controller
{
    public function json(Request $request) {
        $data = json_decode($request->getContent());

        if ($data->apiKey !== env('LAUNCHER_APIKEY')) {
            $response = [
                'error' => 'Неверный ключ. Обратитесь к администратору',
            ];
            return json_encode($response);
        }

        if (Auth::attempt(['name' => $data->username, 'password' => $data->password])) {
            $perm = DB::table('users')
                ->select('launcher_permission')
                ->where('name', '=', $data->username)
                ->first();

            $response = [
                'username' => $data->username,
                'permission' => $perm->launcher_permission,
            ];
        } else {
            $response = [
                'error' => 'Неверный логин или пароль',
            ];
        }
        return json_encode($response);
    }
}
