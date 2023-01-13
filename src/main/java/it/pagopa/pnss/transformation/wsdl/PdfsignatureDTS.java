
package it.pagopa.pnss.transformation.wsdl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java per pdfsignatureDTS complex type.
 * 
 * <p>Il seguente frammento di schema specifica il contenuto previsto contenuto in questa classe.
 * 
 * <pre>
 * &lt;complexType name="pdfsignatureDTS"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="MarkRequest" type="{http://arubasignservice.arubapec.it/}MarkRequest" minOccurs="0"/&gt;
 *         &lt;element name="password" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "pdfsignatureDTS", propOrder = {
    "markRequest",
    "password"
})
public class PdfsignatureDTS {

    @XmlElement(name = "MarkRequest")
    protected MarkRequest markRequest;
    protected String password;

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

    /**
     * Recupera il valore della proprietà password.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPassword() {
        return password;
    }

    /**
     * Imposta il valore della proprietà password.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPassword(String value) {
        this.password = value;
    }

}
