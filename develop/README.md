How to develop for this plugin?
====================================

**Note**:
- It's not recommended that using the developing plugin in the production environment.

## Prepare keycloak.json

First, you should start a new Docker container for Keycloak server.

```bash
# Change the owner of the mapped volume
$ sudo docker run --rm \
                -u root \
                -v "$(pwd)/develop/data/keycloak":/mnt \
                --entrypoint /bin/chown \
                -it jboss/keycloak:4.5.0.Final \
                -R jboss /mnt
$ sudo docker run -d --name keycloak-dev \
                -e KEYCLOAK_USER=admin \
                -e KEYCLOAK_PASSWORD=admin123 \
                -v "$(pwd)/develop/data/keycloak":/opt/jboss/keycloak/standalone/data \
                -p 8086:8080 \
                jboss/keycloak:4.5.0.Final
```

Then, login with username `admin` and password `admin123`
to [Configure Keycloak realm client](https://github.com/flytreeleft/nexus3-keycloak-plugin#4-configure-keycloak-realm-client)
and [Create keycloak.json](https://github.com/flytreeleft/nexus3-keycloak-plugin#5-create-keycloakjson).

At last, put the `keycloak.json` into the directory `develop`.

## Prepare the Nexus3 Docker image

```bash
$ sudo bash ./develop/build.sh
```

After the building, the image named as `nexus3-keycloak-dev:3.6.0-02` will be listed in the results of `sudo docker images`.

**Note**: The image is based on [cavemandaveman/nexus](https://github.com/cavemandaveman/nexus) which is running Nexus v3.6.0-02.

## Build this plugin

```bash
$ mvn clean install -Dmaven.test.skip=true
```

## Startup the Nexus3 Docker container

```bash
$ sudo bash ./develop/run.sh
```

**Note**:
- This script will create a container named as `nexus3-keycloak-dev` and map the Nexus3 web port to the local port `8903`.
- The Nexus3 data will be saved into `./develop/data/nexus3` to prevent to lose the configurations after re-creating the container.

## Check if the plugin works as expected

> Everything is ready, just enjoy your development journey! :)

Access `http://localhost:8903` in your browser, and login with username `admin` and password `admin123` to configure your Nexus3.

If you want to check the logs of Nexus3, just running the following command:

```bash
$ sudo docker logs --tail 500 nexus3-keycloak-dev
```

This will print the latest `500` log lines in the console.

And if you updated the plugin, just rebuild this plugin (`mvn clean install -Dmaven.test.skip=true`), then restart the Nexus3 Docker container:

```bash
$ sudo docker restart nexus3-keycloak-dev
```
