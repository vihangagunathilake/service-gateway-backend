package com.flex.job_module.impl.repositories;

import com.flex.job_module.impl.entities.JobTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobTrackRepository extends JpaRepository<JobTrack, Integer> {

    @Query("SELECT new JobTrack(j.title, j.addedDate, j.addedTime, j.note) FROM JobTrack j WHERE j.job.id=:jobId")
    List<JobTrack> getJobTrackByJobId(@Param("jobId") Integer jobId);

    List<JobTrack> findAllByJob_id(Integer jobId);
}
