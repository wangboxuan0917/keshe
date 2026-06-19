package com.keshe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.keshe.common.Result;
import com.keshe.common.SecurityUtil;
import com.keshe.entity.*;
import com.keshe.mapper.*;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/course-attendance")
public class CourseAttendanceController {

    @Resource
    private ClassScheduleMapper classScheduleMapper;
    @Resource
    private CourseStudentMapper courseStudentMapper;
    @Resource
    private AttendanceRecordMapper attendanceRecordMapper;
    @Resource
    private SysUserMapper userMapper;
    @Resource
    private SysNoticeMapper noticeMapper;

    /** 为课程随机分配学生 */
    @PostMapping("/assign-students/{scheduleId}")
    public Result<Map<String, Object>> assignStudents(@PathVariable Long scheduleId) {
        ClassSchedule schedule = classScheduleMapper.selectById(scheduleId);
        if (schedule == null) return Result.error("课程不存在");

        // 查出所有学生（type=3）
        List<SysUser> allStudents = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getType, 3).eq(SysUser::getStatus, 1));

        // 随机打乱后取最多30人
        Collections.shuffle(allStudents, new java.util.Random());
        List<SysUser> selected = allStudents.size() > 30 ? allStudents.subList(0, 30) : allStudents;

        // 删除旧分配
        courseStudentMapper.delete(new LambdaQueryWrapper<CourseStudent>().eq(CourseStudent::getScheduleId, scheduleId));

        // 分配
        int count = 0;
        for (SysUser s : selected) {
            courseStudentMapper.insert(new CourseStudent(null, scheduleId, s.getId(), null));
            count++;
        }
        return Result.success(Map.of("assigned", count));
    }

    /** 生成课程考勤报告（上课时间结束后调用） */
    @PostMapping("/generate/{scheduleId}")
    public Result<Map<String, Object>> generateAttendance(@PathVariable Long scheduleId) {
        ClassSchedule schedule = classScheduleMapper.selectById(scheduleId);
        if (schedule == null) return Result.error("课程不存在");

        LocalDate today = LocalDate.now();

        // 查出这门课的所有学生
        List<CourseStudent> enrolled = courseStudentMapper.selectList(
                new LambdaQueryWrapper<CourseStudent>().eq(CourseStudent::getScheduleId, scheduleId));

        int present = 0, late = 0, absent = 0;

        for (CourseStudent cs : enrolled) {
            AttendanceRecord record = attendanceRecordMapper.selectOne(
                    new LambdaQueryWrapper<AttendanceRecord>()
                            .eq(AttendanceRecord::getUserId, cs.getUserId())
                            .eq(AttendanceRecord::getDate, today)
                            .eq(AttendanceRecord::getScheduleId, scheduleId)
                            .last("LIMIT 1"));

            SysUser student = userMapper.selectById(cs.getUserId());

            if (record == null || record.getStatus() == 5) {
                // 没有刷卡记录或已标记旷课
                if (record == null) {
                    record = new AttendanceRecord();
                    record.setUserId(cs.getUserId());
                    record.setScheduleId(scheduleId);
                    record.setUserName(student != null ? student.getRealName() : "");
                    record.setDate(today);
                    record.setStatus(5); // 旷课
                    record.setRemark("缺课-" + schedule.getCourseName());
                    attendanceRecordMapper.insert(record);
                }
                absent++;
            } else if (record.getStatus() == 1) {
                late++;
            } else {
                present++;
            }
        }

        // 生成通知给该课程的授课教师
        ClassSchedule sched = classScheduleMapper.selectById(scheduleId);
        if (sched != null && sched.getTeacherName() != null && !sched.getTeacherName().isEmpty()) {
            SysNotice notice = new SysNotice();
            notice.setTitle("课程考勤报表 - " + sched.getCourseName());
            notice.setContent("课程：" + sched.getCourseName() + "\n班级：" + (sched.getGradeClass() != null ? sched.getGradeClass() : "无") +
                    "\n教师：" + sched.getTeacherName() + "\n上课时间：" + sched.getStartTime() + "~" + sched.getEndTime() +
                    "\n\n考勤统计：\n应到：" + enrolled.size() + "人\n实到：" + present + "人\n迟到：" + late +
                    "人\n缺课：" + absent + "人\n\n（系统自动生成）");
            notice.setType(4); // 系统通知
            notice.setStatus(1); // 已发布
            notice.setPublishTime(LocalDateTime.now());
            notice.setPublisher("系统");
            noticeMapper.insert(notice);
        }

        return Result.success(Map.of("total", enrolled.size(), "present", present, "late", late, "absent", absent));
    }

    /** 获取教师的课程考勤报表 */
    @GetMapping("/teacher-report")
    public Result<List<Map<String, Object>>> getTeacherReport() {
        String currentUser = SecurityUtil.getCurrentUsername();
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) return Result.unauthorized("未登录");

        SysUser user = userMapper.selectById(userId);
        // 查找该教师名下的课程
        String teacherName = user != null ? user.getRealName() : "";
        List<ClassSchedule> schedules = classScheduleMapper.selectList(
                new LambdaQueryWrapper<ClassSchedule>()
                        .eq(ClassSchedule::getTeacherName, teacherName)
                        .eq(ClassSchedule::getStatus, 1));

        List<Map<String, Object>> reports = new ArrayList<>();
        for (ClassSchedule s : schedules) {
            // 查这门课的学生数
            int totalStudents = courseStudentMapper.selectCount(
                    new LambdaQueryWrapper<CourseStudent>().eq(CourseStudent::getScheduleId, s.getId())).intValue();

            // 查今天的考勤记录
            List<AttendanceRecord> records = attendanceRecordMapper.selectList(
                    new LambdaQueryWrapper<AttendanceRecord>()
                            .eq(AttendanceRecord::getScheduleId, s.getId())
                            .eq(AttendanceRecord::getDate, LocalDate.now()));

            long present = records.stream().filter(r -> r.getStatus() != null && r.getStatus() == 0).count();
            long late = records.stream().filter(r -> r.getStatus() != null && r.getStatus() == 1).count();
            long absent = records.stream().filter(r -> r.getStatus() != null && r.getStatus() == 5).count();

            Map<String, Object> report = new LinkedHashMap<>();
            report.put("courseName", s.getCourseName());
            report.put("gradeClass", s.getGradeClass());
            report.put("startTime", s.getStartTime());
            report.put("endTime", s.getEndTime());
            report.put("totalStudents", totalStudents);
            report.put("present", present);
            report.put("late", late);
            report.put("absent", absent);
            reports.add(report);
        }
        return Result.success(reports);
    }

    /** 生成所有今天的课程考勤（可手动触发） */
    @PostMapping("/generate-today")
    public Result<Void> generateToday() {
        LocalDate today = LocalDate.now();
        int dayOfWeek = today.getDayOfWeek().getValue();
        LocalTime now = LocalTime.now();

        // 查找今天已下课的所有课程（end_time < now）
        List<ClassSchedule> schedules = classScheduleMapper.selectList(
                new LambdaQueryWrapper<ClassSchedule>()
                        .eq(ClassSchedule::getDayOfWeek, dayOfWeek)
                        .eq(ClassSchedule::getStatus, 1)
                        .lt(ClassSchedule::getEndTime, now));

        for (ClassSchedule s : schedules) {
            generateAttendance(s.getId());
        }
        return Result.success();
    }
}
