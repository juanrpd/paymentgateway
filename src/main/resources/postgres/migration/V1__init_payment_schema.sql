CREATE TABLE orders (
                        id UUID PRIMARY KEY,
                        customer_id VARCHAR(64) NOT NULL,
                        total_amount NUMERIC(19, 4) NOT NULL,
                        currency VARCHAR(3) NOT NULL,
                        status VARCHAR(32) NOT NULL,
                        version BIGINT NOT NULL DEFAULT 0,
                        created_at TIMESTAMPTZ NOT NULL,
                        updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE payment_attempts (
                                  id UUID PRIMARY KEY,
                                  order_id UUID NOT NULL REFERENCES orders(id),
                                  idempotency_key VARCHAR(128) NOT NULL UNIQUE,
                                  provider_payment_id VARCHAR(128) UNIQUE,
                                  provider_name VARCHAR(32) NOT NULL,
                                  amount NUMERIC(19, 4) NOT NULL,
                                  status VARCHAR(32) NOT NULL,
                                  redirect_url TEXT,
                                  failure_reason VARCHAR(255),
                                  retry_count INT NOT NULL DEFAULT 0,
                                  version BIGINT NOT NULL DEFAULT 0,
                                  created_at TIMESTAMPTZ NOT NULL,
                                  updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_payment_attempts_order_id ON payment_attempts(order_id);

-- Índice parcial para acelerar el Cron de Reconciliación sobre intentos inconclusos
CREATE INDEX idx_payment_attempts_reconciliation
    ON payment_attempts(updated_at)
    WHERE status IN ('UNKNOWN_TIMEOUT', 'PENDING_PROVIDER', 'REDIRECT_REQUIRED');

-- Garantiza que una orden no pueda tener dos intentos aprobados por error
CREATE UNIQUE INDEX uq_one_approved_attempt_per_order
    ON payment_attempts(order_id)
    WHERE status = 'APPROVED';

CREATE TABLE payment_webhook_events (
                                        id UUID PRIMARY KEY,
                                        provider_event_id VARCHAR(128) NOT NULL UNIQUE,
                                        provider_payment_id VARCHAR(128) NOT NULL,
                                        event_type VARCHAR(64) NOT NULL,
                                        raw_payload JSONB NOT NULL,
                                        processing_status VARCHAR(32) NOT NULL,
                                        error_message TEXT,
                                        received_at TIMESTAMPTZ NOT NULL,
                                        processed_at TIMESTAMPTZ
);

CREATE TABLE payment_audit_logs (
                                    id BIGSERIAL PRIMARY KEY,
                                    order_id UUID NOT NULL,
                                    payment_attempt_id UUID,
                                    previous_status VARCHAR(32),
                                    new_status VARCHAR(32) NOT NULL,
                                    actor_source VARCHAR(64) NOT NULL,
                                    trace_id VARCHAR(64),
                                    metadata JSONB,
                                    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_payment_audit_logs_order_id ON payment_audit_logs(order_id);