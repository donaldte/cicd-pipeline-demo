package tech.squadplus.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GreetingServiceTest {

  private final GreetingService service = new GreetingService();

  @Test
  @DisplayName("null name falls back to World")
  void greetsNullAsWorld() {
    assertEquals("Hello, World!", service.greet(null));
  }

  @Test
  @DisplayName("blank name falls back to World")
  void greetsBlankAsWorld() {
    assertEquals("Hello, World!", service.greet("   "));
  }

  @Test
  @DisplayName("surrounding whitespace is trimmed")
  void trimsWhitespace() {
    assertEquals("Hello, Donald!", service.greet("  Donald  "));
  }

  @Test
  @DisplayName("first letter is capitalized")
  void capitalizesFirstLetter() {
    assertEquals("Hello, Squadplus!", service.greet("squadplus"));
  }

  @Test
  @DisplayName("already-capitalized names are left as-is")
  void leavesCapitalizedNameAlone() {
    assertEquals("Hello, GitHub!", service.greet("GitHub"));
  }

  @Test
  @DisplayName("overly long names are rejected")
  void rejectsOverlyLongNames() {
    String tooLong = "a".repeat(61);
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> service.greet(tooLong));
    assertEquals("name is too long: 61 characters", ex.getMessage());
  }
}
