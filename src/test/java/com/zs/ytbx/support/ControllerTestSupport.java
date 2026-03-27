package com.zs.ytbx.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zs.ytbx.config.GlobalExceptionHandler;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

public abstract class ControllerTestSupport {

    protected final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    protected MockMvc buildMockMvc(Object controller) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    protected String toJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
