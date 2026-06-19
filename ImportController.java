package com.keshe.controller;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.keshe.common.Result;
import com.keshe.entity.*;
import com.keshe.mapper.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    @Resource
    private SysUserMapper userMapper;
    @Resource
    private SysUserRoleMapper userRoleMapper;
    @Resource
    private SysRoleMapper roleMapper;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private DeviceDoorMapper deviceMapper;
    @Resource
    private VisitorMapper visitorMapper;
    @Resource
    private AccessRuleMapper accessRuleMapper;
    @Resource
    private ClassScheduleMapper classScheduleMapper;
    @Resource
    private AttendanceRuleMapper attendanceRuleMapper;
    @Resource
    private AttendanceRecordMapper attendanceRecordMapper;
    @Resource
    private SysNoticeMapper noticeMapper;

    @PostMapping("/users")
    public Result<Map<String, Object>> importUsers(@RequestParam("file") MultipartFile file) {
        List<SysRole> allRoles = roleMapper.selectList(null);
        List<String> errors = new ArrayList<>(); int success = 0;
        try {
            List<List<Object>> rows = ExcelUtil.getReader(file.getInputStream()).read();
            if (rows.size() < 2) return Result.error("文件为空或只有表头");
            for (int i = 1; i < rows.size(); i++) {
                try {
                    List<Object> row = rows.get(i);
                    String name = getCellValue(row, 0);
                    String cardNo = getCellValue(row, 1);
                    String phone = getCellValue(row, 2);
                    String roleName = getCellValue(row, 3);
                    String email = getCellValue(row, 4);
                    String college = getCellValue(row, 5);
                    String className = getCellValue(row, 6);
                    if (isEmpty(name) || isEmpty(cardNo)) { errors.add("第"+(i+1)+"行: 姓名和卡号不能为空"); continue; }

                    Long matchedRoleId = null;
                    String roleKey = normalize(roleName);
                    for (SysRole r : allRoles) {
                        String dbKey = normalize(r.getName());
                        if (roleKey.equals(dbKey) || roleKey.contains(dbKey) || dbKey.contains(roleKey)) {
                            matchedRoleId = r.getId(); break;
                        }
                    }
                    if (matchedRoleId == null) matchedRoleId = allRoles.get(allRoles.size()-1).getId();

                    int type = 3;
                    if (roleName.indexOf("超级管理") >= 0) type = 1;
                    else if (roleName.indexOf("管理") >= 0 || roleName.indexOf("admin") >= 0) type = 2;
                    else if (roleName.indexOf("教职工") >= 0 || roleName.indexOf("教师") >= 0 || roleName.indexOf("老师") >= 0 || roleName.indexOf("staff") >= 0) type = 4;

                    SysUser exist = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getCardNo, cardNo));
                    if (exist != null) {
                        exist.setRealName(name); exist.setPhone(phone); exist.setEmail(email); exist.setCollege(college); exist.setClassName(className); exist.setType(type);
                        userMapper.updateById(exist);
                        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, exist.getId()));
                        userRoleMapper.insert(new SysUserRole(null, exist.getId(), matchedRoleId));
                        success++; continue;
                    }
                    SysUser user = new SysUser();
                    user.setUsername(cardNo); user.setPassword(passwordEncoder.encode("123456"));
                    user.setRealName(name); user.setCardNo(cardNo); user.setPhone(phone); user.setEmail(email);
                    user.setCollege(college); user.setClassName(className); user.setType(type); user.setStatus(1);
                    userMapper.insert(user);
                    userRoleMapper.insert(new SysUserRole(null, user.getId(), matchedRoleId));
                    success++;
                } catch (Exception e) { errors.add("第"+(i+1)+"行: " + e.getMessage()); }
            }
        } catch (Exception e) { return Result.error("文件解析失败: " + e.getMessage()); }
        return Result.success(Map.of("success", success, "errors", errors));
    }

    @PostMapping("/devices")
    public Result<Map<String, Object>> importDevices(@RequestParam("file") MultipartFile file) {
        List<String> errors = new ArrayList<>(); int success = 0;
        try {
            List<List<Object>> rows = ExcelUtil.getReader(file.getInputStream()).read();
            if (rows.size() < 2) return Result.error("文件为空或只有表头");
            for (int i = 1; i < rows.size(); i++) {
                try {
                    List<Object> row = rows.get(i);
                    String name = getCellValue(row, 0);
                    String code = getCellValue(row, 1);
                    if (isEmpty(name) || isEmpty(code)) { errors.add("第"+(i+1)+"行: 名称和编号不能为空"); continue; }
                    DeviceDoor d = new DeviceDoor();
                    d.setName(name); d.setCode(code);
                    d.setLocation(getCellValue(row, 2));
                    d.setBuilding(getCellValue(row, 3));
                    d.setFloor(getCellValue(row, 4));
                    d.setType(parseInt(row, 5, 1));
                    d.setUsageType(parseInt(row, 6, 1));
                    d.setIpAddress(getCellValue(row, 7));
                    d.setGenderLimit(parseInt(row, 8, 0));
                    d.setStatus(1);
                    deviceMapper.insert(d); success++;
                } catch (Exception e) { errors.add("第"+(i+1)+"行: " + e.getMessage()); }
            }
        } catch (Exception e) { return Result.error("文件解析失败: " + e.getMessage()); }
        return Result.success(Map.of("success", success, "errors", errors));
    }

    @PostMapping("/visitors")
    public Result<Map<String, Object>> importVisitors(@RequestParam("file") MultipartFile file) {
        List<String> errors = new ArrayList<>(); int success = 0;
        try {
            List<List<Object>> rows = ExcelUtil.getReader(file.getInputStream()).read();
            if (rows.size() < 2) return Result.error("文件为空或只有表头");
            for (int i = 1; i < rows.size(); i++) {
                try {
                    List<Object> row = rows.get(i);
                    String name = getCellValue(row, 0);
                    if (isEmpty(name)) { errors.add("第"+(i+1)+"行: 姓名不能为空"); continue; }
                    Visitor v = new Visitor();
                    v.setName(name); v.setPhone(getCellValue(row, 1));
                    v.setIdCard(getCellValue(row, 2)); v.setCardNo(getCellValue(row, 3)); v.setCompany(getCellValue(row, 4));
                    v.setVisitCount(0); v.setBlacklist(0);
                    visitorMapper.insert(v); success++;
                } catch (Exception e) { errors.add("第"+(i+1)+"行: " + e.getMessage()); }
            }
        } catch (Exception e) { return Result.error("文件解析失败: " + e.getMessage()); }
        return Result.success(Map.of("success", success, "errors", errors));
    }

    @PostMapping("/schedules")
    public Result<Map<String, Object>> importSchedules(@RequestParam("file") MultipartFile file) {
        List<String> errors = new ArrayList<>(); int success = 0;
        try {
            List<List<Object>> rows = ExcelUtil.getReader(file.getInputStream()).read();
            if (rows.size() < 2) return Result.error("文件为空或只有表头");
            for (int i = 1; i < rows.size(); i++) {
                try {
                    List<Object> row = rows.get(i);
                    String courseName = getCellValue(row, 0);
                    String deviceCode = getCellValue(row, 3);
                    if (isEmpty(courseName)) { errors.add("第"+(i+1)+"行: 课程名称不能为空"); continue; }
                    ClassSchedule s = new ClassSchedule();
                    s.setCourseName(courseName); s.setGradeClass(getCellValue(row, 1));
                    s.setTeacherName(getCellValue(row, 2));
                    if (!isEmpty(deviceCode)) {
                        DeviceDoor dev = deviceMapper.selectOne(new LambdaQueryWrapper<DeviceDoor>().eq(DeviceDoor::getCode, deviceCode));
                        if (dev != null) { s.setDeviceId(dev.getId()); s.setDeviceName(dev.getName()); }
                    }
                    s.setDayOfWeek(parseInt(row, 4, 1));
                    try { s.setStartTime(LocalTime.parse(getCellValue(row, 5), DateTimeFormatter.ofPattern("HH:mm"))); } catch (Exception ignored) {}
                    try { s.setEndTime(LocalTime.parse(getCellValue(row, 6), DateTimeFormatter.ofPattern("HH:mm"))); } catch (Exception ignored) {}
                    s.setStatus(1);
                    classScheduleMapper.insert(s); success++;
                } catch (Exception e) { errors.add("第"+(i+1)+"行: " + e.getMessage()); }
            }
        } catch (Exception e) { return Result.error("文件解析失败: " + e.getMessage()); }
        return Result.success(Map.of("success", success, "errors", errors));
    }

    /** 导入通行规则 - 支持中文时间类型：工作日/周末/节假日/全天/临时 */
    @PostMapping("/access-rules")
    public Result<Map<String, Object>> importAccessRules(@RequestParam("file") MultipartFile file) {
        List<String> errors = new ArrayList<>(); int success = 0;
        try {
            List<List<Object>> rows = ExcelUtil.getReader(file.getInputStream()).read();
            if (rows.size() < 2) return Result.error("文件为空或只有表头");
            for (int i = 1; i < rows.size(); i++) {
                try {
                    List<Object> row = rows.get(i);
                    String name = getCellValue(row, 0);
                    if (isEmpty(name)) { errors.add("第"+(i+1)+"行: 规则名称不能为空"); continue; }
                    AccessRule r = new AccessRule();
                    r.setName(name);
                    r.setTimeType(parseTimeType(getCellValue(row, 1)));
                    r.setWeekDays(getDefaultWeekDays(r.getTimeType()));
                    r.setPriority(parseInt(row, 2, 0));
                    r.setRemark(getCellValue(row, 3));
                    r.setStatus(1);
                    accessRuleMapper.insert(r); success++;
                } catch (Exception e) { errors.add("第"+(i+1)+"行: " + e.getMessage()); }
            }
        } catch (Exception e) { return Result.error("文件解析失败: " + e.getMessage()); }
        return Result.success(Map.of("success", success, "errors", errors));
    }

    @PostMapping("/notices")
    public Result<Map<String, Object>> importNotices(@RequestParam("file") MultipartFile file) {
        List<String> errors = new ArrayList<>(); int success = 0;
        try {
            List<List<Object>> rows = ExcelUtil.getReader(file.getInputStream()).read();
            if (rows.size() < 2) return Result.error("文件为空或只有表头");
            for (int i = 1; i < rows.size(); i++) {
                try {
                    List<Object> row = rows.get(i);
                    String title = getCellValue(row, 0);
                    if (isEmpty(title)) { errors.add("第"+(i+1)+"行: 标题不能为空"); continue; }
                    SysNotice n = new SysNotice();
                    n.setTitle(title); n.setContent(getCellValue(row, 1));
                    n.setType(parseInt(row, 2, 1)); n.setStatus(parseInt(row, 3, 0));
                    noticeMapper.insert(n); success++;
                } catch (Exception e) { errors.add("第"+(i+1)+"行: " + e.getMessage()); }
            }
        } catch (Exception e) { return Result.error("文件解析失败: " + e.getMessage()); }
        return Result.success(Map.of("success", success, "errors", errors));
    }
    /** 导入考勤规则 */
    @PostMapping("/attendance-rules")
    public Result<Map<String, Object>> importAttendanceRules(@RequestParam("file") MultipartFile file) {
        List<String> errors = new ArrayList<>(); int success = 0;
        try {
            List<List<Object>> rows = ExcelUtil.getReader(file.getInputStream()).read();
            if (rows.size() < 2) return Result.error("文件为空或只有表头");
            for (int i = 1; i < rows.size(); i++) {
                try {
                    List<Object> row = rows.get(i);
                    String name = getCellValue(row, 0);
                    if (isEmpty(name)) { errors.add("第"+(i+1)+"行: 规则名称不能为空"); continue; }
                    AttendanceRule r = new AttendanceRule();
                    r.setName(name);
                    try { r.setCheckInStart(LocalTime.parse(getCellValue(row, 1), DateTimeFormatter.ofPattern("HH:mm"))); } catch (Exception ignored) {}
                    try { r.setCheckInEnd(LocalTime.parse(getCellValue(row, 2), DateTimeFormatter.ofPattern("HH:mm"))); } catch (Exception ignored) {}
                    try { r.setCheckOutStart(LocalTime.parse(getCellValue(row, 3), DateTimeFormatter.ofPattern("HH:mm"))); } catch (Exception ignored) {}
                    try { r.setCheckOutEnd(LocalTime.parse(getCellValue(row, 4), DateTimeFormatter.ofPattern("HH:mm"))); } catch (Exception ignored) {}
                    r.setLateMinutes(parseInt(row, 5, 0));
                    r.setEarlyMinutes(parseInt(row, 6, 0));
                    r.setWorkDays(getCellValue(row, 7));
                    r.setStatus(1);
                    attendanceRuleMapper.insert(r); success++;
                } catch (Exception e) { errors.add("第"+(i+1)+"行: " + e.getMessage()); }
            }
        } catch (Exception e) { return Result.error("文件解析失败: " + e.getMessage()); }
        return Result.success(Map.of("success", success, "errors", errors));
    }

    // ====== 工具方法 ======

    private String getCellValue(List<Object> row, int index) {
        if (index >= row.size() || row.get(index) == null) return "";
        Object val = row.get(index);
        if (val instanceof Number) {
            Number num = (Number) val;
            if (num.doubleValue() == num.longValue()) return String.valueOf(num.longValue());
            return String.valueOf(num.doubleValue());
        }
        return val.toString().trim();
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", "").toLowerCase();
    }

    private boolean isEmpty(String s) { return s == null || s.trim().isEmpty(); }

    private int parseInt(List<Object> row, int index, int def) {
        try { String v = getCellValue(row, index); return isEmpty(v) ? def : Integer.parseInt(v); } catch (Exception e) { return def; }
    }

    /** 解析时间类型：支持数字1-5和中文名 */
    private int parseTimeType(String val) {
        if (isEmpty(val)) return 4; // 默认全天
        // 数字方式
        try { int n = Integer.parseInt(val.trim()); if (n >= 1 && n <= 5) return n; } catch (Exception ignored) {}
        // 中文方式
        String v = val.trim();
        if (v.contains("工作") || v.contains("work") || v.contains("WORK")) return 1;
        if (v.contains("周") || v.contains("末") || v.contains("weekend") || v.contains("WEEKEND")) return 2;
        if (v.contains("节") || v.contains("假") || v.contains("holiday") || v.contains("HOLIDAY")) return 3;
        if (v.contains("全") || v.contains("天") || v.contains("all") || v.contains("ALL")) return 4;
        if (v.contains("临") || v.contains("时") || v.contains("temp") || v.contains("TEMP")) return 5;
        return 4;
    }

    /** 根据时间类型返回默认星期 */
    private String getDefaultWeekDays(int timeType) {
        switch (timeType) {
            case 1: return "1,2,3,4,5"; // 工作日
            case 2: return "6,7";        // 周末
            case 4: return "1,2,3,4,5,6,7"; // 全天
            default: return "";           // 节假日/临时
        }
    }
}
