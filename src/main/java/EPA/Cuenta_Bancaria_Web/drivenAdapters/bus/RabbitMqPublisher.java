package EPA.Cuenta_Bancaria_Web.drivenAdapters.bus;

import EPA.Cuenta_Bancaria_Web.RabbitConfig;
import EPA.Cuenta_Bancaria_Web.models.Mongo.M_CuentaMongo;
import EPA.Cuenta_Bancaria_Web.models.Mongo.M_TransaccionMongo;
import EPA.Cuenta_Bancaria_Web.services.Transaccion.Transaccion_ImpMongo;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Sender;

@Component
public class RabbitMqPublisher {

    @Autowired
    private Sender sender;

    @Autowired
    private Gson gson;

    public RabbitMqPublisher() {
    }

    public void publishMessage(Object object){
        sender
                .send(Mono.just(new OutboundMessage(RabbitConfig.EXCHANGE_NAME,
                        RabbitConfig.ROUTING_KEY_NAME, gson.toJson(object).getBytes()))).subscribe();
    }

    public void publishMessageError(Object object){
        sender
                .send(Mono.just(new OutboundMessage(RabbitConfig.EXCHANGE_NAME,
                        RabbitConfig.ROUTING_KEY_ERROR, gson.toJson(object).getBytes()))).subscribe();
    }

    public void publishLogs(Object object){
        sender
                .send(Mono.just(new OutboundMessage(RabbitConfig.EXCHANGE_NAME,
                        RabbitConfig.ROUTING_KEY_LOGS, gson.toJson(object).getBytes()))).subscribe();
    }

}
