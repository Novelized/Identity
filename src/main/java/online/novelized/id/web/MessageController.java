package online.novelized.id.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
public class MessageController {

    @GetMapping("/messages")
    public List<Map<String, String>> getMessages() {
        return Arrays.asList(
                Map.of("text", "Message 1 from backend!"),
                Map.of("text", "Another secured message."),
                Map.of("text", "Hello from the protected API.")
        );
    }
} 