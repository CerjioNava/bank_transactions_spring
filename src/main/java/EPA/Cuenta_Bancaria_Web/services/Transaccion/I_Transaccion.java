package EPA.Cuenta_Bancaria_Web.services.Transaccion;

import EPA.Cuenta_Bancaria_Web.models.DTO.M_Transaccion_DTO;
import EPA.Cuenta_Bancaria_Web.models.DTO.Transaction_Error_DTO;
import EPA.Cuenta_Bancaria_Web.models.Enum_Tipos_Deposito;
import EPA.Cuenta_Bancaria_Web.models.Mongo.M_CuentaMongo;
import EPA.Cuenta_Bancaria_Web.models.Mongo.M_TransaccionMongo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface I_Transaccion {
    Mono<M_Transaccion_DTO> Procesar_Deposito(String id_Cuenta, Enum_Tipos_Deposito tipo, BigDecimal monto);

    Flux<M_Transaccion_DTO> findAll();

    Mono<M_Transaccion_DTO> rollback(Transaction_Error_DTO dto);

}
