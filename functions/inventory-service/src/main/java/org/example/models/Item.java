package org.example.models;

import lombok.*;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Item implements Serializable {
    @ColumnName("id")
    private int id;
    @ColumnName("name")
    private String name;
    @ColumnName("type")
    private String type;
    @ColumnName("quantity")
    private int quantity;
}
