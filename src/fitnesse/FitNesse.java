// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse;

import fitnesse.http.MockRequestBuilder;
import fitnesse.http.MockResponseSender;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.socketservice.SocketService;
import fitnesse.util.MockSocket;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.BindException;

public class FitNesse {
  public static final FitNesseVersion VERSION = new FitNesseVersion();
  public static FitNesse FITNESSE_INSTANCE;
  private final FitNesseContext context;
  private SocketService theService;

  public FitNesse(FitNesseContext context) {
    this(context, true);
  }

  // TODO MdM. This boolean agument is annoying... please fix.
  // Update AJM: To make this work we need to get rid of FITNESSE_INSTANCE, remove update logic from here (move to FitNesseMain)
  public FitNesse(FitNesseContext context, boolean makeDirs) {
    FITNESSE_INSTANCE = this;
    this.context = context;
    if (makeDirs)
      establishRequiredDirectories();
  }

  private void establishRequiredDirectories() {
    establishDirectory(context.getRootPagePath());
    establishDirectory(context.getRootPagePath() + "/files");
  }

  private static void establishDirectory(String path) {
    File filesDir = new File(path);
    if (!filesDir.exists())
      filesDir.mkdir();
  }

  public static void main(String[] args) throws Exception {
    System.out.println("DEPRECATED:  use java -jar fitnesse.jar or java -cp fitnesse.jar fitnesseMain.FitNesseMain");
    Class<?> mainClass = Class.forName("fitnesseMain.FitNesseMain");
    Method mainMethod = mainClass.getMethod("main", String[].class);
    mainMethod.invoke(null, new Object[]{args});
  }

  public boolean start() {
    try {
      if (context.port > 0) {
        theService = new SocketService(context.port, new FitNesseServer(context));
      }
      return true;
    } catch (BindException e) {
      printBadPortMessage(context.port);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  private static void printBadPortMessage(int port) {
    System.err.println("FitNesse cannot be started...");
    System.err.println("Port " + port + " is already in use.");
    System.err.println("Use the -p <port#> command line argument to use a different port.");
  }

  public void stop() throws IOException {
    if (theService != null) {
      theService.close();
      theService = null;
    }
  }

  public boolean isRunning() {
    return theService != null;
  }

  public FitNesseContext getContext() {
    return context;
  }

  public void executeSingleCommand(String command, OutputStream out) throws Exception {
    Request request = new MockRequestBuilder(command).noChunk().build();
    FitNesseExpediter expediter = new FitNesseExpediter(new MockSocket(), context);
    Response response = expediter.createGoodResponse(request);
    response.withoutHttpHeaders();
    MockResponseSender sender = new MockResponseSender.OutputStreamSender(out);
    sender.doSending(response);
  }
}
