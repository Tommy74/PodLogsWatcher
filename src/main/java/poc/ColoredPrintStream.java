package poc;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class ColoredPrintStream extends PrintStream {
    private PodColor color;
    private String prefix;

    public ColoredPrintStream(OutputStream out, PodColor color, String prefix) {
        super(out, true);
        this.color = color;
        this.prefix = prefix;
    }

    @Override
    public void write(int b) {
        try {
            synchronized (this) {
                if (out == null)
                    throw new IOException("Stream closed");
                out.write(PodColor.ANSI_BRIGHT_BLACK.value.getBytes(StandardCharsets.UTF_8));
                out.write(String.format("[%s] ", prefix).getBytes(StandardCharsets.UTF_8));
                out.write(color.value.getBytes(StandardCharsets.UTF_8));
                out.write(b);
                out.write(PodColor.ANSI_RESET.value.getBytes(StandardCharsets.UTF_8));
                if (b == '\n') {
                    out.flush();
                }
            }
        } catch (InterruptedIOException x) {
            Thread.currentThread().interrupt();
        } catch (IOException x) {
            setError();
        }
    }

    @Override
    public void write(byte buf[], int off, int len) {
        try {
            synchronized (this) {
                if (out == null)
                    throw new IOException("Stream closed");
                out.write(PodColor.ANSI_BRIGHT_BLACK.value.getBytes(StandardCharsets.UTF_8));
                out.write(String.format("[%s] ", prefix).getBytes(StandardCharsets.UTF_8));
                out.write(color.value.getBytes(StandardCharsets.UTF_8));
                out.write(buf, off, len);
                out.write(PodColor.ANSI_RESET.value.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        } catch (InterruptedIOException x) {
            Thread.currentThread().interrupt();
        } catch (IOException x) {
            setError();
        }
    }

}


