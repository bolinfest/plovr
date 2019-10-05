FROM xvilo/plovr-base

# Run tests
ADD . /plovr
WORKDIR /plovr
RUN buck fetch //third-party/...
RUN buck test :test