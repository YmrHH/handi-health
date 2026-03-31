package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.ApiCode;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/hybridaction")
public class HybridActionController {

    @RequestMapping(value = "/zybTrackerStatisticsAction", method = {RequestMethod.GET, RequestMethod.POST})
    public Map<String, Object> zybTrackerStatisticsAction() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "ok");
        result.put("data", new HashMap<>());
        return result;
    }
}
