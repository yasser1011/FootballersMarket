FROM eclipse-temurin:17-jdk-alpine

COPY target/footballersmarket-0.0.2.jar /footballersmarketapp/footballersmarket-0.0.2.jar

WORKDIR /footballersmarketapp

#-Xmx512m make jvm never to take more than 512MB ram for the heap
#-Xms256m allows jvm to grab 256MB of ram immediately on startup
CMD ["java", "-Xmx512m", "-Xms256m", "-jar", "footballersmarket-0.0.2.jar"]

