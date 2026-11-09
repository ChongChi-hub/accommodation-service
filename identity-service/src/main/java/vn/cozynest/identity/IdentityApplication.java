package vn.cozynest.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Clock;
import java.time.ZoneId;

@SpringBootApplication
@EnableAsync
public class IdentityApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityApplication.class, args);
    }

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}
