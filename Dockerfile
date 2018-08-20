FROM nicks/plovr-deps

ADD . /plovr

WORKDIR /plovr

RUN buck fetch //third-party/...

RUN buck test :test