import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;

public class BasicHttpServerExample {
	final int PORT = 1234; //

	public static void main( String [ ] args ) {
		BasicHttpServerExample basicHttpServerExample = new BasicHttpServerExample();
		basicHttpServerExample.launchServer();
	}

	public void launchServer() {
		try {
			HttpServer server = HttpServer.create ( new InetSocketAddress (PORT ), 
					0 );
			HttpContext context = server.createContext( "/" );
			context.setHandler(this::handleRequest);
			System.out.println("This is attempting to run on port: " + PORT +
					"SocketAddress: " + server.getAddress().getHostName());
			server.start();
		} catch ( IOException e ) {
			e.printStackTrace () ;
		}
	}

	private void handleRequest( HttpExchange httpExchange ) throws IOException {
		URI uri = httpExchange.getRequestURI();
		String response = "Path: " + uri.getPath() + "\n";
		response += "Hello, World!\n";
		Headers h = httpExchange.getResponseHeaders();
		h.set( "Content-Type", "text/plain" );
		httpExchange.sendResponseHeaders( 200, response.length());
		OutputStream os = httpExchange.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}
}
