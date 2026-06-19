package com.keshe.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.keshe.entity.DeviceDoor;
import com.keshe.mapper.DeviceDoorMapper;
import com.keshe.service.DeviceDoorService;
import org.springframework.stereotype.Service;

@Service
public class DeviceDoorServiceImpl extends ServiceImpl<DeviceDoorMapper, DeviceDoor> implements DeviceDoorService {
}
