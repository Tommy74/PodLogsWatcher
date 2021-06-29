package poc;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Watches PODs on multiple namespaces ans streams logs for all PODs containers to the selected  {@link PrintStream};
 * Each log is prefixed with <code>namespace.pod-name.container-name</code> and colored differently;
 */
public class PodLogs {
    private KubernetesClient client;
    private List<String> namespaces;
    private PrintStream printStream;
    private Pattern filter;
    private List<Watch> watches;

    private PodLogs(KubernetesClient client, List<String> namespaces, PrintStream printStream, Pattern filter) {
        this.client = client;
        this.namespaces = namespaces;
        this.printStream = printStream == null ? System.out : printStream;
        this.filter = filter;
        this.watches = new ArrayList<>();
    }

    /**
     * Start watching PODs logs in all namespaces specified
     */
    public void start() {
        if (namespaces != null && !namespaces.isEmpty()) {
            for (String namespace : namespaces) {
                Watch watch = client.pods().inNamespace(namespace).watch(
                        new PodLogsWatcher
                                .Builder()
                                .withClient(client)
                                .inNamespace(namespace)
                                .outputTo(System.out)
                                .exclude(filter)
                                .build()
                );
                watches.add(watch);
            }
        }
    }

    /**
     * Stop watching PODs logs in all namespaces specified
     */
    public void stop() {
        for (Watch watch : watches) {
            watch.close();
        }
    }

    public static class Builder {
        private KubernetesClient client;
        private List<String> namespaces;
        private PrintStream printStream = System.out;
        private Pattern filter;

        public Builder withClient(final KubernetesClient client) {
            this.client = client;
            return this;
        }

        public Builder inNamespaces(final List<String> namespaces) {
            this.namespaces = namespaces;
            return this;
        }

        public Builder outputTo(final PrintStream printStream) {
            this.printStream = printStream;
            return this;
        }

        public Builder exclude(final String regexp) {
            this.filter = Pattern.compile(regexp);
            return this;
        }

        public PodLogs build() {
            if (client == null) {
                throw new IllegalStateException("KubernetesClient must be specified!");
            }
            if (namespaces == null) {
                throw new IllegalStateException("namespaces must be specified!");
            }

            return new PodLogs(client, namespaces, printStream, filter);
        }
    }
}
