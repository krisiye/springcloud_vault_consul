package example.springboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * A x-forwarded-* header filter for this service.
 * To be used alongside server.forward-headers-strategy=FRAMEWORK
 * x-forwarded-for and x-forwarded-port are included by default. Any others such as
 * x-forwarded-proto, x-forwarded-prefix will have to be included under ambassador mappings
 * @see <a href="https://www.getambassador.io/docs/edge-stack/latest/topics/using/headers/add_request_headers/">add_request_headers</a>
 * 
 * @author iyerk
 *
 */
@Configuration
public class ForwardedHeaderConfig {

    @Bean
    ForwardedHeaderFilter forwardedHeaderFilter() {
       return new ForwardedHeaderFilter();
    }    
}
