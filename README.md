# PodLogsWatcher

This project shows how you can tail all the PODs logs you want leveraging the API offered by 
[fabric8io/kubernetes-client](https://github.com/fabric8io/kubernetes-client);

Basically it showcases you can do in Java the same thin [stern](https://github.com/wercker/stern) achieves using [GO](https://golang.org/); 

To see it in action, download [minikube](https://minikube.sigs.k8s.io/docs/start/) and run:

```shell
minikube start
mvn test
```

This command starts test `PodLogsTestCase` and this is what happens:

- the test has the custom annotation `@PodLog`
- the custom annotation `@PodLog` has the JUnit 5 annotation `@ExtendWith(PodLogExtension.class)`: this annotation tells 
  JUnit to register the `PodLogExtension` custom extension
- the `PodLogExtension` custom extension starts `watchLog` for every newly running POD

[Loggable#watchLog](https://github.com/fabric8io/kubernetes-client/blob/v5.5.0/kubernetes-client/src/main/java/io/fabric8/kubernetes/client/dsl/Loggable.java#L56) is an API offered by [fabric8io/kubernetes-client](https://github.com/fabric8io/kubernetes-client)
which tails a POD's log and redirects it to an OutputStream;