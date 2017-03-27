package com.generali.outbound.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Date;
import java.util.Map;

/**
 * Created by timdekarz on 27.03.17.
 */
@Controller
public class Hello {

    @RequestMapping("/")
    public String hello(Map<String, Object> model) {
        model.put("time", new Date());
        model.put("message", "Hello World");
        model.put("title", "Hello App");
        return "hello";
    }
}
