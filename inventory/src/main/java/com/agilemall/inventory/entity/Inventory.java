package com.agilemall.inventory.entity;
/*
- 목적: Table과 매핑되는 Entity와 Command Handler인 Aggregate 정의
- 설명
    - Event replay로 최종 상태를 계산하는 일반 Aggregate가 아닌 DB에 최종 상태를 저장하는 State stored Aggregate를 정의
    - Inventory는 제품 원장 데이터이므로 최종상태만 관리하면 되고 DB 손상시엔 백업 복구등으로 복원할 수 있으므로 Event sourcing 패턴 미적용
*/

import com.agilemall.common.command.create.CreateInventoryCommand;
import com.agilemall.common.command.update.UpdateInventoryQtyCommand;
import com.agilemall.common.dto.InventoryQtyAdjustTypeEnum;
import com.agilemall.common.events.create.CreatedInventoryEvent;
import com.agilemall.common.events.update.UpdatedInventoryQtyEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.modelling.command.AggregateMember;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

import java.io.Serial;
import java.io.Serializable;

@Slf4j
@Aggregate
@Data
@Entity
@Table(name = "inventory")
public class Inventory implements Serializable {
    @Serial
    private static final long serialVersionUID = 2169444340219001818L;

    @Id
    @AggregateIdentifier
    @Column(name = "product_id", nullable = false, length = 10)
    private String productId;

    @AggregateMember
    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @AggregateMember
    @Column(name = "unit_price", nullable = false)
    private int unitPrice;

    @AggregateMember
    @Column(name = "inventory_qty", nullable = false)
    private int inventoryQty;

    public Inventory() { }

    @CommandHandler
    private Inventory(CreateInventoryCommand createInventoryCommand) {
        log.info("[@CommandHandler] Executing <CreateInventoryCommand> for Product Id:{}", createInventoryCommand.getProductId());

        //--State Stored Aggregator 는 자신의 상태 업데이트를 CommandHandler 에서 수행
        this.productId = createInventoryCommand.getProductId();
        this.productName = createInventoryCommand.getProductName();
        this.unitPrice = createInventoryCommand.getUnitPrice();
        this.inventoryQty = createInventoryCommand.getInventoryQty();

    }


    //보상 트랜잭션
    @CommandHandler
    private void handle(UpdateInventoryQtyCommand updateInventoryQtyCommand) {
        log.info("[@CommandHandler] Executing <updateInventoryQtyCommand> for productId:{}", updateInventoryQtyCommand.getProductId());

        //--State Stored Aggregator 는 자신의 상태 업데이트를 CommandHandler 에서 수행
        if(InventoryQtyAdjustTypeEnum.INCREASE.value().equals(updateInventoryQtyCommand.getAdjustType())) {
            this.inventoryQty += updateInventoryQtyCommand.getAdjustQty();
        } else if(InventoryQtyAdjustTypeEnum.DECREASE.value().equals(updateInventoryQtyCommand.getAdjustType())) {
            this.inventoryQty -= updateInventoryQtyCommand.getAdjustQty();
            if(this.inventoryQty < 0) this.inventoryQty = 0;
        }

    }
}
