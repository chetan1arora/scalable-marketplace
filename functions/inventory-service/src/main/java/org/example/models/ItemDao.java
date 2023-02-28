package org.example.models;


import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface ItemDao {
    @SqlUpdate("update inventory set quantity=:quantity where id=:id")
    int updateQuantity(@BindBean Item item);

    @SqlQuery("select * from inventory where id=:id limit 1")
    @RegisterBeanMapper(Item.class)
    Item findById(@Bind("id") int id);

    @SqlQuery("select * from inventory where type=:type limit 10")
    @RegisterBeanMapper(Item.class)
    List<Item> findAllByType( @Bind("type") String type);

}

