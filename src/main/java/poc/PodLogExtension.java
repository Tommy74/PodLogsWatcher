package poc;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
public class PodLogExtension implements BeforeAllCallback, AfterAllCallback, TestInstancePostProcessor {

    private PodLogs podLogs = null;
    private static KubernetesClient client = null;

    public static KubernetesClient getClient() {
        return client;
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        log.info("beforeAll");
        client = new DefaultKubernetesClient();
        PodLog[] podLogArr = extensionContext.getRequiredTestClass().getAnnotationsByType(PodLog.class);
        PodLog podLog;
        if (podLogArr.length > 0) {
            podLog = podLogArr[0];

            log.info("namespaces = \"{}\"", Arrays.stream(podLog.namespaces()).collect(Collectors.joining(",")));
            log.info("filter = \"{}\"", podLog.filter());
            log.info("value = \"{}\"", podLog.value());

            podLogs = new PodLogs
                    .Builder()
                    .withClient(client)
                    .inNamespaces(Arrays.asList(PodLog.UNASSIGNED.equals(podLog.namespaces()) ? new String[]{"default"} : podLog.namespaces()))
                    .outputTo(PodLog.UNASSIGNED.equals(podLog.value()) ? System.out : null)
                    .exclude(PodLog.UNASSIGNED.equals(podLog.filter()) ? null : podLog.filter())
                    .build();
            podLogs.start();
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        if (podLogs != null) podLogs.stop();
        if (client != null) client.close();
    }

    @Override
    public void postProcessTestInstance(Object o, ExtensionContext extensionContext) throws Exception {

    }
}
