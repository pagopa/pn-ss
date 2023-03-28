package it.pagopa.pnss.transformation.service;

import com.sun.xml.ws.encoding.xml.XMLMessage;
import com.sun.xml.ws.wsdl.parser.InaccessibleWSDLException;
import it.pagopa.pnss.transformation.model.GenericFileSignRequestV2;
import it.pagopa.pnss.transformation.model.GenericFileSignReturnV2;
import it.pagopa.pnss.transformation.model.InputPdfFileSignRequestV2;
import it.pagopa.pnss.transformation.model.PdfFileSignReturnV2;
import it.pagopa.pnss.transformation.model.pojo.ArubaSecretValue;
import it.pagopa.pnss.transformation.model.pojo.IdentitySecretTimemark;
import it.pagopa.pnss.transformation.wsdl.*;
import jakarta.activation.DataHandler;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.ws.AsyncHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;
import java.io.InputStream;
import java.net.MalformedURLException;

@Service
@Slf4j
public class SignServiceSoap {

    private final ArubaSignService arubaSignService;
    private final IdentitySecretTimemark identitySecretTimemark;
    private final ArubaSecretValue arubaSecretValue;

    @Value("${aruba.cert_id}")
    private String certId;

    @Value("${PnSsTimemarkUrl:#{null}}")
    private String timeMarkUrl;

    @Value("${PnSsTsaIdentity:#{true}}")
    private boolean tsaIdentity;

    public SignServiceSoap(ArubaSignService arubaSignService, IdentitySecretTimemark identitySecretTimemark,
                           ArubaSecretValue arubaSecretValue) {
        this.arubaSignService = arubaSignService;
        this.identitySecretTimemark = identitySecretTimemark;
        this.arubaSecretValue = arubaSecretValue;
    }

    private <T> AsyncHandler<T> into(MonoSink<T> sink) {
        return res -> {
            try {
                sink.success(res.get());
            } catch (Exception throwable) {
                endSoapRequest(sink, throwable);
            }
        };
    }

    private <T> void endSoapRequest(MonoSink<T> sink, Throwable throwable) {
        log.error(throwable.getMessage());
        sink.error(throwable);
        Thread.currentThread().interrupt();
    }

    private Auth createIdentity() {
        var auth = new Auth();
        auth.setDelegatedDomain(arubaSecretValue.getDelegatedDomain());
        auth.setDelegatedPassword(arubaSecretValue.getDelegatedPassword());
        auth.setDelegatedUser(arubaSecretValue.getDelegatedUser());
        auth.setOtpPwd(arubaSecretValue.getOtpPwd());
        auth.setTypeOtpAuth(arubaSecretValue.getTypeOtpAuth());
        auth.setUser(arubaSecretValue.getUser());
        return auth;
    }

    public Mono<PdfsignatureV2Response> signPdfDocument(byte[] pdfFile, Boolean marcatura) {
        return Mono.fromCallable(() -> {
            var signRequestV2 = new SignRequestV2();
            signRequestV2.setCertID(certId);
            signRequestV2.setIdentity(createIdentity());
            signRequestV2.setRequiredmark(marcatura);
            signRequestV2.setBinaryinput(pdfFile);
            signRequestV2.setTransport(TypeTransport.BYNARYNET);

            if (Boolean.TRUE.equals(marcatura) && (tsaIdentity)) {
                var tsaAuth = new TsaAuth();
                tsaAuth.setUser(identitySecretTimemark.getUserTimemark());
                tsaAuth.setPassword(identitySecretTimemark.getPasswordTimemark());
                tsaAuth.setTsaurl(timeMarkUrl);
                signRequestV2.setTsaIdentity(tsaAuth);
            }

            return signRequestV2;
        }).map(signRequestV2 -> {
            var pdfsignatureV2 = new PdfsignatureV2();
            pdfsignatureV2.setSignRequestV2(signRequestV2);
            return pdfsignatureV2;
        }).flatMap(pdfsignatureV2 -> Mono.create(sink -> arubaSignService.pdfsignatureV2Async(pdfsignatureV2, into(sink))));
    }

