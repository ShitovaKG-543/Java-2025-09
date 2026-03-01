package ru.otus.trackingbot.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.otus.trackingbot.model.TrackedItem;

@Repository
public interface TrackedItemRepository extends JpaRepository<TrackedItem, Long> {

    List<TrackedItem> findByChatId(Long chatId);

    List<TrackedItem> findByActiveTrue();

    TrackedItem findByChatIdAndTrackingNumber(Long chatId, String trackingNumber);

    @Query("SELECT t FROM TrackedItem t WHERE t.active = true AND t.lastChecked < :date")
    List<TrackedItem> findItemsToUpdate(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(t) FROM TrackedItem t WHERE t.chatId = :chatId AND t.active = true")
    long countActiveByChatId(@Param("chatId") Long chatId);

    @Query("SELECT t FROM TrackedItem t WHERE t.active = true AND t.serviceName = :serviceName")
    List<TrackedItem> findByServiceName(@Param("serviceName") String serviceName);

    @Query("SELECT DISTINCT t.chatId FROM TrackedItem t WHERE t.active = true")
    List<Long> findDistinctActiveChatIds();

    void deleteByChatIdAndTrackingNumber(Long chatId, String trackingNumber);
}
