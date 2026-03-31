package com.example.zhinengsuifang.service;

import com.example.zhinengsuifang.dto.CreateFollowUpFromAlertRequest;
import com.example.zhinengsuifang.dto.MarkHealthAlertRequest;

import java.util.Map;

/**
 * 健康预警相关業务接口。
 */
public interface HealthAlertService {

    /**
     * 医生查询预警列表。
     *
     * @param doctorUsername 医生账号
     * @param doctorPassword 医生密码
     * @param status 可選状态过濾
     * @return 统一返返结构：success/message/data
     */
    Map<String, Object> listAlerts(String doctorUsername, String doctorPassword, String status);

    Map<String, Object> listAlertsByDoctorId(Long doctorId, String status);

    /**
     * 医生根据患者姓名搜索预警列表。
     *
     * @param doctorUsername 医生账号
     * @param doctorPassword 医生密码
     * @param patientName    患者姓名
     * @param status         可選状态过濾
     * @param limit          限制返回的记录数量
     * @return 统一返返结构：success/message/data
     */
    Map<String, Object> searchAlertsByPatientName(String doctorUsername,
                                                  String doctorPassword,
                                                  String patientName,
                                                  String status,
                                                  Integer limit);

    Map<String, Object> searchAlertsByPatientNameByDoctorId(Long doctorId,
                                                            String patientName,
                                                            String status,
                                                            Integer limit);

    /**
     * 由预警生成随访单。
     *
     * @param request 生成请求
     * @return 统一返返结构：success/message
     */
    Map<String, Object> createFollowUpFromAlert(CreateFollowUpFromAlertRequest request);

    Map<String, Object> createFollowUpFromAlertByDoctorId(Long doctorId,
                                                          Long alertId,
                                                          Long followUpUserId,
                                                          String description,
                                                          java.time.LocalDateTime dueAt);

    /**
     * 标记预警状态（例如 REVIEWED/IGNORED）。
     *
     * @param request 标记请求
     * @return 统一返返结构：success/message
     */
    Map<String, Object> markAlert(MarkHealthAlertRequest request);

    Map<String, Object> markAlertByDoctorId(Long doctorId, Long alertId, String status);

    Map<String, Object> getAlertForm(String doctorUsername, String doctorPassword, Long alertId);

    Map<String, Object> getAlertFormByDoctorId(Long doctorId, Long alertId);
}
