package com.keshe.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.keshe.entity.AttendanceRule;
import com.keshe.mapper.AttendanceRuleMapper;
import com.keshe.service.AttendanceRuleService;
import org.springframework.stereotype.Service;

@Service
public class AttendanceRuleServiceImpl extends ServiceImpl<AttendanceRuleMapper, AttendanceRule> implements AttendanceRuleService {
}
