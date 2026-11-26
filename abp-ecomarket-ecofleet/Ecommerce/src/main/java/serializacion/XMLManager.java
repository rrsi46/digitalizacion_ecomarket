package serializacion;

import modelo.Producto;
import ficheros.ProductoRAFDAO;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

public class XMLManager {
    private ProductoRAFDAO dao;

    public XMLManager(ProductoRAFDAO dao) {
        this.dao = dao;
    }

    // EXPORTACIÓN A XML USANDO DOM
    public void exportarXML(File archivoDestino, int maxId) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            Element elementoRaiz = doc.createElement("productos");
            doc.appendChild(elementoRaiz);

            int exportados = 0;

            for (int i = 1; i <= maxId; i++) {
                Producto p = dao.buscarPorId(i);
                if (p != null) {
                    Element elemProducto = doc.createElement("producto");
                    elemProducto.setAttribute("id", String.valueOf(p.getId()));

                    Element elemNombre = doc.createElement("nombre");
                    elemNombre.setTextContent(p.getNombre());
                    elemProducto.appendChild(elemNombre);

                    Element elemPrecio = doc.createElement("precio");
                    elemPrecio.setTextContent(String.valueOf(p.getPrecio()));
                    elemProducto.appendChild(elemPrecio);

                    Element elemStock = doc.createElement("stock");
                    elemStock.setTextContent(String.valueOf(p.getStock()));
                    elemProducto.appendChild(elemStock);

                    Element elemActivo = doc.createElement("activo");
                    elemActivo.setTextContent(String.valueOf(p.isActivo()));
                    elemProducto.appendChild(elemActivo);

                    elementoRaiz.appendChild(elemProducto);
                    exportados++;
                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(archivoDestino);
            transformer.transform(source, result);

            System.out.println("[INFO] Exportacion a XML completada (DOM). Productos exportados: " + exportados);
        } catch (Exception e) {
            System.err.println("[ERROR] Al exportar a XML: " + e.getMessage());
        }
    }

    // IMPORTACIÓN MASIVA USANDO SAX
    public void importarXMLMasivoSAX(File archivoXML) {
        try {
            if (!archivoXML.exists()) {
                System.out.println("[AVISO] El fichero XML no existe.");
                return;
            }
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            ManejadorSAX handler = new ManejadorSAX();
            
            saxParser.parse(archivoXML, handler);
            System.out.println("[INFO] Importacion masiva XML (SAX) finalizada.");
        } catch (Exception e) {
            System.err.println("[ERROR] Al importar XML con SAX: " + e.getMessage());
        }
    }

    // EVENTOS SAX (Patrón Observador)
    private class ManejadorSAX extends DefaultHandler {
        private Producto productoActual;
        private StringBuilder valorActual = new StringBuilder();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            valorActual.setLength(0);
            if (qName.equalsIgnoreCase("producto") || qName.equalsIgnoreCase("item")) {
                productoActual = new Producto();
                String idAttr = attributes.getValue("id");
                if (idAttr != null) {
                    productoActual.setId(Integer.parseInt(idAttr));
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            valorActual.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (productoActual != null) {
                if (qName.equalsIgnoreCase("nombre")) {
                    productoActual.setNombre(valorActual.toString().trim());
                } else if (qName.equalsIgnoreCase("precio")) {
                    productoActual.setPrecio(Double.parseDouble(valorActual.toString().trim()));
                } else if (qName.equalsIgnoreCase("stock")) {
                    productoActual.setStock(Integer.parseInt(valorActual.toString().trim()));
                } else if (qName.equalsIgnoreCase("activo")) {
                    productoActual.setActivo(Boolean.parseBoolean(valorActual.toString().trim()));
                } else if (qName.equalsIgnoreCase("producto") || qName.equalsIgnoreCase("item")) {
                    // Guarda directo en fichero DAT mediante ProductoRAFDAO
                    dao.insertar(productoActual);
                }
            }
        }
    }
}