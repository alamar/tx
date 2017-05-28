tx
==

This is a sample Play Framework Java project. It is a RESTful API for money transfers between users' accounts with in-memory data storage. Tests are included.

To run it, you need sbt:

    tx$ sbt
    [tx] $ compile
    [success] Total time: 1 s, completed 28.05.2017 15:51:33
    [tx] $ test
    [info] Test run started
    ...
    [success] Total time: 13 s, completed 28.05.2017 15:52:03
    [tx] $ run
    [info] p.c.s.NettyServer - Listening for HTTP on /0:0:0:0:0:0:0:0:9000

Then direct yourself to http://localhost:9000/ for brief API description.
