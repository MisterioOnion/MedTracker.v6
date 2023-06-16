
package com.example.MedTracker.serviceBot;

import com.example.MedTracker.model.User;
import com.example.MedTracker.model.UserRepository;
import org.jvnet.hk2.annotations.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@Component
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getUserByChatId(List<Long> chatIds) throws ExecutionException, InterruptedException {
        List<CompletableFuture<Optional<User>>> futures = new ArrayList<>();

        for (Long chatId : chatIds) {
            CompletableFuture<Optional<User>> future = CompletableFuture.supplyAsync(() -> userRepository.findFirstByChatId(chatId));
            futures.add(future);
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        CompletableFuture<List<User>> allUsersFuture = allFutures.thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList()));

        return allUsersFuture.get();
    }



    public void saveUser(User user) {
        userRepository.save(user);
    }

    public void deleteUser(User user) {
        userRepository.delete(user);
    }

}


