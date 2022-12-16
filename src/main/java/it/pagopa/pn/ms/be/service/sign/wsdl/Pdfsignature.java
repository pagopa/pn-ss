
package it.pagopa.pn.ms.be.service.sign.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per pdfsignature complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="pdfsignature"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="SignRequest" type="{http://arubasignservice.arubapec.it/}signRequest" minOccurs="0"/&gt;
 *         &lt;element name="Apparence" type="{http://arubasignservice.arubapec.it/}pdfSignApparence" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "pdfsignature", propOrder = {
    "signRequest",
    "apparence"
})
public class Pdfsignature {

    @XmlElement(name = "SignRequest")
    protected SignRequest signRequest;
    @XmlElement(name = "Apparence")
    protected PdfSignApparence apparence;

    /**
     * Recupera il valore della proprietà signRequest.
     * 
     * @return
     *     possible object is
     *     {@link SignRequest }
     *     
     */
    public SignRequest getSignRequest() {
        return signRequest;
    }

    /**
     * Imposta il valore della proprietà signRequest.
     * 
     * @param value
     *     allowed object is
     *     {@link SignRequest }
     *     
     */
    public void setSignRequest(SignRequest value) {
        this.signRequest = value;
    }

    /**
     * Recupera il valore della proprietà apparence.
     * 
     * @return
     *     possible object is
     *     {@link PdfSignApparence }
     *     
     */
    public PdfSignApparence getApparence() {
        return apparence;
    }

    /**
     * Imposta il valore della proprietà apparence.
     * 
     * @param value
     *     allowed object is
     *     {@link PdfSignApparence }
     *     
     */
    public void setApparence(PdfSignApparence value) {
        this.apparence = value;
    }

}
