package petitus.petcareplus.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseMessagingService {

    private final FirebaseMessaging firebaseMessaging;

    public void sendNotification(String token, String title, String body, Map<String, String> data) {
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Message message = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .putAllData(data != null ? data : new HashMap<>())
                .build();

        try {
            firebaseMessaging.send(message);
            log.info("Notification sent successfully to token: {}", token);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send notification to token: {}", token, e);
        }
    }
} 