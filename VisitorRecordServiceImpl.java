package com.keshe.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.keshe.entity.VisitorRecord;
import com.keshe.mapper.VisitorRecordMapper;
import com.keshe.service.VisitorRecordService;
import org.springframework.stereotype.Service;

@Service
public class VisitorRecordServiceImpl extends ServiceImpl<VisitorRecordMapper, VisitorRecord> implements VisitorRecordService {
}
