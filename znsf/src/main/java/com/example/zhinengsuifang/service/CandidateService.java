package com.example.zhinengsuifang.service;

import java.util.Map;

/**
 * 候選患者相关業务接口。
 */
public interface CandidateService {

    /**
     * 查询候選患者列表（医生端）。
     *
     * @param doctorUsername 医生账号
     * @param doctorPassword 医生密码
     * @return 统一返返结构：success/message/data
     */
    Map<String, Object> candidatePatients(String doctorUsername, String doctorPassword);
}

