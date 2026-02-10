package com.vasan12sp.ratelimiter.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DocsController {

    @GetMapping("/docs")
    public String docs(Model model) {
        // Future: add dynamic examples or versioning to the model
        return "docs";
    }
}
