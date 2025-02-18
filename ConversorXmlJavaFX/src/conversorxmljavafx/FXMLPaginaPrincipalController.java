/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conversorxmljavafx;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Controlador para la pantalla principal de la aplicación JavaFX.
 */
public class FXMLPaginaPrincipalController implements Initializable {

    // Formato de salida para la fecha (dd/MM/yyyy)
    private static final SimpleDateFormat DATE_OUTPUT_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Inicializaciones si son necesarias.
    }

    @FXML
    private void onClickCargarCarpeta(ActionEvent event) {
        Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Seleccionar Carpeta");
        File selectedDirectory = directoryChooser.showDialog(stage);

        if (selectedDirectory != null) {
            System.out.println("Carpeta seleccionada: " + selectedDirectory.getAbsolutePath());

            File[] xmlFiles = selectedDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
            if (xmlFiles != null && xmlFiles.length > 0) {
                System.out.println("Archivos XML encontrados:");
                for (File file : xmlFiles) {
                    System.out.println("- " + file.getName());
                }
            } else {
                System.out.println("No se encontraron archivos .xml en la carpeta seleccionada.");
            }
        }
    }

    @FXML
    private void onClickCargarArchivo(ActionEvent event) {
        Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Archivo XML");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos XML", "*.xml"));

        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            System.out.println("Archivo seleccionado: " + selectedFile.getAbsolutePath());
            procesarXMLyExportarCSV(selectedFile);
        }
    }

    /**
     * Procesa el XML (CFDI) y genera un CSV con el siguiente formato de columnas:
     *
     * Fecha, Folio, Serie, RFC, Proveedor, Concepto, Subtotal, IVA,
     * ISR RETENIDO, IVA RETENIDO, IEPS
     *
     * IMPORTANTE: Cada concepto del CFDI se muestra en una fila diferente,
     * colocando en la columna "Concepto" solamente la descripción del concepto.
     */
    private void procesarXMLyExportarCSV(File xmlFile) {
        try {
            // 1. Crear parseador XML con soporte para namespaces
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // 2. Obtener el nodo cfdi:Comprobante (raíz)
            Element comprobante = (Element) doc.getElementsByTagName("cfdi:Comprobante").item(0);
            if (comprobante == null) {
                System.out.println("No se encontró el nodo cfdi:Comprobante en el XML.");
                return;
            }

            // Atributos principales del comprobante
            String fechaStr = comprobante.getAttribute("Fecha");  // Ej: "2024-10-02T13:41:14"
            String folio = comprobante.getAttribute("Folio");
            String serie = comprobante.getAttribute("Serie");
            String subTotal = comprobante.getAttribute("SubTotal"); // Ej: "194.02"

            // 3. Emisor = proveedor (RFC, Nombre)
            Element emisor = (Element) doc.getElementsByTagName("cfdi:Emisor").item(0);
            String rfcEmisor = "";
            String nombreEmisor = "";
            if (emisor != null) {
                rfcEmisor = emisor.getAttribute("Rfc");
                nombreEmisor = emisor.getAttribute("Nombre");
            }

            // 4. IVA a nivel global (TotalImpuestosTrasladados) -> Ej: "31.05"
            //    Si no existe, lo dejamos en "0"
            String ivaGlobal = "0";
            Element impuestosElement = (Element) doc.getElementsByTagName("cfdi:Impuestos").item(0);
            if (impuestosElement != null) {
                String totalImpTras = impuestosElement.getAttribute("TotalImpuestosTrasladados");
                if (totalImpTras != null && !totalImpTras.isEmpty()) {
                    ivaGlobal = totalImpTras;
                }
            }

            // 5. ISR RETENIDO, IVA RETENIDO, IEPS
            //    (en tu ejemplo no aparecen, así que los dejamos en "0")
            String isrRetenido = "0";
            String ivaRetenido = "0";
            String ieps = "0";

            // 6. Obtener la lista de cfdi:Concepto
            NodeList conceptos = doc.getElementsByTagName("cfdi:Concepto");

            // 7. Dar formato a la fecha (dd/MM/yyyy)
            String fechaFormateada = formatearFecha(fechaStr);

            // 8. Definir las columnas en el orden solicitado
            String[] columnas = {
                "Fecha", "Folio", "Serie", "RFC", "Proveedor",
                "Concepto", "Subtotal", "IVA",
                "ISR RETENIDO", "IVA RETENIDO", "IEPS"
            };

            // 9. Construir el contenido del CSV en memoria
            StringBuilder csvBuilder = new StringBuilder();

            // 9.1 Fila de cabecera (solo una vez)
            for (int i = 0; i < columnas.length; i++) {
                csvBuilder.append(escapeCSV(columnas[i]));
                if (i < columnas.length - 1) {
                    csvBuilder.append(",");
                }
            }
            csvBuilder.append("\n");

            // 9.2 Por cada concepto, crear una fila nueva
            for (int i = 0; i < conceptos.getLength(); i++) {
                Element concepto = (Element) conceptos.item(i);
                // Solo queremos la descripción en la columna "Concepto"
                String descripcion = concepto.getAttribute("Descripcion");

                // Construir la fila (misma información en columnas fijas)
                // Orden: Fecha, Folio, Serie, RFC, Proveedor, Concepto, Subtotal, IVA, ISR, IVA RET, IEPS
                csvBuilder
                    .append(escapeCSV(fechaFormateada)).append(",") // Fecha
                    .append(escapeCSV(folio)).append(",")           // Folio
                    .append(escapeCSV(serie)).append(",")           // Serie
                    .append(escapeCSV(rfcEmisor)).append(",")       // RFC
                    .append(escapeCSV(nombreEmisor)).append(",")    // Proveedor
                    .append(escapeCSV(descripcion)).append(",")     // Concepto (solo descripción)
                    .append(escapeCSV(subTotal)).append(",")        // Subtotal
                    .append(escapeCSV(ivaGlobal)).append(",")       // IVA
                    .append(escapeCSV(isrRetenido)).append(",")     // ISR RETENIDO
                    .append(escapeCSV(ivaRetenido)).append(",")     // IVA RETENIDO
                    .append(escapeCSV(ieps))                        // IEPS
                    .append("\n");
            }

            // 10. Guardar el archivo CSV en la misma carpeta que el XML
            File csvFile = new File(xmlFile.getParent(), "Factura_Procesada.csv");
            try (PrintWriter pw = new PrintWriter(new FileWriter(csvFile))) {
                pw.write(csvBuilder.toString());
            }

            System.out.println("Archivo CSV creado: " + csvFile.getAbsolutePath());

        } catch (Exception e) {
            System.out.println("Error al procesar el XML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Escapa caracteres especiales para que la cadena sea válida en CSV:
     * - Si contiene comas, comillas o saltos de línea, se encierra entre comillas dobles.
     * - Se duplican las comillas internas.
     */
    private String escapeCSV(String input) {
        if (input == null) {
            return "";
        }
        boolean contieneCaracterEspecial = input.contains(",") || input.contains("\"") || input.contains("\n");
        String resultado = input.replace("\"", "\"\"");
        if (contieneCaracterEspecial) {
            resultado = "\"" + resultado + "\"";
        }
        return resultado;
    }

    /**
     * Convierte la fecha de "yyyy-MM-ddTHH:mm:ss" a "dd/MM/yyyy".
     * Si no puede parsear, devuelve la cadena original.
     */
    private String formatearFecha(String fechaXml) {
        try {
            SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            Date date = sdfIn.parse(fechaXml);
            return DATE_OUTPUT_FORMAT.format(date);
        } catch (Exception ex) {
            return fechaXml; // Si falla, regresamos la cadena tal cual.
        }
    }
}
