package org.example.models;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Order {
    private String id;
    private String name;
    private Integer quantity;
}
