package org.example.models;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Product {
    private String id;
    private String name;
    private String type;
}
