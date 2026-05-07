package com.min9359.creator_settlement_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("크리에이터 정산 API")
                        .version("1.0.0")
                        .description("강의 판매 / 환불 / 정산 계산을 처리하는 백엔드 API"));
    }

}
