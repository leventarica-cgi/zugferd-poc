
package com.example.zugferd;

public record Party(
    String name,
    String street,
    String zip,
    String city,
    String countryCode,
    String vatId
) {}
