package javax.microedition.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface HttpConnection extends Connection {
    String GET = "GET";
    String POST = "POST";
    int HTTP_OK = 200;

    void setRequestMethod(String method) throws IOException;

    void setRequestProperty(String key, String value) throws IOException;

    int getResponseCode() throws IOException;

    String getHeaderField(String name) throws IOException;

    InputStream openInputStream() throws IOException;

    DataInputStream openDataInputStream() throws IOException;

    OutputStream openOutputStream() throws IOException;
}
