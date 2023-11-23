#docker run --name microservices -p 5435:5432 -e POSTGRES_PASSWORD=s3cr3t -d postgres:12-alpine

# kafka
## topic creation
docker-compose exec kafka kafka-topics.sh --create --topic prod.orders.placed --partitions 1 --replication-factor 1 --bootstrap-server kafka:9092
docker-compose exec kafka kafka-topics.sh --create --topic prod.orders.shipped --partitions 1 --replication-factor 1 --bootstrap-server kafka:9092

## consumer
docker-compose exec kafka kafka-console-consumer.sh --topic prod.orders.shipped --from-beginning --bootstrap-server kafka:9092

## producer
docker-compose exec kafka kafka-console-producer.sh --topic prod.orders.shipped --broker-list kafka:9092
