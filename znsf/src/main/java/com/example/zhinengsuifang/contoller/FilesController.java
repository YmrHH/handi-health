package com.example.zhinengsuifang.contoller;

import com.example.zhinengsuifang.ApiCode;
import com.example.zhinengsuifang.entity.FileUpload;
import com.example.zhinengsuifang.mapper.FileUploadMapper;
import com.example.zhinengsuifang.util.ApiResponseUtil;
import com.example.zhinengsuifang.util.AuthHeaderUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/files")
public class FilesController {

    @Resource
    private FileUploadMapper fileUploadMapper;

    @PostMapping("/upload")
    public Map<String, Object> upload(HttpServletRequest request,
                                      @RequestParam("file") MultipartFile file,
                                      @RequestParam(required = false) String bizType,
                                      @RequestParam(required = false) Long patientId) {
        Long userId = AuthHeaderUtil.getUserId(request);
        if (userId == null) {
            return ApiResponseUtil.fail(ApiCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        if (file == null || file.isEmpty()) {
            return ApiResponseUtil.fail(ApiCode.VALIDATION_ERROR, "文件不能为空");
        }

        try {
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (StringUtils.hasText(originalName) && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf('.') + 1);
            }

            byte[] bytes = file.getBytes();
            String sha1 = sha1Hex(bytes);

            Path baseDir = Paths.get(System.getProperty("user.dir"), "uploads");
            Files.createDirectories(baseDir);

            String safeName = sha1 + (ext.isEmpty() ? "" : ("." + ext));
            Path target = baseDir.resolve(safeName);
            Files.write(target, bytes);

            String url = "/uploads/" + safeName;

            FileUpload meta = new FileUpload();
            meta.setBizType(bizType);
            meta.setPatientId(patientId);
            meta.setUploaderUserId(userId);
            meta.setOriginalName(originalName);
            meta.setFileName(safeName);
            meta.setFileExt(ext);
            meta.setMimeType(file.getContentType());
            meta.setFileSize((long) bytes.length);
            meta.setStoragePath(target.toString());
            meta.setUrl(url);
            meta.setSha1(sha1);
            meta.setStatus("OK");

            fileUploadMapper.insert(meta);

            Map<String, Object> data = new HashMap<>();
            data.put("id", meta.getId());
            data.put("url", url);
            data.put("fileName", safeName);
            data.put("sha1", sha1);
            return ApiResponseUtil.ok(data);
        } catch (Exception e) {
            return ApiResponseUtil.fail(ApiCode.INTERNAL_ERROR, "上传失败");
        }
    }

    private static String sha1Hex(byte[] bytes) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] dig = md.digest(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : dig) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
