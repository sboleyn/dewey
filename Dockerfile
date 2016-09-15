FROM clojure:alpine

RUN apk add --update git && \
    rm -rf /var/cache/apk

VOLUME ["/etc/iplant/de"]

ARG git_commit=unknown
ARG version=unknown

LABEL org.cyverse.git-ref="$git_commit"
LABEL org.cyverse.version="$version"

COPY . /usr/src/app
COPY conf/main/logback.xml /usr/src/app/logback.xml

WORKDIR /usr/src/app

RUN lein uberjar && \
    cp target/dewey-standalone.jar .

RUN ln -s "/usr/bin/java" "/bin/dewey"

ENTRYPOINT ["dewey", "-Dlogback.configurationFile=/etc/iplant/de/logging/dewey-logging.xml", "-cp", ".:dewey-standalone.jar", "dewey.core"]
CMD ["--help"]
