package com.agilemall.order.events;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data   //@Getter @Setter @RequiredArgsConstructor @ToString @EqualsAndHashCode 포함
@AllArgsConstructor
public class CancelledUpdateOrderEvent {
    private String orderId;
}
