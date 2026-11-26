package bloque2.entrega4;

import java.util.Locale;

/**
 * Representa un paquete logistico gestionado en el muelle de carga de EcoFleet.
 */
public class Paquete {
    private final int id;
    private final String zonaDestino;
    private final double pesoKg;

    public Paquete(int id, String zonaDestino, double pesoKg) {
        this.id = id;
        this.zonaDestino = zonaDestino;
        this.pesoKg = pesoKg;
    }

    public int getId() {
        return id;
    }

    public String getZonaDestino() {
        return zonaDestino;
    }

    public double getPesoKg() {
        return pesoKg;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "Paquete[#%02d | %s | %.1fkg]", id, zonaDestino, pesoKg);
    }
}
