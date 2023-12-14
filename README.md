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


# apisix - openid plugin
{
"client_id": "order",
"client_secret": "AWMhRNWKJpNxyF7wiJoQlrfcKW3nduVW",
"discovery": "http://keycloak.default.svc.cluster.local/realms/apisix/.well-known/openid-configuration",
"bearer_only": true,
"realm": "apisix",
"redirect_uri": "http://localhost:9080/auth/callback",
"logout_path": "http://localhost:9080/auth/logout"
}

# generate token on keycloak
curl -s -XPOST "http://localhost:8080/realms/apisix/protocol/openid-connect/token" \
-d "grant_type=password&username=order&client_id=order&client_secret=AWMhRNWKJpNxyF7wiJoQlrfcKW3nduVW&password=pwd" | jq

