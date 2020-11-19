FROM ubuntu:16.04
WORKDIR /usr/src/app
COPY * ./
ENV ACCESS_KEY null
RUN apt-get update
RUN apt-get install -y default-jdk
RUN javac -cp *: UserRequestsGUI.java
CMD ["java", "-cp", "*:", "UserRequestsGUI"]