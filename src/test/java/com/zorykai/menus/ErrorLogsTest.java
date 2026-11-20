package com.zorykai.menus;
import com.zorykai.menus.logging.ErrorLogs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.apache.logging.log4j.LogManager;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class ErrorLogsTest {
 @TempDir Path directory;
 @Test void persistsErrorAndStackWithoutCapturingUnrelatedErrors()throws Exception{
  try(var logs=ErrorLogs.install(directory)){
   LogManager.getLogger("zorykaimenus").error("diagnostic-probe",new IllegalStateException("expected-trace"));
   LogManager.getLogger("unrelated").error("unrelated-probe");
  }
  String text=Files.readString(directory.resolve("logs/erros.log"));assertTrue(text.contains("diagnostic-probe"));assertTrue(text.contains("IllegalStateException: expected-trace"));assertFalse(text.contains("unrelated-probe"));
 }
}
