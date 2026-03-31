package com.example.zhinengsuifang;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.zhinengsuifang.mapper")
/**
 * Spring Boot 应用启动入口。
 */
public class ZhinengsuifangApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhinengsuifangApplication.class, args);
    }

    @Bean
    public CommandLineRunner ensureInterventionRecommendTable(JdbcTemplate jdbcTemplate) {
        return args -> {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS intervention_recommend (" +
                            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                            "source_type VARCHAR(16) NOT NULL," +
                            "source_id BIGINT," +
                            "patient_id BIGINT NOT NULL," +
                            "patient_name VARCHAR(64)," +
                            "risk_level VARCHAR(16)," +
                            "trigger_reason VARCHAR(255)," +
                            "trigger_time DATETIME," +
                            "body_type VARCHAR(64)," +
                            "pattern VARCHAR(64)," +
                            "disease VARCHAR(64)," +
                            "doctor VARCHAR(64)," +
                            "status VARCHAR(16) NOT NULL DEFAULT 'PENDING'," +
                            "recommendation TEXT," +
                            "sent_time DATETIME," +
                            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                            "ext1 VARCHAR(255)," +
                            "ext2 VARCHAR(255)," +
                            "ext3 VARCHAR(255)," +
                            "ext4 VARCHAR(255)," +
                            "ext5 VARCHAR(255)," +
                            "UNIQUE KEY uk_intervention_recommend_source (source_type, source_id)," +
                            "INDEX idx_intervention_recommend_patient_status (patient_id, status, created_at)," +
                            "INDEX idx_intervention_recommend_status_created (status, created_at)" +
                            ")"
            );

            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS disease_type (" +
                            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                            "code VARCHAR(64) NOT NULL," +
                            "name VARCHAR(128) NOT NULL," +
                            "status INT NOT NULL DEFAULT 1," +
                            "sort_no INT NOT NULL DEFAULT 0," +
                            "remark VARCHAR(255)," +
                            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                            "ext1 VARCHAR(255)," +
                            "ext2 VARCHAR(255)," +
                            "ext3 VARCHAR(255)," +
                            "ext4 VARCHAR(255)," +
                            "ext5 VARCHAR(255)," +
                            "UNIQUE KEY uk_disease_type_code (code)," +
                            "INDEX idx_disease_type_status_sort (status, sort_no)" +
                            ")"
            );

            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS patient_message (" +
                            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                            "patient_id BIGINT NOT NULL," +
                            "title VARCHAR(128)," +
                            "content TEXT," +
                            "message_type VARCHAR(32)," +
                            "status VARCHAR(16) NOT NULL DEFAULT 'NEW'," +
                            "read_at DATETIME," +
                            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                            "ext1 VARCHAR(255)," +
                            "ext2 VARCHAR(255)," +
                            "ext3 VARCHAR(255)," +
                            "ext4 VARCHAR(255)," +
                            "ext5 VARCHAR(255)," +
                            "INDEX idx_patient_message_patient_created (patient_id, created_at)," +
                            "INDEX idx_patient_message_status_created (status, created_at)" +
                            ")"
            );

            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS operation_audit_log (" +
                            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                            "operator_id BIGINT," +
                            "operator_name VARCHAR(64)," +
                            "module VARCHAR(32)," +
                            "action VARCHAR(64)," +
                            "target VARCHAR(128)," +
                            "request_method VARCHAR(16)," +
                            "request_uri VARCHAR(255)," +
                            "request_query VARCHAR(1024)," +
                            "request_body TEXT," +
                            "success TINYINT NOT NULL DEFAULT 1," +
                            "status_code INT," +
                            "duration_ms BIGINT," +
                            "error_message VARCHAR(512)," +
                            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "ext1 VARCHAR(255)," +
                            "ext2 VARCHAR(255)," +
                            "ext3 VARCHAR(255)," +
                            "ext4 VARCHAR(255)," +
                            "ext5 VARCHAR(255)," +
                            "INDEX idx_operation_audit_log_operator_created (operator_id, created_at)," +
                            "INDEX idx_operation_audit_log_module_created (module, created_at)," +
                            "INDEX idx_operation_audit_log_success_created (success, created_at)" +
                            ")"
            );

            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS doctor_advice (" +
                            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                            "advice_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "doctor_id BIGINT," +
                            "doctor_name VARCHAR(64)," +
                            "title VARCHAR(255) NOT NULL," +
                            "description TEXT," +
                            "patients_json TEXT," +
                            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                            "ext1 VARCHAR(255)," +
                            "ext2 VARCHAR(255)," +
                            "ext3 VARCHAR(255)," +
                            "ext4 VARCHAR(255)," +
                            "ext5 VARCHAR(255)," +
                            "INDEX idx_doctor_advice_doctor_date (doctor_id, advice_date)," +
                            "INDEX idx_doctor_advice_date (advice_date)" +
                            ")"
            );

            try {
                Long cnt = jdbcTemplate.queryForObject("select count(*) from doctor_advice", Long.class);
                if (cnt == null || cnt == 0L) {
                    jdbcTemplate.update(
                            "insert into doctor_advice (advice_date, doctor_id, doctor_name, title, description, patients_json, created_at, updated_at) values (now(), null, '系统', '示例：个体化健康建议', '这是初始化示例数据。请在【健康建议下发】页面下发建议后，这里会显示真实记录。', '[\"示例患者\"]', now(), now())"
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    /**
     * 全局 CORS 配置，允许前端通过 axios 跨域访問后端
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        // Vue 開發/本地環境（兼容 localhost / 127.0.0.1 以及不同端口）
                        .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("Authorization", "Content-Type", "X-User-Id")
                        .allowCredentials(true)
                        .maxAge(3600);
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                String baseDir = System.getProperty("user.dir");
                String location = "file:" + baseDir + "/uploads/";
                registry.addResourceHandler("/uploads/**")
                        .addResourceLocations(location);
            }
        };
    }

}
