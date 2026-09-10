package tech.squadplus.demo;

/**
 * Tiny piece of business logic, kept deliberately simple.
 *
 * <p>The point of this demo project is the CI/CD pipeline around it, not the
 * application itself -- so this class exists mainly to give the pipeline
 * something real to compile, unit test, package and scan.
 */
public class GreetingService {

  private static final String DEFAULT_NAME = "World";
  private static final int MAX_NAME_LENGTH = 60;

  /**
   * Builds a friendly greeting for the given name.
   *
   * <ul>
   *   <li>{@code null} or blank input falls back to {@value #DEFAULT_NAME}.
   *   <li>Surrounding whitespace is trimmed.
   *   <li>The first letter is capitalized, the rest is left untouched.
   *   <li>Overly long input is rejected, to keep the demo endpoint honest.
   * </ul>
   *
   * @param rawName the name to greet, possibly {@code null} or blank
   * @return a greeting such as {@code "Hello, World!"}
   * @throws IllegalArgumentException if the trimmed name is longer than
   *     {@value #MAX_NAME_LENGTH} characters
   */
  public String greet(String rawName) {
    String name = normalize(rawName);
    if (name.length() > MAX_NAME_LENGTH) {
      throw new IllegalArgumentException("name is too long: " + name.length() + " characters");
    }
    return "Hello, " + capitalize(name) + "!";
  }

  private String normalize(String rawName) {
    if (rawName == null) {
      return DEFAULT_NAME;
    }
    String trimmed = rawName.trim();
    return trimmed.isEmpty() ? DEFAULT_NAME : trimmed;
  }

  private String capitalize(String value) {
    if (value.isEmpty()) {
      return value;
    }
    char first = Character.toUpperCase(value.charAt(0));
    return first + value.substring(1);
  }
}
