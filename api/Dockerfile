

# Usar imagen base directamente
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copiar el JAR ya construido (desde el contexto api/)
COPY api/build/libs/*.jar app.jar

# Exponer puerto
EXPOSE 8080

# Comando de ejecución
ENTRYPOINT ["java", "-jar", "app.jar"]
