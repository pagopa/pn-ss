
package it.pagopa.pnss.transformation.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per pdfSignApparence complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="pdfSignApparence"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="image" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="imageBin" type="{http://www.w3.org/2001/XMLSchema}base64Binary" minOccurs="0"/&gt;
 *         &lt;element name="imageOnly" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *         &lt;element name="leftx" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="lefty" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="location" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="page" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="reason" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="rightx" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="righty" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="testo" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="bScaleFont" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *         &lt;element name="bShowDateTime" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *         &lt;element name="resizeMode" type="{http://www.w3.org/2001/XMLSchema}int"/&gt;
 *         &lt;element name="preservePDFA" type="{http://www.w3.org/2001/XMLSchema}boolean"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "pdfSignApparence", propOrder = {
    "image",
    "imageBin",
    "imageOnly",
    "leftx",
    "lefty",
    "location",
    "page",
    "reason",
    "rightx",
    "righty",
    "testo",
    "bScaleFont",
    "bShowDateTime",
    "resizeMode",
    "preservePDFA"
})
public class PdfSignApparence {

    protected String image;
    protected byte[] imageBin;
    protected boolean imageOnly;
    protected int leftx;
    protected int lefty;
    protected String location;
    protected int page;
    protected String reason;
    protected int rightx;
    protected int righty;
    protected String testo;
    protected boolean bScaleFont;
    protected boolean bShowDateTime;
    protected int resizeMode;
    protected boolean preservePDFA;

    /**
     * Recupera il valore della proprietà image.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getImage() {
        return image;
    }

    /**
     * Imposta il valore della proprietà image.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setImage(String value) {
        this.image = value;
    }

    /**
     * Recupera il valore della proprietà imageBin.
     * 
     * @return
     *     possible object is
     *     byte[]
     */
    public byte[] getImageBin() {
        return imageBin;
    }

    /**
     * Imposta il valore della proprietà imageBin.
     * 
     * @param value
     *     allowed object is
     *     byte[]
     */
    public void setImageBin(byte[] value) {
        this.imageBin = value;
    }

    /**
     * Recupera il valore della proprietà imageOnly.
     * 
     */
    public boolean isImageOnly() {
        return imageOnly;
    }

    /**
     * Imposta il valore della proprietà imageOnly.
     * 
     */
    public void setImageOnly(boolean value) {
        this.imageOnly = value;
    }

    /**
     * Recupera il valore della proprietà leftx.
     * 
     */
    public int getLeftx() {
        return leftx;
    }

    /**
     * Imposta il valore della proprietà leftx.
     * 
     */
    public void setLeftx(int value) {
        this.leftx = value;
    }

    /**
     * Recupera il valore della proprietà lefty.
     * 
     */
    public int getLefty() {
        return lefty;
    }

    /**
     * Imposta il valore della proprietà lefty.
     * 
     */
    public void setLefty(int value) {
        this.lefty = value;
    }

    /**
     * Recupera il valore della proprietà location.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLocation() {
        return location;
    }

    /**
     * Imposta il valore della proprietà location.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLocation(String value) {
        this.location = value;
    }

    /**
     * Recupera il valore della proprietà page.
     * 
     */
    public int getPage() {
        return page;
    }

    /**
     * Imposta il valore della proprietà page.
     * 
     */
    public void setPage(int value) {
        this.page = value;
    }

    /**
     * Recupera il valore della proprietà reason.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getReason() {
        return reason;
    }

    /**
     * Imposta il valore della proprietà reason.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setReason(String value) {
        this.reason = value;
    }

    /**
     * Recupera il valore della proprietà rightx.
     * 
     */
    public int getRightx() {
        return rightx;
    }

    /**
     * Imposta il valore della proprietà rightx.
     * 
     */
    public void setRightx(int value) {
        this.rightx = value;
    }

    /**
     * Recupera il valore della proprietà righty.
     * 
     */
    public int getRighty() {
        return righty;
    }

    /**
     * Imposta il valore della proprietà righty.
     * 
     */
    public void setRighty(int value) {
        this.righty = value;
    }

    /**
     * Recupera il valore della proprietà testo.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTesto() {
        return testo;
    }

    /**
     * Imposta il valore della proprietà testo.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTesto(String value) {
        this.testo = value;
    }

    /**
     * Recupera il valore della proprietà bScaleFont.
     * 
     */
    public boolean isBScaleFont() {
        return bScaleFont;
    }

    /**
     * Imposta il valore della proprietà bScaleFont.
     * 
     */
    public void setBScaleFont(boolean value) {
        this.bScaleFont = value;
    }

    /**
     * Recupera il valore della proprietà bShowDateTime.
     * 
     */
    public boolean isBShowDateTime() {
        return bShowDateTime;
    }

    /**
     * Imposta il valore della proprietà bShowDateTime.
     * 
     */
    public void setBShowDateTime(boolean value) {
        this.bShowDateTime = value;
    }

    /**
     * Recupera il valore della proprietà resizeMode.
     * 
     */
    public int getResizeMode() {
        return resizeMode;
    }

    /**
     * Imposta il valore della proprietà resizeMode.
     * 
     */
    public void setResizeMode(int value) {
        this.resizeMode = value;
    }

    /**
     * Recupera il valore della proprietà preservePDFA.
     * 
     */
    public boolean isPreservePDFA() {
        return preservePDFA;
    }

    /**
     * Imposta il valore della proprietà preservePDFA.
     * 
     */
    public void setPreservePDFA(boolean value) {
        this.preservePDFA = value;
    }

}
