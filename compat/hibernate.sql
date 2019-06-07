-- phpMyAdmin SQL Dump
-- version 4.8.4
-- https://www.phpmyadmin.net/
--
-- Хост: localhost
-- Время создания: Июн 06 2019 г., 22:06
-- Версия сервера: 8.0.15-5
-- Версия PHP: 7.3.4

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET AUTOCOMMIT = 0;
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- База данных: `hiber`
--

-- --------------------------------------------------------

--
-- Структура таблицы `users`
--

CREATE TABLE `users` (
  `id` bigint(20) NOT NULL,
  `username` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `uuid` binary(16) DEFAULT NULL,
  `password` varbinary(255) DEFAULT NULL,
  `password_salt` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `permissions` bigint(20) NOT NULL DEFAULT '0',
  `accessToken` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `serverID` varchar(128) COLLATE utf8mb4_general_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Дамп данных таблицы `users`
--

INSERT INTO `users` (`id`, `username`, `uuid`, `password`, `password_salt`, `permissions`, `accessToken`, `serverID`) VALUES
(7, 'demo', 0x705db569b13842e69cc74b84f7397678, 0x97277292b103ff4f3e6a041cfc3df5b74c9a2e98ede5cba8f7cf7a0ef378208a, '09135d5e5567f84f', 0, 'cfdff81b5f5e591d042d9e3203c9ef9a', NULL),
(8, 'test', 0xb653b767c5c449a58e0de47ded3602c7, 0x80b908bde243318e798de50163307fe0669e1539c43da892ec9cfde87fabfa12, '38e98d4646b553d6', 0, NULL, NULL),
(12, 'server', 0xe71c10a067864680b3747c26b4dc327f, 0xb8feffe3563f012187099e5455d5544f19a12c044a4ea1b9dd9a2274b1470738, '450bd35a5482e815', 2, NULL, NULL),
(13, 'admin', 0x439bed406db54e61b05fabbe9f259636, 0x222aabdb687da6e0a98fd0a61ffa3c4714a84ec3f7f4aaaf297c7f37900e6b36, '971cb4eff7e6be03', 33, NULL, NULL);

--
-- Индексы сохранённых таблиц
--

--
-- Индексы таблицы `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`);

--
-- AUTO_INCREMENT для сохранённых таблиц
--

--
-- AUTO_INCREMENT для таблицы `users`
--
ALTER TABLE `users`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
