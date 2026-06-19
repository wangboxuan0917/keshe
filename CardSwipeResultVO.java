package com.keshe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSwipeResultVO {
    /** 结果 SUCCESS / DENIED */
    private String result;
    /** 原因 */
    private String reason;

    private Long userId;
    private String userName;
    private Integer userType;
    private String userTypeName;

    private Long deviceId;
    private String deviceName;
    private String building;
    private String swipeTime;

    /** 考勤类型 CHECK_IN / CHECK_OUT / CLASS / null */
    private String attendanceType;
    /** 考勤状态 NORMAL / LATE / EARLY / null */
    private String attendanceStatus;

    /** 权限诊断信息 */
    private List<String> diagnosis;
}
