package br.com.example.demo.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static br.com.example.demo.DemoApplication.SECURITY_SCHEME;

@RestController
@RequestMapping("/v1")
@SecurityRequirement(name = SECURITY_SCHEME)
public class DemoController {

    @GetMapping("/testes")
    public ResponseEntity<String> testar() {
        return ResponseEntity.ok("Hello world");
    }

}
