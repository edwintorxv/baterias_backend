package com.riesgopsicosocial.infrastructure.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AlcanceWebConfig implements WebMvcConfigurer {

    private final AlcanceClienteInterceptor alcanceCliente;

    public AlcanceWebConfig(AlcanceClienteInterceptor alcanceCliente) {
        this.alcanceCliente = alcanceCliente;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(alcanceCliente).addPathPatterns(AlcanceClienteInterceptor.RUTAS);
    }

}
