.PHONY: infra-up infra-down up down logs ps clean

# ── Infrastructure only ─────────────────────────────────────────────────────────
infra-up:
	docker compose -f docker-compose.infra.yml up -d

infra-down:
	docker compose -f docker-compose.infra.yml down

infra-logs:
	docker compose -f docker-compose.infra.yml logs -f

# ── Full stack ──────────────────────────────────────────────────────────────────
up:
	docker compose up -d

down:
	docker compose down

logs:
	docker compose logs -f

ps:
	docker compose ps

# ── Wipe volumes (destructive) ──────────────────────────────────────────────────
clean:
	docker compose down -v --remove-orphans
	docker compose -f docker-compose.infra.yml down -v --remove-orphans

# ── DB shell helpers ─────────────────────────────────────────────────────────────
psql-user:
	docker exec -it ecommerce-postgres psql -U postgres -d user_db

psql-product:
	docker exec -it ecommerce-postgres psql -U postgres -d product_db

psql-order:
	docker exec -it ecommerce-postgres psql -U postgres -d order_db

psql-payment:
	docker exec -it ecommerce-postgres psql -U postgres -d payment_db

psql-notification:
	docker exec -it ecommerce-postgres psql -U postgres -d notification_db

# ── Kafka topic list ─────────────────────────────────────────────────────────────
kafka-topics:
	docker exec ecommerce-kafka kafka-topics --bootstrap-server localhost:9092 --list
