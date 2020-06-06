package com.example.Sunbrello;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class Controller {

    /**
     * @return: say hello
     */
    @GetMapping("/helloWorld")
    public String sayHello() {
        System.out.println("Hello");
        return "HELLOOOOOOOOO";
    }
}
