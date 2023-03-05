package ru.yandex.practicum.filmorate.storage.film.daoImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exceptions.review.ReviewNotFoundException;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.film.dao.ReviewDao;
import ru.yandex.practicum.filmorate.storage.film.dao.ReviewLikeDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component("reviewDbStorage")
@Primary
@Slf4j
public class ReviewDbDao implements ReviewDao {
    private final JdbcTemplate jdbcTemplate;
    private final ReviewLikeDao reviewLikeDao;

    public ReviewDbDao(JdbcTemplate jdbcTemplate, @Qualifier("reviewLikeDbStorage") ReviewLikeDao reviewLikeDao) {
        this.jdbcTemplate = jdbcTemplate;
        this.reviewLikeDao = reviewLikeDao;
    }

    @Override
    public Review addReview(Review review) {
        log.info("Запрос на добавление отзыва: {} получен хранилищем ДБ", review);

        String sql = "INSERT INTO films_review(film_id, user_id, content, is_positive) VALUES(?,?,?,?);";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps =
                            connection.prepareStatement(sql, new String[]{"review_id"});
                    ps.setInt(1, review.getFilmId());
                    ps.setInt(2, review.getUserId());
                    ps.setString(3, review.getContent());
                    ps.setBoolean(4, review.getIsPositive());
                    return ps;
                },
                keyHolder);
        long reviewId = keyHolder.getKey().intValue();
        review.setReviewId(reviewId);
        log.debug("Добавлено новый отзыв с id={}", reviewId);
        return review;
    }

    @Override
    public Review updateReview(Review review) {
        log.info("Запрос на обновление отзыва: {} получен хранилищем ДБ", review);

        log.debug("Формируем sql запрос...");
        String updateSql = "UPDATE films_review SET content=?,is_positive=? WHERE review_id=?;";
        Object[] args = new Object[]{review.getContent(), review.getIsPositive(), review.getReviewId()};

        int updateRow = jdbcTemplate.update(updateSql, args);
        if (updateRow <= 0) {
            log.debug("Отзыв с id={} для обновления не найден.", review.getReviewId());
            throw new ReviewNotFoundException("Отзыв с id=" + review.getReviewId() + " для обновления не найден.");
        }
        log.debug("Отзыв с id={} обновлён.", review.getReviewId());
        return getReviewById(review.getReviewId());
    }

    @Override
    public void deleteReviewById(Integer id) {
        log.info("Запрос на удаление отзыва c id={} получен хранилищем ДБ", id);

        log.debug("Формируем sql запрос...");
        String deleteReviewSql = "delete from films_review where review_id= ?";
        Object[] args = new Object[]{id};
        int delRow = jdbcTemplate.update(deleteReviewSql, args);
        if (delRow <= 0) {
            log.debug("Отзыв с id={} для удаления не найден.", id);
            throw new ReviewNotFoundException("Отзыв с id=" + id + " для удаления не найден.");
        }
        log.debug("Отзыв с id={} удалён.", id);
    }

    @Override
    public Review getReviewById(long id) {
        log.debug("Получен запрос на отзыв с id={};", id);

        log.debug("Формируем sql запрос...");
        String getReviewSql = "select r.REVIEW_ID,r.FILM_ID,r.USER_ID,r.CONTENT,r.IS_POSITIVE from films_review r " +
                " WHERE r.review_id = ?";
        List<Review> reviews = jdbcTemplate.query(getReviewSql, (rs, rowNum) -> reviewMapper(rs), id);
        Optional<Review> review = reviews.stream().findFirst();
        if (!review.isPresent()) {
            log.debug("Отзыв с id={} не найден.", id);
            throw new ReviewNotFoundException("Отзыв с id=" + id + " не найден.");
        }
        log.debug("Отзыв с id={} возвращён: {}", id, reviews.stream().findFirst().get());
        Review resultReview = review.get();
        resultReview.setUseful(reviewLikeDao.getUsefulByReviewId(id));
        return resultReview;
    }

    @Override
    public List<Review> getAllReviewsByFilmId(Integer id, Integer count) {
        log.debug("Получен запрос на все отзывы для фильма с id={};", id);

        log.debug("Формируем sql запрос...");
        String getReviewSql = "select r.REVIEW_ID,r.FILM_ID,r.USER_ID,r.CONTENT,r.IS_POSITIVE from films_review r " +
                " WHERE r.film_id = ?";
        List<Review> reviews = jdbcTemplate.query(getReviewSql, (rs, rowNum) -> reviewMapper(rs), id);
        reviews.forEach(r -> r.setUseful(reviewLikeDao.getUsefulByReviewId(r.getReviewId())));
        return reviews.stream().sorted(Comparator.comparing(Review::getUseful).reversed()).limit(count).collect(Collectors.toList());
    }

    @Override
    public List<Review> getTopReviews(Integer count) {
        log.debug("Получен запрос на топ 10 отзывов");

        log.debug("Формируем sql запрос...");
        String getReviewSql = "select r.REVIEW_ID,r.FILM_ID,r.USER_ID,r.CONTENT,r.IS_POSITIVE from films_review r;";
        List<Review> reviews = jdbcTemplate.query(getReviewSql, (rs, rowNum) -> reviewMapper(rs));
        reviews.forEach(r -> r.setUseful(reviewLikeDao.getUsefulByReviewId(r.getReviewId())));
        return reviews
                .stream()
                .sorted(Comparator.comparing(Review::getUseful).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    private Review reviewMapper(ResultSet rs) throws SQLException {
        long id = rs.getLong("review_id");
        int filmId = rs.getInt("film_id");
        int userId = rs.getInt("user_id");
        String content = rs.getString("content");
        boolean isPositive = rs.getBoolean("is_positive");
        return new Review(id, filmId, userId, content, 0, isPositive);
    }
}
