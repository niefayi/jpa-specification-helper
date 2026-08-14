package io.github.morphling.jpa.test.entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Test embeddable address (embedded in {@link Customer}).
 */
@Embeddable
public class Address {

    @Column(name = "city")
    private String city;

    @Column(name = "street")
    private String street;

    public Address() {
    }

    public Address(String city, String street) {
        this.city = city;
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }
}