    public Mono<SignReturnV2> pkcs7signV2(byte[] buf, Boolean marcatura) {
        var signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID(certId);
        signRequestV2.setIdentity(createIdentity());
        var source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(new DataHandler(buf, "application/octet-stream"));
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(marcatura);

        if (Boolean.TRUE.equals(marcatura) && (tsaIdentity)) {
            var tsaAuth = new TsaAuth();
            tsaAuth.setUser(identitySecretTimemark.getUserTimemark());
            tsaAuth.setPassword(identitySecretTimemark.getPasswordTimemark());
            tsaAuth.setTsaurl(timeMarkUrl);
            signRequestV2.setTsaIdentity(tsaAuth);
        }

        SignReturnV2 signReturnV2 = service.pkcs7SignV2(signRequestV2, false, true);
        return signReturnV2;
    }

    public SignReturnV2 xmlsignature(String contentType, InputStream xml, Boolean marcatura)
            throws TypeOfTransportNotImplemented_Exception, JAXBException, MalformedURLException {
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID(certId);
        signRequestV2.setIdentity(createIdentity(null));
        DataSource dataSourceXml = XMLMessage.createDataSource(contentType, xml);
        signRequestV2.setStream(new DataHandler(dataSourceXml));
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(marcatura);


        logCallAruba(signRequestV2);


        ArubaSignService service = createArubaService(arubaUrlWsdl).getArubaSignServicePort();
        XmlSignatureParameter parameter = new XmlSignatureParameter();
        parameter.setType(XmlSignatureType.XMLENVELOPED);
        SignReturnV2 signReturnV2 = service.xmlsignature(signRequestV2, parameter);
        signReturnV2.getStream().getContentType();
        return signReturnV2;
    }


    public PdfFileSignReturnV2 callArubaSignPdfFile(InputPdfFileSignRequestV2 input)
            throws JAXBException, TypeOfTransportNotImplemented_Exception {
        logCallAruba(input.getInfoTosigned());

        PdfFileSignReturnV2 response = new PdfFileSignReturnV2();

        try {
            arubaSignService = createArubaService(input.getUrl());
            ArubaSignService service = arubaSignService.getArubaSignServicePort();
            SignReturnV2 signReturnV2 = service.pdfsignatureV2(input.getInfoTosigned(), null, null, null, null, null);
            response.setPdfInfoResultSign(signReturnV2);
            if (!signReturnV2.getStatus().equals(ARUBA_RESP_OK)) {
                response.setCode(signReturnV2.getReturnCode());
                response.setDescription(signReturnV2.getDescription());
            }
        } catch (MalformedURLException e) {
            response.setCode(e.getCause().getMessage());
            response.setDescription(e.getMessage());
        } catch (InaccessibleWSDLException iwe) {
            iwe.getMessage();
            response.setCode("500");
            response.setDescription(iwe.getErrors().get(0).getMessage());
        } catch (Exception ex) {
            response.setCode(ex.getCause().getMessage());
            response.setDescription(ex.getMessage());
        }


        return response;
    }

    public GenericFileSignReturnV2 callGenericFile(GenericFileSignRequestV2 input)
            throws JAXBException, TypeOfTransportNotImplemented_Exception {
        logCallAruba(input.getInfoTosigned());
        GenericFileSignReturnV2 response = new GenericFileSignReturnV2();

        try {
            arubaSignService = createArubaService(input.getUrl());
            ArubaSignService service = arubaSignService.getArubaSignServicePort();
            SignReturnV2 signReturnV2 = service.pkcs7SignV2(input.getInfoTosigned(), false, true);
            response.setPdfInfoResultSign(signReturnV2);
            if (!signReturnV2.getStatus().equals(ARUBA_RESP_OK)) {
                response.setCode(signReturnV2.getReturnCode());
                response.setDescription(signReturnV2.getDescription());
            }
        } catch (MalformedURLException e) {
            response.setCode(e.getCause().getMessage());
            response.setDescription(e.getMessage());
        } catch (InaccessibleWSDLException iwe) {
            iwe.getMessage();
            response.setCode("500");
            response.setDescription(iwe.getErrors().get(0).getMessage());
        } catch (Exception ex) {
            response.setCode(ex.getCause().getMessage());
            response.setDescription(ex.getMessage());
        }
        return response;

    }


}
