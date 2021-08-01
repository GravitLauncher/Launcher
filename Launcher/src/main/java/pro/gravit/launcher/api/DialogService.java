package pro.gravit.launcher.api;

import pro.gravit.launcher.events.NotificationEvent;

import java.util.function.Consumer;

public class DialogService {
    private static DialogServiceImplementation dialogImpl;
    private static DialogServiceNotificationImplementation notificationImpl;

    private DialogService() {
        throw new UnsupportedOperationException();
    }

    public static void setDialogImpl(DialogServiceImplementation impl) {
        DialogService.dialogImpl = impl;
    }

    public static void setNotificationImpl(DialogServiceNotificationImplementation impl) {
        DialogService.notificationImpl = impl;
    }

    public static boolean isDialogsAvailable() {
        return dialogImpl != null;
    }

    public static boolean isNotificationsAvailable() {
        return notificationImpl != null;
    }

    private static void checkIfAvailable() {
        if (!isDialogsAvailable()) {
            throw new UnsupportedOperationException("DialogService dialogs implementation not available");
        }
    }

    public static void createNotification(NotificationEvent.NotificationType type, String head, String message) {
        if (!isNotificationsAvailable()) {
            throw new UnsupportedOperationException("DialogService notifications implementation not available");
        }
        notificationImpl.createNotification(type, head, message);
    }

    public static void showDialog(String header, String text, Runnable onApplyCallback, Runnable onCloseCallback) {
        checkIfAvailable();
        dialogImpl.showDialog(header, text, onApplyCallback, onCloseCallback);
    }

    public static void showApplyDialog(String header, String text, Runnable onApplyCallback, Runnable onDenyCallback) {
        checkIfAvailable();
        dialogImpl.showApplyDialog(header, text, onApplyCallback, onDenyCallback);
    }

    public static void showApplyDialog(String header, String text, Runnable onApplyCallback, Runnable onDenyCallback, Runnable onCloseCallback) {
        checkIfAvailable();
        dialogImpl.showApplyDialog(header, text, onApplyCallback, onDenyCallback, onCloseCallback);
    }

    public static void showTextDialog(String header, Consumer<String> onApplyCallback, Runnable onCloseCallback) {
        checkIfAvailable();
        dialogImpl.showTextDialog(header, onApplyCallback, onCloseCallback);
    }

    public interface DialogServiceImplementation {
        void showDialog(String header, String text, Runnable onApplyCallback, Runnable onCloseCallback);

        void showApplyDialog(String header, String text, Runnable onApplyCallback, Runnable onDenyCallback);

        void showApplyDialog(String header, String text, Runnable onApplyCallback, Runnable onDenyCallback, Runnable onCloseCallback);

        void showTextDialog(String header, Consumer<String> onApplyCallback, Runnable onCloseCallback);
    }

    public interface DialogServiceNotificationImplementation {
        void createNotification(NotificationEvent.NotificationType type, String head, String message);
    }
}
