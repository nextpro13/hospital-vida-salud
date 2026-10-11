package pe.cibertec.hospital.notificaciones.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "hospital.eventos";
    public static final String DLX = "hospital.eventos.dlx";
    public static final String COLA = "notificaciones.cola";
    public static final String COLA_DLQ = "notificaciones.cola.dlq";

    @Bean
    public Declarables topologia() {
        TopicExchange eventos = new TopicExchange(EXCHANGE, true, false);
        DirectExchange dlx = new DirectExchange(DLX, true, false);
        Queue cola = QueueBuilder.durable(COLA)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", COLA_DLQ)
                .build();
        Queue dlq = QueueBuilder.durable(COLA_DLQ).build();
        return new Declarables(eventos, dlx, cola, dlq,
                BindingBuilder.bind(cola).to(eventos).with("paciente.registrado"),
            BindingBuilder.bind(cola).to(eventos).with("cita.*"),
            BindingBuilder.bind(cola).to(eventos).with("atencion.registrada"),
            BindingBuilder.bind(cola).to(eventos).with("pago.registrado"),
                BindingBuilder.bind(dlq).to(dlx).with(COLA_DLQ));
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        typeMapper.setTrustedPackages("*");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
