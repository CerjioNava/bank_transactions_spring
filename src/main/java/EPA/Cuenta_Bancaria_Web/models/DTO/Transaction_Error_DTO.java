package EPA.Cuenta_Bancaria_Web.models.DTO;

import EPA.Cuenta_Bancaria_Web.models.Mongo.M_CuentaMongo;
import EPA.Cuenta_Bancaria_Web.models.Mongo.M_TransaccionMongo;
import lombok.Data;

@Data
public class Transaction_Error_DTO {


    private M_CuentaMongo mCuentaMongo;
    private M_TransaccionMongo mTransaccionMongo;

    public Transaction_Error_DTO(M_CuentaMongo mCuentaMongo, M_TransaccionMongo mTransaccionMongo) {
        this.mCuentaMongo = mCuentaMongo;
        this.mTransaccionMongo = mTransaccionMongo;
    }
}
