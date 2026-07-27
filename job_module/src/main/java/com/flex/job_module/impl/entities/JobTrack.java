package com.flex.job_module.impl.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.flex.common_module.CommonMethods;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "job_tracks")
public class JobTrack {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;
    private String title;
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Colombo")
    private LocalDate addedDate;
    @JsonFormat(pattern = "HH:mm:ss", timezone = "Asia/Colombo")
    private LocalTime addedTime;
    private String note;

    @Transient
    private String time;
    @Transient
    private String description;

    public JobTrack(String title, LocalDate addedDate, LocalTime addedTime, String note) {
        this.title = title;
        this.time = addedDate + " at " + CommonMethods.timeFormat(addedTime);
        this.description = note;
    }
}
