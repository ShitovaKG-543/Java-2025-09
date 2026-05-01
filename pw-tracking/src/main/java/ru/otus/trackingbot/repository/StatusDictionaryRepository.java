package ru.otus.trackingbot.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.otus.trackingbot.entity.StatusDictionary;

@Repository
public interface StatusDictionaryRepository extends JpaRepository<StatusDictionary, String> {

    Optional<StatusDictionary> findByStatusCodeAndServiceName(String statusCode, String serviceName);

    List<StatusDictionary> findByServiceNameOrderBySortOrder(String serviceName);

    List<StatusDictionary> findByServiceNameIsNullOrderBySortOrder();
}
