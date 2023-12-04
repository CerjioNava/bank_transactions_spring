package EPA.Cuenta_Bancaria_Web.services.Transaccion;

import EPA.Cuenta_Bancaria_Web.drivenAdapters.bus.RabbitMqPublisher;
import EPA.Cuenta_Bancaria_Web.models.DTO.M_Cliente_DTO;
import EPA.Cuenta_Bancaria_Web.models.DTO.M_Cuenta_DTO;
import EPA.Cuenta_Bancaria_Web.models.DTO.M_Transaccion_DTO;
import EPA.Cuenta_Bancaria_Web.models.DTO.Transaction_Error_DTO;
import EPA.Cuenta_Bancaria_Web.models.Enum_Tipos_Deposito;
import EPA.Cuenta_Bancaria_Web.models.Mongo.M_CuentaMongo;
import EPA.Cuenta_Bancaria_Web.models.Mongo.M_TransaccionMongo;
import EPA.Cuenta_Bancaria_Web.drivenAdapters.repositorios.I_RepositorioCuentaMongo;
import EPA.Cuenta_Bancaria_Web.drivenAdapters.repositorios.I_Repositorio_TransaccionMongo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@Qualifier("MONGO")
public class Transaccion_ImpMongo implements I_Transaccion
{

    private final double COSTO_CAJERO = 2.0;

    private final double COSTO_SUCURSAL = 0.0;

    private final double COSTO_OTRO = 1.5;
    @Autowired
    I_Repositorio_TransaccionMongo transaccion_repositorio;

    @Autowired
    I_RepositorioCuentaMongo cuenta_repositorio;

    @Autowired
    private RabbitMqPublisher eventBus;



    @Override
    public Mono<M_Transaccion_DTO> Procesar_Deposito(String id_Cuenta, Enum_Tipos_Deposito tipo, BigDecimal monto) {

        eventBus.publishLogs("Procesando deposito de monto '"+monto+"' a la cuenta con id '"+id_Cuenta+"' desde "+tipo);

        // Busqueda de la cuenta a actualizar
        return cuenta_repositorio.findById(id_Cuenta)
            .flatMap(cuenta -> {
                // Calculo de saldos y actualizacion del objeto de la cuenta existente
                BigDecimal costo = switch (tipo) {
                    case CAJERO -> BigDecimal.valueOf(COSTO_CAJERO);
                    case SUCURSAL -> BigDecimal.valueOf(COSTO_SUCURSAL);
                    case OTRA_CUENTA -> BigDecimal.valueOf(COSTO_OTRO);
                };
                BigDecimal bdSaldoActual = cuenta.getSaldo_Global();
                BigDecimal bdSaldoNuevo = cuenta.getSaldo_Global().add(monto.subtract(costo));

                cuenta.setSaldo_Global(bdSaldoNuevo);

                // Nueva transacción
                M_TransaccionMongo transaccion = new M_TransaccionMongo(
                    cuenta,
                    monto,
                    bdSaldoActual,
                    bdSaldoNuevo,
                    costo,
                    tipo.toString()
                );

                eventBus.publishLogs("Cuenta encontrada y actualizada en monto: "+cuenta);

                return transaccion_repositorio.save(transaccion)
//                        .flatMap(transaccionCreada ->
//                            Mono.error(new RuntimeException("Transaccion fallida"))
//                        )
//                        .onErrorResume(error -> {
//                            System.out.println("El error en la transaccion fue: " + error.getMessage());
//                            eventBus.publishMessageError(transaccion);
//                            return Mono.empty();
//                        })
                        .flatMap(transaccionCreada ->
                                cuenta_repositorio.save(cuenta)
                                        .flatMap(cuentaUpdate ->
                                                Mono.error(new RuntimeException("Cuenta update Fallida. Rollback...")))
                                        .onErrorResume(error -> {
                                            eventBus.publishLogs("El error de transaccion fue: " + error.getMessage());
                                            eventBus.publishMessageError(new Transaction_Error_DTO(cuenta, transaccionCreada));
                                            return Mono.empty();
                                        })
                                        .map(cuentaUpdate -> {
                                                eventBus.publishLogs("Transaccion exitosa: "+transaccion);
                                                System.out.println("Transaccion exitosa");
                                                return new M_Transaccion_DTO(
                                                            transaccionCreada.getId(),
                                                            new M_Cuenta_DTO(transaccionCreada.getCuenta().getId(),
                                                                    new M_Cliente_DTO(transaccionCreada.getCuenta().getCliente().getId(),
                                                                            transaccionCreada.getCuenta().getCliente().getNombre()
                                                                    ),
                                                                    transaccionCreada.getCuenta().getSaldo_Global()
                                                            ),
                                                            transaccionCreada.getMonto_transaccion(),
                                                            transaccionCreada.getSaldo_inicial(),
                                                            transaccionCreada.getSaldo_final(),
                                                            transaccionCreada.getCosto_tansaccion(),
                                                            transaccionCreada.getTipo()
                                                    );
                                                }
                                        )
                        );
            });
    }

    @Override
    public Flux<M_Transaccion_DTO> findAll()
    {
        return transaccion_repositorio.findAll()
                .map(transaccion -> {
                    return new M_Transaccion_DTO(transaccion.getId(),
                            new M_Cuenta_DTO(transaccion.getCuenta().getId(),
                                    new M_Cliente_DTO(transaccion.getCuenta().getCliente().getId(),
                                            transaccion.getCuenta().getCliente().getNombre()
                                    ),
                                    transaccion.getCuenta().getSaldo_Global()
                            ),
                            transaccion.getMonto_transaccion(),
                            transaccion.getSaldo_inicial(),
                            transaccion.getSaldo_final(),
                            transaccion.getCosto_tansaccion(),
                            transaccion.getTipo()
                    );
                });
    }

    @Override
    public Mono<M_Transaccion_DTO> rollback(Transaction_Error_DTO dto) {
        M_CuentaMongo cuenta = dto.getMCuentaMongo();
        M_TransaccionMongo transaccion = dto.getMTransaccionMongo();

        System.out.println("Cuenta y transaccion antes de rollback: ...");
        System.out.println(cuenta);
        System.out.println(transaccion);

        cuenta.setSaldo_Global(transaccion.getSaldo_inicial());

        System.out.println("Cuenta con saldo original: ...");
        System.out.println(cuenta);

        eventBus.publishLogs("Rollback de transaccion | "+dto);

        return
                cuenta_repositorio.save(cuenta)
                        .map(cuentaRollback -> {
                            System.out.println("Cuenta luego de rollback: ...");
                            System.out.println(cuentaRollback);

                            transaccion_repositorio.deleteById(transaccion.getId()).subscribe();

                            eventBus.publishLogs("Rollback ahora si final final tururururururu");
                            return new M_Transaccion_DTO();
                        });
    }
}
