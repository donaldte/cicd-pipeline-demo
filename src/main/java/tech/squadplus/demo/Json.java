package tech.squadplus.demo;

/**
 * Deliberately tiny JSON helper.
 *
 * <p>This project has zero third-party runtime dependencies on purpose (see the README) so
 * responses are built by hand instead of pulling in a full JSON library. It only supports what the
 * two demo endpoints need: a flat object made of string keys and string values.
 */
final class Json {

  private Json() {}

  /** Builds {@code {"key":"value"}} with the value safely escaped. */
  static String object(String key, String value) {
    return "{\"" + key + "\":\"" + escape(value) + "\"}";
  }

  private static String escape(String value) {
    StringBuilder out = new StringBuilder(value.length() + 8);
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\t' -> out.append("\\t");
        default -> {
          if (c < 0x20) {
            out.append(String.format("\\u%04x", (int) c));
          } else {
            out.append(c);
          }
        }
      }
    }
    return out.toString();
  }
}
