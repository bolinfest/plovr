package org.plovr;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.io.LineReader;

/**
 * {@link JsonCommentStripper} reads a file of JSON but removes lines that
 * start with // style comments. For now, it does not parse other types of
 * JavaScript comments.
 * 
 * @author bolinfest@gmail.com (Michael Bolin)
 */
final class JsonCommentStripper {

  private static final Pattern STARTS_WITH_COMMENT = Pattern.compile("^\\s*//(.*)");

  /** Utility class; do not instantiate. */
  private JsonCommentStripper() {}
  
  static String stripCommentsFromJson(File jsonFile) throws IOException {
    FileReader fileReader = new FileReader(jsonFile);
    LineReader lineReader = new LineReader(fileReader);
    StringBuilder builder = new StringBuilder();
    String line;
    while ((line = lineReader.readLine()) != null) {
      Matcher matcher = STARTS_WITH_COMMENT.matcher(line);
      // This has the same number of lines as the input so that if JsonParser
      // reports an error, it will be easier to match back to the original file.
      if (matcher.matches()) {
        builder.append("\n");
      } else {
        builder.append(line + "\n");
      }
    }
    return builder.toString();
  }

}
