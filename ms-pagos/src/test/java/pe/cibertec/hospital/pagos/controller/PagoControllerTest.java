package pe.cibertec.hospital.pagos.controller;

import pe.cibertec.hospital.pagos.dto.PagoRequest;
import pe.cibertec.hospital.pagos.service.PagoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@WebFluxTest(controllers = PagoController.class)
class PagoControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PagoService service;

    @Test
    @DisplayName("POST /api/pagos con monto negativo devuelve 400 sin llegar al servicio")
    void montoNegativoDevuelve400() {
        String json = """
                {
                  "pacienteId": 1,
                  "concepto": "Consulta externa (simulada)",
                  "monto": -10.00,
                  "medioPago": "EFECTIVO",
                  "numeroOperacion": "OP-TEST-0002"
                }
                """;

        webTestClient.post().uri("/api/pagos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.detalles[0]").value(org.hamcrest.Matchers.containsString("monto"));

        verify(service, never()).registrar(any(PagoRequest.class));
    }

    @Test
    @DisplayName("POST /api/pagos con medio de pago inexistente devuelve 400")
    void medioPagoInvalidoDevuelve400() {
        String json = """
                { "pacienteId": 1, "concepto": "X", "monto": 10, "medioPago": "BITCOIN", "numeroOperacion": "OP-TEST-0003" }
                """;

        webTestClient.post().uri("/api/pagos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
