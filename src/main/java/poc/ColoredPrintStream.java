package poc;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class ColoredPrintStream extends PrintStream {
    private PodColor color;
    private String prefix;

    private ColoredPrintStream(OutputStream out, PodColor color, String prefix) {
        super(out, true);
        this.color = color;
        this.prefix = prefix;
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

    public static class Builder {
        private OutputStream outputStream;
        private PodColor color;
        private String prefix;

        public Builder outputTo(final OutputStream outputStream) {
            this.outputStream = outputStream;
            return this;
        }

        public Builder witColor(final PodColor color) {
            this.color = color;
            return this;
        }

        public Builder witPrefix(final String prefix) {
            this.prefix = prefix;
            return this;
        }

        public ColoredPrintStream build() {
            if (outputStream == null) {
                throw new IllegalStateException("outputStream must be specified!");
            }
            if (color == null) {
                throw new IllegalStateException("color must be specified!");
            }
            if (prefix == null) {
                throw new IllegalStateException("prefix must be specified!");
            }
            return new ColoredPrintStream(outputStream, color, prefix);
        }

    }

}
