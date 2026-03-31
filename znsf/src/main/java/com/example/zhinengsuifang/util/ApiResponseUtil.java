package com.example.zhinengsuifang.util;

import com.example.zhinengsuifang.ApiCode;

import java.util.HashMap;
import java.util.Map;

public class ApiResponseUtil {

    private ApiResponseUtil() {
    }

    public static Map<String, Object> ok(Object data) {
        Map<String, Object> r = new HashMap<>();
        r.put("code", ApiCode.SUCCESS.getCode());
        r.put("msg", "ok");
        r.put("data", data);
        return r;
    }

    public static Map<String, Object> fail(ApiCode code, String msg) {
        Map<String, Object> r = new HashMap<>();
        r.put("code", code.getCode());
        r.put("msg", msg == null || msg.isEmpty() ? code.getMessage() : msg);
        return r;
    }
}
