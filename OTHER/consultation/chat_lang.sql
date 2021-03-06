-- phpMyAdmin SQL Dump
-- version 4.1.14
-- http://www.phpmyadmin.net
--
-- Хост: 127.0.0.1
-- Время создания: Мар 26 2016 г., 09:59
-- Версия сервера: 5.6.17
-- Версия PHP: 5.5.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- База данных: `qa_intita`
--

-- --------------------------------------------------------

--
-- Структура таблицы `chat_lang`
--

CREATE TABLE IF NOT EXISTS `chat_lang` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `lang` text COLLATE utf8_bin NOT NULL,
  `map` text COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_bin AUTO_INCREMENT=4 ;

--
-- Дамп данных таблицы `chat_lang`
--

INSERT INTO `chat_lang` (`id`, `lang`, `map`) VALUES
(2, 'en', '{\n"lable_dialog": "Dialog",\n"lable_participants":"Participants",\n"lable_send":"Send",\n"lable_messages":"Messages",\n"lable_message_placeholder":"Write your message and hit Enter...",\n"rating_global":"How are fine",\n"rating_smart":"How are abstruse"\n}'),
(3, 'ua', '{\n"lable_dialog": "Діалоги",\n"lable_participants":"Учасники",\n"lable_send":"Відіслати",\n"lable_messages":"Повідомлення",\n"lable_message_placeholder":"Напишіть ваше повідомлення і натисність Enter...",\n"rating_global":"Наскільки все файно",\n"rating_smart":"Наскільки все заумно"\n}');

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
