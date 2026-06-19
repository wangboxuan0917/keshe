package com.keshe.controller;

import com.keshe.common.Result;
import com.keshe.entity.AttendanceRecord;
import com.keshe.service.AttendanceRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attendance/stats")
public class AttendanceStatsController {

    @Resource
    private AttendanceRecordService recordService;

    @GetMapping
    public Result<Map<String, Object>> stats() {
        List<AttendanceRecord> all = recordService.list();
        LocalDate today = LocalDate.now();

        List<AttendanceRecord> todayRecords = all.stream()
                .filter(r -> r.getDate() != null && r.getDate().equals(today))
                .collect(Collectors.toList());

        Map<Integer, Long> statusCount = all.stream()
                .filter(r -> r.getStatus() != null)
                .collect(Collectors.groupingBy(AttendanceRecord::getStatus, Collectors.counting()));

        return Result.success(Map.of(
                "total", all.size(),
                "todayTotal", todayRecords.size(),
                "normal", statusCount.getOrDefault(0, 0L),
                "late", statusCount.getOrDefault(1, 0L),
                "early", statusCount.getOrDefault(2, 0L),
                "absent", statusCount.getOrDefault(5, 0L)
        ));
    }
}
