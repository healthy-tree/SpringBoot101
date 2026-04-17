package com.example.springboot.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HelloController 单元测试类
 * @author Kevin
 */
@WebMvcTest(HelloController.class)
public class HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 测试不带参数的 /api/hello 接口
     */
    @Test
    public void testHello() throws Exception {
        mockMvc.perform(get("/api/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, Spring Boot 101!"));
    }

    /**
     * 测试带路径参数的 /api/hello/{name} 接口
     */
    @Test
    public void testHelloWithName() throws Exception {
        mockMvc.perform(get("/api/hello/Kevin"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, Kevin!"));
    }

    /**
     * 测试带不同路径参数的 /api/hello/{name} 接口
     */
    @Test
    public void testHelloWithDifferentName() throws Exception {
        mockMvc.perform(get("/api/hello/Spring"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, Spring!"));
    }
}
