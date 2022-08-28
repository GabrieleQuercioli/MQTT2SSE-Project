package com.example.MQTT2SSE_BackEnd.controller;

import com.example.MQTT2SSE_BackEnd.UserEntity;
import com.example.MQTT2SSE_BackEnd.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/* GET THE REST REQUESTS FROM WEB AND CALLS THE RIGHT METHODS IN SERVICE */
@RestController
@RequestMapping("/users")
public class TestController {

    private final UserService userService;

    public TestController(UserService userService) {
        this.userService = userService;
    }

    //Select * FROM table
    @GetMapping
    public List<UserEntity> findAllUsers(){
        return userService.findAllUsers();
    }

    //Select * from table where id = ""
    @GetMapping("/{id}")
    public Optional<UserEntity> findUsersById(@PathVariable(value = "id") Long id){
        return userService.findByID(id);
    }

    //Insert INTO
    @PostMapping
    public UserEntity saveUser(@RequestBody UserEntity userEntity){
        return userService.saveUser(userEntity);
    }

    @DeleteMapping
    public void deleteUser(@PathVariable("id") Long id){
        userService.deleteUser(id);
    }


}
