package org.ezmeal.payment.domain.event.payload;

import com.ezmeal.common.message.DomainEvent;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvent implements DomainEvent, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUID orderId;
    private UUID userId;
    private Integer totalAmount;
}
