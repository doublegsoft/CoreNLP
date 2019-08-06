package edu.stanford.nlp.process;

import java.util.regex.Pattern;

/** This class contains various static utility methods invoked by our JFlex NL lexers.
 *  Having this utility code placed outside the lexers facilitates normal
 *  IDE code editing.
 *
 *  @author Christopher Manning
 */
public class LexerUtils {

  private LexerUtils() {} // static methods

  private static final Pattern CENTS_PATTERN = Pattern.compile("\u00A2");
  private static final Pattern POUND_PATTERN = Pattern.compile("\u00A3");
  private static final Pattern GENERIC_CURRENCY_PATTERN = Pattern.compile("[\u0080\u00A4\u20A0\u20AC\u20B9]");
  private static final Pattern CP1252_EURO_PATTERN = Pattern.compile("\u0080");

  private static final Pattern ONE_FOURTH_PATTERN = Pattern.compile("\u00BC");
  private static final Pattern ONE_HALF_PATTERN = Pattern.compile("\u00BD");
  private static final Pattern THREE_FOURTHS_PATTERN = Pattern.compile("\u00BE");
  private static final Pattern ONE_THIRD_PATTERN = Pattern.compile("\u2153");
  private static final Pattern TWO_THIRDS_PATTERN = Pattern.compile("\u2154");


  /** Change precomposed fraction characters to spelled out letter forms.
   *
   *  @param normalizeFractions If false, do nothing; if true normalize to ASCII character sequence
   *  @param escapeForwardSlashAsterisk If true also escape forward slash with backslash (deprecated historical PTB)
   *  @param in The input string to normalize
   *  @return The normalized fraction string
   */
  public static String normalizeFractions(boolean normalizeFractions, boolean escapeForwardSlashAsterisk, final String in) {
    String out = in;
    if (normalizeFractions) {
      if (escapeForwardSlashAsterisk) {
        out = ONE_FOURTH_PATTERN.matcher(out).replaceAll("1\\\\/4");
        out = ONE_HALF_PATTERN.matcher(out).replaceAll("1\\\\/2");
        out = THREE_FOURTHS_PATTERN.matcher(out).replaceAll("3\\\\/4");
        out = ONE_THIRD_PATTERN.matcher(out).replaceAll("1\\\\/3");
        out = TWO_THIRDS_PATTERN.matcher(out).replaceAll("2\\\\/3");
      } else {
        out = ONE_FOURTH_PATTERN.matcher(out).replaceAll("1/4");
        out = ONE_HALF_PATTERN.matcher(out).replaceAll("1/2");
        out = THREE_FOURTHS_PATTERN.matcher(out).replaceAll("3/4");
        out = ONE_THIRD_PATTERN.matcher(out).replaceAll("1/3");
        out = TWO_THIRDS_PATTERN.matcher(out).replaceAll("2/3");
      }
    }
    return out;
  }


  @SuppressWarnings("unused")
  public static String normalizeCurrency(String in) {
    String s1 = in;
    s1 = CENTS_PATTERN.matcher(s1).replaceAll("cents");
    s1 = POUND_PATTERN.matcher(s1).replaceAll("#");  // historically used for pound in PTB3
    s1 = GENERIC_CURRENCY_PATTERN.matcher(s1).replaceAll("\\$");  // Euro (ECU, generic currency)  -- no good translation!
    s1 = CP1252_EURO_PATTERN.matcher(s1).replaceAll("\u20AC");
    return s1;
  }

  /** Still at least turn cp1252 euro symbol to Unicode one. */
  public static String minimallyNormalizeCurrency(String in) {
    String s1 = in;
    s1 = CP1252_EURO_PATTERN.matcher(s1).replaceAll("\u20AC");
    return s1;
  }

  public static String removeSoftHyphens(String in) {
    // \u00AD is the soft hyphen character, which we remove, regarding it as inserted only for line-breaking
    if (in.indexOf('\u00AD') < 0) {
      // shortcut doing work
      return in;
    }
    int length = in.length();
    StringBuilder out = new StringBuilder(length - 1);
    /*
    // This isn't necessary, as BMP, low, and high surrogate encodings are disjoint!
    for (int offset = 0, cp; offset < length; offset += Character.charCount(cp)) {
      cp = in.codePointAt(offset);
      if (cp != '\u00AD') {
        out.appendCodePoint(cp);
      }
    }
    */
    for (int i = 0; i < length; i++) {
      char ch = in.charAt(i);
      if (ch != '\u00AD') {
       out.append(ch);
      }
    }
    if (out.length() == 0) {
      out.append('-'); // don't create an empty token, put in a regular hyphen
    }
    return out.toString();
  }

  /* CP1252: dagger, double dagger, per mille, bullet, small tilde, trademark */
  public static String processCp1252misc(String arg) {
    switch (arg) {
    case "\u0086":
      return "\u2020";
    case "\u0087":
      return "\u2021";
    case "\u0089":
      return "\u2030";
    case "\u0095":
      return "\u2022";
    case "\u0098":
      return "\u02DC";
    case "\u0099":
      return "\u2122";
    default:
      throw new IllegalArgumentException("Bad process cp1252");
    }
  }

  private static final Pattern AMP_PATTERN = Pattern.compile("(?i:&amp;)");

