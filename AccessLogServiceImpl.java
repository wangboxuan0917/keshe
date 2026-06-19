package com.keshe.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.keshe.entity.AccessLog;
import com.keshe.mapper.AccessLogMapper;
import com.keshe.service.AccessLogService;
import org.springframework.stereotype.Service;

@Service
public class AccessLogServiceImpl extends ServiceImpl<AccessLogMapper, AccessLog> implements AccessLogService {
}
