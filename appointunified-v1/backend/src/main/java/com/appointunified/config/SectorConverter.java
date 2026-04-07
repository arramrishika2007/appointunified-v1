package com.appointunified.config;

import com.appointunified.enums.Sector;
import jakarta.persistence.Converter;

@Converter
public class SectorConverter extends PostgresEnumType<Sector> {
    public SectorConverter() {
        super(Sector.class);
    }
}
