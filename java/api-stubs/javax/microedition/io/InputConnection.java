package javax.microedition.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Compile/oracle declaration for the MIDP generic input connection. */
public interface InputConnection extends Connection {
    InputStream openInputStream() throws IOException;

    DataInputStream openDataInputStream() throws IOException;
}
