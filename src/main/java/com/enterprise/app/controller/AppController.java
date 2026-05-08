package com.enterprise.app.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AppController {

    @Value("${app.environment:production}")
    private String environment;

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("environment", environment);
        response.put("timestamp", new Date());
        response.put("version", "1.0.0");
        return response;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("app", "Java Enterprise App");
        response.put("environment", environment);
        response.put("developer", "Enterprise Team");
        response.put("features", List.of("3D UI", "REST API", "CI/CD", "Multi-Environment"));
        return response;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> response = new HashMap<>();
        response.put("uptime", System.currentTimeMillis());
        response.put("memory_used", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        response.put("memory_total", Runtime.getRuntime().totalMemory());
        response.put("processors", Runtime.getRuntime().availableProcessors());
        return response;
    }
}
