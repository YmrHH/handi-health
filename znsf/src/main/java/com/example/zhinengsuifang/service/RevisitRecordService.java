package com.example.zhinengsuifang.service;

import com.example.zhinengsuifang.entity.RevisitRecord;

import java.util.Map;

/**
 * 复诊记录相关業务接口。
 */
public interface RevisitRecordService {

    /**
     * 医生新增患者复诊记录。
     *
     * @param record 复诊记录
     * @param doctorUsername 医生账号
     * @param doctorPassword 医生密码
     * @return 统一返返结构：success/message
     */
    Map<String, Object> addRevisitRecord(RevisitRecord record, String doctorUsername, String doctorPassword);
}

