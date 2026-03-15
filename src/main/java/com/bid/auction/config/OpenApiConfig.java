package com.bid.auction.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI auctionDeckOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AuctionDeck API")
                        .description("""
                                ## Cricket Auction Management Platform
                                
                                REST API for managing cricket tournaments, teams, players and live auctions.
                                
                                ### Authentication
                                1. Call `POST /api/auth/register` to create an account
                                2. Call `POST /api/auth/login` to get a JWT token
                                3. Click **Authorize** (🔒) at the top and enter: `Bearer <your_token>`
                                4. All protected endpoints will now work
                                
                                ### Public Endpoints (no token needed)
                                - `POST /api/auth/register`
                                - `POST /api/auth/login`
                                - `POST /api/players/register/{tournamentId}`
                                - `GET /api/tournaments/{id}/logo`
                                - `GET /api/teams/{id}/logo`
                                - `GET /api/auction-players/{id}/photo`
                                - `GET /api/players/{id}/photo`
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("AuctionDeck Support")
                                .email("support@auctiondeck.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("https://your-prod-server.com").description("Production")
                ))
                // Apply JWT security globally – can be overridden per operation
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste your JWT token here (without 'Bearer ' prefix)")));
    }
}

