package com.example.controller;

import org.springframework.boot.autoconfigure.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

@Controller
@EnableAutoConfiguration
public class MainPageController {

	@RequestMapping(value = {"/","/index"})
	public String home() {
		return "index";
	}
	
	@RequestMapping("/generic")
	public String generic() {
		return "generic";
	}
	
	@RequestMapping("/elements")
	public String elements() {
		return "elements";
	}
	
}
