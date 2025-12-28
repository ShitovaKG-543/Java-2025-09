package ru.otus.crm.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "client")
public class Client implements Cloneable {

    @Id
    @SequenceGenerator(name = "client_gen", sequenceName = "client_seq", initialValue = 1, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "client_gen")
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "address_id")
    private Address address;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<Phone> phones = new ArrayList<>();

    public Client(String name) {
        this.id = null;
        this.name = name;
    }

    public Client(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Client(Long id, String name, Address address, List<Phone> phones) {
        this.id = id;
        this.name = name;
        this.address = address;
        if (address != null) {
            address.setClient(this);
        }
        setPhones(phones);
    }

    public void setPhones(List<Phone> phones) {
        this.phones.clear();
        if (phones != null) {
            this.phones.addAll(phones);
            phones.forEach(phone -> phone.setClient(this));
        }
    }

    public void addPhone(Phone phone) {
        phones.add(phone);
        phone.setClient(this);
    }

    @Override
    @SuppressWarnings({"java:S2975", "java:S1182"})
    public Client clone() {
        List<Phone> clonedPhones = this.phones != null
                ? this.phones.stream()
                        .map(p -> new Phone(p.getId(), p.getNumber()))
                        .toList()
                : null;

        Address clonedAddress =
                this.address != null ? new Address(this.address.getId(), this.address.getStreet()) : null;

        return new Client(this.id, this.name, clonedAddress, clonedPhones);
    }

    @Override
    public String toString() {
        return "Client{" + "id="
                + id + ", name='"
                + name + '\'' + ", address="
                + (address != null ? address.getStreet() : null) + ", phones="
                + (phones != null ? phones.stream().map(Phone::getNumber).toList() : null) + '}';
    }
}
