package org.example.models;


import lombok.*;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Cart {
    private String id;
    private Map<Integer, Integer> cartItems;
    private Integer totalAmount;
}
