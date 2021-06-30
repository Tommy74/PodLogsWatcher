# PodLogsWatcher

This project shows how you can tail all the PODs logs you want leveraging the APIs API offered by 
[fabric8io/kubernetes-client](https://github.com/fabric8io/kubernetes-client);

Install `minikube` and run:

```shell
minikube start
mvn test
```

This command starts test `PodLogsTestCase` and this is what happens:

- the test has the custom annotation `@PodLog`
- the custom annotation `@PodLog` has the JUnit 5 annotation `@ExtendWith(PodLogExtension.class)`: this annotation tells 
  JUnit to register the `PodLogExtension` extension
- the `PodLogExtension` extension starts a new `watchLog` for every newly running POD

`watchLog` is an API offered by [fabric8io/kubernetes-client](https://github.com/fabric8io/kubernetes-client)
which tails a POD's log and redirects it to an OutputStream;