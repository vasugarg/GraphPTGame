FROM openjdk:11
WORKDIR /usr/src/app
COPY ./target/scala-2.13/graphPTGame.jar /usr/src/app/graphPTGame.jar
COPY ./inputs/ /usr/src/app/
ENV ORIGINAL_GRAPH_PATH /usr/src/app/NetGraph_14-11-23-20-29-30.ngs
ENV PERTURBED_GRAPH_PATH /usr/src/app/NetGraph_14-11-23-20-29-30.ngs.perturbed
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/usr/src/app/graphPTGame.jar"]