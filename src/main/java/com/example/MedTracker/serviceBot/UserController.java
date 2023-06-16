package com.example.MedTracker.serviceBot;

import com.example.MedTracker.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.ExecutionException;


@RestController
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/users")
    public List<User> getUsersByChatIds(@RequestParam("chatIds") List<Long> chatIds) throws InterruptedException, ExecutionException {
        return userService.getUserByChatId(chatIds);
    }
}

