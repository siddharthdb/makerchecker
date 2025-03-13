package com.example.makerchecker.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TaskDto {
    private String taskId;
    private String taskName;
    private String assignee;
}

