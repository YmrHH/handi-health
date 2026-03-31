package com.example.zhinengsuifang.mapper;

import com.example.zhinengsuifang.dto.PatientBrief;
import com.example.zhinengsuifang.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户表相关 Mapper。
 */
public interface UserMapper {
    @Select("select id, username, password, name, address, age, sex, phone, risk_level as riskLevel, role, status from user")
    /**
     * 查询所有用户。
     *
     * @return 用户列表
     */
    List<User> findAll();

    @Insert("INSERT INTO user (username, password, name, address, age, sex, phone, risk_level, role, status, created_at, updated_at) " +
            "VALUES (#{username}, #{password}, #{name}, #{address}, #{age}, #{sex}, #{phone}, #{riskLevel}, #{role}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /**
     * 新增用户（id 自增返填到 user.id）。
     *
     * @param user 用户
     */
    void insert(User user);

    @Select("select id, username, password, name, address, age, sex, phone, risk_level as riskLevel, role, status from user where username = #{username}")
    /**
     * 按 username 查询用户。
     *
     * @param username 登录账号
     * @return 用户（不存在返返 null）
     */
    User findByUsername(@Param("username") String username);

    @Select("select id, username, password, name, address, age, sex, phone, risk_level as riskLevel, role, status from user where phone = #{phone}")
    User findByPhone(@Param("phone") String phone);

    @Select("select id, username, password, name, address, age, sex, phone, risk_level as riskLevel, role, status from user where id = #{id}")
    /**
     * 按 id 查询用户。
     *
     * @param id 用户 id
     * @return 用户（不存在返返 null）
     */
    User findById(@Param("id") Long id);

     @Select("select id as id, username as username, name as name, phone as phone, status as status from user where role = 'DOCTOR' order by id desc")
     List<Map<String, Object>> selectDoctorsForOrgUser();

    @Select("select id from user where role = 'FOLLOW_UP' order by id asc limit 1")
    Long findFirstFollowUpUserId();

    @Update("update user set password = #{password} where id = #{id}")
    int updatePasswordById(@Param("id") Long id, @Param("password") String password);

    @Update("update user set status = #{status}, updated_at = NOW() where id = #{id}")
    int updateStatusById(@Param("id") Long id, @Param("status") Integer status);

    @Update("update user set name = #{name}, address = #{address}, age = #{age}, sex = #{sex}, phone = #{phone}, updated_at = NOW() where id = #{id}")
    int updateProfileById(@Param("id") Long id,
                          @Param("name") String name,
                          @Param("address") String address,
                          @Param("age") Integer age,
                          @Param("sex") String sex,
                          @Param("phone") String phone);

    @Update("update user set risk_level = #{riskLevel}, updated_at = NOW() where id = #{id}")
    int updateRiskLevelById(@Param("id") Long id, @Param("riskLevel") String riskLevel);

    /**
     * 统计患者总数（role = 'PATIENT'）
     */
    @Select("select count(*) from user where role = 'PATIENT'")
    Long countPatients();

    @Select("select count(*) from user where role = 'PATIENT' and created_at >= #{startAt} and created_at < #{endAt}")
    Long countPatientsCreatedInRange(@Param("startAt") java.time.LocalDateTime startAt,
                                     @Param("endAt") java.time.LocalDateTime endAt);

    @Select("select id from user where role = 'PATIENT'")
    /**
     * 查询所有患者 id 列表。
     *
     * @return 患者 id 列表
     */
    List<Long> findPatientIds();

    @Select("select risk_level as riskLevel, count(*) as count from user where role = 'PATIENT' group by risk_level")
    /**
     * 按风险等级统计患者数量。
     *
     * @return 统计行列表
     */
    List<com.example.zhinengsuifang.dto.RiskLevelCount> countPatientsGroupByRiskLevel();

    @Select("select id, username, password, name, address, age, sex, phone, risk_level as riskLevel, role, status from user where id = #{id} and role = 'PATIENT'")
    User findPatientById(@Param("id") Long id);

    @Select("<script>" +
            "select count(*) from user where role = 'PATIENT' " +
            "<if test='keyword != null and keyword.trim() != \"\"'>" +
            "  and (name like concat('%', #{keyword}, '%') or phone like concat('%', #{keyword}, '%') or username like concat('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='riskLevel != null and riskLevel.trim() != \"\"'>" +
            "  and upper(risk_level) = upper(#{riskLevel}) " +
            "</if>" +
            "<if test='disease != null and disease.trim() != \"\"'>" +
            "  and exists (select 1 from patient_basic_info pbi where pbi.patient_id = id and trim(pbi.ext3) = trim(#{disease})) " +
            "</if>" +
            "<if test='syndrome != null and syndrome.trim() != \"\"'>" +
            "  and exists (select 1 from patient_basic_info pbi2 where pbi2.patient_id = id and trim(pbi2.ext4) = trim(#{syndrome})) " +
            "</if>" +
            "</script>")
    Long countPatientSummary(@Param("keyword") String keyword,
                             @Param("riskLevel") String riskLevel,
                             @Param("disease") String disease,
                             @Param("syndrome") String syndrome,
                             @Param("responsibleDoctor") String responsibleDoctor);

    @Select("<script>" +
            "select " +
            "  u.id as patientId, " +
            "  u.name as name, " +
            "  u.sex as gender, " +
            "  u.age as age, " +
            "  u.phone as phone, " +
            "  pbi.id_card as idCard, " +
            "  pbi.ext3 as disease, " +
            "  pbi.ext4 as syndrome, " +
            "  u.id as riskId, " +
            "  u.risk_level as riskLevel, " +
            "  null as latestIndex, " +
            "  (select max(s.due_at) from follow_up_schedule s where s.patient_id = u.id) as lastVisitDate, " +
            "  coalesce(pbi.ext2, (select d.name from follow_up_task t left join user d on d.id = t.doctor_id where t.patient_id = u.id order by t.created_at desc limit 1), '') as responsibleDoctor, " +
            "  0 as followupCount, " +
            "  null as lastFollowupTime, " +
            "  0 as activeAlertCount, " +
            "  0 as pendingTaskCount, " +
            "  null as latestSbp, " +
            "  null as latestDbp, " +
            "  null as latestHeartRate, " +
            "  null as latestWeight " +
            "from user u left join patient_basic_info pbi on pbi.patient_id = u.id " +
            "where u.role = 'PATIENT' " +
            "<if test='keyword != null and keyword.trim() != \"\"'>" +
            "  and (u.name like concat('%', #{keyword}, '%') or u.phone like concat('%', #{keyword}, '%') or u.username like concat('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='riskLevel != null and riskLevel.trim() != \"\"'>" +
            "  and upper(u.risk_level) = upper(#{riskLevel}) " +
            "</if>" +
            "<if test='disease != null and disease.trim() != \"\"'>" +
            "  and pbi.ext3 is not null and trim(pbi.ext3) = trim(#{disease}) " +
            "</if>" +
            "<if test='syndrome != null and syndrome.trim() != \"\"'>" +
            "  and pbi.ext4 is not null and trim(pbi.ext4) = trim(#{syndrome}) " +
            "</if>" +
            "<if test='responsibleDoctor != null and responsibleDoctor.trim() != \"\"'>" +
            "  and coalesce(pbi.ext2, (select d.name from follow_up_task t left join user d on d.id = t.doctor_id where t.patient_id = u.id order by t.created_at desc limit 1), '') = #{responsibleDoctor} " +
            "</if>" +
            "order by u.id desc " +
            "limit #{limit} offset #{offset}" +
            "</script>")
    List<Map<String, Object>> selectPatientSummaryPage(@Param("keyword") String keyword,
                                                       @Param("riskLevel") String riskLevel,
                                                       @Param("disease") String disease,
                                                       @Param("syndrome") String syndrome,
                                                       @Param("responsibleDoctor") String responsibleDoctor,
                                                       @Param("offset") Integer offset,
                                                       @Param("limit") Integer limit);

    @Select("select max(due_at) from follow_up_schedule where patient_id = #{patientId}")
    LocalDateTime findPatientLastFollowTime(@Param("patientId") Long patientId);

    @Select("select coalesce(" +
            "  (select pbi.ext2 from patient_basic_info pbi where pbi.patient_id = #{patientId} and pbi.ext2 is not null and trim(pbi.ext2) <> '' limit 1)," +
            "  (select d.name from follow_up_task t left join user d on d.id = t.doctor_id where t.patient_id = #{patientId} order by t.created_at desc limit 1)," +
            "  ''" +
            ")")
    String findPatientResponsibleDoctorName(@Param("patientId") Long patientId);

    @Select("<script>" +
            "select count(*) from user where role = 'FOLLOW_UP' " +
            "<if test='keyword != null and keyword.trim() != \"\"'>" +
            "  and (name like concat('%', #{keyword}, '%') or phone like concat('%', #{keyword}, '%') or username like concat('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='status != null'> and status = #{status} </if>" +
            "</script>")
    Long countFollowUpStaff(@Param("keyword") String keyword,
                            @Param("status") Integer status);

    @Select("<script>" +
            "select count(*) " +
            "from patient_followup_record r " +
            "join user u on u.id = r.staff_id " +
            "where u.role = 'FOLLOW_UP' " +
            "<if test='keyword != null and keyword.trim() != \"\"'>" +
            "  and (u.name like concat('%', #{keyword}, '%') or u.phone like concat('%', #{keyword}, '%') or u.username like concat('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='status != null'> and u.status = #{status} </if>" +
            "</script>")
    Long countFollowupRecordsForStaff(@Param("keyword") String keyword,
                                     @Param("status") Integer status);

    @Select("<script>" +
            "select count(*) " +
            "from patient_followup_record r " +
            "join user u on u.id = r.staff_id " +
            "where u.role = 'FOLLOW_UP' " +
            "and date_format(r.followup_time, '%Y-%m') = date_format(now(), '%Y-%m') " +
            "<if test='keyword != null and keyword.trim() != \"\"'>" +
            "  and (u.name like concat('%', #{keyword}, '%') or u.phone like concat('%', #{keyword}, '%') or u.username like concat('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='status != null'> and u.status = #{status} </if>" +
            "</script>")
    Long countFollowupRecordsForStaffThisMonth(@Param("keyword") String keyword,
                                              @Param("status") Integer status);

    @Select("<script>" +
            "select count(distinct r.staff_id) " +
            "from patient_followup_record r " +
            "join user u on u.id = r.staff_id " +
            "where u.role = 'FOLLOW_UP' " +
            "and date_format(r.followup_time, '%Y-%m') = date_format(now(), '%Y-%m') " +
            "<if test='keyword != null and keyword.trim() != \"\"'>" +
            "  and (u.name like concat('%', #{keyword}, '%') or u.phone like concat('%', #{keyword}, '%') or u.username like concat('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='status != null'> and u.status = #{status} </if>" +
            "</script>")
    Long countActiveStaffThisMonth(@Param("keyword") String keyword,
                                  @Param("status") Integer status);

    @Select("<script>" +
            "select count(distinct t.patient_id) " +
            "from follow_up_task t " +
            "join user u on u.id = t.follow_up_user_id " +
            "where u.role = 'FOLLOW_UP' " +
            "<if test='keyword != null and keyword.trim() != \"\"'>" +
            "  and (u.name like concat('%', #{keyword}, '%') or u.phone like concat('%', #{keyword}, '%') or u.username like concat('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='status != null'> and u.status = #{status} </if>" +
            "</script>")
    Long countDistinctAssignedPatientsForStaff(@Param("keyword") String keyword,
                                              @Param("status") Integer status);

    @Select("<script>" +
            "select " +
            "  u.id as id, " +
            "  u.name as name, " +
            "  u.username as jobNo, " +
            "  u.phone as mobile, " +
            "  '' as orgName, " +
            "  ifnull(u.address, '') as deptName, " +
            "  ifnull(u.address, '') as department, " +
            "  'FOLLOW_UP' as role, " +
            "  u.status as status, " +
            "  0 as currentTaskCount, " +
            "  (select count(distinct t.patient_id) from follow_up_task t where t.follow_up_user_id = u.id) as patientCount, " +
            "  (select count(*) from patient_followup_record r where r.staff_id = u.id) as totalFollowupCount, " +
            "  (select count(*) from patient_followup_record r where r.staff_id = u.id and date_format(r.followup_time, '%Y-%m') = date_format(now(), '%Y-%m')) as monthFollowupCount " +
            "from user u " +
            "where u.role = 'FOLLOW_UP' " +
            "<if test='keyword != null and keyword.trim() != \"\"'>" +
            "  and (u.name like concat('%', #{keyword}, '%') or u.phone like concat('%', #{keyword}, '%') or u.username like concat('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='status != null'> and u.status = #{status} </if>" +
            "order by u.id desc " +
            "limit #{limit} offset #{offset}" +
            "</script>")
    List<Map<String, Object>> selectFollowUpStaffPage(@Param("keyword") String keyword,
                                                      @Param("status") Integer status,
                                                      @Param("offset") Integer offset,
                                                      @Param("limit") Integer limit);

    @Select("select count(distinct t.patient_id) from follow_up_task t where t.follow_up_user_id = #{staffId}")
    Long countAssignedPatientCountForStaff(@Param("staffId") Long staffId);

    @Select("select count(*) from patient_followup_record r where r.staff_id = #{staffId}")
    Long countFollowupCountForStaff(@Param("staffId") Long staffId);

    @Select("select count(*) from patient_followup_record r " +
            "where r.staff_id = #{staffId} " +
            "and date_format(r.followup_time, '%Y-%m') = date_format(now(), '%Y-%m')")
    Long countMonthFollowupCountForStaff(@Param("staffId") Long staffId);

    @Select("select count(*) from patient_followup_record r " +
            "where r.staff_id = #{staffId} " +
            "and upper(coalesce(r.result_status,'')) in ('COMPLETED','FINISHED','DONE','SUCCESS')")
    Long countCompletedFollowupCountForStaff(@Param("staffId") Long staffId);

    @Select("<script>" +
            "select " +
            "  p.id as patientId, " +
            "  p.name as patientName, " +
            "  p.risk_level as riskLevel, " +
            "  (select date_format(max(r.followup_time), '%Y-%m-%d %H:%i:%s') from patient_followup_record r where r.staff_id = #{staffId} and r.patient_id = p.id) as lastFollowupTime, " +
            "  (select count(*) from patient_followup_record r where r.staff_id = #{staffId} and r.patient_id = p.id) as followupCount " +
            "from follow_up_task t " +
            "join user p on p.id = t.patient_id " +
            "where t.follow_up_user_id = #{staffId} " +
            "group by p.id, p.name, p.risk_level " +
            "order by p.id desc " +
            "limit #{limit}" +
            "</script>")
    List<Map<String, Object>> selectAssignedPatientsForStaff(@Param("staffId") Long staffId,
                                                            @Param("limit") Integer limit);

    @Select("select " +
            "  r.id as id, " +
            "  date_format(r.followup_time, '%Y-%m-%d %H:%i:%s') as followupTime, " +
            "  p.name as patientName, " +
            "  r.followup_type as followupType, " +
            "  r.result_status as resultStatus, " +
            "  r.risk_level as riskLevel " +
            "from patient_followup_record r " +
            "left join user p on p.id = r.patient_id " +
            "where r.staff_id = #{staffId} " +
            "order by r.followup_time desc, r.id desc " +
            "limit #{limit}")
    List<Map<String, Object>> selectRecentFollowupsForStaff(@Param("staffId") Long staffId,
                                                           @Param("limit") Integer limit);

    @Select("select id as id, name as name, phone as phone, risk_level as riskLevel " +
            "from user " +
            "where role = 'PATIENT' " +
            "and (#{keyword} is null or #{keyword} = '' " +
            "  or name like concat('%', #{keyword}, '%') " +
            "  or phone like concat('%', #{keyword}, '%') " +
            "  or username like concat('%', #{keyword}, '%')) " +
            "order by id desc limit #{limit}")
    List<PatientBrief> searchPatients(@Param("keyword") String keyword, @Param("limit") Integer limit);

    @Select("select " +
            "  u.id as patientId, " +
            "  u.name as patientName, " +
            "  '风险等级变为高危' as changeDesc, " +
            "  date_format(u.updated_at, '%Y-%m-%d %H:%i:%s') as timeText " +
            "from user u " +
            "where u.role = 'PATIENT' " +
            "and u.risk_level = 'HIGH' " +
            "and u.updated_at >= date_sub(now(), interval 7 day) " +
            "order by u.updated_at desc " +
            "limit #{limit}")
    List<Map<String, Object>> selectRecentHighRiskPatients(@Param("limit") Integer limit);
}
