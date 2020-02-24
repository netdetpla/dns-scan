FROM openjdk:11.0.5-jre-stretch

ADD ["sources.list", "/etc/apt/"]

RUN apt update && apt install -y fping

ADD ["build/libs/dns-scan-ns-1-all.jar", "settings.properties", "/"]

CMD java -jar dns-scan-ns-1-all.jar