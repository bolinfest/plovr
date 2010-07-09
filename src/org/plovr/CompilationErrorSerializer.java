package org.plovr;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

final class CompilationErrorSerializer implements JsonSerializer<CompilationError> {

  @Override
  public JsonElement serialize(CompilationError error, Type typeOfError,
      JsonSerializationContext context) {
    JsonObject record = new JsonObject();
    record.addProperty("input", error.getSourceName());
    record.addProperty("message", error.getMessage());
    record.addProperty("isError", error.isError());
    record.addProperty("lineNumber", error.getLineNumber());
    return record;
  }

}
