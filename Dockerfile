FROM container-registry.oracle.com/graalvm/jdk:17
WORKDIR .
COPY target/scala-3.3.1/finance-api-assembly-0.1.0-SNAPSHOT.jar /
CMD java -jar finance-api-assembly-0.1.0-SNAPSHOT.jar
ENTRYPOINT finance-api-assembly-0.1.0-SNAPSHOT.jar