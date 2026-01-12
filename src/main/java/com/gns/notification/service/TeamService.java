package com.gns.notification.service;

import com.gns.notification.dto.PageResult;
import com.gns.notification.dto.TeamRequest;
import com.gns.notification.dto.TeamResponse;
import org.springframework.data.domain.Pageable;

public interface TeamService {

    TeamResponse createTeam(TeamRequest request);

    TeamResponse updateTeam(Long id, TeamRequest request);

    TeamResponse getTeam(Long id);

    PageResult<TeamResponse> listTeams(Pageable pageable);

    void deleteTeam(Long id);
}
