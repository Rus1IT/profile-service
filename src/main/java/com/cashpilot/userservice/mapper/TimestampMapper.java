package com.cashpilot.userservice.mapper;

import com.google.protobuf.Timestamp;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TimestampMapper {


    public Instant timestampToInstant(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        if (timestamp.getSeconds() == 0 && timestamp.getNanos() == 0) {
            return null;
        }
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    public Timestamp instantToTimestamp(Instant instant) {
        if (instant == null) {
            return Timestamp.getDefaultInstance();
        }
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}