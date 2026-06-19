package com.keshe.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.keshe.entity.AttendanceRecord;
import com.keshe.mapper.AttendanceRecordMapper;
import com.keshe.service.AttendanceRecordService;
import org.springframework.stereotype.Service;

@Service
public class AttendanceRecordServiceImpl extends ServiceImpl<AttendanceRecordMapper, AttendanceRecord> implements AttendanceRecordService {
}
