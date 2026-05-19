package org.ezmeal.payment.domain.event;

import org.ezmeal.payment.domain.event.payload.PaymentCancelledEvent;
import org.ezmeal.payment.domain.event.payload.PaymentCompletedEvent;
import org.ezmeal.payment.domain.event.payload.PaymentFailedEvent;

public interface PaymentEventProducer {

    void  publishCompletedEvent(PaymentCompletedEvent event);

    void  publishCancelledEvent(PaymentCancelledEvent event);

    void  publishFailedEvent(PaymentFailedEvent event);

}
