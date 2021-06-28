package poc;

import io.fabric8.kubernetes.client.KubernetesClient;

import java.io.PrintStream;
import java.util.regex.Pattern;

public class PodLogsWatcherBuilder {
    private KubernetesClient client;
    private String namespace;
    private PrintStream printStream = System.out;
    private Pattern filter;

    public PodLogsWatcherBuilder withClient(KubernetesClient client) {
        this.client = client;
        return this;
    }

    public PodLogsWatcherBuilder inNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public PodLogsWatcherBuilder outputTo(PrintStream printStream) {
        this.printStream = printStream;
        return this;
    }

    public PodLogsWatcherBuilder exclude(String regexp) {
        this.filter = Pattern.compile(regexp);
        return this;
    }

    public PodLogsWatcher build() {
        if (client == null) {
            throw new IllegalStateException("KubernetesClient must be specified!");
        }
        if (namespace == null) {
            throw new IllegalStateException("namespace must be specified!");
        }

        return new PodLogsWatcher(client, namespace, printStream, filter);
    }
}
