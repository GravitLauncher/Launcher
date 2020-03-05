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
            return response()->json([
                'error' => 'Неверный ключ. Обратитесь к администратору',
            ]);
        }

        if (Auth::attempt(['name' => $data->username, 'password' => $data->password])) {
            $perm = DB::table('users')
                ->select('launcher_permission')
                ->where('name', '=', $data->username)
                ->first();

            return response()->json([
                'username' => $data->username,
                'permission' => $perm->launcher_permission,
            ]);
        } else {
            return response()->json([
                'error' => 'Неверный логин или пароль',
            ]);
        }
    }
}
