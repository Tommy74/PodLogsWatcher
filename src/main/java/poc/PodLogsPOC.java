package poc;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class PodLogsPOC {

    private static final Logger logger = LoggerFactory.getLogger(PodLogsPOC.class.getSimpleName());

    public static void main(String[] args) throws InterruptedException {
        String namespace = "default";

        try (final KubernetesClient client = new DefaultKubernetesClient()) {

            // watches for POD's events
            Watch watch = client.pods().inNamespace(namespace).watch(new PodLogsWatcher(client, namespace, System.out));

            final Pod pod = client.pods().inNamespace(namespace).create(
                    new PodBuilder()
                            .withNewMetadata().withName("myapp-pod").withLabels(Collections.singletonMap("app", "myapp-pod")).endMetadata()
                            .withNewSpec()
                            .addNewContainer()
                            .withName("myapp-container")
                            .withImage("busybox:1.28")
                            .withCommand("sh", "-c", "while :; do echo 'myapp-container is running!'; sleep 2; done")
                            .endContainer()
                            .addNewInitContainer()
                            .withName("init-a")
                            .withImage("busybox:1.28")
                            .withCommand("sh", "-c", "for i in 1 2 3 4 5 6 7 8 9 10; do echo 'init-a initializing...'; sleep 2; done")
                            .endInitContainer()
                            .addNewInitContainer()
                            .withName("init-b")
                            .withImage("busybox:1.28")
                            .withCommand("sh", "-c", "for i in 1 2 3 4 5 6 7 8 9 10; do echo 'init-b initializing...'; sleep 2; done")
                            .endInitContainer()
                            .endSpec()
                            .build()
            );
            logger.info("Pod created");

            TimeUnit.SECONDS.sleep(60L);

            logger.info("Deleting Pod...");
            try {
                client.resource(pod).inNamespace(namespace).delete().wait();
            } catch (Exception e) {}

            watch.close();
        }
    }
}
