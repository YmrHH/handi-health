package com.example.zhinengsuifang.service;

import com.example.zhinengsuifang.dto.ChangePasswordRequest;
import com.example.zhinengsuifang.dto.CreateUserByDoctorRequest;
import com.example.zhinengsuifang.dto.LoginByIdRequest;
import com.example.zhinengsuifang.dto.LoginByPhoneRequest;
import com.example.zhinengsuifang.dto.RegisterRequest;

import java.util.Map;

/**
 * 认证相关業务接口。
 */
public interface AuthService {

    /**
     * 注册。
     *
     * @param request 注册请求
     * @return 统一返返结构：success/message
     */
    Map<String, Object> register(RegisterRequest request);

    /**
     * 医生创建新用户（患者/随访员）。
     *
     * @param request 创建请求
     * @return 统一返返结构：success/message/userId
     */
    Map<String, Object> createUserByDoctor(CreateUserByDoctorRequest request);

    /**
     * 登录校验。
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 统一返返结构：success/message/role/userId/name
     */
    Map<String, Object> login(String username, String password);

    /**
     * Web 端医生登录（基于手机号）。
     *
     * @param request 登录请求
     * @return 统一返返结构：success/message/role/userId/name
     */
    Map<String, Object> loginByPhone(LoginByPhoneRequest request);

    /**
     * 小程序端登录（基于 userId）。
     *
     * @param request 登录请求
     * @return 统一返返结构：success/message/role/userId/name
     */
    Map<String, Object> loginById(LoginByIdRequest request);

    /**
     * 小程序端修改密码（需要校验旧密码）。
     *
     * @param request 修改密码请求
     * @return 统一返返结构：success/message
     */
    Map<String, Object> changePassword(ChangePasswordRequest request);

    /**
     * 获取用户信息。
     *
     * @param userId 用户ID
     * @return 统一返返结构：success/message/data
     */
    Map<String, Object> getUserInfo(Long userId);
}

