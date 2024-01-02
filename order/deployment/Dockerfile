FROM openjdk:17-alpine

ARG ARTIFACTORY_FILE
ENV ARTIFACTORY_FILE ${ARTIFACTORY_FILE}
ENV ARTIFACTORY_HOME ${HOME}

WORKDIR ${ARTIFACTORY_HOME}

# Copy your jar to the container
COPY ./build/libs/${ARTIFACTORY_FILE} ${ARTIFACTORY_HOME}

# Launch the artifactory as docker user
ENTRYPOINT [ "sh", "-c" ]
CMD [ "java -jar ${ARTIFACTORY_FILE}" ]