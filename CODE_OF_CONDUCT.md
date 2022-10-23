# GravitLauncher GitConvention #

Цель конвенции — внедрить простые, прозрачные и эффективные правила работы с Git.

Разработка GravitLauncher идёт на базе [Git Flow](https://leanpub.com/git-flow/read). Подробности ниже.

## Ветвление ##

| Название ветки | Значение ветки | Исходная ветка | Пример ветки |
| ------------- | ------------- | ------------- | ------------- |
| **master**  | Полностью готовая для production-а | **release** | |
| **develop**  | Разработка нового функционала  | **master** | |
| **release**  | Тестирование всего нового функционала | **develop** | |
|  |  |  | |
| **bugfix-***  | Исправляет баг нового функционала | **release** | *bugfix-auth* |
| **feature-***  | Добавляет новую возможность  |  **develop** | *feature-auth* |
| **hotfix-***  | Вносит срочное исправление для production-а  |  **master** | *hotfix-auth* |

-----
![Image of GitFlow](https://i.ytimg.com/vi/w2r0oLFtXAw/maxresdefault.jpg)
-----

## Коммиты ##

**Основные правила:**

1. Все коммиты должны быть на английском языке.
2. Запрещено использовать прошедшее время.
3. Обязательно должен быть использован префикс.
4. В конце не должно быть лишнего знака препинания.
5. Длина любой части не должна превышать 100 символов.

**Структура:**

```
[Префикс] <Сообщение>                             
```

| Префикс | Значение | Пример |
| ------- | -------- | ------ |
| **[FIX]** | Всё, что касается исправления багов | [FIX] Bug with failed authorization |
| **[DOCS]** | Всё, что касается документации | [DOCS] Documenting Authorization API |
| **[FEATURE]** | Всё, что касается новых возможностей | [FEATURE] 2FA on authorization |
| **[STYLE]** | Всё, что касается опечаток и форматирования | [STYLE] Typos in the authorization module |
| **[REFACTOR]** | Всё, что касается рефакторинга | [REFACTOR] Switching to EDA in the authorization module |
| **[TEST]** | Всё, что касается тестирования | [TEST] Coverage of the authorization module with tests |
| **[ANY]** | Всё, что не подходит к предыдущему. | [ANY] Connecting Travis CI | 
