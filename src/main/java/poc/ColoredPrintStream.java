package poc;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class ColoredPrintStream extends PrintStream
{
    private String COLOR_DEFAULT;
    private static final String COLOR_RESET = "\u001b[0m";

    public ColoredPrintStream(OutputStream out, String COLOR_DEFAULT) {
        super(out, true);
        this.COLOR_DEFAULT = COLOR_DEFAULT;
    }

    @Override
    public void write(int b) {
        try {
            synchronized (this) {
                if (out == null)
                    throw new IOException("Stream closed");

                out.write(b);
                if (b == '\n') {
                    out.write(COLOR_DEFAULT.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    out.write(COLOR_RESET.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
        catch (InterruptedIOException x) {
            Thread.currentThread().interrupt();
        }
        catch (IOException x) {
            setError();
        }
    }

    @Override
    public void write(byte buf[], int off, int len) {
        try {
            synchronized (this) {
                if (out == null)
                    throw new IOException("Stream closed");
                out.write(COLOR_DEFAULT.getBytes(StandardCharsets.UTF_8));
                out.write(buf, off, len);
                out.write(COLOR_RESET.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        }
        catch (InterruptedIOException x) {
            Thread.currentThread().interrupt();
        }
        catch (IOException x) {
            setError();
        }
    }

}


