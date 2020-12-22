# Se descargan los dos proyectos
FROM alpine/git
WORKDIR /app
RUN git clone https://github.com/josefsabater/eeutil-shared.git
RUN git clone https://github.com/josefsabater/eeutil-util-firma.git

# Se compilan los dos proyectos
FROM maven:3.6.3-jdk-8
WORKDIR /app
COPY --from=0 /app /app
WORKDIR /app/eeutil-shared
RUN mvn install
WORKDIR /app/eeutil-util-firma
RUN mvn install

# Se copiar el war generado en el directorio del tomcat y se arranca con las argumentos establecidos en JAVA_OPTS
FROM tomcat:8.5-jdk8
ENV JAVA_OPTS="-Djavax.net.ssl.trustStore=/usr/local/tomcat/conf/eeutil/truststore.jks \
-Djavax.net.ssl.trustStorePassword=changeit \
-Dlocal_home_app=/usr/local/tomcat/conf/eeutil \
-Deeutil-util-firma.config.path=/usr/local/tomcat/conf/eeutil"
WORKDIR /app
COPY --from=1 /app/eeutil-util-firma/fuentes/eeutil-util-firma/target/eeutil-util-firma.war /usr/local/tomcat/webapps
EXPOSE 8080
CMD ["catalina.sh", "run"]
