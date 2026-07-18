import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.ExceptionRequest;
import com.sun.jdi.request.MethodEntryRequest;
import java.time.Instant;
import java.util.Map;

/**
 * Host-only P13 command-boundary observer.
 *
 * This tool never reads arguments, local variables, object fields, return values, or bearer data.
 * It records constructor entry for the two immutable command types during one operator-bounded
 * attempt window. It is compiled and run on the host and is never packaged in an APK.
 */
public final class P13JdiCommandObserver {
  private static final String PLAN_CLASS =
      "chat.simplex.common.model.CC$APIConnectPlan";
  private static final String CONNECT_CLASS =
      "chat.simplex.common.model.CC$APIConnect";
  private static final String RETAINED_STATE_CLASS =
      "chat.simplex.common.ui.nome.connection.RetainedConnectionPreviewState";
  private static final String CANCELLATION_CHECK_CLASS =
      "chat.simplex.common.model.ChatController";
  private static final String JOB_SUPPORT_CLASS =
      "kotlinx.coroutines.JobSupport";
  private static final String PLAN_SIGNATURE =
      "(JLjava/lang/String;Lchat/simplex/common/model/LinkOwnerSig;)V";
  private static final String CONNECT_SIGNATURE =
      "(JZLchat/simplex/common/model/CreatedConnLink;)V";

