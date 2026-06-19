package com.keshe.controller;

import com.keshe.common.Result;
import com.keshe.entity.AccessLog;
import com.keshe.entity.AttendanceRecord;
import com.keshe.entity.DeviceDoor;
import com.keshe.entity.VisitorRecord;
import com.keshe.service.AccessLogService;
import com.keshe.service.AttendanceRecordService;
import com.keshe.service.DeviceDoorService;
import com.keshe.service.VisitorRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Resource
    private AccessLogService accessLogService;
    @Resource
    private AttendanceRecordService attendanceRecordService;
    @Resource
    private DeviceDoorService deviceDoorService;
    @Resource
    private VisitorRecordService visitorRecordService;

    /** 首页仪表盘数据 */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Map<String, Object> data = new LinkedHashMap<>();
        List<DeviceDoor> devices = deviceDoorService.list();
        data.put("deviceTotal", devices.size());
        data.put("deviceOnline", devices.stream().filter(d -> d.getStatus() == 1).count());
        List<AccessLog> todayLogs = accessLogService.list().stream()
                .filter(l -> l.getAccessTime() != null &&
                        l.getAccessTime().toLocalDate().equals(java.time.LocalDate.now()))
                .collect(Collectors.toList());
        data.put("todayAccess", todayLogs.size());
        long pendingVisitors = visitorRecordService.list().stream()
                .filter(v -> v.getStatus() == 0).count();
        data.put("pendingVisitors", pendingVisitors);
        return Result.success(data);
    }

    /** 出入报表 */
    @GetMapping("/access")
    public Result<List<Map<String, Object>>> accessReport(@RequestParam(required = false) Integer days) {
        if (days == null) days = 30;
        LocalDate now = LocalDate.now();
        LocalDate start = now.minusDays(days - 1);
        List<AccessLog> logs = accessLogService.list().stream()
                .filter(l -> l.getAccessTime() != null &&
                        !l.getAccessTime().toLocalDate().isBefore(start) &&
                        !l.getAccessTime().toLocalDate().isAfter(now))
                .collect(Collectors.toList());
        Map<LocalDate, Long> dayCount = logs.stream()
                .collect(Collectors.groupingBy(l -> l.getAccessTime().toLocalDate(), Collectors.counting()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = now.minusDays(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", date.toString());
            item.put("count", dayCount.getOrDefault(date, 0L));
            result.add(item);
        }
        return Result.success(result);
    }

    /** 考勤报表 */
    @GetMapping("/attendance")
    public Result<Map<String, Object>> attendanceReport(@RequestParam(required = false) String month) {
        List<AttendanceRecord> records = attendanceRecordService.list();
        Map<Integer, Long> statusCount = records.stream()
                .filter(r -> r.getStatus() != null)
                .collect(Collectors.groupingBy(AttendanceRecord::getStatus, Collectors.counting()));
        return Result.success(Map.of("statusStats", statusCount, "total", records.size()));
    }

    /** 导出出入报表 CSV */
    @GetMapping("/access/export")
    public void exportAccess(@RequestParam(required = false) Integer days, HttpServletResponse response) throws Exception {
        if (days == null) days = 30;
        response.setContentType("text/csv;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("出入报表.csv", "utf-8"));
        StringBuilder sb = new StringBuilder("日期,通行次数\n");
        LocalDate now = LocalDate.now();
        LocalDate start = now.minusDays(days - 1);
        List<AccessLog> logs = accessLogService.list().stream()
                .filter(l -> l.getAccessTime() != null &&
                        !l.getAccessTime().toLocalDate().isBefore(start) &&
                        !l.getAccessTime().toLocalDate().isAfter(now))
                .collect(Collectors.toList());
        Map<LocalDate, Long> dayCount = logs.stream()
                .collect(Collectors.groupingBy(l -> l.getAccessTime().toLocalDate(), Collectors.counting()));
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = now.minusDays(i);
            sb.append(date).append(",").append(dayCount.getOrDefault(date, 0L)).append("\n");
        }
        response.getWriter().write(sb.toString());
    }

    /** 导出考勤报表 CSV */
    @GetMapping("/attendance/export")
    public void exportAttendance(HttpServletResponse response) throws Exception {
        response.setContentType("text/csv;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("考勤报表.csv", "utf-8"));
        String[] statusNames = {"正常", "迟到", "早退", "迟到早退", "缺卡", "旷工", "休息"};
        List<AttendanceRecord> records = attendanceRecordService.list();
        Map<Integer, Long> statusCount = records.stream()
                .filter(r -> r.getStatus() != null)
                .collect(Collectors.groupingBy(AttendanceRecord::getStatus, Collectors.counting()));
        long total = records.size();
        StringBuilder sb = new StringBuilder("考勤状态,人数,占比\n");
        for (Map.Entry<Integer, Long> e : statusCount.entrySet()) {
            int idx = e.getKey();
            String name = idx >= 0 && idx < statusNames.length ? statusNames[idx] : "未知";
            sb.append(name).append(",").append(e.getValue()).append(",")
              .append(String.format("%.1f%%", e.getValue() * 100.0 / total)).append("\n");
        }
        sb.append("总计,").append(total).append(",100%\n");
        response.getWriter().write(sb.toString());
    }
}
