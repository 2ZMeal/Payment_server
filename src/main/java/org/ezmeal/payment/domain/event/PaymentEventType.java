package org.ezmeal.payment.domain.event;

public enum PaymentEventType {
    PAYMENT_COMPLETED("payment.completed"),
    PAYMENT_CANCELLED("payment.cancelled"),
    PAYMENT_FAILED("payment.failed");

    private final String eventType;

    PaymentEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventType() {
        return eventType;
    }
}
