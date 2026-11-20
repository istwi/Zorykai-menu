package com.zorykai.menus.logging;
import java.nio.file.*;
import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.*;
import org.apache.logging.log4j.core.layout.PatternLayout;
/** Bounded persistent diagnostics; captures this mod and uncaught exceptions containing its frames. */
public final class ErrorLogs implements AutoCloseable {
 private final LoggerContext context;private final RollingFileAppender appender;
 private ErrorLogs(LoggerContext context,RollingFileAppender appender){this.context=context;this.appender=appender;}
 public static ErrorLogs install(Path root)throws Exception{
  Path directory=root.resolve("logs");Files.createDirectories(directory);
  var context=(LoggerContext)LogManager.getContext(false);var configuration=context.getConfiguration();
  var filter=new AbstractFilter(){@Override public Result filter(LogEvent event){
   if(!event.getLevel().isMoreSpecificThan(Level.WARN))return Result.DENY;
   boolean own=event.getLoggerName()!=null&&event.getLoggerName().startsWith("zorykaimenus");
   Throwable failure=event.getThrown();for(int depth=0;!own&&failure!=null&&depth<16;depth++,failure=failure.getCause())for(var frame:failure.getStackTrace())if(frame.getClassName().startsWith("com.zorykai.menus.")){own=true;break;}
   return own?Result.ACCEPT:Result.DENY;
  }};
  RollingFileAppender appender=RollingFileAppender.newBuilder().setName("ZorykaiMenusErrors-"+Integer.toHexString(root.toAbsolutePath().hashCode())).setConfiguration(configuration)
   .withFileName(directory.resolve("erros.log").toString()).withFilePattern(directory.resolve("erros-%i.log.gz").toString())
   .withAppend(true).withCreateOnDemand(false).withPolicy(SizeBasedTriggeringPolicy.createPolicy("5MB"))
   .withStrategy(DefaultRolloverStrategy.newBuilder().withMax("5").withConfig(configuration).build())
   .setLayout(PatternLayout.newBuilder().withConfiguration(configuration).withPattern("[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%t/%level] %logger: %msg%n%throwable").build()).setFilter(filter).build();
  if(appender==null)throw new java.io.IOException("Não foi possível criar o arquivo de erros");
  appender.start();configuration.getRootLogger().addAppender(appender,Level.WARN,filter);context.updateLoggers();return new ErrorLogs(context,appender);
 }
 @Override public void close(){context.getConfiguration().getRootLogger().removeAppender(appender.getName());appender.stop();context.updateLoggers();}
}
