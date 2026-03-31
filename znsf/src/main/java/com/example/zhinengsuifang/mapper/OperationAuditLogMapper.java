package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.entity.OperationAuditLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface OperationAuditLogMapper {

    @Insert("INSERT INTO operation_audit_log (operator_id, operator_name, module, action, target, request_method, request_uri, request_query, request_body, success, status_code, duration_ms, error_message, created_at) " +
            "VALUES (#{operatorId}, #{operatorName}, #{module}, #{action}, #{target}, #{requestMethod}, #{requestUri}, #{requestQuery}, #{requestBody}, #{success}, #{statusCode}, #{durationMs}, #{errorMessage}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(OperationAuditLog log);

    @Select("SELECT id, operator_id as operatorId, operator_name as operatorName, module, action, target, request_method as requestMethod, request_uri as requestUri, request_query as requestQuery, request_body as requestBody, success, status_code as statusCode, duration_ms as durationMs, error_message as errorMessage, created_at as createdAt " +
            "FROM operation_audit_log " +
            "WHERE created_at >= #{start} AND created_at <= #{end} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<OperationAuditLog> selectRange(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end,
                                       @Param("limit") int limit);

    @Select("SELECT id, operator_id as operatorId, operator_name as operatorName, module, action, target, request_method as requestMethod, request_uri as requestUri, request_query as requestQuery, request_body as requestBody, success, status_code as statusCode, duration_ms as durationMs, error_message as errorMessage, created_at as createdAt " +
            "FROM operation_audit_log " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<OperationAuditLog> selectLatest(@Param("limit") int limit);
}
