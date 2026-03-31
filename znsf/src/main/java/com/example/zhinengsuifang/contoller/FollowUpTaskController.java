package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.dto.CancelFollowUpTaskRequest;
import com.example.zhinengsuifang.dto.CreateFollowUpTaskRequest;
import com.example.zhinengsuifang.dto.RepublishFollowUpTaskRequest;
import com.example.zhinengsuifang.entity.User;
import com.example.zhinengsuifang.mapper.FollowUpTaskMapper;
import com.example.zhinengsuifang.mapper.UserMapper;
import com.example.zhinengsuifang.service.FollowUpTaskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/followUpTask")
@Tag(name = "随访任务", description = "随访任务（派单）相关接口")
/**
 * 随访任务（派单）相关接口。
 * <p>
 * 提供医生派单、医生查询派单列表、随访员查询自己的任务、更新任务状态等能力。
 * </p>
 */
public class FollowUpTaskController {

    @Resource
    private FollowUpTaskService followUpTaskService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private FollowUpTaskMapper followUpTaskMapper;

    @PostMapping("/create")
    /**
     * 医生为患者创建随访任务（派单）。
     *
     * @param request 派单请求
     * @return 统一返返结构：success/message
     */
    public Map<String, Object> create(@RequestBody CreateFollowUpTaskRequest request) {
        return followUpTaskService.createTask(request);
    }

    @GetMapping("/doctor/list")
    /**
     * 医生查询自己的派单列表。
     *
     * @param doctorUsername 医生账号
     * @param doctorPassword 医生密码
     * @return 统一返返结构：success/message/data
     */
    public Map<String, Object> doctorList(@RequestParam String doctorUsername, @RequestParam String doctorPassword) {
        return followUpTaskService.listTasksByDoctor(doctorUsername, doctorPassword);
    }

    @GetMapping("/followup/my")
    /**
     * 随访员查询自己的任务列表。
     *
     * @param followUpUsername 随访员账号
     * @param followUpPassword 随访员密码
     * @return 统一返返结构：success/message/data
     */
    public Map<String, Object> followUpMy(jakarta.servlet.http.HttpServletRequest request,
                                          @RequestParam(required = false) String followUpUsername,
                                          @RequestParam(required = false) String followUpPassword) {
        // 1) 优先 Header 鉴权：Bearer <userId>
        Long userId = com.example.zhinengsuifang.util.AuthHeaderUtil.getUserId(request);
        if (userId != null) {
            User u = userMapper.findById(userId);
            if (u != null && u.getRole() != null && "FOLLOW_UP".equalsIgnoreCase(u.getRole().trim())) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("code", com.example.zhinengsuifang.ApiCode.SUCCESS.getCode());
                result.put("message", "查询成功");
                result.put("data", followUpTaskMapper.findByFollowUpUserId(u.getId()));
                return result;
            }
        }

        // 2) 回退到原账号密码二次校验
        return followUpTaskService.listMyTasks(followUpUsername, followUpPassword);
    }

    @PostMapping("/republish")
    public Map<String, Object> republish(@RequestBody RepublishFollowUpTaskRequest request) {
        return followUpTaskService.republishTask(request);
    }

    @PostMapping("/cancel")
    public Map<String, Object> cancel(@RequestBody CancelFollowUpTaskRequest request) {
        return followUpTaskService.cancelTask(request);
    }
}

