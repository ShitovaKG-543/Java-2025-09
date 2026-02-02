package ru.otus.crm.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.ResultSetExtractor;
import ru.otus.crm.model.Address;
import ru.otus.crm.model.Client;
import ru.otus.crm.model.Phone;

public class ClientResultSetExtractorClass implements ResultSetExtractor<List<Client>> {

    @Override
    public List<Client> extractData(ResultSet rs) throws SQLException {

        Map<Long, Client> clientMap = new HashMap<>();
        Long prevClientId = null;
        while (rs.next()) {
            Long clientId = (Long) rs.getObject("client_id");

            Client client = clientMap.get(clientId);

            if (prevClientId == null || !prevClientId.equals(clientId)) {
                Long addressId = (Long) rs.getObject("address_id");
                client = new Client(
                        clientId,
                        rs.getString("client_name"),
                        new Address(addressId, rs.getString("address_street"), clientId),
                        new HashSet<>());
                clientMap.put(clientId, client);
                prevClientId = clientId;
            }
            Long phoneId = (Long) rs.getObject("phone_id");
            if (client != null && phoneId != null) {
                client.phones().add(new Phone(phoneId, rs.getString("phone_number"), clientId));
            }
        }

        return clientMap.values().stream().toList();
    }
}
