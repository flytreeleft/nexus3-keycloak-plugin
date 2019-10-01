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

$ sudo docker run -d --name nexus3-keycloak \
                -e KEYCLOAK_LOGLEVEL=DEBUG \
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

At last, put the `keycloak.json` into the directory `develop`, or overwrite the existing one.

**Note**: If you want to test multiple Keycloak realms, just export the `keycloak.json` and rename it as `keycloak.{index}.json`,
the `{index}` can be `0`, `1` or `2`.

## Prepare the Nexus3 Docker image

Execute the image building script:

```bash
$ sudo bash ./develop/build-nexus3.sh
```

After that, a Docker image which is named as `nexus3-keycloak-dev:3.16.2-01` will be shown in the list of `sudo docker images`.

**Note**:
- The image is based on [cavemandaveman/nexus](./cavemandaveman-nexus-docker/) (source from https://github.com/cavemandaveman/nexus) which is running Nexus `v3.16.2-01`.

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
# Print the latest 500 lines
$ sudo docker logs --tail 500 nexus3-dev

# Print the log to file
$ sudo docker logs nexus3-dev >& nexus3.log
```

And if you updated this plugin, just [rebuild](#build-this-plugin) it,
then you just only need to restart the Nexus3 container like this:

```bash
$ sudo docker restart nexus3-dev
```

## Single Sign On (SSO) supports via Nginx gateway

If you want to use or test the SSO feature, you should build the Nginx gateway first:

```bash
$ sudo bash ./develop/run-nginx-gateway.sh
```

Then you can access `http://172.17.0.1` in your browser, it will redirect to the Keycloak login page like
`http://172.17.0.1:8086/auth/realms/master/protocol/openid-connect/auth?scope=xxx&client_id=xxx&state=xxx&nonce=xxx&redirect_uri=http%3A%2F%2F172.17.0.1%2Fredirect_uri&response_type=code`.
When you login successfully, it will go back to `http://172.17.0.1`, and you already login Nexus3 too.

With the Nginx gateway configuration, you can be authenticated with HTTP basic authentication also, check it through:

```bash
# Administration priviledges checking
$ auth_basic=$(echo -n "nexus3:nexus321" | base64); \
  curl -H "Authorization: BASIC $auth_basic" \
       -H "Content-Type: application/json" \
       -d '{"action":"coreui_User","method":"readSources","data":null,"type":"rpc","tid":9}' \
       -v -4 \
       "http://172.17.0.1/service/extdirect"

# User state checking
$ auth_basic=$(echo -n "nexus3:nexus321" | base64); \
  curl -H "Authorization: BASIC $auth_basic" \
       -H "Content-Type: application/json" \
       -v -4 \
       "http://172.17.0.1/service/extdirect/poll/rapture_State_get"
```

**Note**:
- Then Nginx gateway Docker image is built on [flytreeleft/docker-nginx-gateway](https://github.com/flytreeleft/docker-nginx-gateway).