  /** Convert an XML-escaped ampersand back into an ampersand. */
  public static String normalizeAmp(final String in) {
    return AMP_PATTERN.matcher(in).replaceAll("&");
  }

  /** This quotes a character with a backslash, but doesn't do it
   *  if the character is already preceded by a backslash.
   */
  public static String escapeChar(String s, char c) {
    int i = s.indexOf(c);
    while (i != -1) {
      if (i == 0 || s.charAt(i - 1) != '\\') {
        s = s.substring(0, i) + '\\' + s.substring(i);
        i = s.indexOf(c, i + 2);
      } else {
        i = s.indexOf(c, i + 1);
      }
    }
    return s;
  }


  private static final Pattern singleQuote = Pattern.compile("&apos;|'");
  // If they typed `` they probably meant it, but if it's '' or mixed, we use our heuristics.
  private static final Pattern doubleQuote = Pattern.compile("\"|''|'`|`'|&quot;");

  // U+00B4 should be acute accent, but stuff happens
  private static final Pattern asciiSingleQuote = Pattern.compile("&apos;|[\u0082\u008B\u0091\u00B4\u2018\u0092\u2019\u009B\u201A\u201B\u2039\u203A']");
  private static final Pattern asciiDoubleQuote = Pattern.compile("&quot;|[\u0084\u0093\u201C\u0094\u201D\u201E\u00AB\u00BB\"]");

  // 82,84,91,92,93,94 aren't valid unicode points, but sometimes they show
  // up from cp1252 and need to be translated
  private static final Pattern leftSingleQuote = Pattern.compile("[\u0082\u008B\u0091\u2018\u201A\u201B\u2039]");
  private static final Pattern rightSingleQuote = Pattern.compile("[\u0092\u009B\u00B4\u2019\u203A]");
  private static final Pattern leftDoubleQuote = Pattern.compile("[\u0084\u0093\u201C\u201E\u00AB]|[\u0091\u2018]'");
  private static final Pattern rightDoubleQuote = Pattern.compile("[\u0094\u201D\u00BB]|[\u0092\u2019]'");

  /** Convert all single and double quote like characters to the ASCII quote characters: ' ". */
  public static String asciiQuotes(String in) {
    String s1 = in;
    s1 = asciiSingleQuote.matcher(s1).replaceAll("'");
    s1 = asciiDoubleQuote.matcher(s1).replaceAll("\"");
    return s1;
  }

  public static String latexQuotes(String in, boolean probablyLeft) {
    // System.err.println("Handling quote on " + in + " probablyLeft=" + probablyLeft);
    String s1 = in;
    if (probablyLeft) {
      s1 = singleQuote.matcher(s1).replaceAll("`");
      s1 = doubleQuote.matcher(s1).replaceAll("``");
    } else {
      s1 = singleQuote.matcher(s1).replaceAll("'");
      s1 = doubleQuote.matcher(s1).replaceAll("''");
    }
    s1 = leftSingleQuote.matcher(s1).replaceAll("`");
    s1 = rightSingleQuote.matcher(s1).replaceAll("'");
    s1 = leftDoubleQuote.matcher(s1).replaceAll("``");
    s1 = rightDoubleQuote.matcher(s1).replaceAll("''");
    // System.err.println("  Mapped to " + s1);
    return s1;
  }

  private static final Pattern unicodeLeftSingleQuote = Pattern.compile("\u0091");
  private static final Pattern unicodeRightSingleQuote = Pattern.compile("\u0092");
  private static final Pattern unicodeLeftDoubleQuote = Pattern.compile("\u0093");
  private static final Pattern unicodeRightDoubleQuote = Pattern.compile("\u0094");
  private static final Pattern leftDuck = Pattern.compile("\u008B");
  private static final Pattern rightDuck = Pattern.compile("\u009B");

  public static String unicodeQuotes(String in, boolean probablyLeft) {
    String s1 = in;
    if (probablyLeft) {
      s1 = singleQuote.matcher(s1).replaceAll("\u2018");
      s1 = doubleQuote.matcher(s1).replaceAll("\u201c");
    } else {
      s1 = singleQuote.matcher(s1).replaceAll("\u2019");
      s1 = doubleQuote.matcher(s1).replaceAll("\u201d");
    }
    s1 = unicodeLeftSingleQuote.matcher(s1).replaceAll("\u2018");
    s1 = unicodeRightSingleQuote.matcher(s1).replaceAll("\u2019");
    s1 = unicodeLeftDoubleQuote.matcher(s1).replaceAll("\u201c");
    s1 = unicodeRightDoubleQuote.matcher(s1).replaceAll("\u201d");
    s1 = leftDuck.matcher(s1).replaceAll("\u2039");
    s1 = rightDuck.matcher(s1).replaceAll("\u203A");
    return s1;
  }

  /** This was the version originally written for Spanish to just recode cp1252 range quotes. */
  public static String nonCp1252Quotes(String in) {
    switch(in) {
      case "\u008B":
        return "\u2039";
      case "\u0091":
        return "\u2018";
      case "\u0092":
        return "\u2019";
      case "\u0093":
        return "\u201C";
      case "\u0094":
        return "\u201D";
      case "\u009B":
        return "\u203A";
      default:
        return in;
    }
  }

}