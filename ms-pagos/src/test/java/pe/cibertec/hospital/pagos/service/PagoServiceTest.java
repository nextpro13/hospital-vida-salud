package pe.cibertec.hospital.pagos.service;

import pe.cibertec.hospital.pagos.client.PacienteClient;
import pe.cibertec.hospital.pagos.dto.PagoRequest;
import pe.cibertec.hospital.pagos.event.EventoDominio;
import pe.cibertec.hospital.pagos.event.EventoPublisher;
import pe.cibertec.hospital.pagos.exception.RecursoDuplicadoException;
import pe.cibertec.hospital.pagos.exception.ReglaNegocioException;
import pe.cibertec.hospital.pagos.model.EstadoPago;
import pe.cibertec.hospital.pagos.model.MedioPago;
import pe.cibertec.hospital.pagos.model.Pago;
import pe.cibertec.hospital.pagos.repository.PagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {
    @Mock
    private PagoRepository repository;

    @Mock
    private PacienteClient pacienteClient;

    @Mock
    private EventoPublisher publisher;

    private PagoService service;

    @BeforeEach
    void setUp() {
        service = new PagoService(repository, pacienteClient, publisher, Clock.systemUTC());
    }

    private PagoRequest solicitud(String monto) {
        return new PagoRequest(1L, 3L, "Consulta externa (simulada)", new BigDecimal(monto),
                MedioPago.TARJETA_DEBITO, "OP-TEST-0001");
    }

    @Test
    @DisplayName("Registra un pago válido en estado REGISTRADO")
    void registraPagoValido() {
        when(repository.existsByNumeroOperacion("OP-TEST-0001")).thenReturn(Mono.just(false));
        when(pacienteClient.verificarExistencia(1L)).thenReturn(Mono.empty());
        when(repository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            p.setId(1L);
            return Mono.just(p);
        });
        when(publisher.publicar(eq("pago.registrado"), any(EventoDominio.class))).thenReturn(Mono.empty());

        StepVerifier.create(service.registrar(solicitud("80.00")))
                .assertNext(p -> {
                    assertThat(p.getEstado()).isEqualTo(EstadoPago.REGISTRADO);
                    assertThat(p.getMonto()).isEqualByComparingTo("80.00");
                    assertThat(p.getFechaPago()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Rechaza un pago con monto cero")
    void rechazaMontoCero() {
        StepVerifier.create(service.registrar(solicitud("0")))
                .expectError(ReglaNegocioException.class)
                .verify();

        verify(repository, never()).save(any(Pago.class));
    }

    @Test
    @DisplayName("Rechaza un pago con monto negativo")
    void rechazaMontoNegativo() {
        StepVerifier.create(service.registrar(solicitud("-50.00")))
                .expectError(ReglaNegocioException.class)
                .verify();
    }

    @Test
    @DisplayName("Rechaza montos con más de dos decimales")
    void rechazaMasDeDosDecimales() {
        assertThatThrownBy(() -> PagoService.validarMonto(new BigDecimal("10.005")))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("Rechaza un número de operación duplicado")
    void rechazaOperacionDuplicada() {
        when(repository.existsByNumeroOperacion("OP-TEST-0001")).thenReturn(Mono.just(true));

        StepVerifier.create(service.registrar(solicitud("80.00")))
                .expectError(RecursoDuplicadoException.class)
                .verify();

        verify(repository, never()).save(any(Pago.class));
    }
}
