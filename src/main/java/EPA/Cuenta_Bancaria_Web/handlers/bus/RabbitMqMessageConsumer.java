package EPA.Cuenta_Bancaria_Web.handlers.bus;


import EPA.Cuenta_Bancaria_Web.RabbitConfig;
import EPA.Cuenta_Bancaria_Web.models.DTO.M_Cuenta_DTO;
import EPA.Cuenta_Bancaria_Web.models.DTO.Transaction_Error_DTO;
import EPA.Cuenta_Bancaria_Web.models.Mongo.M_CuentaMongo;
import EPA.Cuenta_Bancaria_Web.models.Mongo.M_TransaccionMongo;
import EPA.Cuenta_Bancaria_Web.services.Transaccion.Transaccion_ImpMongo;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.rabbitmq.Receiver;

@Component
public class RabbitMqMessageConsumer implements CommandLineRunner {

    @Autowired
    private Receiver receiver;

    @Autowired
    private Gson gson;

    @Autowired
    private Transaccion_ImpMongo transaccionImpMongo;

    @Override
    public void run(String... args) throws Exception {

        receiver.consumeAutoAck(RabbitConfig.QUEUE_NAME)
                .map(message -> {
                   M_Cuenta_DTO transaction = gson
                           .fromJson(new String(message.getBody()),
                                   M_Cuenta_DTO.class);

                    System.out.println("La cuenta creada fue:  " + transaction);
                    return transaction;
                }).subscribe();


        receiver.consumeAutoAck(RabbitConfig.QUEUE_NAME_ERROR)
                .map(message -> {
                    Transaction_Error_DTO transactionErrorDto = gson
                            .fromJson(new String(message.getBody()),
                                    Transaction_Error_DTO.class);

                    System.out.println("La transaccion fallo:  " + transactionErrorDto);
                    transaccionImpMongo.rollback(transactionErrorDto).subscribe();

                    return transactionErrorDto;
                }).subscribe();

        receiver.consumeAutoAck(RabbitConfig.QUEUE_CLOUDWATCH)
                .map(message -> {
                    String log = gson
                            .fromJson(new String(message.getBody()),
                                    String.class);

                    System.out.println("LOG DE EJECUCION: " + log);
                    return log;
                }).subscribe();
    }
}
