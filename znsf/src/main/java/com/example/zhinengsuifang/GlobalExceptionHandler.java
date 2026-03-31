package com.example.zhinengsuifang;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Map<String, Object> handleMissingParam(MissingServletRequestParameterException ex) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("code", ApiCode.PARAM_ERROR.getCode());
        result.put("message", "缺少参数: " + ex.getParameterName());
        return result;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Map<String, Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("code", ApiCode.PARAM_ERROR.getCode());
        result.put("message", "参数类型错误: " + ex.getName());
        return result;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Map<String, Object> handleNotReadable(HttpMessageNotReadableException ex) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("code", ApiCode.PARAM_ERROR.getCode());
        result.put("message", "请求体解析失败");
        return result;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("code", ApiCode.VALIDATION_ERROR.getCode());
        String msg = ex.getBindingResult().getAllErrors().isEmpty() ? "参数校验失败" : ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        result.put("message", msg);
        return result;
    }

    @ExceptionHandler(BindException.class)
    public Map<String, Object> handleBindException(BindException ex) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("code", ApiCode.VALIDATION_ERROR.getCode());
        String msg = ex.getBindingResult().getAllErrors().isEmpty() ? "参数绑定失败" : ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        result.put("message", msg);
        return result;
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoResourceFoundException.class)
    public Map<String, Object> handleNoResourceFound(NoResourceFoundException ex) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("code", ApiCode.NOT_FOUND.getCode());
        result.put("message", "资源不存在");
        return result;
    }

    @ExceptionHandler(Exception.class)
    public Map<String, Object> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("code", ApiCode.INTERNAL_ERROR.getCode());
        result.put("message", "系统异常");
        return result;
    }
}
