package com.sports.sales.service;

import com.sports.sales.entity.Manufacturer;
import java.util.List;

public interface ManufacturerService {

    List<Manufacturer> list();

    Manufacturer getByCode(String manufacturerCode);

    boolean add(Manufacturer manufacturer);

    boolean update(Manufacturer manufacturer);

    boolean delete(String manufacturerCode);
}
