package com.keshe.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.keshe.entity.ClassSchedule;
import com.keshe.mapper.ClassScheduleMapper;
import com.keshe.service.ClassScheduleService;
import org.springframework.stereotype.Service;

@Service
public class ClassScheduleServiceImpl extends ServiceImpl<ClassScheduleMapper, ClassSchedule> implements ClassScheduleService {
}
