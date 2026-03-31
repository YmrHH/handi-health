package com.example.zhinengsuifang.service;

import com.example.zhinengsuifang.dto.PatientBrief;
import com.example.zhinengsuifang.entity.User;

import java.util.List;
import java.util.Map;

/**
 * 用户相关業务接口。
 */
public interface UserService {

    /**
     * 查询所有用户。
     *
     * @return 用户列表
     */
    List<User> findAll();

    /**
     * 新增用户。
     *
     * @param user 用户信息
     */
    void addUser(User user);

    /**
     * 统计患者总数。
     *
     * @return 患者数量
     */
    Long countPatients();

    /**
     * 患者风险等级统计。
     *
     * @return 统计结果（key/value 结构）
     */
    Map<String, Object> patientRiskStats();

    User findPatientById(Long id);

    List<PatientBrief> searchPatients(String keyword, Integer limit);

    Map<String, Object> getPatientByIdForDoctor(String doctorUsername, String doctorPassword, Long patientId);

    Map<String, Object> searchPatientsForDoctor(String doctorUsername, String doctorPassword, String keyword, Integer limit);
}



