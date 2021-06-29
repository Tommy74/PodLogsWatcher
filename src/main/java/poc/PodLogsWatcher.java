package poc;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * Watches PODs on a single namespace ans streams logs for all PODs containers to the selected  {@link PrintStream};
 * Each log is prefixed with <code>namespace.pod-name.container-name</code> and colored differently;
 */
public class PodLogsWatcher implements Watcher<Pod> {

    private static final Logger logger = LoggerFactory.getLogger(PodLogsWatcher.class.getSimpleName());
    private KubernetesClient client;
    private String namespace;
    private PrintStream printStream;
    private Pattern filter;

    protected PodLogsWatcher(KubernetesClient client, String namespace, PrintStream printStream, Pattern filter) {
        this.client = client;
        this.namespace = namespace;
        this.printStream = printStream;
        this.filter = filter;
    }

    private List<ContainerStatus> runningStatusesBefore = Collections.emptyList();
    private List<ContainerStatus> runningStatuses = Collections.emptyList();
    private final List<LogWatch> logWatches = new ArrayList<>();

    @SneakyThrows
    @Override
    public void eventReceived(Action action, Pod pod) {
        logger.debug("\n================================ \n{} {}\n================================\n", action.name(), pod.getMetadata().getName());
        runningStatuses = getRunningContainers(pod);
        logger.debug("runningStatuses.size={} names={} runningStatuses={}",
                runningStatuses.size(),
                runningStatuses.stream().map(cs -> cs.getName()).collect(Collectors.joining()),
                runningStatuses
        );
        List<ContainerStatus> newRunning = getNewRunningContainers(runningStatusesBefore, runningStatuses);
        logger.debug("newRunning.size={}", newRunning.size());
        runningStatusesBefore = runningStatuses;

        for (ContainerStatus status : newRunning) {
            logger.debug("CONTAINER status name {} started {} ready {} \n\t waiting {} \n\t running {} \n\t terminated {} \n\t complete status {}",
                    status.getName(),
                    status.getStarted(),
                    status.getReady(),
                    status.getState().getWaiting(),
                    status.getState().getRunning(),
                    status.getState().getTerminated(),
                    pod.getStatus()
            );
            if (filter != null && filter.matcher(pod.getMetadata().getName()).matches()) {
                logger.info("Skipped POD {}.{}", namespace, pod.getMetadata().getName());
                continue;
            }
            if (filter != null && filter.matcher(status.getName()).matches()) {
                logger.info("Skipped Container {}.{}.{}", namespace, pod.getMetadata().getName(), status.getName());
                continue;
            }
            final LogWatch lw = client.pods().inNamespace(namespace).withName(pod.getMetadata().getName()).inContainer(status.getName())
                    .tailingLines(10)
                    .watchLog(new ColoredPrintStream(
                            printStream,
                            PodColor.getNext(),
                            String.format("%s.%s.%s", namespace, pod.getMetadata().getName(), status.getName())
                    ));
            logWatches.add(lw);
        }
    }

    /**
     * All containers that moved into running state
     *
     * @return
     */
    private List<ContainerStatus> getNewRunningContainers(final List<ContainerStatus> before, final List<ContainerStatus> now) {
        List<String> namesBefore = before.stream().map(cs -> cs.getContainerID()).collect(Collectors.toList());
        return now.stream()
                .filter(element -> !namesBefore.contains(element.getContainerID()))
                .collect(Collectors.toList());
    }

    /**
     * All running containers inside POD
     *
     * @param pod
     * @return
     */
    private List<ContainerStatus> getRunningContainers(final Pod pod) {
        List<ContainerStatus> containers = new ArrayList<>();
        containers.addAll(
                pod.getStatus().getInitContainerStatuses().stream().filter(
                        containerStatus -> containerStatus.getState().getRunning() != null
                ).collect(toList())
        );
        containers.addAll(
                pod.getStatus().getContainerStatuses().stream().filter(
                        containerStatus -> containerStatus.getState().getRunning() != null
                ).collect(toList())
        );
        return containers;
    }

    @Override
    public void onClose(WatcherException e) {
        logWatches.stream().forEach(lw -> lw.close());
        logger.info("PodLogsWatcher Closed");
    }

    protected static class Builder {
        private KubernetesClient client;
        private String namespace;
        private PrintStream printStream = System.out;
        private Pattern filter;

        protected Builder withClient(final KubernetesClient client) {
            this.client = client;
            return this;
        }

        protected Builder inNamespace(final String namespace) {
            this.namespace = namespace;
            return this;
        }

        protected Builder outputTo(final PrintStream printStream) {
            this.printStream = printStream;
            return this;
        }

        protected Builder exclude(final Pattern filter) {
            this.filter = filter;
            return this;
        }

        protected PodLogsWatcher build() {
            if (client == null) {
                throw new IllegalStateException("KubernetesClient must be specified!");
            }
            if (namespace == null) {
                throw new IllegalStateException("namespace must be specified!");
            }

            return new PodLogsWatcher(client, namespace, printStream, filter);
        }
    }
}
