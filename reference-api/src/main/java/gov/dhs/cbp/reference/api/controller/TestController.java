package gov.dhs.cbp.reference.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class TestController {

    @GetMapping("/test")
    public Map<String, String> test() {
        return Map.of("status", "working", "message", "Test endpoint is functioning");
    }

    @GetMapping("/api/test")
    public Map<String, String> apiTest() {
        return Map.of("status", "working", "message", "API test endpoint is functioning");
    }
}