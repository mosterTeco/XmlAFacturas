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

            // Extraer atributos de <cfdi:Comprobante>
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

            // Extraer atributos de <cfdi:Emisor>
            Element emisor = (Element) doc.getElementsByTagName("cfdi:Emisor").item(0);
            String rfcEmisor = emisor != null ? emisor.getAttribute("Rfc") : "";
            String nombreEmisor = emisor != null ? emisor.getAttribute("Nombre") : "";
            String regimenFiscal = emisor != null ? emisor.getAttribute("RegimenFiscal") : "";

            // Extraer atributos de <cfdi:Receptor>
            Element receptor = (Element) doc.getElementsByTagName("cfdi:Receptor").item(0);
            String rfcReceptor = receptor != null ? receptor.getAttribute("Rfc") : "";
            String nombreReceptor = receptor != null ? receptor.getAttribute("Nombre") : "";
            String domicilioFiscalReceptor = receptor != null ? receptor.getAttribute("DomicilioFiscalReceptor") : "";
            String regimenFiscalReceptor = receptor != null ? receptor.getAttribute("RegimenFiscalReceptor") : "";
            String usoCFDI = receptor != null ? receptor.getAttribute("UsoCFDI") : "";

            // Extraer datos de <cfdi:TimbreFiscalDigital>
            Element timbre = (Element) doc.getElementsByTagName("cfdi:TimbreFiscalDigital").item(0);
            String versionTimbre = timbre != null ? timbre.getAttribute("Version") : "";
            String uuid = timbre != null ? timbre.getAttribute("UUID") : "";
            String fechaTimbrado = timbre != null ? timbre.getAttribute("FechaTimbrado") : "";
            String noCertificadoSAT = timbre != null ? timbre.getAttribute("NoCertificadoSAT") : "";

            // Extraer datos de <cfdi:Impuestos>
            Element impuestos = (Element) doc.getElementsByTagName("cfdi:Impuestos").item(0);
            String totalImpuestosTrasladados = impuestos != null ? impuestos.getAttribute("TotalImpuestosTrasladados") : "";

            NodeList traslados = doc.getElementsByTagName("cfdi:Traslado");
            String base = "", impuesto = "", tipoFactor = "", tasaOCuota = "", importeImpuesto = "";

            if (traslados.getLength() > 0) {
                Element traslado = (Element) traslados.item(0);
                base = traslado.getAttribute("Base");
                impuesto = traslado.getAttribute("Impuesto");
                tipoFactor = traslado.getAttribute("TipoFactor");
                tasaOCuota = traslado.getAttribute("TasaOCuota");
                importeImpuesto = traslado.getAttribute("Importe");
            }

            // Definir las columnas del CSV
            String[] columnas = {
                "Version", "Serie", "Folio", "Fecha", "FormaPago", "NoCertificado",
                "SubTotal", "Moneda", "TipoCambio", "Total", "TipoDeComprobante",
                "Exportacion", "MetodoPago", "LugarExpedicion",
                "Rfc Emisor", "Nombre Emisor", "RegimenFiscal Emisor",
                "Rfc Receptor", "Nombre Receptor", "DomicilioFiscalReceptor",
                "RegimenFiscalReceptor", "UsoCFDI",
                "ClaveProdServ", "NoIdentificacion", "Cantidad", "ClaveUnidad",
                "Unidad", "Descripcion", "ValorUnitario", "Importe Concepto", "ObjetoImp",
                "Base", "Impuesto", "TipoFactor", "TasaOCuota", "Importe Impuesto",
                "TotalImpuestosTrasladados",
                "Version Timbre", "UUID", "FechaTimbrado", "NoCertificadoSAT"
            };

            // Construcción del CSV
            StringBuilder csvBuilder = new StringBuilder();

            // Encabezados
            for (int i = 0; i < columnas.length; i++) {
                csvBuilder.append(escapeCSV(columnas[i]));
                if (i < columnas.length - 1) {
                    csvBuilder.append(",");
                }
            }
            csvBuilder.append("\n");

            // Procesar conceptos
            NodeList conceptos = doc.getElementsByTagName("cfdi:Concepto");

            for (int i = 0; i < conceptos.getLength(); i++) {
                Element concepto = (Element) conceptos.item(i);
                String claveProdServ = concepto.getAttribute("ClaveProdServ");
                String noIdentificacion = concepto.getAttribute("NoIdentificacion");
                String cantidad = concepto.getAttribute("Cantidad");
                String claveUnidad = concepto.getAttribute("ClaveUnidad");
                String unidad = concepto.getAttribute("Unidad");
                String descripcion = concepto.getAttribute("Descripcion");
                String valorUnitario = concepto.getAttribute("ValorUnitario");
                String importeConcepto = concepto.getAttribute("Importe");
                String objetoImp = concepto.getAttribute("ObjetoImp");

                // Agregar fila al CSV
                csvBuilder
                        .append(escapeCSV(version)).append(",")
                        .append(escapeCSV(serie)).append(",")
                        .append(escapeCSV(folio)).append(",")
                        .append(escapeCSV(fecha)).append(",")
                        .append(escapeCSV(formaPago)).append(",")
                        .append(escapeCSV(noCertificado)).append(",")
                        .append(escapeCSV(subTotal)).append(",")
                        .append(escapeCSV(moneda)).append(",")
                        .append(escapeCSV(tipoCambio)).append(",")
                        .append(escapeCSV(total)).append(",")
                        .append(escapeCSV(tipoDeComprobante)).append(",")
                        .append(escapeCSV(exportacion)).append(",")
                        .append(escapeCSV(metodoPago)).append(",")
                        .append(escapeCSV(lugarExpedicion)).append(",")
                        .append(escapeCSV(rfcEmisor)).append(",")
                        .append(escapeCSV(nombreEmisor)).append(",")
                        .append(escapeCSV(regimenFiscal)).append(",")
                        .append(escapeCSV(rfcReceptor)).append(",")
                        .append(escapeCSV(nombreReceptor)).append(",")
                        .append(escapeCSV(domicilioFiscalReceptor)).append(",")
                        .append(escapeCSV(regimenFiscalReceptor)).append(",")
                        .append(escapeCSV(usoCFDI)).append(",")
                        .append(escapeCSV(claveProdServ)).append(",")
                        .append(escapeCSV(noIdentificacion)).append(",")
                        .append(escapeCSV(cantidad)).append(",")
                        .append(escapeCSV(claveUnidad)).append(",")
                        .append(escapeCSV(unidad)).append(",")
                        .append(escapeCSV(descripcion)).append(",")
                        .append(escapeCSV(valorUnitario)).append(",")
                        .append(escapeCSV(importeConcepto)).append(",")
                        .append(escapeCSV(objetoImp)).append(",")
                        .append("\n");
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
