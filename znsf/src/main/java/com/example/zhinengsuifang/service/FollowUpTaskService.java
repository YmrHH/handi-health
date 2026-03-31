package com.example.zhinengsuifang.service;

import com.example.zhinengsuifang.dto.CreateFollowUpTaskRequest;
import com.example.zhinengsuifang.dto.CancelFollowUpTaskRequest;
import com.example.zhinengsuifang.dto.RepublishFollowUpTaskRequest;

import java.util.Map;

/**
 * 随访任务（派单）相关業务接口。
 */
public interface FollowUpTaskService {

    /**
     * 医生派单：创建随访任务。
     *
     * @param request 派单请求
     * @return 统一返返结构：success/message
     */
    Map<String, Object> createTask(CreateFollowUpTaskRequest request);

    /**
     * 医生查询自己的派单列表。
     *
     * @param doctorUsername 医生账号
     * @param doctorPassword 医生密码
     * @return 统一返返结构：success/message/data
     */
    Map<String, Object> listTasksByDoctor(String doctorUsername, String doctorPassword);

    /**
     * 随访员查询自己的任务列表。
     *
     * @param followUpUsername 随访员账号
     * @param followUpPassword 随访员密码
     * @return 统一返返结构：success/message/data
     */
    Map<String, Object> listMyTasks(String followUpUsername, String followUpPassword);

    Map<String, Object> republishTask(RepublishFollowUpTaskRequest request);

    /**
     * 医生取消任务。
     *
     * @param request 取消任务请求
     * @return 统一返返结构：success/message
     */
    Map<String, Object> cancelTask(CancelFollowUpTaskRequest request);
}

