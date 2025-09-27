package com.stride.stride.CONTROLLER;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HELLOCONTROLLER {

    @GetMapping("/hello")
    public String helloStride() {
        return "Hello";
    }

}
