package ru.yandex.practicum.filmorate.storage.film.daoImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.storage.film.dao.ReviewLikeDao;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component("reviewLikeDbStorage")
@Slf4j
public class ReviewLikeDbDao implements ReviewLikeDao {

    private final JdbcTemplate jdbcTemplate;

    public ReviewLikeDbDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void addLike(int userId, int reviewId) {
        String sql = "insert into useful_review (review_id, user_id, is_like) values (?, ?, ?)";
        jdbcTemplate.update(sql, reviewId, userId, true);
    }

    @Override
    public void addDislike(int userId, int reviewId) {
        String sql = "insert into useful_review (review_id, user_id, is_like) values (?, ?, ?)";
        jdbcTemplate.update(sql, reviewId, userId, false);
    }

    @Override
    public void deleteLike(int userId, int reviewId) {
        String sql = "delete from useful_review where user_id = ? and review_id = ? and is_like = ?";
        jdbcTemplate.update(sql, reviewId, userId, true);
    }

    @Override
    public void deleteDislike(int userId, int reviewId) {
        String sql = "delete from useful_review where user_id = ? and review_id = ? and is_like = ?";
        jdbcTemplate.update(sql, reviewId, userId, false);
    }

    @Override
    public int getUsefulByReviewId(long id) {
        String sql = "select (count(case is_like when true then 1 end) - " +
                "count(case is_like when false then 1 end)) as useful from useful_review where review_id = ?;";
        return jdbcTemplate.query(sql, (rs, rowNum) -> usefulMapper(rs), id).stream().findFirst().orElse(0);
    }


    private int usefulMapper(ResultSet resultSet) {
        try {
            return resultSet.getInt("useful");
        } catch (SQLException e) {
            return 0;
        }
    }
}
