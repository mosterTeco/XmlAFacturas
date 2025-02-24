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
                "DomicilioFiscalReceptor", "RegimenFiscalReceptor", "UsoCFDI", "ClaveProdServ", "NoIdentificacion",
                "Cantidad", "ClaveUnidad", "Unidad", "Descripcion", "ValorUnitario", "Importe", "ObjetoImp", "Base",
                "Impuesto", "TipoFactor", "TasaOCuota", "Importe Impuesto", "TotalImpuestosTrasladados", "Version Timbre",
                "UUID", "FechaTimbrado", "NoCertificadoSAT"};

            // Encabezados
            csvBuilder.append(String.join(",", columnas)).append("\n");

            // Recorrer cada concepto y agregarlo como una fila en el CSV
            for (int i = 0; i < conceptosList.getLength(); i++) {
                Element concepto = (Element) conceptosList.item(i);
                String claveProdServ = concepto.getAttribute("ClaveProdServ");
                String noIdentificacion = concepto.getAttribute("NoIdentificacion");
                String cantidad = concepto.getAttribute("Cantidad");
                String claveUnidad = concepto.getAttribute("ClaveUnidad");
                String unidad = concepto.getAttribute("Unidad");
                String descripcion = concepto.getAttribute("Descripcion");
                String valorUnitario = concepto.getAttribute("ValorUnitario");
                String importe = concepto.getAttribute("Importe");
                String objetoImp = concepto.getAttribute("ObjetoImp");

                // Extraer impuestos trasladados (si existen)
                String base = "", impuesto = "", tipoFactor = "", tasaOCuota = "", importeImpuesto = "";
                NodeList impuestosList = concepto.getElementsByTagName("cfdi:Impuestos");
                if (impuestosList.getLength() > 0) {
                    Element impuestos = (Element) impuestosList.item(0);
                    NodeList trasladosList = impuestos.getElementsByTagName("cfdi:Traslado");
                    if (trasladosList.getLength() > 0) {
                        Element traslado = (Element) trasladosList.item(0);
                        base = traslado.getAttribute("Base");
                        impuesto = traslado.getAttribute("Impuesto");
                        tipoFactor = traslado.getAttribute("TipoFactor");
                        tasaOCuota = traslado.getAttribute("TasaOCuota");
                        importeImpuesto = traslado.getAttribute("Importe");
                    }
                }

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
                        .append(claveProdServ).append(",")
                        .append(noIdentificacion).append(",")
                        .append(cantidad).append(",")
                        .append(claveUnidad).append(",")
                        .append(unidad).append(",")
                        .append(descripcion).append(",")
                        .append(valorUnitario).append(",")
                        .append(importe).append(",")
                        .append(objetoImp).append(",")
                        .append(base).append(",")
                        .append(impuesto).append(",")
                        .append(tipoFactor).append(",")
                        .append(tasaOCuota).append(",")
                        .append(importeImpuesto).append(",")
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

    private void procesarMultiplesXMLyExportarCSV(File[] xmlFiles, File directorioSalida) {
        StringBuilder csvBuilder = new StringBuilder();

        // Definir los encabezados del CSV
        String[] columnas = {"Version", "Serie", "Folio", "Fecha", "FormaPago", "NoCertificado", "SubTotal",
            "Moneda", "TipoCambio", "Total", "TipoDeComprobante", "Exportacion", "MetodoPago", "LugarExpedicion",
            "Rfc Emisor", "Nombre Emisor", "RegimenFiscal Emisor", "Rfc Receptor", "Nombre Receptor",
            "DomicilioFiscalReceptor", "RegimenFiscalReceptor", "UsoCFDI", "ClaveProdServ", "NoIdentificacion",
            "Cantidad", "ClaveUnidad", "Unidad", "Descripcion", "ValorUnitario", "Importe", "ObjetoImp", "Base",
            "Impuesto", "TipoFactor", "TasaOCuota", "Importe Impuesto", "TotalImpuestosTrasladados", "Version Timbre",
            "UUID", "FechaTimbrado", "NoCertificadoSAT"};

        // Agregar encabezado
        csvBuilder.append(String.join(",", columnas)).append("\n");

        // Procesar cada archivo XML
        for (File xmlFile : xmlFiles) {
            procesarXML(xmlFile, csvBuilder);
        }

        // Guardar en un solo archivo CSV
        File archivoCSV = new File(directorioSalida, "Facturas_Procesadas.csv");
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivoCSV))) {
            pw.write(csvBuilder.toString());
            System.out.println("Archivo CSV generado correctamente en: " + archivoCSV.getAbsolutePath());
        } catch (IOException e) {
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

            // Extraer TotalImpuestosTrasladados (fuera del ciclo de conceptos)
            NodeList impuestosListComprobante = doc.getElementsByTagName("cfdi:Impuestos");
            String totalImpuestosTrasladados = "";
            if (impuestosListComprobante.getLength() > 0) {
                Element impuestosComprobante = (Element) impuestosListComprobante.item(0);
                totalImpuestosTrasladados = impuestosComprobante.getAttribute("TotalImpuestosTrasladados").trim(); // Aplicar trim()
                System.out.println("TotalImpuestosTrasladados: " + totalImpuestosTrasladados); // Depuración
            }

            // Extraer los conceptos
            NodeList conceptosList = doc.getElementsByTagName("cfdi:Concepto");

            // Recorrer cada concepto y agregarlo como una fila en el CSV
            for (int i = 0; i < conceptosList.getLength(); i++) {
                Element concepto = (Element) conceptosList.item(i);
                String claveProdServ = concepto.getAttribute("ClaveProdServ");
                String noIdentificacion = concepto.getAttribute("NoIdentificacion");
                String cantidad = concepto.getAttribute("Cantidad");
                String claveUnidad = concepto.getAttribute("ClaveUnidad");
                String unidad = concepto.getAttribute("Unidad");
                String descripcion = concepto.getAttribute("Descripcion");
                String valorUnitario = concepto.getAttribute("ValorUnitario");
                String importe = concepto.getAttribute("Importe");
                String objetoImp = concepto.getAttribute("ObjetoImp");

                // Extraer impuestos trasladados (si existen)
                String base = "", impuesto = "", tipoFactor = "", tasaOCuota = "", importeImpuesto = "";
                NodeList impuestosConceptoList = concepto.getElementsByTagName("cfdi:Impuestos");
                if (impuestosConceptoList.getLength() > 0) {
                    Element impuestos = (Element) impuestosConceptoList.item(0);
                    NodeList trasladosList = impuestos.getElementsByTagName("cfdi:Traslado");
                    if (trasladosList.getLength() > 0) {
                        Element traslado = (Element) trasladosList.item(0);
                        base = traslado.getAttribute("Base");
                        impuesto = traslado.getAttribute("Impuesto");
                        tipoFactor = traslado.getAttribute("TipoFactor");
                        tasaOCuota = traslado.getAttribute("TasaOCuota");
                        importeImpuesto = traslado.getAttribute("Importe");
                    }
                }

                // Asignar correctamente los valores a los campos
                csvBuilder.append(String.join(",", version, serie, folio, fecha, formaPago, noCertificado, subTotal, moneda,
                        tipoCambio, total, tipoDeComprobante, exportacion, metodoPago, lugarExpedicion,
                        rfcEmisor, nombreEmisor, regimenFiscal, rfcReceptor, nombreReceptor,
                        domicilioFiscalReceptor, regimenFiscalReceptor, usoCFDI, claveProdServ,
                        noIdentificacion, cantidad, claveUnidad, unidad, descripcion, valorUnitario,
                        importe, objetoImp, base, impuesto, tipoFactor, tasaOCuota, importeImpuesto,
                        totalImpuestosTrasladados, versionTimbre, uuid, fechaTimbrado, noCertificadoSAT)).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
