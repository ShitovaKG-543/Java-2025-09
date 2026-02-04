package ru.otus.crm.repository;

import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import ru.otus.crm.model.Client;

public interface ClientRepository extends ListCrudRepository<Client, Long> {
    @Override
    @Query(
            value =
                    """
                select    c.id as client_id,
                          c.name as client_name,
                          a.id as address_id,
                          a.street as address_street,
                          p.id as phone_id,
                          p.number as phone_number
                from client c
                         left outer join address a
                                         on c.id = a.client_id
                         left outer join phone p
                                         on p.client_id = c.id
                order by c.id, p.id
                """,
            resultSetExtractorClass = ClientResultSetExtractorClass.class)
    List<Client> findAll();
}
