package com.example.zhinengsuifang.service;

import com.example.zhinengsuifang.entity.DoctorAdvice;
import com.example.zhinengsuifang.mapper.DoctorAdviceMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DoctorAdviceService {

    @Resource
    private DoctorAdviceMapper doctorAdviceMapper;

    public int create(DoctorAdvice advice) {
        if (advice == null) {
            return 0;
        }
        if (advice.getAdviceDate() == null) {
            advice.setAdviceDate(LocalDateTime.now());
        }
        return doctorAdviceMapper.insert(advice);
    }

    public List<DoctorAdvice> latest(int limit) {
        int l = Math.max(1, Math.min(limit, 500));
        return doctorAdviceMapper.selectLatest(l);
    }

    public DoctorAdvice getById(Long id) {
        if (id == null) {
            return null;
        }
        return doctorAdviceMapper.findById(id);
    }

    public Long count(Long doctorId, String keyword, LocalDateTime startAt, LocalDateTime endAt) {
        return doctorAdviceMapper.count(doctorId, keyword, startAt, endAt);
    }

    public List<DoctorAdvice> page(Long doctorId, String keyword, LocalDateTime startAt, LocalDateTime endAt, int offset, int limit) {
        int l = Math.max(1, Math.min(limit, 200));
        int o = Math.max(0, offset);
        return doctorAdviceMapper.selectPage(doctorId, keyword, startAt, endAt, o, l);
    }
}
