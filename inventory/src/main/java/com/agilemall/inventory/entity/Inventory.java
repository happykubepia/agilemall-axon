package com.agilemall.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Entity
@Table(name = "inventory")
public class Inventory implements Serializable {
    @Serial
    private static final long serialVersionUID = 2169444340219001818L;

    @Id
    @Column(name = "product_id", nullable = false, length = 10)
    private String productId;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "unit_price", nullable = false)
    private int unitPrice;

    @Column(name = "inventory_qty", nullable = false)
    private int inventoryQty;
}
