package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.entity.User;
import com.example.zhinengsuifang.mapper.DiseaseTypeMapper;
import com.example.zhinengsuifang.mapper.UserMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/disease-type")
@Tag(name = "病种", description = "病种类型字典")
public class ApiDiseaseTypeController {

    @Resource
    private DiseaseTypeMapper diseaseTypeMapper;

    @Resource
    private UserMapper userMapper;

    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam(required = false, name = "q") String q,
                                      @RequestParam(required = false) Integer limit) {
        String keyword = q == null ? "" : q.trim();
        int lim = limit == null ? 20 : Math.max(1, Math.min(limit, 100));

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = new HashMap<>();

        if (keyword.isEmpty()) {
            data.put("rows", new ArrayList<>());
            result.put("success", true);
            result.put("code", ApiCode.SUCCESS.getCode());
            result.put("message", "查询成功");
            result.put("data", data);
            return result;
        }

        List<Map<String, Object>> rows = diseaseTypeMapper.search(keyword, lim);
        List<Map<String, Object>> options = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                if (r == null) {
                    continue;
                }
                Map<String, Object> item = new HashMap<>();
                Object id = r.get("id");
                Object code = r.get("code");
                Object name = r.get("name");
                String codeStr = code == null ? "" : String.valueOf(code).trim();
                String nameStr = name == null ? "" : String.valueOf(name).trim();
                item.put("id", id);
                item.put("code", codeStr);
                item.put("name", nameStr);
                item.put("value", nameStr);
                item.put("label", nameStr);
                options.add(item);
            }
        }

        data.put("rows", options);
        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "查询成功");
        result.put("data", data);
        return result;
    }

    @PostMapping("")
    public Map<String, Object> create(jakarta.servlet.http.HttpServletRequest request,
                                      @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        if (body == null) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "参数错誤");
            return result;
        }

        String name = body.get("name") == null ? "" : String.valueOf(body.get("name")).trim();
        if (name.isEmpty()) {
            result.put("success", false);
            result.put("code", ApiCode.PARAM_ERROR.getCode());
            result.put("message", "name不能为空");
            return result;
        }

        Long userId = com.example.zhinengsuifang.util.AuthHeaderUtil.getUserId(request);
        User currentUser = userId != null ? userMapper.findById(userId) : null;
        if (currentUser == null || currentUser.getRole() == null || !"DOCTOR".equalsIgnoreCase(currentUser.getRole())) {
            result.put("success", false);
            result.put("code", ApiCode.FORBIDDEN.getCode());
            result.put("message", "無权限");
            return result;
        }

        Map<String, Object> existing = diseaseTypeMapper.findByName(name);
        if (existing != null && !existing.isEmpty()) {
            Map<String, Object> data = new HashMap<>();
            Map<String, Object> item = new HashMap<>();
            Object id = existing.get("id");
            String codeStr = existing.get("code") == null ? "" : String.valueOf(existing.get("code")).trim();
            String nameStr = existing.get("name") == null ? name : String.valueOf(existing.get("name")).trim();
            item.put("id", id);
            item.put("code", codeStr);
            item.put("name", nameStr);
            item.put("value", nameStr);
            item.put("label", nameStr);
            data.put("item", item);
            result.put("success", true);
            result.put("code", ApiCode.SUCCESS.getCode());
            result.put("message", "已存在");
            result.put("data", data);
            return result;
        }

        Long maxNo = diseaseTypeMapper.selectMaxCodeNumber();
        long nextNo = (maxNo == null ? 0L : maxNo) + 1L;
        String code = "D" + String.format("%04d", nextNo);

        Integer maxSortNo = diseaseTypeMapper.selectMaxSortNo();
        int sortNo = (maxSortNo == null ? 0 : maxSortNo) + 10;

        int inserted = diseaseTypeMapper.insertOne(code, name, sortNo, "manual");
        if (inserted <= 0) {
            result.put("success", false);
            result.put("code", ApiCode.INTERNAL_ERROR.getCode());
            result.put("message", "创建失败");
            return result;
        }

        Map<String, Object> created = diseaseTypeMapper.findByCode(code);
        Map<String, Object> item = new HashMap<>();
        if (created != null && !created.isEmpty()) {
            item.put("id", created.get("id"));
            item.put("code", created.get("code"));
            item.put("name", created.get("name"));
        } else {
            item.put("code", code);
            item.put("name", name);
        }
        item.put("value", name);
        item.put("label", name);

        Map<String, Object> data = new HashMap<>();
        data.put("item", item);
        result.put("success", true);
        result.put("code", ApiCode.SUCCESS.getCode());
        result.put("message", "创建成功");
        result.put("data", data);
        return result;
    }
}
