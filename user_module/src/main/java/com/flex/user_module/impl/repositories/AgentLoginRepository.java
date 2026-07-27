package com.flex.user_module.impl.repositories;

import com.flex.user_module.impl.entities.AgentLogin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AgentLoginRepository extends JpaRepository<AgentLogin, Integer> {

    @Query("SELECT a FROM AgentLogin a WHERE a.user.id=:uid AND a.loginDate = current_date " +
            "AND a.logoutTime is null")
    AgentLogin getAgentLogin(@Param("uid") Integer userId);

    @Query("SELECT a FROM AgentLogin a WHERE a.user.id=:uid AND a.logoutTime is null")
    List<AgentLogin> getAgentLogins(@Param("uid") Integer userId);

    @Query("SELECT a FROM AgentLogin a WHERE a.servicePoint.id=:pointId " +
            "AND a.user.id <> :userId AND a.logoutTime is null " +
            "AND a.loginDate = current_date ")
    AgentLogin getUserLoginToPoint(@Param("pointId") Integer pointId, @Param("userId") Integer userId);

    @Query("SELECT count(al) FROM AgentLogin al WHERE al.servicePoint.serviceCenter.serviceProvider.id=:spId " +
            "AND al.logoutTime is null AND al.loginDate = current_date ")
    int getAgentLoginCountByServiceProvider(@Param("spId") Integer serviceProviderId);

    @Query("SELECT count(al.servicePoint.serviceCenter) FROM AgentLogin al WHERE al.servicePoint.serviceCenter.serviceProvider.id=:spId " +
            "AND al.logoutTime is null AND al.loginDate = current_date")
    int getActiveCentersByServiceProvider(@Param("spId") Integer serviceProviderId);
}
