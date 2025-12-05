package com.tienda.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Entity
@Table(name = "factura")
@NoArgsConstructor 
@AllArgsConstructor
public class Factura implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_factura")
    private Integer idFactura;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario") // Nombre de la columna FK en la tabla factura
    private Usuario usuario;
    private LocalDateTime fecha;
    private BigDecimal total;    
    @Enumerated(EnumType.STRING) // Mapea el enum de Java al campo ENUM de SQL como un String
    private EstadoFactura estado;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @OneToMany(mappedBy = "factura", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Venta> ventas;
}