package com.keshe.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.keshe.common.Result;
import com.keshe.dto.CardSwipeDTO;
import com.keshe.dto.CardSwipeResultVO;
import com.keshe.entity.*;
import com.keshe.mapper.*;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/access")
public class CardSwipeController {

    @Resource
    private SysUserMapper userMapper;
    @Resource
    private SysUserRoleMapper userRoleMapper;
    @Resource
    private SysRoleMapper roleMapper;
    @Resource
    private DeviceDoorMapper deviceMapper;
    @Resource
    private AccessRuleMapper ruleMapper;
    @Resource
    private AccessRuleDeviceMapper ruleDeviceMapper;
    @Resource
    private AccessLogMapper accessLogMapper;
    @Resource
    private AttendanceRecordMapper attendanceRecordMapper;
    @Resource
    private com.keshe.mapper.VisitorMapper visitorMapper;
    @Resource
    private com.keshe.mapper.VisitorRecordMapper visitorRecordMapper;
    @Resource
    private AttendanceRuleMapper attendanceRuleMapper;
    @Resource
    private ClassScheduleMapper classScheduleMapper;
    @Resource
    private com.keshe.mapper.SysConfigMapper sysConfigMapper;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostMapping("/card-swipe")
    public Result<CardSwipeResultVO> cardSwipe(@Valid @RequestBody CardSwipeDTO dto) {
        LocalDateTime now = getEffectiveNow();
        CardSwipeResultVO.CardSwipeResultVOBuilder builder = CardSwipeResultVO.builder()
                .deviceId(dto.getDeviceId())
                .swipeTime(now.format(DTF));

        // find user
        // try system user first
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getCardNo, dto.getCardNo()));
        if (user == null) {
            // try visitor
            com.keshe.entity.Visitor visitor = visitorMapper.selectOne(
                    new LambdaQueryWrapper<com.keshe.entity.Visitor>().eq(com.keshe.entity.Visitor::getCardNo, dto.getCardNo()));
            if (visitor != null) {
                return handleVisitorSwipe(visitor, dto, now, builder);
            }
            builder.result("DENIED").reason("card not recognized");
            writeLog(null, dto.getDeviceId(), 2, "card not recognized", now);
            return Result.success(builder.build());
        }

        builder.userId(user.getId()).userName(user.getRealName()).userType(user.getType());
        String typeName = user.getType() == 1 ? "superadmin" : user.getType() == 2 ? "admin" : user.getType() == 4 ? "staff" : "student";
        builder.userTypeName(typeName);

        if (user.getStatus() == 0) {
            builder.result("DENIED").reason("account disabled");
            writeLog(user.getId(), dto.getDeviceId(), 2, "account disabled", now);
            return Result.success(builder.build());
        }

        DeviceDoor device = deviceMapper.selectById(dto.getDeviceId());
        if (device == null) {
            builder.result("DENIED").reason("device not found");
            writeLog(user.getId(), dto.getDeviceId(), 2, "device not found", now);
            return Result.success(builder.build());
        }
        builder.deviceName(device.getName()).building(device.getBuilding());

        // check permission with diagnosis
        List<String> diagnosis = new ArrayList<>();
        boolean hasPermission = checkAccessPermission(user, device, now, diagnosis);
        builder.diagnosis(diagnosis);
        if (!hasPermission) {
            builder.result("DENIED").reason("no access permission");
            writeLog(user.getId(), dto.getDeviceId(), 2, String.join("; ", diagnosis).substring(0, 250), now);
            return Result.success(builder.build());
        }

        writeLog(user.getId(), dto.getDeviceId(), 1, "access granted", now);
        builder.result("SUCCESS").reason("access granted");

        if (device.getUsageType() != null && device.getUsageType() > 1) {
            if (device.getUsageType() == 2) {
                handleTeacherAttendance(user, device, now, builder);
            } else if (device.getUsageType() == 3) {
                handleStudentAttendance(user, device, now, builder);
            }
        }

        return Result.success(builder.build());
    }

    private boolean checkAccessPermission(SysUser user, DeviceDoor device, LocalDateTime now, List<String> diag) {
        if (user.getType() != null && user.getType() == 1) {
            diag.add("super admin - no check needed");
            return true;
        }

        List<Long> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()))
                .stream().map(SysUserRole::getRoleId).collect(Collectors.toList());

        // 如果sys_user_role为空，使用user.type作为隐式角色
        List<Long> checkRoleIds = roleIds;
        String typeToRoleName = "";
        if (roleIds.isEmpty() && user.getType() != null) {
            checkRoleIds = java.util.Arrays.asList(Long.valueOf(user.getType()));
            typeToRoleName = " (from user type)";
        }

        List<SysRole> roles = roleMapper.selectBatchIds(checkRoleIds);
        String roleNames = roles.stream().map(SysRole::getName).collect(Collectors.joining(","));
        diag.add("user roles: " + (roleNames.isEmpty() ? "none" : roleNames) + typeToRoleName);

        List<Long> ruleIds = ruleDeviceMapper.selectList(
                new LambdaQueryWrapper<AccessRuleDevice>().eq(AccessRuleDevice::getDeviceId, device.getId()))
                .stream().map(AccessRuleDevice::getRuleId).collect(Collectors.toList());

        if (ruleIds.isEmpty()) {
            diag.add("device '" + device.getName() + "' has NO access rules configured");
            diag.add("go to Access Rules page to assign rules to this device");
            return false;
        }

        diag.add("device has " + ruleIds.size() + " rule(s), checking...");

        for (Long ruleId : ruleIds) {
            AccessRule rule = ruleMapper.selectById(ruleId);
            if (rule == null || rule.getStatus() != 1) {
                if (rule == null) diag.add("  rule id=" + ruleId + ": not found");
                else diag.add("  rule '" + rule.getName() + "': disabled");
                continue;
            }

            boolean matchUser = rule.getUserId() != null && rule.getUserId().equals(user.getId());
            boolean matchRole = rule.getRoleId() != null && checkRoleIds.contains(rule.getRoleId());
            if (!matchUser && !matchRole) {
                diag.add("  rule '" + rule.getName() + "': no match (user/role mismatch)");
                continue;
            }

            LocalDate today = now.toLocalDate();
            if (rule.getStartDate() != null && today.isBefore(rule.getStartDate())) {
                diag.add("  rule '" + rule.getName() + "': not yet active (starts " + rule.getStartDate() + ")");
                continue;
            }
            if (rule.getEndDate() != null && today.isAfter(rule.getEndDate())) {
                diag.add("  rule '" + rule.getName() + "': expired (ended " + rule.getEndDate() + ")");
                continue;
            }

            if (rule.getWeekDays() != null && !rule.getWeekDays().isEmpty()) {
                String[] days = rule.getWeekDays().split(",");
                String todayDow = String.valueOf(now.getDayOfWeek().getValue());
                boolean dayMatch = false;
                for (String d : days) {
                    if (d.equals(todayDow)) { dayMatch = true; break; }
                }
                if (!dayMatch) {
                    diag.add("  rule '" + rule.getName() + "': today not in allowed weekdays");
                    continue;
                }
            }

            if (rule.getTimeType() != null && rule.getTimeType() == 2) {
                LocalTime current = now.toLocalTime();
                if (rule.getStartTime() != null && current.isBefore(rule.getStartTime())) {
                    diag.add("  rule '" + rule.getName() + "': before allowed time (" + rule.getStartTime() + ")");
                    continue;
                }
                if (rule.getEndTime() != null && current.isAfter(rule.getEndTime())) {
                    diag.add("  rule '" + rule.getName() + "': after allowed time (" + rule.getEndTime() + ")");
                    continue;
                }
            }

            // 检查设备性别限制
            if (device.getGenderLimit() != null && device.getGenderLimit() > 0) {
                if (user.getGender() == null || !device.getGenderLimit().equals(user.getGender())) {
                    String limitStr = device.getGenderLimit() == 1 ? "男生" : "女生";
                    diag.add("  device gender limit: " + limitStr + " only, user gender does not match");
                    continue;
                }
            }

            diag.add("  MATCHED rule '" + rule.getName() + "' - access allowed");
            return true;
        }

        diag.add("no matching rule found for roles: " + roleNames);
        return false;
    }

    private void handleTeacherAttendance(SysUser user, DeviceDoor device,
                                          LocalDateTime now, CardSwipeResultVO.CardSwipeResultVOBuilder builder) {
        LocalDate today = now.toLocalDate();
        LocalTime current = now.toLocalTime();
        AttendanceRule rule = attendanceRuleMapper.selectOne(
                new LambdaQueryWrapper<AttendanceRule>().eq(AttendanceRule::getStatus, 1).last("LIMIT 1"));
        if (rule == null) return;

        AttendanceRecord record = attendanceRecordMapper.selectOne(
                new LambdaQueryWrapper<AttendanceRecord>()
                        .eq(AttendanceRecord::getUserId, user.getId())
                        .eq(AttendanceRecord::getDate, today));

        boolean isMorning = current.isBefore(LocalTime.NOON);

        if (isMorning) {
            if (record == null) {
                record = new AttendanceRecord();
                record.setUserId(user.getId());
                record.setUserName(user.getRealName());
                record.setDate(today);
            }
            record.setCheckInTime(now);
            record.setCheckInDevice(device.getName());

            if (current.isAfter(rule.getCheckInEnd())) {
                int lateMin = (int) Duration.between(rule.getCheckInEnd(), current).toMinutes();
                int grace = rule.getLateMinutes() != null ? rule.getLateMinutes() : 0;
                record.setLateMinutes(Math.max(0, lateMin - grace));
                record.setStatus(record.getStatus() != null && record.getStatus() >= 2 ? 3 : 1);
                builder.attendanceType("CHECK_IN").attendanceStatus("LATE");
            } else {
                record.setLateMinutes(0);
                if (record.getStatus() == null) record.setStatus(0);
                builder.attendanceType("CHECK_IN").attendanceStatus("NORMAL");
            }

            if (record.getId() == null) attendanceRecordMapper.insert(record);
            else attendanceRecordMapper.updateById(record);
        } else {
            if (record == null) {
                record = new AttendanceRecord();
                record.setUserId(user.getId());
                record.setUserName(user.getRealName());
                record.setDate(today);
                record.setStatus(4);
            }
            record.setCheckOutTime(now);
            record.setCheckOutDevice(device.getName());

            if (current.isBefore(rule.getCheckOutStart())) {
                int earlyMin = (int) Duration.between(current, rule.getCheckOutStart()).toMinutes();
                int grace = rule.getEarlyMinutes() != null ? rule.getEarlyMinutes() : 0;
                record.setEarlyMinutes(Math.max(0, earlyMin - grace));
                int s = record.getStatus() != null ? record.getStatus() : 0;
                record.setStatus(s == 1 ? 3 : (s == 0 ? 2 : s));
                builder.attendanceType("CHECK_OUT").attendanceStatus("EARLY");
            } else {
                record.setEarlyMinutes(0);
                builder.attendanceType("CHECK_OUT").attendanceStatus("NORMAL");
            }

            if (record.getCheckInTime() != null) {
                double hours = Duration.between(record.getCheckInTime(), now).toMinutes() / 60.0;
                record.setWorkHours(BigDecimal.valueOf(Math.round(hours * 10) / 10.0));
            }

            if (record.getId() == null) attendanceRecordMapper.insert(record);
            else attendanceRecordMapper.updateById(record);
        }
    }

    private void handleStudentAttendance(SysUser user, DeviceDoor device,
                                          LocalDateTime now, CardSwipeResultVO.CardSwipeResultVOBuilder builder) {
        LocalDate today = now.toLocalDate();
        int dayOfWeek = now.getDayOfWeek().getValue();
        LocalTime current = now.toLocalTime();

        List<ClassSchedule> schedules = classScheduleMapper.selectList(
                new LambdaQueryWrapper<ClassSchedule>()
                        .eq(ClassSchedule::getDeviceId, device.getId())
                        .eq(ClassSchedule::getDayOfWeek, dayOfWeek)
                        .eq(ClassSchedule::getStatus, 1)
                        .le(ClassSchedule::getStartTime, current)
                        .ge(ClassSchedule::getEndTime, current)
                        .last("LIMIT 1"));

        if (schedules.isEmpty()) return;

        ClassSchedule schedule = schedules.get(0);

        // 有效打卡时间：上课前30分钟到上课结束
        LocalTime validStart = schedule.getStartTime().minusMinutes(30);
        LocalTime validEnd = schedule.getEndTime();
        LocalTime currentTime = now.toLocalTime();
        if (currentTime.isBefore(validStart) || currentTime.isAfter(validEnd)) {
            // 不在有效打卡时间，仅记录通行不生成考勤
            return;
        }
        AttendanceRecord record = attendanceRecordMapper.selectOne(
                new LambdaQueryWrapper<AttendanceRecord>()
                        .eq(AttendanceRecord::getUserId, user.getId())
                        .eq(AttendanceRecord::getDate, today));

        if (record == null) {
            record = new AttendanceRecord();
            record.setUserId(user.getId());
            record.setUserName(user.getRealName());
            record.setDate(today);
            record.setScheduleId(schedule.getId());
        }
        record.setCheckInTime(now);
        record.setCheckInDevice(schedule.getCourseName());
        record.setRemark(schedule.getCourseName());

        LocalTime graceTime = schedule.getStartTime().plusMinutes(5);
        if (current.isAfter(graceTime)) {
            int lateMin = (int) Duration.between(schedule.getStartTime(), current).toMinutes();
            record.setLateMinutes(lateMin);
            record.setStatus(1);
            builder.attendanceType("CLASS").attendanceStatus("LATE");
        } else {
            record.setLateMinutes(0);
            record.setStatus(0);
            builder.attendanceType("CLASS").attendanceStatus("NORMAL");
        }

        if (record.getId() == null) attendanceRecordMapper.insert(record);
        else attendanceRecordMapper.updateById(record);
    }

    private void writeLog(Long userId, Long deviceId, int result, String remark, LocalDateTime now) {
        DeviceDoor device = deviceMapper.selectById(deviceId);
        SysUser user = userId != null ? userMapper.selectById(userId) : null;
        AccessLog log = new AccessLog();
        log.setUserId(userId);
        log.setUserName(user != null ? user.getRealName() : "unknown");
        log.setDeviceId(deviceId);
        log.setDeviceName(device != null ? device.getName() : "unknown");
        log.setLocation(device != null ? device.getLocation() : "");
        log.setAccessType(1);
        log.setVerifyMode(2);
        log.setResult(result);
        log.setRemark(remark);
        log.setAccessTime(now);
        accessLogMapper.insert(log);
    }

    private Result<CardSwipeResultVO> handleVisitorSwipe(com.keshe.entity.Visitor visitor, CardSwipeDTO dto,
                                          LocalDateTime now, CardSwipeResultVO.CardSwipeResultVOBuilder builder) {
        DeviceDoor device = deviceMapper.selectById(dto.getDeviceId());
        
        builder.userName(visitor.getName()).userTypeName("visitor");
        
        // check device permission via visitor access rule
        com.keshe.entity.AccessRule visitorRule = ruleMapper.selectOne(
                new LambdaQueryWrapper<com.keshe.entity.AccessRule>().eq(com.keshe.entity.AccessRule::getName, "访客设备权限").eq(com.keshe.entity.AccessRule::getStatus, 1).last("LIMIT 1"));
        if (visitorRule != null) {
            long count = ruleDeviceMapper.selectCount(
                    new LambdaQueryWrapper<com.keshe.entity.AccessRuleDevice>().eq(com.keshe.entity.AccessRuleDevice::getRuleId, visitorRule.getId()).eq(com.keshe.entity.AccessRuleDevice::getDeviceId, dto.getDeviceId()));
            if (count == 0) {
                builder.result("DENIED").reason("visitor no access to this device");
                writeLog(null, dto.getDeviceId(), 2, "visitor no access to device", now);
                return Result.success(builder.build());
            }
        }

        // check blacklist
        if (visitor.getBlacklist() != null && visitor.getBlacklist() == 1) {
            builder.result("DENIED").reason("visitor blacklisted");
            writeLog(null, dto.getDeviceId(), 2, "visitor blacklisted", now);
            return Result.success(builder.build());
        }
        
        // increment visit count
        visitor.setVisitCount((visitor.getVisitCount() != null ? visitor.getVisitCount() : 0) + 1);
        visitorMapper.updateById(visitor);
        
        // write log
        writeLog(null, dto.getDeviceId(), 1, "visitor access: " + visitor.getName(), now);
        builder.result("SUCCESS").reason("visitor access granted");
        builder.deviceName(device != null ? device.getName() : null);
        builder.building(device != null ? device.getBuilding() : null);
        
        // create visitor_record entry
        java.time.LocalDate today = now.toLocalDate();
        com.keshe.entity.VisitorRecord record = visitorRecordMapper.selectOne(
                new LambdaQueryWrapper<com.keshe.entity.VisitorRecord>()
                        .eq(com.keshe.entity.VisitorRecord::getVisitorId, visitor.getId())
                        .eq(com.keshe.entity.VisitorRecord::getStatus, 1)
                        .last("LIMIT 1"));
        if (record != null) {
            record.setStatus(2);
            record.setActualStart(now);
            visitorRecordMapper.updateById(record);
        } else {
            // auto-create visit record
            com.keshe.entity.VisitorRecord newRecord = new com.keshe.entity.VisitorRecord();
            newRecord.setVisitorId(visitor.getId());
            newRecord.setVisitorName(visitor.getName());
            newRecord.setVisitorPhone(visitor.getPhone());
            newRecord.setDeviceId(dto.getDeviceId());
            newRecord.setDeviceName(device != null ? device.getName() : null);
            newRecord.setHostUserId(1L);
            newRecord.setHostUserName("系统管理员");
            newRecord.setReason("刷卡自动签到");
            newRecord.setStatus(2); // checked in
            newRecord.setActualStart(now);
            newRecord.setValidStart(java.time.LocalDateTime.of(today, java.time.LocalTime.MIN));
            newRecord.setValidEnd(java.time.LocalDateTime.of(today, java.time.LocalTime.MAX));
            visitorRecordMapper.insert(newRecord);
        }
        
        return Result.success(builder.build());
    }

    /** 获取有效时间（优先使用虚拟时间） */
    private LocalDateTime getEffectiveNow() {
        try {
            com.keshe.entity.SysConfig config = sysConfigMapper.selectById("virtual_time");
            if (config != null && config.getConfigValue() != null && !config.getConfigValue().isEmpty()) {
                return LocalDateTime.parse(config.getConfigValue(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } catch (Exception ignored) {}
        return LocalDateTime.now();
    }
}
