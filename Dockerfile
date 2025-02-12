# compile source
FROM amazoncorretto:21-alpine as build

RUN mkdir -p /workspace/app
WORKDIR /workspace/app

COPY pom.xml pom.xml
COPY mvnw mvnw
COPY .mvn/ .mvn/

# build dependencies in separate layer (for caching purposes)
RUN ./mvnw dependency:go-offline -B

# copy source (this layer is rebuild everytime there are changes to code)
COPY src/ src/

# actually compile
RUN ./mvnw clean package -DskipTests

FROM amazoncorretto:21

# Danish timezone
RUN ln -fs /usr/share/zoneinfo/Europe/Copenhagen /etc/localtime
RUN mkdir -p /app
WORKDIR /app
VOLUME /tmp

ARG TARGET=/workspace/app/target
COPY --from=build ${TARGET}/os2compliance.jar .

COPY /deploy .
RUN chmod +x run.sh
EXPOSE 8085

ENTRYPOINT ["/bin/bash", "run.sh"]
