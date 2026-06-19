package com.flex.user_module.impl.repositories;

import com.flex.user_module.impl.entities.User;
import com.flex.user_module.impl.entities.UserPasswordToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserPasswordTokenRepository extends JpaRepository<UserPasswordToken, Integer> {

    @Query("""
        SELECT up
        FROM UserPasswordToken up
        WHERE up.user.id = :id
          AND up.token = :token
          AND up.expireTime > :time
          AND up.used = false
    """)
    List<UserPasswordToken> getNonExpiredUserPasswordTokens(
            @Param("id") Integer id,
            @Param("token") String token,
            @Param("time") LocalDateTime time);

    @Query("SELECT up FROM UserPasswordToken up WHERE up.user.id=:id AND up.token=:token AND up.expireTime is null AND up.used = false")
    UserPasswordToken findTokenByUserId(@Param("id") Integer id,
                                        @Param("token") String token);
}
