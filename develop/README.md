How to develop for this plugin?
====================================

**Note**:
- It's not recommended that using the developing plugin in the production environment.

## Prepare keycloak.json

First, you should create a new Docker container to start a Keycloak server instance.

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
                -p 172.17.0.1:8086:8080 \
                jboss/keycloak:4.5.0.Final
```

Or just run the script:
```bash
$ sudo bash ./develop/run-keycloak.sh
```

Then, access `http://172.17.0.1:8086` and login with username `admin` and password `admin123`
to [Configure Keycloak realm client](https://github.com/flytreeleft/nexus3-keycloak-plugin#4-configure-keycloak-realm-client)
and [Create keycloak.json](https://github.com/flytreeleft/nexus3-keycloak-plugin#5-create-keycloakjson).

> Bind port to `172.17.0.1` to make sure that Nexus3 and Keycloak can access each other.

At last, put the `keycloak.json` into the directory `develop`.

## Prepare the Nexus3 Docker image

Execute the image building script:

```bash
$ sudo bash ./develop/build-nexus3.sh
```

After that, a Docker image which is named as `nexus3-keycloak-dev:3.6.0-02` will be shown in the list of `sudo docker images`.

**Note**: The image is based on [cavemandaveman/nexus](https://github.com/cavemandaveman/nexus) which is running Nexus v3.6.0-02.

## Build this plugin

```bash
$ mvn clean install -Dmaven.test.skip=true
```

## Startup the Nexus3 Docker container

```bash
$ sudo bash ./develop/run-nexus3.sh
```

**Note**:
- This script will create a container which is named as `nexus3-keycloak-dev` and will map the Nexus3 web port to the local port `8903`.
- The Nexus3 data will be saved into `./develop/data/nexus3` to prevent to lose the configurations after re-creating the container.

## Check if the plugin works as expected

> Everything is ready, just enjoy your development journey! :)

Access `http://172.17.0.1:8903` in your browser, and login with username `admin` and password `admin123`
to [configure](https://github.com/flytreeleft/nexus3-keycloak-plugin#usage) your Nexus3.

If you want to check the logs of the Nexus3 server, just running the following command:

```bash
$ sudo docker logs --tail 500 nexus3-keycloak-dev
```

This will print the latest `500` log lines into the console.

And if you updated this plugin, just [rebuild](#build-this-plugin) it,
then you just only need to restart the Nexus3 container like this:

```bash
$ sudo docker restart nexus3-keycloak-dev
```
