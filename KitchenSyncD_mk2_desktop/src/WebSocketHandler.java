import org.webbitserver.BaseWebSocketHandler;
import org.webbitserver.WebServer;
import org.webbitserver.WebServers;
import org.webbitserver.WebSocketConnection;
import org.webbitserver.handler.StaticFileHandler;

/**
 * Created by souradip on 16/12/15.
 */
public class WebSocketHandler extends BaseWebSocketHandler {
    private int connectionCount;

    public void onOpen(WebSocketConnection connection) {
        connection.send("Hello! There are " + connectionCount + " other connections active");
        connectionCount++;
    }

    public void onClose(WebSocketConnection connection) {
        connectionCount--;
    }

    public void onMessage(WebSocketConnection connection, String message) {
        // if we just connected, we want to know what phones are available

        // ask a phone what the x to y most recent contacts were, what their numbers are and what the status line should be

        // get latest x to y messages from a contact from a phone

        // received a new message while app was open, update ui

        // send sms to this number pls
        connection.send(message.toUpperCase()); // echo back message in upper case
    }

    public static void main(String[] args) {
        WebServer webServer = WebServers.createWebServer(9999)
                .add("/hellowebsocket", new WebSocketHandler())
                .add(new StaticFileHandler("/web"));
        webServer.start();
        System.out.println("Server running at " + webServer.getUri());
    }
}
