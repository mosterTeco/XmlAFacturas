/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conversorxmljavafx;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
            File[] xmlFiles = selectedDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
            if (xmlFiles != null && xmlFiles.length > 0) {
                procesarMultiplesXMLyExportarCSV(xmlFiles, selectedDirectory);
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
     * Procesa el XML (CFDI) y genera un CSV con el siguiente formato de
     * columnas:
     *
     * Fecha, Folio, Serie, RFC, Proveedor, Concepto, Subtotal, IVA, ISR
     * RETENIDO, IVA RETENIDO, IEPS
     *
     * IMPORTANTE: Cada concepto del CFDI se muestra en una fila diferente,
     * colocando en la columna "Concepto" solamente la descripción del concepto.
     */
       private void procesarXMLyExportarCSV(File xmlFile) {
    try {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        Element comprobante = (Element) doc.getElementsByTagName("cfdi:Comprobante").item(0);
        if (comprobante == null) {
            System.out.println("No se encontró el nodo cfdi:Comprobante en el XML.");
            return;
        }

        // Extraer atributos generales del comprobante
        String version = comprobante.getAttribute("Version");
        String serie = comprobante.getAttribute("Serie");
        String folio = comprobante.getAttribute("Folio");
        String fecha = comprobante.getAttribute("Fecha");
        String formaPago = comprobante.getAttribute("FormaPago");
        String noCertificado = comprobante.getAttribute("NoCertificado");
        String subTotal = comprobante.getAttribute("SubTotal");
        String moneda = comprobante.getAttribute("Moneda");
        String tipoCambio = comprobante.getAttribute("TipoCambio");
        String total = comprobante.getAttribute("Total");
        String tipoDeComprobante = comprobante.getAttribute("TipoDeComprobante");
        String exportacion = comprobante.getAttribute("Exportacion");
        String metodoPago = comprobante.getAttribute("MetodoPago");
        String lugarExpedicion = comprobante.getAttribute("LugarExpedicion");

        // Extraer datos del emisor
        Element emisor = (Element) doc.getElementsByTagName("cfdi:Emisor").item(0);
        String rfcEmisor = emisor != null ? emisor.getAttribute("Rfc") : "";
        String nombreEmisor = emisor != null ? emisor.getAttribute("Nombre") : "";
        String regimenFiscal = emisor != null ? emisor.getAttribute("RegimenFiscal") : "";

        // Extraer datos del receptor
        Element receptor = (Element) doc.getElementsByTagName("cfdi:Receptor").item(0);
        String rfcReceptor = receptor != null ? receptor.getAttribute("Rfc") : "";
        String nombreReceptor = receptor != null ? receptor.getAttribute("Nombre") : "";
        String domicilioFiscalReceptor = receptor != null ? receptor.getAttribute("DomicilioFiscalReceptor") : "";
        String regimenFiscalReceptor = receptor != null ? receptor.getAttribute("RegimenFiscalReceptor") : "";
        String usoCFDI = receptor != null ? receptor.getAttribute("UsoCFDI") : "";

        // Extraer datos del timbre fiscal
        NodeList timbres = doc.getElementsByTagNameNS("*", "TimbreFiscalDigital");
        Element timbre = (timbres.getLength() > 0) ? (Element) timbres.item(0) : null;
        String versionTimbre = timbre != null ? timbre.getAttribute("Version") : "";
        String uuid = timbre != null ? timbre.getAttribute("UUID") : "";
        String fechaTimbrado = timbre != null ? timbre.getAttribute("FechaTimbrado") : "";
        String noCertificadoSAT = timbre != null ? timbre.getAttribute("NoCertificadoSAT") : "";

        // Extraer los conceptos
        NodeList conceptosList = doc.getElementsByTagName("cfdi:Concepto");

        // Construcción del CSV
        StringBuilder csvBuilder = new StringBuilder();
        String[] columnas = {"Version", "Serie", "Folio", "Fecha", "FormaPago", "NoCertificado", "SubTotal",
                "Moneda", "TipoCambio", "Total", "TipoDeComprobante", "Exportacion", "MetodoPago", "LugarExpedicion",
                "Rfc Emisor", "Nombre Emisor", "RegimenFiscal Emisor", "Rfc Receptor", "Nombre Receptor",
                "DomicilioFiscalReceptor", "RegimenFiscalReceptor", "UsoCFDI", "Descripcion", "Cantidad", "ClaveUnidad",
                "Unidad", "ValorUnitario", "Importe", "TotalImpuestosTrasladados", "Version Timbre", "UUID", "FechaTimbrado",
                "NoCertificadoSAT" };

        // Encabezados
        for (int i = 0; i < columnas.length; i++) {
            csvBuilder.append(columnas[i]);
            if (i < columnas.length - 1) {
                csvBuilder.append(",");
            }
        }
        csvBuilder.append("\n");

        // Recorrer cada concepto y agregarlo como una fila en el CSV
        for (int i = 0; i < conceptosList.getLength(); i++) {
            Element concepto = (Element) conceptosList.item(i);
            String descripcion = concepto.getAttribute("Descripcion");
            String cantidad = concepto.getAttribute("Cantidad");
            String claveUnidad = concepto.getAttribute("ClaveUnidad");
            String unidad = concepto.getAttribute("Unidad");
            String valorUnitario = concepto.getAttribute("ValorUnitario");
            String importe = concepto.getAttribute("Importe");

            csvBuilder.append(version).append(",")
                    .append(serie).append(",")
                    .append(folio).append(",")
                    .append(fecha).append(",")
                    .append(formaPago).append(",")
                    .append(noCertificado).append(",")
                    .append(subTotal).append(",")
                    .append(moneda).append(",")
                    .append(tipoCambio).append(",")
                    .append(total).append(",")
                    .append(tipoDeComprobante).append(",")
                    .append(exportacion).append(",")
                    .append(metodoPago).append(",")
                    .append(lugarExpedicion).append(",")
                    .append(rfcEmisor).append(",")
                    .append(nombreEmisor).append(",")
                    .append(regimenFiscal).append(",")
                    .append(rfcReceptor).append(",")
                    .append(nombreReceptor).append(",")
                    .append(domicilioFiscalReceptor).append(",")
                    .append(regimenFiscalReceptor).append(",")
                    .append(usoCFDI).append(",")
                    .append(descripcion).append(",")
                    .append(cantidad).append(",")
                    .append(claveUnidad).append(",")
                    .append(unidad).append(",")
                    .append(valorUnitario).append(",")
                    .append(importe).append(",")
                    .append(total).append(",")
                    .append(versionTimbre).append(",")
                    .append(uuid).append(",")
                    .append(fechaTimbrado).append(",")
                    .append(noCertificadoSAT).append("\n");
        }

        // Guardar CSV
        try (PrintWriter pw = new PrintWriter(new FileWriter(new File(xmlFile.getParent(), "Factura_Procesada.csv")))) {
            pw.write(csvBuilder.toString());
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}

    /**
     * Escapa caracteres especiales para que la cadena sea válida en CSV: - Si
     * contiene comas, comillas o saltos de línea, se encierra entre comillas
     * dobles. - Se duplican las comillas internas.
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
     * Convierte la fecha de "yyyy-MM-ddTHH:mm:ss" a "dd/MM/yyyy". Si no puede
     * parsear, devuelve la cadena original.
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

    private void procesarMultiplesXMLyExportarCSV(File[] xmlFiles, File carpeta) {
        StringBuilder csvBuilder = new StringBuilder();
        String[] columnas = {"Fecha", "Folio", "Serie", "RFC", "Proveedor", "Concepto", "Subtotal", "IVA", "ISR RETENIDO", "IVA RETENIDO", "IEPS"};

        for (int i = 0; i < columnas.length; i++) {
            csvBuilder.append(escapeCSV(columnas[i]));
            if (i < columnas.length - 1) {
                csvBuilder.append(",");
            }
        }
        csvBuilder.append("\n");

        for (File xmlFile : xmlFiles) {
            procesarXML(xmlFile, csvBuilder);
        }

        File csvFile = new File(carpeta, "Facturas_Procesadas.csv");
        try (PrintWriter pw = new PrintWriter(new FileWriter(csvFile))) {
            pw.write(csvBuilder.toString());
            System.out.println("Archivo CSV creado: " + csvFile.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("Error al escribir el archivo CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void procesarXML(File xmlFile, StringBuilder csvBuilder) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            Element comprobante = (Element) doc.getElementsByTagName("cfdi:Comprobante").item(0);
            if (comprobante == null) {
                return;
            }

            String fechaStr = comprobante.getAttribute("Fecha");
            String folio = comprobante.getAttribute("Folio");
            String serie = comprobante.getAttribute("Serie");
            String subTotal = comprobante.getAttribute("SubTotal");

            Element emisor = (Element) doc.getElementsByTagName("cfdi:Emisor").item(0);
            String rfcEmisor = emisor != null ? emisor.getAttribute("Rfc") : "";
            String nombreEmisor = emisor != null ? emisor.getAttribute("Nombre") : "";

            String ivaGlobal = "0";
            Element impuestosElement = (Element) doc.getElementsByTagName("cfdi:Impuestos").item(0);
            if (impuestosElement != null) {
                String totalImpTras = impuestosElement.getAttribute("TotalImpuestosTrasladados");
                if (totalImpTras != null && !totalImpTras.isEmpty()) {
                    ivaGlobal = totalImpTras;
                }
            }

            String isrRetenido = "0", ivaRetenido = "0", ieps = "0";
            NodeList conceptos = doc.getElementsByTagName("cfdi:Concepto");
            String fechaFormateada = formatearFecha(fechaStr);

            for (int i = 0; i < conceptos.getLength(); i++) {
                Element concepto = (Element) conceptos.item(i);
                String descripcion = concepto.getAttribute("Descripcion");

                csvBuilder.append(escapeCSV(fechaFormateada)).append(",")
                        .append(escapeCSV(folio)).append(",")
                        .append(escapeCSV(serie)).append(",")
                        .append(escapeCSV(rfcEmisor)).append(",")
                        .append(escapeCSV(nombreEmisor)).append(",")
                        .append(escapeCSV(descripcion)).append(",")
                        .append(escapeCSV(subTotal)).append(",")
                        .append(escapeCSV(ivaGlobal)).append(",")
                        .append(escapeCSV(isrRetenido)).append(",")
                        .append(escapeCSV(ivaRetenido)).append(",")
                        .append(escapeCSV(ieps)).append("\n");
            }
        } catch (Exception e) {
            System.out.println("Error al procesar XML " + xmlFile.getName() + ": " + e.getMessage());
        }
    }

}
