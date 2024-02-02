# database postgres
#docker run --name microservices -p 5435:5432 -e POSTGRES_PASSWORD=s3cr3t -d postgres:12-alpine

# kafka
## topic creation
docker-compose exec kafka kafka-topics.sh --create --topic prod.orders.placed --partitions 1 --replication-factor 1 --bootstrap-server kafka:9092
docker-compose exec kafka kafka-topics.sh --create --topic prod.orders.shipped --partitions 1 --replication-factor 1 --bootstrap-server kafka:9092

## consumer
docker-compose exec kafka kafka-console-consumer.sh --topic prod.orders.shipped --from-beginning --bootstrap-server kafka:9092

## producer
docker-compose exec kafka kafka-console-producer.sh --topic prod.orders.shipped --broker-list kafka:9092


# argocd
helm repo add argo https://argoproj.github.io/argo-helm
helm install argocd argo/argo-cd -n argocd --create-namespace

## to get pwd
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d

kubectl port-forward service/argocd-server -n argocd 8090:443

kubectl apply -f argocd/manifests/repositories.yaml
kubectl apply -f argocd/manifests/projects.yaml
kubectl apply -f argocd/manifests/applications.yaml


# install vault
helm repo add hashicorp https://helm.releases.hashicorp.com
helm install vault hashicorp/vault -n vault --create-namespace


# install external-secrets
helm repo add external-secrets https://charts.external-secrets.io
helm install external-secrets external-secrets/external-secrets --version "${HELM_CHART_VERSION}" --namespace=external-secrets --create-namespace --set installCRDs=true

# ClusterSecretStore
kubectl apply -f vault/secret-store.yaml

# vault port forward to 8200:8200


# configuration of vault, secret store and external secret
namespace vault for all now.
kubectl apply -f vault/vault-secret.yaml
kubectl apply -f vault/secret-store.yaml
kubectl apply -f vault/external-secret.yaml


# apisix - api gateway
helm install apisix apisix/apisix --create-namespace  --namespace apisix
helm install apisix-dashboard apisix/apisix-dashboard --create-namespace --namespace apisix

## add route /order and info below is for openid plugin
{
"client_id": "order",
"client_secret": "AWMhRNWKJpNxyF7wiJoQlrfcKW3nduVW",
"discovery": "http://keycloak.default.svc.cluster.local/realms/apisix/.well-known/openid-configuration",
"bearer_only": true,
"realm": "apisix",
"redirect_uri": "http://localhost:9080/auth/callback",
"logout_path": "http://localhost:9080/auth/logout"
}

## upstream
order.argocd.svc.cluster.local:8080

# keycloak - identity management
helm install keycloak oci://registry-1.docker.io/bitnamicharts/keycloak
## create a user named order
## create a client named order
## generate token on keycloak
curl -s -XPOST "http://localhost:8080/realms/apisix/protocol/openid-connect/token" \
-d "grant_type=password&username=order&client_id=order&client_secret=AWMhRNWKJpNxyF7wiJoQlrfcKW3nduVW&password=pwd" | jq


# test
port forward apisix 9080:9080
curl -v -XGET -H "Authorization: Bearer PUT TOKEN HERE" http://localhost:9080/test | jq


# instrumentation & tracing
Micrometer (lib to capture metrics) -> Prometheus (db)

## dependencies
spring-boot-starter-actuator -> exposes metrics API to be collected by Prometheus (scraping).
micrometer-registry-prometheus -> allows metrics to be exposed in a format that will be understood by Prometheus.
spring-boot-starter-aop -> Micrometer times methods need and depends on AspectJ. For using @Observed on class and enabled @ObservedAspect.
micrometer-tracing-bridge-otel -> For Tracing Context Propagation with Micrometer Tracing
opentelemetry-exporter-zipkin -> For Latency Visualization to send data for traces and spans.
loki-logback-appender:1.3.2  -> Sends logs to Loki


## config
management.endpoints.web.exposure.include=health,info,prometheus -> in application.properties.  (/actuator/prometheus)
management.tracing.sampling.probability=1.0
management.metrics.distribution.percentiles-histogram.http.server.requests=true
logging.pattern.level="%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"


Prometheus is a time-series database. Polls your application for its latest metrics data – this is known as scraping.

Micrometer is a set of libraries for Java that allow you to capture metrics and expose them to several different tools – including Prometheus.
Micrometer acts as a facade – an intermediate layer – between your application and some of the more popular monitoring tools. 
This makes it easier to publish metrics to Prometheus and other tools like Elastic, Datadog or Dynatrace.

promql

Grafana uses Prometheus as a data source to visualize the data on dashboards.


# feign client
https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/
