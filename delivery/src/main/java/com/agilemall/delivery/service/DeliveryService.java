package com.agilemall.delivery.service;

import com.agilemall.common.command.InventoryQtyUpdateCommand;
import com.agilemall.common.dto.DeliveryStatus;
import com.agilemall.common.dto.InventoryQtyAdjustType;
import com.agilemall.common.dto.OrderDetailDTO;
import com.agilemall.common.quries.GetOrderDetailsByOrderIdQuery;
import com.agilemall.delivery.command.DeliveryUpdateCommand;
import com.agilemall.delivery.dto.DeliveryDTO;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DeliveryService {
    @Autowired
    private transient CommandGateway commandGateway;

    @Autowired
    private transient QueryGateway queryGateway;

    public void updateDeliveryStatus(DeliveryDTO deliveryDTO) {
        log.info("[DeliveryService] Executing updateDeliveryStatus for Delivery Id: {}", deliveryDTO.getDeliveryId());

        //-- Devering 상태로 변경 시 Inventory의 재고량을 줄이는 요청을 먼저 수행
        if(DeliveryStatus.DELIVERING.value().equals(deliveryDTO.getDeliveryStatus())) {
            GetOrderDetailsByOrderIdQuery qry = new GetOrderDetailsByOrderIdQuery(deliveryDTO.getOrderId());
            List<OrderDetailDTO> orderDetails = queryGateway.query(qry,
                    ResponseTypes.multipleInstancesOf(OrderDetailDTO.class)).join();
            if(orderDetails == null) return;

            log.info("Get Order details: {}", orderDetails.toString());
            InventoryQtyUpdateCommand cmd;
            List<OrderDetailDTO> successList = new ArrayList<>();
            for(OrderDetailDTO item:orderDetails) {
                cmd = InventoryQtyUpdateCommand.builder()
                        .productId(item.getProductId())
                        .adjustType(InventoryQtyAdjustType.DECREASE.value())
                        .adjustQty(item.getQty())
                        .build();
                commandGateway.send(cmd, (commandMessage, commandResultMessage) -> {
                   if(!commandResultMessage.isExceptional()) {
                       successList.add(item);
                   }
                });
            }

            if(successList.size() < orderDetails.size()) {
                for(OrderDetailDTO item:successList) {
                    cmd = InventoryQtyUpdateCommand.builder()
                            .productId(item.getProductId())
                            .adjustType(InventoryQtyAdjustType.INCREASE.value())
                            .adjustQty(item.getQty())
                            .build();
                    commandGateway.sendAndWait(cmd);
                }
            } else {
                update(deliveryDTO);
            }
        } else {
            update(deliveryDTO);
        }

    }

    private void update(DeliveryDTO deliveryDTO) {
        DeliveryUpdateCommand deliveryUpdateCommand = DeliveryUpdateCommand.builder()
                .deliveryId(deliveryDTO.getDeliveryId())
                .deliveryStatus(deliveryDTO.getDeliveryStatus())
                .build();
        commandGateway.sendAndWait(deliveryUpdateCommand);
    }
}
