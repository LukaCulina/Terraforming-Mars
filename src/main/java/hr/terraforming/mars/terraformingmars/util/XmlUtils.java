package hr.terraforming.mars.terraformingmars.util;

import hr.terraforming.mars.terraformingmars.enums.ActionType;
import hr.terraforming.mars.terraformingmars.enums.TileType;
import hr.terraforming.mars.terraformingmars.exception.FxmlLoadException;
import hr.terraforming.mars.terraformingmars.model.GameMove;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class XmlUtils {

    private XmlUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static final String GAME_MOVES_XML_FILE = "xml/gameMoves.xml";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String TILE_TYPE = "TileType";

    @SuppressWarnings("HttpUrlsUsage")
    private static DocumentBuilderFactory createSecureDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        return dbf;
    }

    private static Document loadOrCreateDocument(DocumentBuilder db, File xmlFile) throws IOException {
        if (xmlFile.exists() && xmlFile.length() > 0) {
            try {
                return db.parse(xmlFile);
            } catch (SAXException _) {
                // File is corrupted, create a new document with a root element.
            }
        }
        Document doc = db.newDocument();
        Element root = doc.createElement("GameMoves");
        doc.appendChild(root);
        return doc;
    }

    public static synchronized void appendGameMove(GameMove move) {
        File xmlFile = new File(GAME_MOVES_XML_FILE);

        try {
            DocumentBuilderFactory dbf = createSecureDocumentBuilderFactory();
            DocumentBuilder db = dbf.newDocumentBuilder();

            Document doc = loadOrCreateDocument(db, xmlFile);

            Element rootElement = doc.getDocumentElement();
            Element newMoveElement = createGameMoveElement(doc, move);
            rootElement.appendChild(newMoveElement);

            writeDocument(doc);

        } catch (ParserConfigurationException | IOException | TransformerException e) {
            throw new FxmlLoadException("Error appending game move to XML", e);
        }
    }

    public static List<GameMove> readGameMoves() {
        List<GameMove> moves = new ArrayList<>();
        File xmlFile = new File(GAME_MOVES_XML_FILE);
        if (!xmlFile.exists()) {
            return moves;
        }

        try {
            DocumentBuilderFactory dbf = createSecureDocumentBuilderFactory();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("GameMove");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    GameMove move = new GameMove();
                    move.setPlayerName(element.getElementsByTagName("PlayerName").item(0).getTextContent());
                    move.setActionType(ActionType.valueOf(element.getElementsByTagName("ActionType").item(0).getTextContent()));
                    move.setDetails(element.getElementsByTagName("Details").item(0).getTextContent());
                    move.setTimestamp(LocalDateTime.parse(element.getElementsByTagName("Timestamp").item(0).getTextContent(), FORMATTER));

                    if (element.getElementsByTagName("Row").getLength() > 0) {
                        move.setRow(Integer.parseInt(element.getElementsByTagName("Row").item(0).getTextContent()));
                    }
                    if (element.getElementsByTagName("Col").getLength() > 0) {
                        move.setCol(Integer.parseInt(element.getElementsByTagName("Col").item(0).getTextContent()));
                    }
                    if (element.getElementsByTagName(TILE_TYPE).getLength() > 0) {
                        move.setTileType(TileType.valueOf(element.getElementsByTagName(TILE_TYPE).item(0).getTextContent()));
                    }
                    moves.add(move);
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new FxmlLoadException("Error reading game moves from XML", e);
        }
        return moves;
    }

    public static void clearGameMoves() {
        Path path = Paths.get(GAME_MOVES_XML_FILE);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new FxmlLoadException("Failed to delete game moves XML file", e);
        }
    }

    private static void writeDocument(Document doc) throws TransformerException, IOException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);

        try (java.io.FileWriter writer = new java.io.FileWriter(XmlUtils.GAME_MOVES_XML_FILE)) {
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);
        }
    }

    private static Element createGameMoveElement(Document doc, GameMove move) {
        Element gameMoveElement = doc.createElement("GameMove");

        gameMoveElement.appendChild(createElement(doc, "PlayerName", move.getPlayerName()));
        gameMoveElement.appendChild(createElement(doc, "ActionType", move.getActionType().name()));
        gameMoveElement.appendChild(createElement(doc, "Details", move.getDetails()));
        gameMoveElement.appendChild(createElement(doc, "Timestamp", move.getTimestamp().format(FORMATTER)));

        if (move.getRow() != null) {
            gameMoveElement.appendChild(createElement(doc, "Row", move.getRow().toString()));
        }
        if (move.getCol() != null) {
            gameMoveElement.appendChild(createElement(doc, "Col", move.getCol().toString()));
        }
        if (move.getTileType() != null) {
            gameMoveElement.appendChild(createElement(doc, TILE_TYPE, move.getTileType().name()));
        }

        return gameMoveElement;
    }

    private static Element createElement(Document doc, String name, String value) {
        Element el = doc.createElement(name);
        el.appendChild(doc.createTextNode(value));
        return el;
    }
}
