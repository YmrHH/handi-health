package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.util.ApiResponseUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/common")
public class CommonController {

    @GetMapping("/server-time")
    public Map<String, Object> serverTime() {
        Map<String, Object> data = new HashMap<>();
        data.put("serverTime", LocalDateTime.now().toString());
        return ApiResponseUtil.ok(data);
    }
}
