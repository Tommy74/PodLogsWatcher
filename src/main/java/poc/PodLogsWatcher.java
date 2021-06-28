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
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class PodLogsWatcher implements Watcher<Pod> {

    private static final Logger logger = LoggerFactory.getLogger(PodLogsWatcher.class.getSimpleName());
    private KubernetesClient client;
    private String namespace;
    private PrintStream printStream;

    public PodLogsWatcher(KubernetesClient client, String namespace, PrintStream printStream) {
        this.client = client;
        this.namespace = namespace;
        this.printStream = printStream;
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
            // TODO: add an optional filter for unwanted PODs / Containers
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
    private List<ContainerStatus> getNewRunningContainers(List<ContainerStatus> before, List<ContainerStatus> now) {
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
    private List<ContainerStatus> getRunningContainers(Pod pod) {
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
}
