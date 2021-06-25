package poc;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class PodLogsWatcher implements Watcher<Pod> {

    private static final Logger logger = LoggerFactory.getLogger(PodLogsWatcher.class.getSimpleName());
    private KubernetesClient client;
    private String namespace;

    public PodLogsWatcher(KubernetesClient client, String namespace) {
        this.client = client;
        this.namespace = namespace;
    }

    private void logPodStatus(Pod pod) {
        for (ContainerStatus status: pod.getStatus().getInitContainerStatuses()) {
            logger.info("INIT CONTAINER status name {} started {} ready {} \n\t waiting {} \n\t running {} \n\t terminated {} \n\t complete status {}",
                    status.getName(),
                    status.getStarted(),
                    status.getReady(),
                    status.getState().getWaiting(),
                    status.getState().getRunning(),
                    status.getState().getTerminated(),
                    pod.getStatus()
            );
        }
        for (ContainerStatus status: pod.getStatus().getContainerStatuses()) {
            logger.info("CONTAINER status name {} started {} ready {} \n\t waiting {} \n\t running {} \n\t terminated {} \n\t complete status {}",
                    status.getName(),
                    status.getStarted(),
                    status.getReady(),
                    status.getState().getWaiting(),
                    status.getState().getRunning(),
                    status.getState().getTerminated(),
                    pod.getStatus()
            );
        }
    }

    private List<ContainerStatus> runningStatusesBefore = Collections.emptyList();
    private List<ContainerStatus> runningStatuses = Collections.emptyList();
    private final List<LogWatch> logWatches = new ArrayList<>();

    @Override
    public void eventReceived(Action action, Pod pod) {
        logger.info("\n================================ \n{} {}\n================================\n", action.name(), pod.getMetadata().getName());
        runningStatuses = getRunningContainers(pod);
        logger.info("runningStatuses.size={} names={} runningStatuses={}",
                runningStatuses.size(),
                runningStatuses.stream().map(cs -> cs.getName()).collect(Collectors.joining()),
                runningStatuses
        );
        //logPodStatus(pod);
        List<ContainerStatus> newRunning = getNewRunningContainers(runningStatusesBefore, runningStatuses);
        logger.info("newRunning.size={}", newRunning.size());
        runningStatusesBefore = runningStatuses;

        for (ContainerStatus status: newRunning) {
            logger.info("CONTAINER status name {} started {} ready {} \n\t waiting {} \n\t running {} \n\t terminated {} \n\t complete status {}",
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
                    /*.watchLog(System.out);*/
                    .watchLog(new ColoredPrintStream(System.out, "\u001b[32m")); // TODO: parametrize color
            logWatches.add(lw);
        }
    }

    /**
     * All containers that moved into running state
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
        logger.info( "PodLogsWatcher Closed");
    }
}
