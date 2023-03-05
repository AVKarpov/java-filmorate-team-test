package ru.yandex.practicum.filmorate.storage.film.dao;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Set;

//методы добавления, удаления и модификации объектов.

public interface FilmDao {

    //добавление фильма
    Film addFilm(Film film);

    //обновление данных о фильме
    Film updateFilm(Film film);

    //удаление фильма
    void deleteFilm(long filmId);

    //получение фильма
    Film getFilm(long filmId);

    //получение всех фильмов
    List<Film> getFilms();

    //получение списка фильмов с конкретными номерами
    List<Film> getFilms(List<Long> filmsId);
    List<Film> getPopularFilms(long maxCount);

}
