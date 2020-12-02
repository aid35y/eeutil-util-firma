# mvn-repo

Repositorio de librerias externas (no publicadas en maven-central). 

Para que el Maven de su aplicación pueda descargarlas deberá **hacer referencia a la rama mvn-repo desde el pom.xml** de su proyecto. 

También es posible instalar la librerias externas en local descargando esta rama e ejecutando los siguientes comandos:

```sh
mvn install:install-file -Dfile=miniapplet-full_1_5.jar -DgroupId=es.gob.afirma -DartifactId=miniapplet-afirma -Dversion=5.0 -Dpackaging=jar
mvn install:install-file -Dfile=vistaDocumentoIGAE-3.0.23.jar -DgroupId=es.igae -DartifactId=vistaDocumentoIGAE -Dversion=3.0.23 -Dpackaging=jar 
mvn install:install-file -Dfile=jodconverter-2.2.2.jar -DgroupId=com.artofsolving -DartifactId=jodconverter -Dversion=2.2.2 -Dpackaging=jar
mvn install:install-file -Dfile=jodconverter-cli-2.2.2.jar -DgroupId=com.artofsolving -DartifactId=jodconverter-cli -Dversion=2.2.2 -Dpackaging=jar
mvn install:install-file -Dfile=jodconverter-core-3.0-beta-4-jahia2.jar -DgroupId=com.artofsolving -DartifactId=jodconverter-core -Dversion=3.0-beta-4-jahia2 -Dpackaging=jar
mvn install:install-file -Dfile=POLA.jar -DgroupId=com.pdfTools -DartifactId=pDFOptimizer-windows -Dversion=4.8 -Dpackaging=jar
mvn install:install-file -Dfile=POLA.jar -DgroupId=com.pdfTools -DartifactId=pDFOptimizer-linux -Dversion=4.8 -Dpackaging=jar
```

