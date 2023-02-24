package org.example.models;


import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class Order extends Cart {
    private Integer totalAmount;
}
