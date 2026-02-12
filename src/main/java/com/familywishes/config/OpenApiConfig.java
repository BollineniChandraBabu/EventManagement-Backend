package com.familywishes.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI familyWishesOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Family Wishes API")
                        .description("Production-ready backend APIs for family wish automation")
                        .version("v1")
                        .contact(new Contact().name("Family Wishes Support").email("chandrababubollineni416@gmail.com"))
                        .license(new License().name("Proprietary")));
    }
}
