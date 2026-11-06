package com.example.javaea;

import org.springframework.web.bind.annotation.GetMapping;

@org.springframework.stereotype.Controller
public class Controller {

    @GetMapping("/")
    public String home() {
        return "index";
    }
}
