package com.practice.rabbitmq.controller;

import com.practice.rabbitmq.dto.User;
import com.practice.rabbitmq.publisher.RabbitMQJsonProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class MessageJsonController {
    @Autowired
    private final RabbitMQJsonProducer jsonProducer;
    // In-memory list to simulate a database for this example
    private final List<User> userList = new ArrayList<>();

    public MessageJsonController(RabbitMQJsonProducer jsonProducer) {
        this.jsonProducer = jsonProducer;
    }

    // POST: Create a new user
    @PostMapping("/post")
    public ResponseEntity<String> createUser(@RequestBody User user) {
        // Assign a random ID if not provided
        if (user.getId() == null || user.getId().isEmpty()) {
            user.setId(UUID.randomUUID().toString());
        }

        userList.add(user);
        jsonProducer.sendJsonMessage(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("User created and JSON message sent to RabbitMQ: " + user.getId());
    }

    // GET: Retrieve a user by ID
    @GetMapping("/get/{id}")
    public ResponseEntity<User> getUserById(@PathVariable String id) {
        Optional<User> user = userList.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst();

        return user.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null));
    }

    // GET: Retrieve all users
    @GetMapping("/get")
    public ResponseEntity<List<User>> getAllUsers() {
        if (userList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ArrayList<>());
        }
        return ResponseEntity.ok(userList);
    }

    // PUT: Update an existing user
    @PutMapping("/put/{id}")
    public ResponseEntity<String> updateUser(@PathVariable String id, @RequestBody User updatedUser) {
        Optional<User> existingUser = userList.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst();

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // Update fields
            user.setName(updatedUser.getName() != null ? updatedUser.getName() : user.getName());
            user.setEmail(updatedUser.getEmail() != null ? updatedUser.getEmail() : user.getEmail());
            // Add other fields as per your User DTO

            jsonProducer.sendJsonMessage(user);
            return ResponseEntity.ok("User updated and JSON message sent to RabbitMQ: " + id);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("User not found with ID: " + id);
    }

    // DELETE: Delete a user by ID
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable String id) {
        Optional<User> userToDelete = userList.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst();

        if (userToDelete.isPresent()) {
            userList.remove(userToDelete.get());
            jsonProducer.sendJsonMessage(new User(id, "DELETED", null)); // Sending a delete notification
            return ResponseEntity.ok("User deleted and notification sent to RabbitMQ: " + id);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("User not found with ID: " + id);
    }

    // PATCH: Partially update a user (optional, if you want partial updates)
    @PatchMapping("/patch/{id}")
    public ResponseEntity<String> patchUser(@PathVariable String id, @RequestBody User partialUser) {
        Optional<User> existingUser = userList.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst();

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // Only update fields that are provided
            if (partialUser.getName() != null) user.setName(partialUser.getName());
            if (partialUser.getEmail() != null) user.setEmail(partialUser.getEmail());
            // Add other fields as per your User DTO

            jsonProducer.sendJsonMessage(user);
            return ResponseEntity.ok("User partially updated and JSON message sent to RabbitMQ: " + id);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("User not found with ID: " + id);
    }
}