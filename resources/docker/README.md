# Docker

Se añade un fichero **Dockerfile** para construir el contenedor docker que permita la prueba y ejecución de la aplicación de forma autónoma e independiente del entorno de explotación de la CARM.

El único requisito que debe cumplir la máquina donde se pretenda ejecutar el proyecto **Eeutils-util-firma** es que tenga instalada la aplicación [Docker](https://www.docker.com/)

### Dockerfile

Para el montaje solo es necesario construir una imagen docker a partir del fichero **Dockerfile**. Para ello situese en la carpeta */docker* del proyecto y ejecute el siguiente comando:

```sh
$ sudo docker build -t imagen-eeutil .
```

El comando *build* crea una imagen Docker a partir de un Dockerfile y la opción -t se usa para poner nombre a la imagen. 

Tras su ejecución se irán sucediendo una serie de pasos definidos en el Dockerfile que darán como resultado una imagen con un servidor tomcat y el war del proyecto, listo para ejecutarse.

El Dockerfile se divide en 3 etapas en la que cada una se utiliza la imagen de Docker necesaria para realizar la tarea:

* La primera utiliza una imagen de **git** para descargar el proyecto del repositorio
```docker 
FROM alpine/git
WORKDIR /app
RUN git clone https://github.com/josefsabater/eeutil-util-firma.git
```

* La segunda utiliza una imagen de **maven** para compilar el proyecto
```docker 
FROM maven:3.6.3-jdk-8
WORKDIR /app
COPY --from=0 /app/eeutil-util-firma /app
RUN mvn install 
```

* La tercera y última utiliza una imagen de **tomcat** para desplegar el war generado en la etapa anterior. Además, se le pasa al servidor los argumentos de entrada mediante la variable de entorno JAVA_OPTS
```docker
FROM tomcat:8.5-jdk8
ENV JAVA_OPTS="-Djavax.net.ssl.trustStore=/usr/local/tomcat/conf/eeutil/truststore.jks \
-Djavax.net.ssl.trustStorePassword=changeit \
-Dlocal_home_app=/usr/local/tomcat/conf/eeutil \
-Deeutil-util-firma.config.path=/usr/local/tomcat/conf/eeutil"
WORKDIR /app
COPY --from=1 /app/fuentes/eeutil-util-firma/target/eeutil-util-firma.war /usr/local/tomcat/webapps
EXPOSE 8080
CMD ["catalina.sh", "run"]
```

Tras finalizar el proceso, se habrá creado la imagen **imagen-eeutil** en el repositorio local de Docker. Se puede consultar dicho repositorio ejecutando el comando:

```sh
$ sudo docker images
```

Una vez tenemos la imagen con el servidor tomcat y el war, hay que ejecutar un contenedor que despliegue la aplicación partiendo de dicha imagen. Para ello debemos ejecutar el siguiente comando, sustituyendo los siguientes parámetros:

* <path_config>: Ruta al directorio con los ficheros de configuración
* <path_logs>: Ruta al directorio donde se generan los logs

```sh
$ sudo docker run --name eeutil-util-firma-c -v <path_config>:/usr/local/tomcat/conf/eeutil -v <path_logs>:/usr/local/tomcat/logs -p 8080:8080 eeutil-util-firma-i
```

### Fichero de configuración
La ruta de donde se obtienen los ficheros de configuración, se establece como argumento del servidor Tomcat al arrancarlo, mediante la variable de entorno **JAVA_OPTS**. Estos parámetros establecen rutas propias del contenedor donde se ejecuta el servidor y a las que **no** se tiene acceso desde el exterior. Por poder pasárle al contenedor los ficheros de configuración desde el exterior utilizamos la opción -v, que permite mapear directorios entre un contenedor y el host donde se ejecuta.

### Logs
Notesé que entre los parámetros de entrada de despliegue del servidor, no se encuentra ninguno que indique la localización del log. Esto se debe a que dicha localización se encuentra definida en el fichero de configuración **log4j.properties**. Es por eso que, antes de ejecutar el contenedor, deberá modificar esta ruta para establecer una propia del contenedor:

```
log4j.appender.GENERAL.File=/usr/local/tomcat/logs/eeutil-firma.log
```
