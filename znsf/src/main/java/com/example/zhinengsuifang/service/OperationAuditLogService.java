package com.example.zhinengsuifang.service;

import com.example.zhinengsuifang.entity.OperationAuditLog;
import com.example.zhinengsuifang.mapper.OperationAuditLogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OperationAuditLogService {

    @Resource
    private OperationAuditLogMapper operationAuditLogMapper;

    public void record(OperationAuditLog log) {
        if (log == null) {
            return;
        }
        operationAuditLogMapper.insert(log);
    }

    public List<OperationAuditLog> latest(int limit) {
        int l = Math.max(1, Math.min(limit, 500));
        return operationAuditLogMapper.selectLatest(l);
    }

    public List<OperationAuditLog> range(LocalDateTime start, LocalDateTime end, int limit) {
        if (start == null || end == null) {
            return latest(limit);
        }
        int l = Math.max(1, Math.min(limit, 500));
        return operationAuditLogMapper.selectRange(start, end, l);
    }
}
