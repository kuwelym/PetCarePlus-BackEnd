package petitus.petcareplus.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .info(new Info()
                        .title("PetCare+ API Documentation")
                        .version("1.0")
                        .description("API documentation for PetCare+ system")
                        .contact(new Contact()
                                .name("Support Team")
                                .email("support@petcareplus.com")
                                .url("https://petcareplus.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")));
    }




    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("Admin")
                .pathsToMatch("/roles/**", "/notifications/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("User")
                .pathsToMatch("/auth/**", "/users/**", "/pets/**", "/notifications/**")
                .build();
    }

    @Bean
    public GroupedOpenApi providerApi() {
        return GroupedOpenApi.builder()
                .group("Provider")
                .pathsToMatch("/auth/**", "/users/**", "/notifications/**")
                .build();
    }
}
