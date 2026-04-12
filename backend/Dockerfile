FROM eclipse-temurin:17-jdk-alpine

COPY footballersmarketapi.jar /footballersmarketapp/footballersmarketapi.jar

WORKDIR /footballersmarketapp

#-Xmx512m make jvm never to take more than 512MB ram for the heap
#-Xms256m allows jvm to grab 256MB of ram immediately on startup
CMD ["java", "-Xmx512m", "-Xms256m", "-jar", "footballersmarketapi.jar"]

