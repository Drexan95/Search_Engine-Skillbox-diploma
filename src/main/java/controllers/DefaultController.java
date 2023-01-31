package controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


@Controller
public class DefaultController {


    @RequestMapping("/")
    public String index() {
        return "index";
    }


}
