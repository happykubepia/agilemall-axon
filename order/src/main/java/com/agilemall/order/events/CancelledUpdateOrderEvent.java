package com.agilemall.order.events;

import lombok.Data;

@Data   //@Getter @Setter @RequiredArgsConstructor @ToString @EqualsAndHashCode 포함
public class CancelledUpdateOrderEvent {
    private String orderId;
}
