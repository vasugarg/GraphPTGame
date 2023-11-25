FROM openjdk:11
WORKDIR /usr/src/app
COPY ./target/scala-2.13/akkaTutorial.jar /usr/src/app/akkaTutorial.jar
COPY inputs/NetGraph_14-11-23-20-29-30.ngs /usr/src/app/NetGraph_14-11-23-20-29-30.ngs
COPY inputs/NetGraph_14-11-23-20-29-30.ngs.perturbed /usr/src/app/NetGraph_14-11-23-20-29-30.ngs.perturbed
ENV ORIGINAL_GRAPH_PATH /usr/src/app/NetGraph_14-11-23-20-29-30.ngs
ENV PERTURBED_GRAPH_PATH /usr/src/app/NetGraph_14-11-23-20-29-30.ngs.perturbed
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/usr/src/app/akkaTutorial.jar"]