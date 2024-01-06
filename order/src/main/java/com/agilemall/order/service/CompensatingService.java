package com.agilemall.order.service;

import com.agilemall.common.command.CancelDeliveryCommand;
import com.agilemall.common.command.CancelOrderCommand;
import com.agilemall.common.command.CancelPaymentCommand;
import com.agilemall.common.dto.ServiceName;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Slf4j
@Service
public class CompensatingService {
    @Autowired
    private transient CommandGateway commandGateway;

    public void cancelOrderCommand(HashMap<String, String> aggregateIdMap) {
        log.info("[CompensatingService] cancelOrderCommand for Order Id: {}", aggregateIdMap.get(ServiceName.ORDER.value()));

        try {
            CancelOrderCommand cancelOrderCommand = new CancelOrderCommand(aggregateIdMap.get(ServiceName.ORDER.value()));
            commandGateway.sendAndWait(cancelOrderCommand);
        } catch(Exception e) {
            log.error("Error is occurred during <cancelOrderCommand>: {}", e.getMessage());
        }

    }

    public void cancelPaymentCommand(HashMap<String, String> aggregateIdMap) {
        log.info("[CompensatingService] cancelPaymentCommand for Order Id: {}", aggregateIdMap.get(ServiceName.ORDER.value()));

        try {
            //do compensating transaction: Payment
            CancelPaymentCommand cancelPaymentCommand = new CancelPaymentCommand(
                    aggregateIdMap.get(ServiceName.PAYMENT.value()),
                    aggregateIdMap.get(ServiceName.ORDER.value()));
            commandGateway.sendAndWait(cancelPaymentCommand);

        } catch (Exception e) {
            log.error("Error is occurred during <cancelPaymentCommand>: {}", e.getMessage());
        }
    }

    public void cancelDeliveryCommand(HashMap<String, String> aggregateIdMap) {
        log.info("[CompensatingService] cancelDeliveryCommand for Order Id: {}", aggregateIdMap.get(ServiceName.ORDER.value()));
        try {
            //do compensating transaction: Delivery
            CancelDeliveryCommand cancelDeliveryCommand = new CancelDeliveryCommand(
                    aggregateIdMap.get(ServiceName.DELIVERY.value()),
                    aggregateIdMap.get(ServiceName.ORDER.value()));
            commandGateway.sendAndWait(cancelDeliveryCommand);
        } catch(Exception e) {
            log.error("Error is occurred during <cancelDeliveryCommand>: {}", e.getMessage());
        }
    }
/*
    public void cancelInventoryQtyAdjustCommand(String orderId, String inventoryId, List<OrderDetailDTO> orderDetails) {
        List<InventoryQtyAdjustDTO> adjustQtyList = new ArrayList<>();
        InventoryQtyAdjustDTO adjustQty;

        for(OrderDetailDTO detail:orderDetails) {
            adjustQty = InventoryQtyAdjustDTO.builder()
                    .productId(detail.getProductId())
                    .adjustType(InventoryQtyAdjustType.INCREASE.value())
                    .adjustQty(detail.getQty())
                    .build();

            adjustQtyList.add(adjustQty);
        }

        InventoryQtyDecreaseCommand inventoryQtyDecreaseCommand = InventoryQtyDecreaseCommand.builder()
                .inventoryId(inventoryId)
                .orderId(orderId)
                .inventoryQtyAdjustDetails(adjustQtyList)
                .build();

        commandGateway.sendAndWait(inventoryQtyDecreaseCommand);
    }
*/
}
