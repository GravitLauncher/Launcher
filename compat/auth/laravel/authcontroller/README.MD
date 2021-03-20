# Контроллер авторизации методом json

Route:

```php
Route::put('launcher/auth', 'LauncherAuthController@json');
```

Добавить в **.env** строку `LAUNCHER_APIKEY=none`
Где `none` ваш apiKey