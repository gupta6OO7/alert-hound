package com.incident.alerthound.consumerfacing.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ConsumerDashboardController {

    @GetMapping({"/dashboard", "/dashboard/"})
    public String dashboard() {
        return "forward:/dashboard/index.html";
    }
}
