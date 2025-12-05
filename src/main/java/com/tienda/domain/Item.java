package com.tienda.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor 
@AllArgsConstructor
public class Item implements Serializable {
    private static final long serialVersionUID = 1L;

    // Referencia a la entidad Producto (ya cargada de la BD)
    private Producto producto;

    // Cantidad deseada por el usuario
    private int cantidad;
    private BigDecimal precioHistorico;

    // Método para calcular el subtotal
    public BigDecimal getSubTotal() {
        return producto.getPrecio().multiply(new BigDecimal(cantidad));
    }
}
