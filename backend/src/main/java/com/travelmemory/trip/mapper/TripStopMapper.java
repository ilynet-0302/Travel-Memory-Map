package com.travelmemory.trip.mapper;

import com.travelmemory.trip.dto.TripStopResponse;
import com.travelmemory.trip.entity.TripStop;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TripStopMapper {

    @Mapping(target = "createdByUserId", source = "createdBy.id")
    TripStopResponse toResponse(TripStop tripStop);
}
