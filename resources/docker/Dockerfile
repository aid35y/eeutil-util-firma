FROM alpine/git
WORKDIR /app
RUN git clone https://github.com/josefsabater/eeutil-util-firma.git

FROM maven:3.6.3-jdk-8
WORKDIR /app
COPY --from=0 /app/eeutil-util-firma /app
RUN mvn install

FROM tomcat:8.5-jdk8
ENV JAVA_OPTS="-Djavax.net.ssl.trustStore=/usr/local/tomcat/conf/eeutil/truststore.jks \
-Djavax.net.ssl.trustStorePassword=changeit \
-Dlocal_home_app=/usr/local/tomcat/conf/eeutil \
-Deeutil-util-firma.config.path=/usr/local/tomcat/conf/eeutil"
WORKDIR /app
# ADD ./config /usr/local/tomcat/conf/eeutil
COPY --from=1 /app/fuentes/eeutil-util-firma/target/eeutil-util-firma.war /usr/local/tomcat/webapps
EXPOSE 8080
CMD ["catalina.sh", "run"]
