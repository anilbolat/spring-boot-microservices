package com.anilbolat.order.repository;

import com.anilbolat.order.model.Orders;
import org.springframework.data.repository.CrudRepository;

public interface OrderRepository extends CrudRepository<Orders, Long> {

}
