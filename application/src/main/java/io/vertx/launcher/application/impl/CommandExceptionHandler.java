package io.vertx.launcher.application.impl;

import io.vertx.launcher.application.ExitCodes;
import picocli.CommandLine;

import static picocli.CommandLine.IExecutionExceptionHandler;
import static picocli.CommandLine.ParseResult;

public class CommandExceptionHandler implements IExecutionExceptionHandler {

  @Override
  public int handleExecutionException(Exception ex, CommandLine commandLine, ParseResult parseResult) {
    if (ex instanceof CommandException) {
      CommandException commandException = (CommandException) ex;
      return commandException.getCode();
    }
    return ExitCodes.SOFTWARE;
  }
}
