package com.example.springboot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 示例 RESTful 控制器
 * @author Kevin
 */
@RestController
@RequestMapping("/api")
public class HelloController {

    /**
     * 简单的 GET 请求示例
     */
    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot 101!";
    }

    /**
     * 带路径参数的 GET 请求示例
     */
    @GetMapping("/hello/{name}")
    public String helloWithName(@PathVariable String name) {
        return "Hello, " + name + "!";
    }

}
