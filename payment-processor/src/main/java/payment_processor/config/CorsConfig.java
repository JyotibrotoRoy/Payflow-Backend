package payment_processor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:5173", //local Vite server
                        "https://your-future-production-domain.vercel.app" // Add prod frontend later
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // OPTIONS is required for the preflight check
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
