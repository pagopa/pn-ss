
package it.pagopa.pnss.transformation.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per tsr complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="tsr"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="MarkRequest" type="{http://arubasignservice.arubapec.it/}MarkRequest"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tsr", propOrder = {
    "markRequest"
})
public class Tsr {

    @XmlElement(name = "MarkRequest", required = true)
    protected MarkRequest markRequest;

    /**
     * Recupera il valore della proprietà markRequest.
     * 
     * @return
     *     possible object is
     *     {@link MarkRequest }
     *     
     */
    public MarkRequest getMarkRequest() {
        return markRequest;
    }

    /**
     * Imposta il valore della proprietà markRequest.
     * 
     * @param value
     *     allowed object is
     *     {@link MarkRequest }
     *     
     */
    public void setMarkRequest(MarkRequest value) {
        this.markRequest = value;
    }

}
