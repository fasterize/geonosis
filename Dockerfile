FROM bigtruedata/sbt:0.13.13-2.12

WORKDIR /app

COPY . .

RUN cp src/main/resources/log4j-console.properties src/main/resources/log4j.properties
RUN sbt compile

ENTRYPOINT ["sbt"]
CMD ["run", "-Dconfig.file=./geonosis.conf"]
