# We need an image with jdk7 and ant, which CircleCI no longer provides.

FROM openjdk:7-jdk

# From https://github.com/circleci/circleci-images/blob/master/openjdk/generate-images
RUN curl --silent --show-error --location --fail --retry 3 --output /tmp/apache-ant.tar.gz \
    https://archive.apache.org/dist/ant/binaries/apache-ant-1.9.6-bin.tar.gz \
  && tar xf /tmp/apache-ant.tar.gz -C /opt/ \
  && ln -s /opt/apache-ant-* /opt/apache-ant \
  && rm -rf /tmp/apache-ant.tar.gz \
  && /opt/apache-ant/bin/ant -version
ENV ANT_HOME=/opt/apache-ant
ENV PATH="/opt/sbt/bin:/opt/apache-maven/bin:/opt/apache-ant/bin:/opt/gradle/bin:$PATH"
