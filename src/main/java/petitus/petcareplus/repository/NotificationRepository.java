package petitus.petcareplus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import petitus.petcareplus.model.Notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    @Query("SELECT n FROM Notification n WHERE n.userIdReceive = :userIdReceive AND n.deletedAt IS NULL")
    List<Notification> findByUserIdReceive(UUID userIdReceive);

    @Query("SELECT n FROM Notification n WHERE n.deletedAt IS NULL AND n.id = :notificationId")
    Optional<Notification> findById(UUID notificationId);

    Page<Notification> findByDeletedAtIsNull(Pageable pageable);

}
