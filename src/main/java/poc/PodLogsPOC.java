package poc;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PodLogsPOC {

    private static final Logger logger = LoggerFactory.getLogger(PodLogsPOC.class.getSimpleName());

    public static void main(String[] args) throws InterruptedException {
        List<String> namespaces = Arrays.asList("default", "default2");
        PodLogs podLogs = null;

        try (final KubernetesClient client = new DefaultKubernetesClient()) {

            NamespaceList namespaceList = client.namespaces().list();
            if (namespaceList.getItems().stream().filter(
                    ns -> ns.getMetadata().getName().equalsIgnoreCase("default2")).collect(Collectors.toList()).isEmpty()) {
                Namespace ns = new NamespaceBuilder().withNewMetadata().withName("default2").endMetadata().build();
                logger.info("Created namespace: {}", client.namespaces().create(ns).getMetadata().getName());
            }

            podLogs = new PodLogs
                    .Builder()
                    .withClient(client)
                    .inNamespaces(namespaces)
                    .outputTo(System.out)
                    .exclude(".*init-skip.*")
                    .build();
            podLogs.start();

            final Pod pod1 = client.pods().inNamespace("default").create(
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
                            .withCommand("sh", "-c", "for i in 1 2 3 4 5 6 7; do echo 'init-a initializing...'; sleep 2; done")
                            .endInitContainer()
                            .addNewInitContainer()
                            .withName("init-b")
                            .withImage("busybox:1.28")
                            .withCommand("sh", "-c", "for i in 1 2 3 4 5 6 7; do echo 'init-b initializing...'; sleep 2; done")
                            .endInitContainer()
                            .addNewInitContainer()
                            .withName("init-skip")
                            .withImage("busybox:1.28")
                            .withCommand("sh", "-c", "for i in 1 2 3 4; do echo 'init-skip initializing...'; sleep 2; done")
                            .endInitContainer()
                            .endSpec()
                            .build()
            );
            logger.info("Pod1 created");

            final Pod pod2 = client.pods().inNamespace("default2").create(
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
                            .withCommand("sh", "-c", "for i in 1 2 3 4 5 6 7; do echo 'init-a initializing...'; sleep 2; done")
                            .endInitContainer()
                            .addNewInitContainer()
                            .withName("init-b")
                            .withImage("busybox:1.28")
                            .withCommand("sh", "-c", "for i in 1 2 3 4 5 6 7; do echo 'init-b initializing...'; sleep 2; done")
                            .endInitContainer()
                            .addNewInitContainer()
                            .withName("init-skip")
                            .withImage("busybox:1.28")
                            .withCommand("sh", "-c", "for i in 1 2 3 4; do echo 'init-skip initializing...'; sleep 2; done")
                            .endInitContainer()
                            .endSpec()
                            .build()
            );
            logger.info("Pod2 created");

            TimeUnit.SECONDS.sleep(80L);

            logger.info("Deleting Pod1...");
            try {
                client.resource(pod1).inNamespace("default").delete().wait();
            } catch (Exception e) {
            }

            logger.info("Deleting Pod2...");
            try {
                client.resource(pod2).inNamespace("default2").delete().wait();
            } catch (Exception e) {
            }

            podLogs.stop();
        }
    }
}