  private P13JdiCommandObserver() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 4 && args.length != 5) {
      throw new IllegalArgumentException(
          "usage: P13JdiCommandObserver <host> <port> <attempt-id> <timeout-seconds> [hold-cancellation-check-millis]");
    }
    String host = args[0];
    String port = args[1];
    String attemptId = args[2];
    long timeoutSeconds = Long.parseLong(args[3]);
    long holdCancellationCheckMillis =
        args.length == 5 ? Long.parseLong(args[4]) : 0;
    if (
        holdCancellationCheckMillis < 0 ||
        holdCancellationCheckMillis > 30_000
    ) {
      throw new IllegalArgumentException(
          "hold-cancellation-check-millis must be between 0 and 30000");
    }
    if (!attemptId.matches("[A-Za-z0-9._-]+")) {
      throw new IllegalArgumentException("attempt-id contains unsupported characters");
    }

    VirtualMachine vm = attach(host, port);
    int planCount = 0;
    int connectCount = 0;
    int cancellationCount = 0;
    int cancelSignalCount = 0;
    int cancellationBoundaryCount = 0;
    int cancellationExceptionFactoryCount = 0;
    long cancellationCheckThreadId = -1;
    long sequence = 0;
    long deadlineNanos =
        System.nanoTime() + timeoutSeconds * 1_000_000_000L;
    try {
      addConstructorRequest(vm, PLAN_CLASS);
      addConstructorRequest(vm, CONNECT_CLASS);
      addLifecycleRequest(vm, RETAINED_STATE_CLASS);
      addLifecycleRequest(vm, JOB_SUPPORT_CLASS);
      addCancellationCheckRequest(vm);
      addCancellationRequest(vm);
      System.out.printf(
          "OBSERVER_READY\tattempt=%s\tstarted=%s\ttimeoutSeconds=%d%n",
          attemptId,
          Instant.now(),
          timeoutSeconds);
      while (System.nanoTime() < deadlineNanos) {
        EventSet eventSet = vm.eventQueue().remove(250);
        if (eventSet == null) continue;
        try {
          for (Event event : eventSet) {
            if (event instanceof MethodEntryEvent methodEntry) {
              String className =
                  methodEntry.method().declaringType().name();
              String signature = methodEntry.method().signature();
              String command = null;
              if (
                  PLAN_CLASS.equals(className) &&
                  PLAN_SIGNATURE.equals(signature)
              ) {
                planCount += 1;
                command = "PLAN";
              } else if (
                  CONNECT_CLASS.equals(className) &&
                  CONNECT_SIGNATURE.equals(signature)
              ) {
                connectCount += 1;
                command = "CONNECT";
              }
              if (command != null) {
                sequence += 1;
                System.out.printf(
                    "COMMAND\tattempt=%s\tseq=%d\tcommand=%s\tevent=CONSTRUCTED\tclass=%s\tmethod=<init>%n",
                    attemptId,
                    sequence,
                    command,
                    className);
              } else if (
                  RETAINED_STATE_CLASS.equals(className) &&
                  "cancel".equals(methodEntry.method().name())
              ) {
                cancelSignalCount += 1;
                sequence += 1;
                System.out.printf(
                    "LIFECYCLE\tattempt=%s\tseq=%d\tevent=CANCEL_SIGNAL\tclass=%s\tmethod=cancel\tthread=%s%n",
                    attemptId,
                    sequence,
                    className,
                    methodEntry.thread().name());
              } else if (
                  CANCELLATION_CHECK_CLASS.equals(className) &&
                  "ensureP13ConnectAttemptActive".equals(
                      methodEntry.method().name()
                  ) &&
                  connectCount > 0 &&
                  cancellationBoundaryCount == 0
              ) {
                cancellationBoundaryCount += 1;
                cancellationCheckThreadId =
                    methodEntry.thread().uniqueID();
                sequence += 1;
                System.out.printf(
                    "LIFECYCLE\tattempt=%s\tseq=%d\tevent=P13_CONNECT_CANCELLATION_BOUNDARY\tclass=%s\tmethod=%s\tthread=%s\tholdMillis=%d%n",
                    attemptId,
                    sequence,
                    className,
                    methodEntry.method().name(),
                    methodEntry.thread().name(),
                    holdCancellationCheckMillis);
                if (holdCancellationCheckMillis > 0) {
                  Thread.sleep(holdCancellationCheckMillis);
                }
              } else if (
                  JOB_SUPPORT_CLASS.equals(className) &&
                  "getCancellationException".equals(
                      methodEntry.method().name()
                  ) &&
                  connectCount > 0 &&
                  cancellationBoundaryCount > 0 &&
                  cancellationCheckThreadId ==
                      methodEntry.thread().uniqueID()
              ) {
                cancellationExceptionFactoryCount += 1;
                sequence += 1;
                System.out.printf(
                    "LIFECYCLE\tattempt=%s\tseq=%d\tevent=CANCELLATION_EXCEPTION_CREATED\tclass=%s\tmethod=getCancellationException\tthread=%s%n",
                    attemptId,
                    sequence,
                    className,
                    methodEntry.thread().name());
              }
            } else if (
                event instanceof ExceptionEvent exceptionEvent &&
                connectCount > 0 &&
                cancellationBoundaryCount > 0 &&
                cancellationCheckThreadId ==
                    exceptionEvent.thread().uniqueID() &&
                exceptionEvent.exception()
                    .referenceType()
                    .name()
                    .contains("CancellationException")
            ) {
              var catchLocation = exceptionEvent.catchLocation();
              String catchClass = catchLocation == null
                  ? "UNCAUGHT"
                  : catchLocation.declaringType().name();
              String catchMethod = catchLocation == null
                  ? "UNCAUGHT"
                  : catchLocation.method().name();
              cancellationCount += 1;
              sequence += 1;
              System.out.printf(
                  "LIFECYCLE\tattempt=%s\tseq=%d\tevent=CANCELLATION_EXCEPTION\texception=%s\tcatchClass=%s\tcatchMethod=%s\tthread=%s%n",
                  attemptId,
                  sequence,
                  exceptionEvent.exception().referenceType().name(),
                  catchClass,
                  catchMethod,
                  exceptionEvent.thread().name());
            }
          }
        } finally {
          eventSet.resume();
        }
      }
    } finally {
      System.out.printf(
          "SUMMARY\tattempt=%s\tplan=%d\tconnect=%d\tcancellationBoundaries=%d\tcancelSignals=%d\tcancellationExceptionsCreated=%d\tcancellations=%d\tevents=%d\tended=%s%n",
          attemptId,
          planCount,
          connectCount,
          cancellationBoundaryCount,
          cancelSignalCount,
          cancellationExceptionFactoryCount,
          cancellationCount,
          sequence,
          Instant.now());
      vm.dispose();
    }
  }

  private static VirtualMachine attach(
      String host,
      String port
  ) throws Exception {
    AttachingConnector connector =
        Bootstrap.virtualMachineManager()
            .attachingConnectors()
            .stream()
            .filter(candidate ->
                "com.sun.jdi.SocketAttach".equals(candidate.name()))
            .findFirst()
            .orElseThrow(() ->
                new IllegalStateException("SocketAttach connector unavailable"));
    Map<String, Connector.Argument> arguments =
        connector.defaultArguments();
    arguments.get("hostname").setValue(host);
    arguments.get("port").setValue(port);
    return connector.attach(arguments);
  }

  private static void addConstructorRequest(
      VirtualMachine vm,
      String className
  ) {
    MethodEntryRequest request =
        vm.eventRequestManager().createMethodEntryRequest();
    request.addClassFilter(className);
    request.setSuspendPolicy(EventRequest.SUSPEND_NONE);
    request.enable();
  }

  private static void addLifecycleRequest(
      VirtualMachine vm,
      String className
  ) {
    MethodEntryRequest request =
        vm.eventRequestManager().createMethodEntryRequest();
    request.addClassFilter(className);
    request.setSuspendPolicy(EventRequest.SUSPEND_NONE);
    request.enable();
  }

  private static void addCancellationCheckRequest(
      VirtualMachine vm
  ) {
    MethodEntryRequest request =
        vm.eventRequestManager().createMethodEntryRequest();
    request.addClassFilter(CANCELLATION_CHECK_CLASS);
    request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
    request.enable();
  }

  private static void addCancellationRequest(
      VirtualMachine vm
  ) {
    ExceptionRequest request =
        vm.eventRequestManager().createExceptionRequest(
            null,
            true,
            true);
    request.setSuspendPolicy(EventRequest.SUSPEND_NONE);
    request.enable();
  }
}
