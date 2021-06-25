package prova;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.kubernetes.client.dsl.base.OperationSupport;
import io.fabric8.kubernetes.client.dsl.internal.LogWatchCallback;
import io.fabric8.kubernetes.client.utils.HttpClientUtils;
import io.fabric8.kubernetes.client.utils.PodOperationUtil;
import io.fabric8.kubernetes.client.utils.URLUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class WaitUntilReadyExample {
    private static final Logger logger = LoggerFactory.getLogger(WaitUntilReadyExample.class);

    @SuppressWarnings("java:S106")
    public static void main(String[] args) throws InterruptedException, MalformedURLException {
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            final String namespace = Optional.ofNullable(client.getNamespace()).orElse("default");
            final Pod pod = client.pods().inNamespace(namespace).create(
                    new PodBuilder()
                            .withNewMetadata().withName("myapp-pod").withLabels(Collections.singletonMap("app", "myapp-pod")).endMetadata()
                            .withNewSpec()
                            .addNewContainer()
                            .withName("myapp-container")
                            .withImage("busybox:1.28")
                            .withCommand("sh", "-c", "while :; do echo 'The app is running!'; sleep 3; done")
                            .endContainer()
                            .addNewInitContainer()
                            .withName("init-myservice")
                            .withImage("busybox:1.28")
                            .withCommand("sh", "-c", "for i in 1 2 3 4 5 6 7 8 9 10; do echo 'initializing ...'; sleep 2; done")
                            .endInitContainer()
                            .endSpec()
                            .build()
            );
            logger.info("Pod created, waiting for it to get ready...");


            for (Container container: client.resource(pod).inNamespace(namespace).get().getSpec().getInitContainers()){
                logger.info("init container {}", container.getName());
                //watchContainerLog(container, client, System.out);
                final LogWatch lwi = client.pods().inNamespace(namespace).withName("myapp-pod").inContainer(container.getName())
                        .tailingLines(10)
                        .watchLog(System.out);
                TimeUnit.SECONDS.sleep(20L);
                lwi.close();
            }

            client.resource(pod).inNamespace(namespace).waitUntilReady(30, TimeUnit.SECONDS);
            logger.info("Pod is ready now");

            final LogWatch lw = client.pods().inNamespace(namespace).withName("myapp-pod").inContainer("")
                    .tailingLines(10)
                    .watchLog(System.out);
            logger.info("Watching Pod logs for 20 seconds...");
            TimeUnit.SECONDS.sleep(20L);

            logger.info("Deleting Pod...");
            client.resource(pod).inNamespace(namespace).delete();
            logger.info("Closing Pod log watch");
            lw.close();
        }
    }

    public static LogWatch watchContainerLog(Container container, KubernetesClient client, OutputStream out) throws MalformedURLException {
            OkHttpClient okHttpClient = HttpClientUtils.createHttpClient(client.getConfiguration());
            OperationSupport operationSupport = new OperationSupport(okHttpClient, client.getConfiguration());
            // Issue Pod Logs HTTP request
            URL url = new URL(URLUtils.join(
                    operationSupport.getResourceUrl().toString(),
                    "log?pretty=true&follow=true&container="+container.getName()));
            Request request = new Request.Builder().url(url).get().build();
            final LogWatchCallback callback = new LogWatchCallback(out);
            OkHttpClient clone = okHttpClient.newBuilder().readTimeout(0, TimeUnit.MILLISECONDS).build();
            clone.newCall(request).enqueue(callback);
            callback.waitUntilReady();
            return callback;
    }
}
