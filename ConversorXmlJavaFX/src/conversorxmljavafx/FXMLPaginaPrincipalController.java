/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package conversorxmljavafx;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ResourceBundle;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.stage.DirectoryChooser;
import org.apache.commons.io.IOUtils;

/**
 * FXML Controller class
 *
 * @author reyes
 */
public class FXMLPaginaPrincipalController implements Initializable {

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
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
            procesarXMLyExportarExcel(selectedFile);
        }
    }

    private void procesarXMLyExportarExcel(File xmlFile) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList conceptos = doc.getElementsByTagName("cfdi:Concepto");

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Facturas");

            String[] columnas = { "ClaveProdServ", "NoIdentificacion", "Cantidad", "ClaveUnidad", "Unidad",
                                  "Descripcion", "ValorUnitario", "Importe", "ObjetoImp", "IVA" };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnas[i]);
            }
            
            for (int i = 0; i < conceptos.getLength(); i++) {
                Node conceptoNode = conceptos.item(i);
                if (conceptoNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element concepto = (Element) conceptoNode;
                    
                    Row row = sheet.createRow(i + 1); 

                    row.createCell(0).setCellValue(concepto.getAttribute("ClaveProdServ"));
                    row.createCell(1).setCellValue(concepto.getAttribute("NoIdentificacion"));
                    row.createCell(2).setCellValue(concepto.getAttribute("Cantidad"));
                    row.createCell(3).setCellValue(concepto.getAttribute("ClaveUnidad"));
                    row.createCell(4).setCellValue(concepto.getAttribute("Unidad"));
                    row.createCell(5).setCellValue(concepto.getAttribute("Descripcion"));
                    row.createCell(6).setCellValue(concepto.getAttribute("ValorUnitario"));
                    row.createCell(7).setCellValue(concepto.getAttribute("Importe"));
                    row.createCell(8).setCellValue(concepto.getAttribute("ObjetoImp"));

                    NodeList impuestos = concepto.getElementsByTagName("cfdi:Traslado");
                    if (impuestos.getLength() > 0) {
                        Element impuesto = (Element) impuestos.item(0);
                        row.createCell(9).setCellValue(impuesto.getAttribute("Importe"));
                    }
                }
            }
            
            File excelFile = new File(xmlFile.getParent(), "Factura_Procesada.xlsx");
            FileOutputStream fileOut = new FileOutputStream(excelFile);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();

            System.out.println("Archivo Excel creado: " + excelFile.getAbsolutePath());

        } catch (Exception e) {
            System.out.println("Error al procesar el XML: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
}
