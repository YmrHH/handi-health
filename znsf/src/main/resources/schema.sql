-- 中医四诊表
CREATE TABLE IF NOT EXISTS tcm_diagnosis (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT,
    doctor_name VARCHAR(64),
    diagnosis_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    main_complaint TEXT COMMENT '主诉',
    tongue_description TEXT COMMENT '舌像描述',
    tongue_image_url VARCHAR(255) COMMENT '舌像图片URL',
    pulse_description TEXT COMMENT '脉象描述',
    tcm_summary TEXT COMMENT '证候总结',
    physical_exam TEXT COMMENT '体格检查',
    lifestyle TEXT COMMENT '生活习惯',
    status VARCHAR(16) NOT NULL DEFAULT 'COMPLETED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ext1 VARCHAR(255),
    ext2 VARCHAR(255),
    ext3 VARCHAR(255),
    ext4 VARCHAR(255),
    ext5 VARCHAR(255),
    INDEX idx_tcm_diagnosis_patient (patient_id),
    INDEX idx_tcm_diagnosis_date (diagnosis_date)
);

-- 患者消息表（已存在）
CREATE TABLE IF NOT EXISTS patient_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    patient_id BIGINT NOT NULL,
    title VARCHAR(128),
    content TEXT,
    message_type VARCHAR(32),
    status VARCHAR(16) NOT NULL DEFAULT 'NEW',
    read_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ext1 VARCHAR(255),
    ext2 VARCHAR(255),
    ext3 VARCHAR(255),
    ext4 VARCHAR(255),
    ext5 VARCHAR(255),
    INDEX idx_patient_message_patient_created (patient_id, created_at),
    INDEX idx_patient_message_status_created (status, created_at)
);

-- 操作审计日志表（已存在）
CREATE TABLE IF NOT EXISTS operation_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_id BIGINT,
    operator_name VARCHAR(64),
    module VARCHAR(32),
    action VARCHAR(64),
    target VARCHAR(128),
    request_method VARCHAR(16),
    request_uri VARCHAR(255),
    request_query VARCHAR(1024),
    request_body TEXT,
    success TINYINT NOT NULL DEFAULT 1,
    status_code INT,
    duration_ms BIGINT,
    error_message VARCHAR(512),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ext1 VARCHAR(255),
    ext2 VARCHAR(255),
    ext3 VARCHAR(255),
    ext4 VARCHAR(255),
    ext5 VARCHAR(255),
    INDEX idx_operation_audit_log_operator_created (operator_id, created_at),
    INDEX idx_operation_audit_log_module_created (module, created_at),
    INDEX idx_operation_audit_log_success_created (success, created_at)
);

-- 医生建议表（已存在）
CREATE TABLE IF NOT EXISTS doctor_advice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    advice_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    doctor_id BIGINT,
    doctor_name VARCHAR(64),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    patients_json TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ext1 VARCHAR(255),
    ext2 VARCHAR(255),
    ext3 VARCHAR(255),
    ext4 VARCHAR(255),
    ext5 VARCHAR(255),
    INDEX idx_doctor_advice_doctor_date (doctor_id, advice_date),
    INDEX idx_doctor_advice_date (advice_date)
);

-- 干预建议表（已存在）
CREATE TABLE IF NOT EXISTS intervention_recommend (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_type VARCHAR(16) NOT NULL,
    source_id BIGINT,
    patient_id BIGINT NOT NULL,
    patient_name VARCHAR(64),
    risk_level VARCHAR(16),
    trigger_reason VARCHAR(255),
    trigger_time DATETIME,
    body_type VARCHAR(64),
    pattern VARCHAR(64),
    disease VARCHAR(64),
    doctor VARCHAR(64),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    recommendation TEXT,
    sent_time DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ext1 VARCHAR(255),
    ext2 VARCHAR(255),
    ext3 VARCHAR(255),
    ext4 VARCHAR(255),
    ext5 VARCHAR(255),
    UNIQUE KEY uk_intervention_recommend_source (source_type, source_id),
    INDEX idx_intervention_recommend_patient_status (patient_id, status, created_at),
    INDEX idx_intervention_recommend_status_created (status, created_at)
);

-- 疾病类型表（已存在）
CREATE TABLE IF NOT EXISTS disease_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    status INT NOT NULL DEFAULT 1,
    sort_no INT NOT NULL DEFAULT 0,
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ext1 VARCHAR(255),
    ext2 VARCHAR(255),
    ext3 VARCHAR(255),
    ext4 VARCHAR(255),
    ext5 VARCHAR(255),
    UNIQUE KEY uk_disease_type_code (code),
    INDEX idx_disease_type_status_sort (status, sort_no)
);