package org.example.models;


import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InventoryRequest {
    private int id;
    private String type;
    private int quantity;
}
