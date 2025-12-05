package com.tienda.controller;

import com.tienda.domain.Item;
import com.tienda.domain.Factura;
import com.tienda.domain.Usuario;
import com.tienda.service.CarritoService;
import com.tienda.service.FacturaService;
import com.tienda.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class CarritoController {

    private final CarritoService carritoService;
    private final UsuarioService usuarioService;
    private final FacturaService facturaService;

    public CarritoController(CarritoService carritoService, UsuarioService usuarioService, FacturaService facturaService) {
        this.carritoService = carritoService;
        this.usuarioService = usuarioService;
        this.facturaService = facturaService;
    }

    // --- 1. MOSTRAR EL CARRITO ---
    @GetMapping("/carrito/listado")
    public String listado(HttpSession session, Model model) {
        List<Item> carrito = carritoService.obtenerCarrito(session);

        model.addAttribute("carritoItems", carrito);
        model.addAttribute("totalCarrito", carritoService.calcularTotal(carrito));

        return "/carrito/listado";
    }

    // --- 2. AGREGAR PRODUCTO AL CARRITO ---
    @PostMapping("/carrito/agregar")
    public ModelAndView agregar(
            @RequestParam("idProducto") Integer idProducto,
            HttpSession session,
            Model model) {
        try {
            
            System.out.println("Entro al carrito");
            // 1. Obtener el carrito de la sesión
            List<Item> carrito = carritoService.obtenerCarrito(session);

            // 2. Ejecutar la lógica de negocio (el Service asume cantidad = 1)
            carritoService.agregarProducto(carrito, idProducto);

            // 3. Guardar el carrito actualizado en la sesión
            carritoService.guardarCarrito(session, carrito);

            // 4. Recalcular y actualizar el Model con los datos necesarios
            model.addAttribute("carritoTotal", carritoService.calcularTotal(carrito));
            model.addAttribute("listaItems", carrito);

            // 5. Retornar el fragmento HTML
            return new ModelAndView("/carrito/fragmentos :: verCarrito", model.asMap());

        } catch (RuntimeException e) {
            // 6. Manejo de errores (p. ej., stock insuficiente, producto no existe)
            model.addAttribute("errorMensaje", e.getMessage());

            // Retorna un fragmento de error genérico que muestre el mensaje
            return new ModelAndView("/errores/fragmentos :: errorMensaje", model.asMap());
        }
    }

    // --- 3. ELIMINAR ITEM DEL CARRITO ---
    @PostMapping("/carrito/eliminar/{idProducto}")
    public String eliminarItem(
            @PathVariable("idProducto") Integer idProducto,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        List<Item> carrito = carritoService.obtenerCarrito(session);
        carritoService.eliminarItem(carrito, idProducto);
        carritoService.guardarCarrito(session, carrito);

        redirectAttributes.addFlashAttribute("mensaje", "Producto eliminado del carrito.");
        return "redirect:/carrito/listado";
    }
    
    @GetMapping("/carrito/modificar/{idProducto}")
    public String modificar(
            @PathVariable("idProducto") Integer idProducto,
            HttpSession session,
            Model model) {
        
        // 1. Obtener la lista del carrito de la sesión
        List<Item> carrito = carritoService.obtenerCarrito(session);
        
        // 2. Buscar el ítem en la lista del carrito
        Item item = carritoService.buscarItem(carrito, idProducto);
        
        if (item == null) {
            // Manejar el caso de que el ítem no esté en el carrito
            System.out.println("Hubo problemas");
            return "redirect:/carrito/listado"; 
        }
        
        // 3. Pasar el ítem encontrado (con su cantidad actual) al modelo
        model.addAttribute("item", item);
        
        // 4. Retornar la vista
        return "/carrito/modifica";
    }

    // --- 4. ACTUALIZAR CANTIDAD DESDE LA VISTA ---
    @PostMapping("/carrito/actualizar")
    public String actualizarCantidad(
            @RequestParam("producto.idProducto") Integer idProducto,
            @RequestParam("cantidad") int nuevaCantidad,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            List<Item> carrito = carritoService.obtenerCarrito(session);
            carritoService.actualizarCantidad(carrito, idProducto, nuevaCantidad);
            carritoService.guardarCarrito(session, carrito);

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/carrito/listado";
    }

    // --- 5. PROCESAR COMPRA (CHECKOUT) ---
    @GetMapping("/facturar/carrito")
    public String facturarCarrito(HttpSession session, RedirectAttributes redirectAttributes) {
        System.out.println("Va a facturar");
        
        try {
            List<Item> carrito = carritoService.obtenerCarrito(session);

            // Obtención del usuario autenticado*
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            System.out.println("El username es:"+username);
            Usuario usuario = usuarioService.getUsuarioPorUsername(username).get();

            // 1. La lógica transaccional ocurre en el servicio
            Factura factura = carritoService.procesarCompra(carrito, usuario);

            // 2. Limpiar el carrito de la sesión después de una compra exitosa
            carritoService.limpiarCarrito(session);

            // 3. Pasar el ID de la factura como Flash Attribute
            redirectAttributes.addFlashAttribute("idFactura", factura.getIdFactura());
            redirectAttributes.addFlashAttribute("mensaje", "Compra procesada con éxito. Factura Nro: " + factura.getIdFactura());
            
            // 4. Redirigir a una ruta nueva para ver la factura
            System.out.println("Ver la Factura");
            return "redirect:/carrito/verFactura";

        } catch (RuntimeException e) {
            // Captura errores de stock, carrito vacío o de la transacción
            redirectAttributes.addFlashAttribute("error", "Error al procesar la compra: " + e.getMessage());
            return "redirect:/carrito/listado";
        }
    }
    
    // Nuevo método para mostrar la factura
    @GetMapping("/carrito/verFactura")
    public String verFactura(@ModelAttribute("idFactura") Integer idFactura, Model model) {
        if (idFactura == null) {
            // Si no se pasó el ID por flash, redirigir a donde lista de facturas o index
            return "redirect:/index"; 
        }
        
        // 1. Obtener la factura COMPLETA (incluyendo ventas)        
        Factura factura = facturaService.getFacturaConVentas(idFactura); 
        
        model.addAttribute("factura", factura);
        return "/carrito/verFactura"; // Nombre del archivo Thymeleaf
    }
}
