
package com.example.zugferd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;

public final class SampleDataLoader {
  private SampleDataLoader() {}

  public static CanonicalInvoice loadFromJson(String resourceName) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    try (InputStream is = SampleDataLoader.class.getResourceAsStream(resourceName)) {
      if (is == null) throw new IOException("Resource not found: " + resourceName);
      return mapper.readValue(is, CanonicalInvoice.class);
    }
  }
}
