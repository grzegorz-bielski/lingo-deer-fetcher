# lingo-deer-fetcher
A scala-cli program that fetches publicly available tips from [lingo deer](https://lingodeer.com/).

Tips are in HTML format and are immediately browserable through any static file server.

## setup
1. install [coursier](https://get-coursier.io/)

    (mac)
    ```
    curl -fL https://github.com/coursier/launchers/raw/master/cs-x86_64-apple-darwin.gz | gzip -d > cs
    chmod +x cs
    ./cs setup
    ```
1. install [scala-cli](https://scala-cli.virtuslab.org/)
    ```
    cs install scala-cli --contrib
    ```
1. run program
    ```
    scala-cli ./fetch.scala
    ```