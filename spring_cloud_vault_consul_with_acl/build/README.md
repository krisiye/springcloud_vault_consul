# hello-world-springboot-example

## What is it?

A spring boot example that demonstrates some basic capabilities for Spring boot 2.6 and k8s integration.

## Dependencies

### amazon-corretto-17 - OpenJDK
https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html

#### Mac
https://www.macports.org/ports.php?by=name&substr=jdk

https://github.com/AdoptOpenJDK/homebrew-openjdk


### Maven 3.6.3
https://archive.apache.org/dist/maven/maven-3/3.6.3/binaries/

### Docker
https://docs.docker.com/docker-for-mac/install/
https://docs.docker.com/docker-for-windows/install/

## Build

### Maven

```
mvn clean install
```

### Run

#### spring boot
```
mvn clean install spring-boot:run
mvn clean install spring-boot:run -Dspring-boot.run.profiles=local
```


## Swagger
1. Connect to the Swagger on your local through (http://localhost:8080/swagger-ui.html)
3. You are all set to go and perform actions on Swagger and check the responses accordingly


## Testing Vault and Consul on local profile

A `docker-compose` file has been provided with the project that is configured with Consul KV, Vault and Localstack. It also comes with a startup script that allows us to configure the various backends with a single command. 

Please follow the following guidelines if you wish to run vault, consul and localstack(aws) for your local profile against spring-boot-example (or adapt the same for your apps):

#### Start docker-compose

```
cd ../docker-compose
sh start.sh
```

The start up script starts the following containers:
    - Localstack (IAM, STS, S3)
    - Consul
    - Vault
  - Sets up Consul KV
  - Sets up ACL for Consul and Vault Integration
  - Sets up a S3 bucket with Localstack
    - Adds a sample file for testing s3 integration.
  - Enables Vault 
    - KV
    - Secret Engines
      - Consul
      - Vault
  - Creates necessary vault policies for the secret engines.

#### verify docker status


```
docker-compose % docker ps
CONTAINER ID   IMAGE                           COMMAND                  CREATED          STATUS         PORTS                                                                      NAMES
14b3d6c78e21   vault:1.3.2                     "docker-entrypoint.s…"   11 seconds ago   Up 7 seconds   0.0.0.0:8200->8200/tcp                                                     vault
e03d59db74db   localstack/localstack:0.12.10   "docker-entrypoint.sh"   11 seconds ago   Up 9 seconds   4571/tcp, 0.0.0.0:4566->4566/tcp, 8080/tcp                                 localstack
286bcb290cf2   consul:1.9.5                    "docker-entrypoint.s…"   11 seconds ago   Up 9 seconds   8300-8302/tcp, 8301-8302/udp, 8600/tcp, 8600/udp, 0.0.0.0:8500->8500/tcp   consul

```

- Run the application against the local profile and verify against provided endpoints:
  - mvn clean install spring-boot:run -Dspring-boot.run.profiles=local
  - `/config` for vault kv and consul kv
  - `/aws/s3/list` - list files from s3
  - `/aws/s3/presign` - presign against s3 key

#### Login to Vault and Consul UI
  - Use `foo` as the token (simplified for ease of usage. Feel free to update as appropriate. Make sure to update docker-compose and start scripts if you do decide to update)
  - Vault UI - http://localhost:8200/ui/
  - Consul UI - http://localhost:8500/


#### stop docker compose
```
sh stop.sh
```
