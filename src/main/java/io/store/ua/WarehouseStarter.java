package io.store.ua;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class WarehouseStarter {
    public static void main(String[] args) {
        SpringApplication.run(WarehouseStarter.class, args);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}
