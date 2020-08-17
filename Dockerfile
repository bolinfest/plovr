FROM nicks/plovr-deps

# Run tests
ADD . /plovr
WORKDIR /plovr
RUN buck fetch //third-party/...
RUN buck test :test